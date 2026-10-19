# Mentara 幻域美术资源 · 程序化生成
# 使用 System.Drawing (GDI+) 生成 PNG，无任何外部下载。
# 输出：desktopApp/src/main/resources/art/ + androidApp/src/main/res/drawable/

param(
    [string]$ProjectRoot = "C:\Users\Administrator\Documents\Default Project\learnagent"
)

$ErrorActionPreference = "Stop"

$code = @'
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.IO;
using System.Runtime.InteropServices;

public static class MentaraArt
{
    public static string Report = "";

    public static void Generate(string dir)
    {
        Directory.CreateDirectory(dir);
        Save(SplashBg(2048), Path.Combine(dir, "splash_bg.png"));
        Save(MainBg(1600, 900), Path.Combine(dir, "main_bg.png"));
        Save(LogoMark(512), Path.Combine(dir, "logo_mark.png"));
        Save(EmptyOrb(1024), Path.Combine(dir, "empty_orb.png"));
        Save(Noise(512), Path.Combine(dir, "noise.png"));
    }

    static void Save(Bitmap b, string p)
    {
        b.Save(p, ImageFormat.Png);
        FileInfo fi = new FileInfo(p);
        Report += p + " " + fi.Length + "\n";
        b.Dispose();
    }

    static int Clamp(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }

    static Color Lerp(Color a, Color b, float t)
    {
        if (t < 0) t = 0; if (t > 1) t = 1;
        return Color.FromArgb(
            (int)(a.A + (b.A - a.A) * t),
            (int)(a.R + (b.R - a.R) * t),
            (int)(a.G + (b.G - a.G) * t),
            (int)(a.B + (b.B - a.B) * t));
    }

