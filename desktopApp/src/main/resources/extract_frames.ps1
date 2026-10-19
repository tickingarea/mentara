param(
    [Parameter(Mandatory = $true)][string]$Video,
    [Parameter(Mandatory = $true)][string]$OutDir,
    [int]$Width = 1280,
    [int]$Count = 12,
    [int]$Fps = 4,
    [string]$Mode = "scene",
    [int]$DenseWidth = 640,
    [string]$CsPath = ""
)

# 视频抽帧（多帧优先）：
#   1) Media Foundation 软件解码（自研 C#，零外部依赖）→ 全片均匀抽 Count 帧
#   2) ffmpeg（PATH 或 <项目>/tools/ffmpeg.exe）
#   3) 回退：StorageFile.GetThumbnailAsync → 1 张代表帧
# 输出 frames.json：[{ timestamp, imagePath }, ...]

Add-Type -AssemblyName System.Runtime.WindowsRuntime | Out-Null
if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Force -Path $OutDir | Out-Null }
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
$framesJson = Join-Path $OutDir "frames.json"

# ---------- 1) Media Foundation 多帧（首选） ----------
if ([string]::IsNullOrWhiteSpace($CsPath)) {
    $cand = Join-Path $PSScriptRoot "mfframegrab.cs"
    if (Test-Path $cand) { $CsPath = $cand }
}
if ((Test-Path $CsPath) -and (Test-Path $Video)) {
    try {
        Add-Type -Path $CsPath -ReferencedAssemblies System.Drawing -ErrorAction Stop
        $n = if ($Mode -eq "dense") { [MfFrameGrab]::GrabScenes($Video, $OutDir, $Fps, -1, $DenseWidth) }
                  elseif ($Fps -gt 0) { [MfFrameGrab]::GrabScenes($Video, $OutDir, $Fps, 8) }
                  else { [MfFrameGrab]::Grab($Video, $OutDir, $Count, $Fps) }
        if ((Test-Path $framesJson) -and $n -gt 0) {
            Write-Output "DECODER=media-foundation"
            Write-Output ("FRAMES=" + $n)
            exit 0
        }
    } catch {
        Write-Output ("MF_ERROR: " + $_.Exception.Message)
    }
}

# ---------- 2) ffmpeg 多帧 ----------
$ff = (Get-Command ffmpeg -ErrorAction SilentlyContinue).Source
if (-not $ff) {
    $cand = Join-Path (Split-Path -Parent $PSScriptRoot) "tools\ffmpeg.exe"
    if (Test-Path $cand) { $ff = $cand }
}
if ($ff -and (Test-Path $Video)) {
    $items = New-Object System.Collections.ArrayList
    $dur = 0.0
    $info = & $ff -hide_banner -i $Video 2>&1 | Out-String
    $m = [regex]::Match($info, "Duration:\s*(\d+):(\d+):([\d.]+)")
    if ($m.Success) {
        $dur = [int]$m.Groups[1].Value * 3600 + [int]$m.Groups[2].Value * 60 +
            [double]::Parse($m.Groups[3].Value, [Globalization.CultureInfo]::InvariantCulture)
    }
    if ($dur -le 0) { $dur = 60.0 }
    $n = [Math]::Max(2, $Count)
    for ($i = 1; $i -le $n; $i++) {
        $sec = [Math]::Round($dur * $i / ($n + 1), 3)
        $path = Join-Path $OutDir ("frame_{0:D3}.jpg" -f $i)
        & $ff -hide_banner -loglevel error -ss $sec -i $Video -frames:v 1 -q:v 2 -y $path 2>$null
        if (Test-Path $path) { $null = $items.Add([pscustomobject]@{ timestamp = $sec; imagePath = $path }) }
    }
    $json = if ($items.Count -eq 0) { "[]" } else { $items | ConvertTo-Json -Depth 4 }
    if (-not $json.TrimStart().StartsWith("[")) { $json = "[$json]" }
    [System.IO.File]::WriteAllText($framesJson, $json, $utf8NoBom)
    Write-Output "DECODER=ffmpeg"
    Write-Output ("FRAMES=" + $items.Count)
    exit 0
}

# ---------- 3) 回退：单张代表帧 ----------
[Windows.Storage.StorageFile, Windows.Storage, ContentType = WindowsRuntime] | Out-Null
[Windows.Storage.FileProperties.ThumbnailMode, Windows.Storage.FileProperties, ContentType = WindowsRuntime] | Out-Null
[Windows.Storage.FileProperties.StorageItemThumbnail, Windows.Storage.FileProperties, ContentType = WindowsRuntime] | Out-Null

$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
})[0]
function AwaitOp($op, $t) {
    $task = $asTaskGeneric.MakeGenericMethod($t).Invoke($null, @($op))
    $task.Wait(-1) | Out-Null
    $task.Result
}

if (-not (Test-Path $Video)) { Write-Error "Video not found: $Video"; exit 2 }

$file = AwaitOp ([Windows.Storage.StorageFile]::GetFileFromPathAsync($Video)) ([Windows.Storage.StorageFile])
$items = New-Object System.Collections.ArrayList
try {
    $thumb = AwaitOp ($file.GetThumbnailAsync([Windows.Storage.FileProperties.ThumbnailMode]::VideosView, $Width)) ([Windows.Storage.FileProperties.StorageItemThumbnail])
    $path = Join-Path $OutDir "frame_000.bmp"
    $net = [System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead($thumb)
    $fs = [System.IO.File]::Create($path)
    $net.CopyTo($fs)
    $fs.Dispose(); $net.Dispose(); $thumb.Dispose()
    $null = $items.Add([pscustomobject]@{ timestamp = 0.0; imagePath = $path })
} catch {
    Write-Output ("FRAME_ERROR: " + $_.Exception.Message)
}
$json = if ($items.Count -eq 0) { "[]" } else { $items | ConvertTo-Json -Depth 4 }
if (-not $json.TrimStart().StartsWith("[")) { $json = "[$json]" }
[System.IO.File]::WriteAllText($framesJson, $json, $utf8NoBom)
Write-Output "DECODER=winrt-thumbnail"
Write-Output ("FRAMES=" + $items.Count)
