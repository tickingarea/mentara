param(
    [string]$Root = "C:\Users\Administrator\Documents\Default Project\learnagent"
)

Add-Type -AssemblyName System.Drawing

$cs = @'
using System;
using System.Drawing;
using System.Drawing.Drawing2D;

public class IconGen {
    public static void Generate(string path, int size) {
        var bmp = new Bitmap(size, size);
        var g = Graphics.FromImage(bmp);
        g.SmoothingMode = SmoothingMode.AntiAlias;

        var rect = new Rectangle(0, 0, size, size);
        var bgBrush = new LinearGradientBrush(rect,
            Color.FromArgb(45, 91, 255),
            Color.FromArgb(0, 184, 169),
            LinearGradientMode.ForwardDiagonal);
        g.FillPath(bgBrush, RoundedRect(rect, (int)(size * 0.22f)));

        var white = new SolidBrush(Color.White);
        var pen = new Pen(Color.FromArgb(210, 255, 255, 255), size * 0.031f);
        pen.StartCap = LineCap.Round;
        pen.EndCap = LineCap.Round;

        var n1 = new PointF(size * 0.32f, size * 0.35f);
        var n2 = new PointF(size * 0.68f, size * 0.42f);
        var n3 = new PointF(size * 0.50f, size * 0.72f);

        g.DrawLine(pen, n1, n2);
        g.DrawLine(pen, n2, n3);
        g.DrawLine(pen, n1, n3);

        g.FillEllipse(white, n1.X - size * 0.0625f, n1.Y - size * 0.0625f, size * 0.125f, size * 0.125f);
        g.FillEllipse(white, n2.X - size * 0.0625f, n2.Y - size * 0.0625f, size * 0.125f, size * 0.125f);
        g.FillEllipse(white, n3.X - size * 0.078f, n3.Y - size * 0.078f, size * 0.156f, size * 0.156f);

        g.Dispose();
        bmp.Save(path, System.Drawing.Imaging.ImageFormat.Png);
        bmp.Dispose();
    }

    public static void Resize(string src, string dst, int size) {
        var img = Image.FromFile(src);
        var bmp = new Bitmap(size, size);
        var g = Graphics.FromImage(bmp);
        g.InterpolationMode = InterpolationMode.HighQualityBicubic;
        g.SmoothingMode = SmoothingMode.AntiAlias;
        g.DrawImage(img, 0, 0, size, size);
        bmp.Save(dst, System.Drawing.Imaging.ImageFormat.Png);
        g.Dispose(); bmp.Dispose(); img.Dispose();
    }

    static GraphicsPath RoundedRect(Rectangle bounds, int radius) {
        var path = new GraphicsPath();
        int d = radius * 2;
        path.AddArc(bounds.X, bounds.Y, d, d, 180, 90);
        path.AddArc(bounds.Right - d, bounds.Y, d, d, 270, 90);
        path.AddArc(bounds.Right - d, bounds.Bottom - d, d, d, 0, 90);
        path.AddArc(bounds.X, bounds.Bottom - d, d, d, 90, 90);
        path.CloseFigure();
        return path;
    }
}
'@

Add-Type -TypeDefinition $cs -ReferencedAssemblies System.Drawing | Out-Null

$resDir = Join-Path $Root "desktopApp\src\main\resources"
New-Item -ItemType Directory -Force -Path $resDir | Out-Null
$png = Join-Path $resDir "icon.png"
[IconGen]::Generate($png, 256)
Write-Output ("ICON_PNG_BYTES=" + (Get-Item $png).Length)

# PNG -> ICO (ICO with embedded PNG, Vista+)
$pngBytes = [System.IO.File]::ReadAllBytes($png)
$ms = New-Object System.IO.MemoryStream
$bw = New-Object System.IO.BinaryWriter($ms)
$bw.Write([UInt16]0); $bw.Write([UInt16]1); $bw.Write([UInt16]1)      # reserved, type=icon, count=1
$bw.Write([Byte]0); $bw.Write([Byte]0); $bw.Write([Byte]0); $bw.Write([Byte]0)  # 256x256
$bw.Write([UInt16]1); $bw.Write([UInt16]32)                            # planes, bpp
$bw.Write([UInt32]$pngBytes.Length)                                    # size
$bw.Write([UInt32]22)                                                  # offset
$bw.Write($pngBytes)
$bw.Flush()
$ico = Join-Path $resDir "icon.ico"
[System.IO.File]::WriteAllBytes($ico, $ms.ToArray())
$bw.Dispose(); $ms.Dispose()
Write-Output ("ICON_ICO_BYTES=" + (Get-Item $ico).Length)

# Android mipmaps
$mip = @{ "mdpi" = 48; "hdpi" = 72; "xhdpi" = 96; "xxhdpi" = 144; "xxxhdpi" = 192 }
foreach ($k in $mip.Keys) {
    $d = Join-Path $Root ("androidApp\src\main\res\mipmap-" + $k)
    New-Item -ItemType Directory -Force -Path $d | Out-Null
    $dst = Join-Path $d "ic_launcher.png"
    [IconGen]::Resize($png, $dst, $mip[$k])
    Write-Output ("MIPMAP_" + $k + "=" + (Get-Item $dst).Length)
}
