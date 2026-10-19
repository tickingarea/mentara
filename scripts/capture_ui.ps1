# Mentara UI 截图脚本（项目内唯一截图脚本，替代旧 capture_live / capture_splash）
#
# 用法：
#   powershell -ExecutionPolicy Bypass -File scripts\capture_ui.ps1
#   powershell -ExecutionPolicy Bypass -File scripts\capture_ui.ps1 -Attach        # 捕获已运行的实例
#   powershell -ExecutionPolicy Bypass -File scripts\capture_ui.ps1 -KeepRunning   # 截完不关闭应用
#
# 产物（项目根目录）：
#   ui_mentara_splash.png / ui_mentara_main.png / ui_mentara_content.png
#   ui_mentara_graph.png  / ui_mentara_qa.png

param(
    [string]$ProjectRoot = "C:\Users\Administrator\Documents\Default Project\learnagent",
    [switch]$Attach,
    [switch]$KeepRunning
)

$ErrorActionPreference = "Continue"
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms

Add-Type -Namespace WinMentara -Name Native -MemberDefinition @'
[DllImport("user32.dll")] public static extern bool GetWindowRect(System.IntPtr hWnd, out RECT lpRect);
[DllImport("user32.dll")] public static extern bool SetCursorPos(int X, int Y);
[DllImport("user32.dll")] public static extern void mouse_event(uint dwFlags, uint dx, uint dy, uint dwData, System.UIntPtr dwExtraInfo);
[DllImport("user32.dll")] public static extern bool SetForegroundWindow(System.IntPtr hWnd);
[DllImport("user32.dll")] public static extern bool ShowWindow(System.IntPtr hWnd, int nCmdShow);
[DllImport("user32.dll")] public static extern bool BringWindowToTop(System.IntPtr hWnd);
[DllImport("user32.dll")] public static extern System.IntPtr GetForegroundWindow();
public struct RECT { public int Left; public int Top; public int Right; public int Bottom; }
'@

function Get-ReadyWindow {
    for ($i = 0; $i -lt 15; $i++) {
        $p = Get-MentaraProcess
        if ($p -eq $null) { Start-Sleep -Seconds 1; continue }
        $h = $p.MainWindowHandle
        [void][WinMentara.Native]::ShowWindow($h, 6)   # minimize
        Start-Sleep -Milliseconds 250
        [void][WinMentara.Native]::ShowWindow($h, 9)   # restore
        [void][WinMentara.Native]::BringWindowToTop($h)
        [void][WinMentara.Native]::SetForegroundWindow($h)
        Start-Sleep -Milliseconds 900
        $r = New-Object WinMentara.Native+RECT
        [void][WinMentara.Native]::GetWindowRect($h, [ref]$r)
        $w = $r.Right - $r.Left
        $hh = $r.Bottom - $r.Top
        $fg = [WinMentara.Native]::GetForegroundWindow()
        if ($w -ge 800 -and $hh -ge 500 -and $fg -eq $h) {
            return @{ H = $h; L = $r.Left; T = $r.Top; R = $r.Right; B = $r.Bottom }
        }
    }
    return $null
}

function Get-MentaraProcess {
    Get-Process | Where-Object { $_.MainWindowTitle -eq "Mentara" } | Select-Object -First 1
}

function Test-Blank([System.Drawing.Bitmap]$bmp) {
    $first = $bmp.GetPixel(12, 12)
    for ($x = 12; $x -lt $bmp.Width; $x += 64) {
        for ($y = 12; $y -lt $bmp.Height; $y += 64) {
            $c = $bmp.GetPixel($x, $y)
            if ([math]::Abs($c.R - $first.R) + [math]::Abs($c.G - $first.G) + [math]::Abs($c.B - $first.B) -gt 14) { return $false }
        }
    }
    return $true
}

function Wake-Screen {
    try {
        $pos = [System.Windows.Forms.Cursor]::Position
        [System.Windows.Forms.Cursor]::Position = New-Object System.Drawing.Point(($pos.X + 1), $pos.Y)
        Start-Sleep -Milliseconds 50
        [System.Windows.Forms.Cursor]::Position = $pos
    } catch { }
}

function Capture-Window([IntPtr]$handle, [string]$path) {
    for ($attempt = 0; $attempt -lt 6; $attempt++) {
        Wake-Screen
        $r = New-Object WinMentara.Native+RECT
        [void][WinMentara.Native]::GetWindowRect($handle, [ref]$r)
        $w = $r.Right - $r.Left
        $h = $r.Bottom - $r.Top
        $bmp = New-Object System.Drawing.Bitmap($w, $h)
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.CopyFromScreen($r.Left, $r.Top, 0, 0, (New-Object System.Drawing.Size($w, $h)))
        $g.Dispose()
        if (-not (Test-Blank $bmp)) {
            $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
            $bmp.Dispose()
            return
        }
        $bmp.Dispose()
        Start-Sleep -Milliseconds 700
    }
    Write-Output ("CAPTURE_BLANK: " + $path)
}