    static Bitmap FromFunc(int w, int h, Func<int, int, Color> f)
    {
        Bitmap bmp = new Bitmap(w, h, PixelFormat.Format32bppArgb);
        BitmapData data = bmp.LockBits(new Rectangle(0, 0, w, h), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
        byte[] bytes = new byte[w * h * 4];
        int i = 0;
        for (int y = 0; y < h; y++)
        {
            for (int x = 0; x < w; x++)
            {
                Color c = f(x, y);
                bytes[i++] = c.B; bytes[i++] = c.G; bytes[i++] = c.R; bytes[i++] = c.A;
            }
        }
        Marshal.Copy(bytes, 0, data.Scan0, bytes.Length);
        bmp.UnlockBits(data);
        return bmp;
    }

    static void AddGlow(Graphics g, float cx, float cy, float radius, Color c, int steps)
    {
        for (int i = steps; i >= 1; i--)
        {
            float r = radius * i / steps;
            float k = 1f - (float)i / steps;
            int a = (int)(c.A * k * k * 2.2f);
            if (a <= 0) continue;
            if (a > 255) a = 255;
            using (SolidBrush b = new SolidBrush(Color.FromArgb(a, c.R, c.G, c.B)))
                g.FillEllipse(b, cx - r, cy - r, r * 2, r * 2);
        }
    }

    static Bitmap SplashBg(int s)
    {
        float cx = s * 0.5f, cy = s * 0.44f;
        float maxR = s * 0.80f;
        Bitmap bmp = FromFunc(s, s, delegate(int x, int y)
        {
            float dx = x - cx, dy = y - cy;
            float d = (float)Math.Sqrt(dx * dx + dy * dy) / maxR;
            float t = d > 1 ? 1 : d;
            t = t * t * (3 - 2 * t);
            Color center = Color.FromArgb(255, 15, 10, 24);
            Color mid = Color.FromArgb(255, 11, 7, 16);
            Color edge = Color.FromArgb(255, 6, 3, 8);
            Color c = t < 0.5f ? Lerp(center, mid, t * 2f) : Lerp(mid, edge, (t - 0.5f) * 2f);
            int n = ((x * 7349 + y * 9151) % 7) - 3;
            return Color.FromArgb(255, Clamp(c.R + n), Clamp(c.G + n), Clamp(c.B + n));
        });
        using (Graphics g = Graphics.FromImage(bmp))
        {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            AddGlow(g, cx, cy, s * 0.44f, Color.FromArgb(90, 201, 168, 255), 70);
            AddGlow(g, s * 0.74f, s * 0.28f, s * 0.20f, Color.FromArgb(46, 232, 200, 122), 46);
            AddGlow(g, s * 0.26f, s * 0.70f, s * 0.22f, Color.FromArgb(40, 168, 128, 255), 46);
            Random rnd = new Random(1234);
            for (int i = 0; i < 280; i++)
            {
                float x = (float)rnd.NextDouble() * s;
                float y = (float)rnd.NextDouble() * s;
                float r = 1.2f + (float)rnd.NextDouble() * 2.6f;
                bool gold = i % 2 == 1;
                Color c = gold ? Color.FromArgb(190, 232, 200, 122) : Color.FromArgb(200, 201, 168, 255);
                AddGlow(g, x, y, r * 6f, Color.FromArgb(58, c.R, c.G, c.B), 22);
                using (SolidBrush b = new SolidBrush(c))
                    g.FillEllipse(b, x - r / 2, y - r / 2, r, r);
            }
            for (int i = 0; i < 90; i++)
            {
                float x = (float)rnd.NextDouble() * s;
                float y = (float)rnd.NextDouble() * s;
                using (SolidBrush b = new SolidBrush(Color.FromArgb(14, 240, 234, 245)))
                    g.FillEllipse(b, x, y, 1.4f, 1.4f);
            }
        }
        return bmp;
    }

    static Bitmap MainBg(int w, int h)
    {
        float cx = w * 0.5f, cy = h * 0.42f;
        float rx = w * 0.78f, ry = h * 0.95f;
        Bitmap bmp = FromFunc(w, h, delegate(int x, int y)
        {
            float dx = (x - cx) / rx, dy = (y - cy) / ry;
            float d = (float)Math.Sqrt(dx * dx + dy * dy);
            float t = d > 1 ? 1 : d;
            t = t * t * (3 - 2 * t);
            Color center = Color.FromArgb(255, 15, 10, 24);
            Color edge = Color.FromArgb(255, 6, 3, 8);
            Color c = Lerp(center, edge, t);
            int n = ((x * 6749 + y * 3251) % 7) - 3;
            return Color.FromArgb(255, Clamp(c.R + n), Clamp(c.G + n), Clamp(c.B + n));
        });
        using (Graphics g = Graphics.FromImage(bmp))
        {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            AddGlow(g, w * 0.30f, h * 0.36f, w * 0.26f, Color.FromArgb(34, 201, 168, 255), 60);
            AddGlow(g, w * 0.78f, h * 0.62f, w * 0.22f, Color.FromArgb(26, 232, 200, 122), 52);
            AddGlow(g, w * 0.55f, h * 0.85f, w * 0.20f, Color.FromArgb(22, 168, 128, 255), 48);
        }
        return bmp;
    }

    static Bitmap LogoMark(int s)
    {
        Bitmap bmp = new Bitmap(s, s, PixelFormat.Format32bppArgb);
        using (Graphics g = Graphics.FromImage(bmp))
        {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            float cx = s * 0.5f;
            PointF n1 = new PointF(s * 0.5f, s * 0.24f);
            PointF n2 = new PointF(s * 0.26f, s * 0.64f);
            PointF n3 = new PointF(s * 0.74f, s * 0.64f);
            AddGlow(g, cx, s * 0.5f, s * 0.46f, Color.FromArgb(52, 201, 168, 255), 60);
            using (LinearGradientBrush lb = new LinearGradientBrush(
                new PointF(s * 0.2f, s * 0.2f), new PointF(s * 0.8f, s * 0.8f),
                Color.FromArgb(150, 201, 168, 255), Color.FromArgb(150, 232, 200, 122)))
            using (Pen pen = new Pen(lb, s * 0.014f))
            {
                pen.StartCap = LineCap.Round; pen.EndCap = LineCap.Round;
                g.DrawLine(pen, n1, n2);
                g.DrawLine(pen, n2, n3);
                g.DrawLine(pen, n3, n1);
            }
            DrawNode(g, n1, s * 0.062f, Color.FromArgb(235, 201, 168, 255));
            DrawNode(g, n2, s * 0.052f, Color.FromArgb(220, 201, 168, 255));
            DrawNode(g, n3, s * 0.075f, Color.FromArgb(240, 232, 200, 122));
        }
        return bmp;
    }

    static void DrawNode(Graphics g, PointF p, float r, Color c)
    {
        AddGlow(g, p.X, p.Y, r * 4.2f, Color.FromArgb(70, c.R, c.G, c.B), 26);
        using (SolidBrush b = new SolidBrush(Color.FromArgb(90, c.R, c.G, c.B)))
            g.FillEllipse(b, p.X - r * 1.7f, p.Y - r * 1.7f, r * 3.4f, r * 3.4f);
        using (SolidBrush b = new SolidBrush(c))
            g.FillEllipse(b, p.X - r, p.Y - r, r * 2, r * 2);
    }

    static Bitmap EmptyOrb(int s)
    {
        Bitmap bmp = new Bitmap(s, s, PixelFormat.Format32bppArgb);
        using (Graphics g = Graphics.FromImage(bmp))
        {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            float cx = s / 2f, cy = s / 2f;
            AddGlow(g, cx, cy, s * 0.40f, Color.FromArgb(80, 201, 168, 255), 80);
            using (SolidBrush b = new SolidBrush(Color.FromArgb(60, 201, 168, 255)))
                g.FillEllipse(b, cx - s * 0.16f, cy - s * 0.16f, s * 0.32f, s * 0.32f);
            using (SolidBrush b = new SolidBrush(Color.FromArgb(120, 216, 190, 255)))
                g.FillEllipse(b, cx - s * 0.10f, cy - s * 0.10f, s * 0.20f, s * 0.20f);
            using (SolidBrush b = new SolidBrush(Color.FromArgb(200, 240, 234, 245)))
                g.FillEllipse(b, cx - s * 0.045f, cy - s * 0.045f, s * 0.09f, s * 0.09f);
            using (Pen ring = new Pen(Color.FromArgb(70, 168, 176, 192), s * 0.006f))
            {
                ring.DashStyle = DashStyle.Dash;
                g.DrawEllipse(ring, cx - s * 0.30f, cy - s * 0.22f, s * 0.60f, s * 0.44f);
            }
            using (Pen ring = new Pen(Color.FromArgb(46, 201, 168, 255), s * 0.004f))
            {
                g.DrawEllipse(ring, cx - s * 0.38f, cy - s * 0.30f, s * 0.76f, s * 0.60f);
            }
            Random rnd = new Random(77);
            for (int i = 0; i < 26; i++)
            {
                double ang = rnd.NextDouble() * Math.PI * 2;
                float rad = s * (0.20f + (float)rnd.NextDouble() * 0.22f);
                float x = cx + (float)Math.Cos(ang) * rad;
                float y = cy + (float)Math.Sin(ang) * rad * 0.7f;
                float r = 1.5f + (float)rnd.NextDouble() * 2.5f;
                bool gold = i % 3 == 0;
                Color c = gold ? Color.FromArgb(190, 232, 200, 122) : Color.FromArgb(180, 201, 168, 255);
                using (SolidBrush b = new SolidBrush(c))
                    g.FillEllipse(b, x - r / 2, y - r / 2, r, r);
            }
        }
        return bmp;
    }

    static Bitmap Noise(int s)
    {
        Random rnd = new Random(99);
        return FromFunc(s, s, delegate(int x, int y)
        {
            int a = rnd.Next(0, 7);
            return Color.FromArgb(a, 255, 255, 255);
        });
    }
}
'@

Add-Type -TypeDefinition $code -Language CSharp -ReferencedAssemblies "System.Drawing" -ErrorAction Stop

$artDir = Join-Path $ProjectRoot "desktopApp\src\main\resources\art"
$drawableDir = Join-Path $ProjectRoot "androidApp\src\main\res\drawable"

[MentaraArt]::Generate($artDir)
[MentaraArt]::Generate($drawableDir)

Write-Output ([MentaraArt]::Report)