function Click-At([int]$x, [int]$y) {
    [void][WinMentara.Native]::SetCursorPos($x, $y)
    Start-Sleep -Milliseconds 150
    [WinMentara.Native]::mouse_event(0x0002, 0, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 70
    [WinMentara.Native]::mouse_event(0x0004, 0, 0, 0, [UIntPtr]::Zero)
}

# 临时备份并清空 last_selected（让应用从首页开始，截完自动还原）
$userHome = [Environment]::GetFolderPath('UserProfile')
if (-not $userHome) { $userHome = $HOME }
if (-not $userHome) { $userHome = "$env:SystemDrive\Users\Administrator" }
$lsPath = Join-Path $userHome ".mentara\last_selected.txt"
Write-Output ("HOMECHECK: " + $lsPath + " exists=" + (Test-Path -LiteralPath $lsPath))
$lsBackup = $null
if (Test-Path -LiteralPath $lsPath) {
    $lsBackup = Get-Content -LiteralPath $lsPath -Raw
    Remove-Item -LiteralPath $lsPath -Force
}

if (-not $Attach) {
    Get-Process | Where-Object { $_.MainWindowTitle -eq "Mentara" } | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
    $gradle = Join-Path $ProjectRoot "gradle-dist\gradle-8.10\bin\gradle.bat"
    $log = Join-Path $env:TEMP "mentara_capture_run.log"
    Start-Process -FilePath $gradle -ArgumentList ":desktopApp:run --offline --no-daemon" -WorkingDirectory $ProjectRoot -WindowStyle Hidden -RedirectStandardOutput $log -RedirectStandardError ($log + ".err") | Out-Null
    $proc = $null
    for ($i = 0; $i -lt 120; $i++) {
        Start-Sleep -Seconds 1
        $proc = Get-MentaraProcess
        if ($proc -ne $null) { break }
    }
} else {
    $proc = Get-MentaraProcess
}

if ($proc -eq $null) { Write-Output "WINDOW_NOT_FOUND"; exit 1 }

$win = Get-ReadyWindow
if ($win -eq $null) { Write-Output "WINDOW_NOT_READY"; exit 1 }
$proc = Get-MentaraProcess
$L = $win.L; $T = $win.T; $R = $win.R
Write-Output "WINDOW: $L,$T $($R-$L)x$($win.B-$T)"

# ---- Splash 密集连拍（Compose 起动晚于窗口出现，需覆盖 0-14s）----
if (-not $Attach) {
    $burst = Join-Path $env:TEMP "mentara_burst"
    New-Item -ItemType Directory -Force $burst | Out-Null
    Get-ChildItem $burst -File | Remove-Item -Force
    for ($i = 0; $i -lt 22; $i++) {
        Capture-Window $proc.MainWindowHandle (Join-Path $burst ("splash_" + $i.ToString("00") + ".png"))
        Start-Sleep -Milliseconds 600
    }
    Start-Sleep -Seconds 2
}

# ---- 回到首页（App 启动即自动选中第一个视频 → 点 ← 返回 HomeGrid）----
$modeY = $T + 151
Click-At ($L + 52) $modeY
Start-Sleep -Milliseconds 1500

# ---- 主界面 ----
Capture-Window $proc.MainWindowHandle (Join-Path $ProjectRoot "ui_mentara_main.png")
Write-Output "MAIN"

# ---- 选中视频卡片（已在专注页时此点击无害）----
Click-At ($L + 160) ($T + 311)
Start-Sleep -Milliseconds 1500
Capture-Window $proc.MainWindowHandle (Join-Path $ProjectRoot "ui_mentara_content.png")
Write-Output "CONTENT"

# ---- 图谱页 / 辩论页 / 问答页（专注页右上四个模式按钮：时间线 R-200 · 图谱 R-148 · 问答 R-96 · 辩论 R-44）----
$modeY = $T + 151
Click-At ($R - 148) $modeY
Start-Sleep -Milliseconds 1800
Capture-Window $proc.MainWindowHandle (Join-Path $ProjectRoot "ui_mentara_graph.png")
Write-Output "GRAPH"

Click-At ($R - 44) $modeY
Start-Sleep -Milliseconds 1800
Capture-Window $proc.MainWindowHandle (Join-Path $ProjectRoot "ui_mentara_debate.png")
Write-Output "DEBATE"

Click-At ($R - 96) $modeY
Start-Sleep -Milliseconds 1800
Capture-Window $proc.MainWindowHandle (Join-Path $ProjectRoot "ui_mentara_qa.png")
Write-Output "QA"

if (-not $KeepRunning) {
    Get-Process | Where-Object { $_.MainWindowTitle -eq "Mentara" } | Stop-Process -Force -ErrorAction SilentlyContinue
}

# 还原 last_selected
if ($lsBackup -ne $null) {
    Set-Content -LiteralPath $lsPath -Value $lsBackup -NoNewline
}

Write-Output "DONE"
