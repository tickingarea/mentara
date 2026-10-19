// Media Foundation 软件解码抽帧（P/Invoke，零外部依赖）
// 用途：从视频按时间均匀抽取 N 帧（RGB32），保存 PNG；供 extract_frames.ps1 通过 Add-Type 调用。
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;

public static class MfFrameGrab
{
    // ---- PROPVARIANT / GUID ----
    [StructLayout(LayoutKind.Sequential)]
    public struct Guid
    {
        public uint Data1; public ushort Data2; public ushort Data3;
        public byte B0, B1, B2, B3, B4, B5, B6, B7;
        public Guid(uint d1, ushort d2, ushort d3, byte b0, byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7)
        { Data1 = d1; Data2 = d2; Data3 = d3; B0 = b0; B1 = b1; B2 = b2; B3 = b3; B4 = b4; B5 = b5; B6 = b6; B7 = b7; }
    }

    [StructLayout(LayoutKind.Explicit)]
    public struct PropVariant
    {
        [FieldOffset(0)] public ushort vt;
        [FieldOffset(2)] public ushort r1;
        [FieldOffset(4)] public ushort r2;
        [FieldOffset(6)] public ushort r3;
        [FieldOffset(8)] public long hVal;
        [FieldOffset(8)] public ulong uhVal;
    }

    static Guid MF_MT_MAJOR_TYPE = new Guid(0x48eba18e, 0xf8c9, 0x4687, 0xbf, 0x11, 0x0a, 0x74, 0xc9, 0xf9, 0x6a, 0x8f);
    static Guid MF_MT_SUBTYPE = new Guid(0xf7e34c9a, 0x42e8, 0x4714, 0xb7, 0x4b, 0xcb, 0x29, 0xd7, 0x2c, 0x35, 0xe5);
    static Guid MF_MT_FRAME_SIZE = new Guid(0x1652c33d, 0xd6b2, 0x4012, 0xb8, 0x34, 0x72, 0x03, 0x08, 0x49, 0xa3, 0x7d);
    static Guid MFMediaType_Video = new Guid(0x73646976, 0x0000, 0x0010, 0x80, 0x00, 0x00, 0xaa, 0x00, 0x38, 0x9b, 0x71);
    static Guid MFVideoFormat_RGB32 = new Guid(0x00000016, 0x0000, 0x0010, 0x80, 0x00, 0x00, 0xaa, 0x00, 0x38, 0x9b, 0x71);
    static Guid MF_PD_DURATION = new Guid(0x6c990d33, 0xbb8e, 0x477a, 0x85, 0x98, 0x0d, 0x5d, 0x96, 0xfc, 0xa9, 0x4b);
    static Guid MF_SOURCE_READER_ENABLE_VIDEO_PROCESSING = new Guid(0xfb394f3d, 0xccf1, 0x42ee, 0xbb, 0xb3, 0xf9, 0xb8, 0x45, 0xd5, 0x68, 0x1d);
    static Guid MF_MT_VIDEO_ROTATION = new Guid(0xc380465d, 0x2271, 0x428c, 0x9b, 0x83, 0xec, 0xea, 0x3b, 0x4a, 0x85, 0xc1);

    const uint MF_SOURCE_READER_FIRST_VIDEO_STREAM = 0xFFFFFFFC;
    const uint MF_SOURCE_READER_MEDIASOURCE = 0xFFFFFFFF;
    const uint MF_SOURCE_READERF_ENDOFSTREAM = 0x2;
    const ushort VT_I8 = 20;
    const uint MF_VERSION = 0x00020070;

    // ---- COM 接口（vtable 顺序必须完整） ----
    [ComImport, Guid("2cd2d921-c447-44a7-a13c-4adabfc247e3"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IMFAttributes
    {
        [PreserveSig] int GetItem(ref Guid key, IntPtr val);
        [PreserveSig] int GetItemType(ref Guid key, out uint type);
        [PreserveSig] int CompareItem(ref Guid key, IntPtr val, [MarshalAs(UnmanagedType.Bool)] out bool result);
        [PreserveSig] int Compare(IMFAttributes a, uint matchType, out int result);
        [PreserveSig] int GetUINT32(ref Guid key, out uint val);
        [PreserveSig] int GetUINT64(ref Guid key, out ulong val);
        [PreserveSig] int GetDouble(ref Guid key, out double val);
        [PreserveSig] int GetGUID(ref Guid key, out Guid val);
        [PreserveSig] int GetStringLength(ref Guid key, out uint len);
        [PreserveSig] int GetString(ref Guid key, [Out, MarshalAs(UnmanagedType.LPWStr)] System.Text.StringBuilder val, uint size, out uint len);
        [PreserveSig] int GetAllocatedString(ref Guid key, out IntPtr val, out uint len);
        [PreserveSig] int GetBlobSize(ref Guid key, out uint size);
        [PreserveSig] int GetBlob(ref Guid key, IntPtr buf, uint size, out uint blobSize);
        [PreserveSig] int GetAllocatedBlob(ref Guid key, out IntPtr buf, out uint size);
        [PreserveSig] int GetUnknown(ref Guid key, ref Guid riid, out IntPtr val);
        [PreserveSig] int SetItem(ref Guid key, IntPtr val);
        [PreserveSig] int DeleteItem(ref Guid key);
        [PreserveSig] int DeleteAllItems();
        [PreserveSig] int SetUINT32(ref Guid key, uint val);
        [PreserveSig] int SetUINT64(ref Guid key, ulong val);
        [PreserveSig] int SetDouble(ref Guid key, double val);
        [PreserveSig] int SetGUID(ref Guid key, ref Guid val);
        [PreserveSig] int SetString(ref Guid key, [MarshalAs(UnmanagedType.LPWStr)] string val);
        [PreserveSig] int SetBlob(ref Guid key, IntPtr buf, uint size);
        [PreserveSig] int SetUnknown(ref Guid key, IMFAttributes val);
        [PreserveSig] int LockStore();
        [PreserveSig] int UnlockStore();
        [PreserveSig] int GetCount(out uint count);
        [PreserveSig] int GetItemByIndex(uint index, out Guid key, IntPtr val);
        [PreserveSig] int CopyAllItems(IMFAttributes dest);
    }

    [ComImport, Guid("c40a00f2-b93a-4d80-ae8c-5a1c634f58e4"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IMFSample
    {
        // IMFAttributes 段（占位，保持 vtable 对齐）
        [PreserveSig] int GetItem(ref Guid key, IntPtr val);
        [PreserveSig] int GetItemType(ref Guid key, out uint type);
        [PreserveSig] int CompareItem(ref Guid key, IntPtr val, [MarshalAs(UnmanagedType.Bool)] out bool result);
        [PreserveSig] int Compare(IMFAttributes a, uint matchType, out int result);
        [PreserveSig] int GetUINT32(ref Guid key, out uint val);
        [PreserveSig] int GetUINT64(ref Guid key, out ulong val);
        [PreserveSig] int GetDouble(ref Guid key, out double val);
        [PreserveSig] int GetGUID(ref Guid key, out Guid val);
        [PreserveSig] int GetStringLength(ref Guid key, out uint len);
        [PreserveSig] int GetString(ref Guid key, [Out, MarshalAs(UnmanagedType.LPWStr)] System.Text.StringBuilder val, uint size, out uint len);
        [PreserveSig] int GetAllocatedString(ref Guid key, out IntPtr val, out uint len);
        [PreserveSig] int GetBlobSize(ref Guid key, out uint size);
        [PreserveSig] int GetBlob(ref Guid key, IntPtr buf, uint size, out uint blobSize);
        [PreserveSig] int GetAllocatedBlob(ref Guid key, out IntPtr buf, out uint size);
        [PreserveSig] int GetUnknown(ref Guid key, ref Guid riid, out IntPtr val);
        [PreserveSig] int SetItem(ref Guid key, IntPtr val);
        [PreserveSig] int DeleteItem(ref Guid key);
        [PreserveSig] int DeleteAllItems();
        [PreserveSig] int SetUINT32(ref Guid key, uint val);
        [PreserveSig] int SetUINT64(ref Guid key, ulong val);
        [PreserveSig] int SetDouble(ref Guid key, double val);
        [PreserveSig] int SetGUID(ref Guid key, ref Guid val);
        [PreserveSig] int SetString(ref Guid key, [MarshalAs(UnmanagedType.LPWStr)] string val);
        [PreserveSig] int SetBlob(ref Guid key, IntPtr buf, uint size);
        [PreserveSig] int SetUnknown(ref Guid key, IMFAttributes val);
        [PreserveSig] int LockStore();
        [PreserveSig] int UnlockStore();
        [PreserveSig] int GetCount(out uint count);
        [PreserveSig] int GetItemByIndex(uint index, out Guid key, IntPtr val);
        [PreserveSig] int CopyAllItems(IMFAttributes dest);
        // IMFSample 段
        [PreserveSig] int GetSampleFlags(out uint flags);
        [PreserveSig] int SetSampleFlags(uint flags);
        [PreserveSig] int GetSampleTime(out long time);
        [PreserveSig] int SetSampleTime(long time);
        [PreserveSig] int GetSampleDuration(out long dur);
        [PreserveSig] int SetSampleDuration(long dur);
        [PreserveSig] int GetBufferCount(out uint count);
        [PreserveSig] int GetBufferByIndex(uint index, out IMFMediaBuffer buffer);
        [PreserveSig] int ConvertToContiguousBuffer(out IMFMediaBuffer buffer);
        [PreserveSig] int AddBuffer(IMFMediaBuffer buffer);
        [PreserveSig] int RemoveBufferByIndex(uint index);
        [PreserveSig] int RemoveAllBuffers();
        [PreserveSig] int GetTotalLength(out uint len);
        [PreserveSig] int CopyToBuffer(IMFMediaBuffer buffer);
    }

    [ComImport, Guid("045fa593-8799-42b8-bc8d-8968c6453507"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IMFMediaBuffer
    {
        [PreserveSig] int Lock(out IntPtr ptr, out int maxLen, out int curLen);
        [PreserveSig] int Unlock();
        [PreserveSig] int GetCurrentLength(out int len);
        [PreserveSig] int SetCurrentLength(int len);
        [PreserveSig] int GetMaxLength(out int maxLen);
    }

    [ComImport, Guid("44ae0fa8-ea31-4109-8d2e-4cae4997c555"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IMFMediaType
    {
        [PreserveSig] int GetItem(ref Guid key, IntPtr val);
        [PreserveSig] int GetItemType(ref Guid key, out uint type);
        [PreserveSig] int CompareItem(ref Guid key, IntPtr val, [MarshalAs(UnmanagedType.Bool)] out bool result);
        [PreserveSig] int Compare(IMFAttributes a, uint matchType, out int result);
        [PreserveSig] int GetUINT32(ref Guid key, out uint val);
        [PreserveSig] int GetUINT64(ref Guid key, out ulong val);
        [PreserveSig] int GetDouble(ref Guid key, out double val);
        [PreserveSig] int GetGUID(ref Guid key, out Guid val);
        [PreserveSig] int GetStringLength(ref Guid key, out uint len);
        [PreserveSig] int GetString(ref Guid key, [Out, MarshalAs(UnmanagedType.LPWStr)] System.Text.StringBuilder val, uint size, out uint len);
        [PreserveSig] int GetAllocatedString(ref Guid key, out IntPtr val, out uint len);
        [PreserveSig] int GetBlobSize(ref Guid key, out uint size);
        [PreserveSig] int GetBlob(ref Guid key, IntPtr buf, uint size, out uint blobSize);
        [PreserveSig] int GetAllocatedBlob(ref Guid key, out IntPtr buf, out uint size);
        [PreserveSig] int GetUnknown(ref Guid key, ref Guid riid, out IntPtr val);
        [PreserveSig] int SetItem(ref Guid key, IntPtr val);
        [PreserveSig] int DeleteItem(ref Guid key);
        [PreserveSig] int DeleteAllItems();
        [PreserveSig] int SetUINT32(ref Guid key, uint val);
        [PreserveSig] int SetUINT64(ref Guid key, ulong val);
        [PreserveSig] int SetDouble(ref Guid key, double val);
        [PreserveSig] int SetGUID(ref Guid key, ref Guid val);
        [PreserveSig] int SetString(ref Guid key, [MarshalAs(UnmanagedType.LPWStr)] string val);
        [PreserveSig] int SetBlob(ref Guid key, IntPtr buf, uint size);
        [PreserveSig] int SetUnknown(ref Guid key, IMFAttributes val);
        [PreserveSig] int LockStore();
        [PreserveSig] int UnlockStore();
        [PreserveSig] int GetCount(out uint count);
        [PreserveSig] int GetItemByIndex(uint index, out Guid key, IntPtr val);
        [PreserveSig] int CopyAllItems(IMFAttributes dest);
        // IMFMediaType 段
        [PreserveSig] int GetMajorType(out Guid major);
        [PreserveSig] int IsCompressedFormat([MarshalAs(UnmanagedType.Bool)] out bool compressed);
        [PreserveSig] int IsEqual(IMFMediaType t, out uint flags);
        [PreserveSig] int GetRepresentation(ref Guid rep, out IntPtr val);
        [PreserveSig] int FreeRepresentation(ref Guid rep, IntPtr val);
    }

    [ComImport, Guid("70ae66f2-c809-4e4f-8915-bdcb406b7993"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IMFSourceReader
    {
        [PreserveSig] int GetStreamSelection(uint index, [MarshalAs(UnmanagedType.Bool)] out bool selected);
        [PreserveSig] int SetStreamSelection(uint index, [MarshalAs(UnmanagedType.Bool)] bool selected);
        [PreserveSig] int GetNativeMediaType(uint index, uint typeIndex, out IMFMediaType type);
        [PreserveSig] int GetCurrentMediaType(uint index, out IMFMediaType type);
        [PreserveSig] int SetCurrentMediaType(uint index, IntPtr reserved, IMFMediaType type);
        [PreserveSig] int SetCurrentPosition(ref Guid format, ref PropVariant pos);
        [PreserveSig] int ReadSample(uint index, uint flags, out uint actualIndex, out uint streamFlags, out long timestamp, out IMFSample sample);
        [PreserveSig] int Flush(uint index);
        [PreserveSig] int GetPresentationAttribute(uint index, ref Guid attr, out PropVariant val);
        [PreserveSig] int GetCharacteristics(out uint characteristics);
    }

    [DllImport("mfplat.dll")] static extern int MFStartup(uint version, uint flags);
    [DllImport("mfplat.dll")] static extern int MFShutdown();
    [DllImport("mfplat.dll")] static extern int MFCreateAttributes(out IMFAttributes attrs, uint count);
    [DllImport("mfplat.dll")] static extern int MFCreateMediaType(out IMFMediaType type);
    [DllImport("mfreadwrite.dll", CharSet = CharSet.Unicode)]
    static extern int MFCreateSourceReaderFromURL(string url, IMFAttributes attrs, out IMFSourceReader reader);

    /// <summary>从视频按时间均匀抽 count 帧到 outDir（PNG），返回实际帧数。</summary>
    public static int Grab(string video, string outDir, int count, int fps = 0)
    {
        if (!Directory.Exists(outDir)) Directory.CreateDirectory(outDir);
        int hr = MFStartup(MF_VERSION, 0);
        if (hr < 0) throw new Exception("MFStartup 0x" + hr.ToString("X8"));
        IMFAttributes attrs = null; IMFSourceReader reader = null;
        try
        {
            if (MFCreateAttributes(out attrs, 1) < 0) throw new Exception("MFCreateAttributes");
            attrs.SetUINT32(ref MF_SOURCE_READER_ENABLE_VIDEO_PROCESSING, 1);
            if (MFCreateSourceReaderFromURL(video, attrs, out reader) < 0) throw new Exception("MFCreateSourceReaderFromURL");

            uint videoIndex = MF_SOURCE_READER_FIRST_VIDEO_STREAM;
            try { reader.SetStreamSelection(MF_SOURCE_READER_MEDIASOURCE, false); } catch { }
            for (uint i = 0; i < 8; i++)
            {
                IMFMediaType t;
                if (reader.GetCurrentMediaType(i, out t) < 0 || t == null) continue;
                Guid major;
                hr = t.GetMajorType(out major);
                bool isVideo = hr >= 0 && major.Data1 == MFMediaType_Video.Data1 &&
                    major.Data2 == MFMediaType_Video.Data2 && major.Data3 == MFMediaType_Video.Data3 &&
                    major.B0 == MFMediaType_Video.B0 && major.B1 == MFMediaType_Video.B1 &&
                    major.B2 == MFMediaType_Video.B2 && major.B3 == MFMediaType_Video.B3;
                if (isVideo) { videoIndex = i; break; }
            }
            if (videoIndex == MF_SOURCE_READER_FIRST_VIDEO_STREAM) throw new Exception("未找到视频流");
            reader.SetStreamSelection(videoIndex, true);

            IMFMediaType outType;
            MFCreateMediaType(out outType);
            var vguid = MFMediaType_Video; outType.SetGUID(ref MF_MT_MAJOR_TYPE, ref vguid);
            var rguid = MFVideoFormat_RGB32; outType.SetGUID(ref MF_MT_SUBTYPE, ref rguid);
            hr = reader.SetCurrentMediaType(videoIndex, IntPtr.Zero, outType);
            if (hr < 0) throw new Exception("SetCurrentMediaType(RGB32) 0x" + hr.ToString("X8"));

            IMFMediaType cur;
            reader.GetCurrentMediaType(videoIndex, out cur);
            ulong frameSize;
            cur.GetUINT64(ref MF_MT_FRAME_SIZE, out frameSize);
            int width = (int)(frameSize >> 32);
            int height = (int)(frameSize & 0xFFFFFFFF);
            if (width <= 0 || height <= 0) throw new Exception("取帧尺寸失败");
            uint rot = 0; try { cur.GetUINT32(ref MF_MT_VIDEO_ROTATION, out rot); } catch { rot = 0; }

            PropVariant durPv;
            reader.GetPresentationAttribute(MF_SOURCE_READER_MEDIASOURCE, ref MF_PD_DURATION, out durPv);
            double total = (durPv.vt == 20 || durPv.vt == 21) ? durPv.uhVal / 1e7 : 0.0;
            if (total <= 0.05)
            {
                // 时长属性不可用时：跳到远端读到最后样本的最大时间戳
                try
                {
                    var farPos = new PropVariant { vt = VT_I8, hVal = (long)(3600.0 * 1e7) };
                    var ng = new Guid();
                    reader.SetCurrentPosition(ref ng, ref farPos);
                    reader.Flush(videoIndex);
                    long maxTs = 0;
                    for (int g = 0; g < 5000; g++)
                    {
                        uint ai, sf; long ts; IMFSample s;
                        if (reader.ReadSample(videoIndex, 0, out ai, out sf, out ts, out s) < 0) break;
                        if (s == null) { if ((sf & MF_SOURCE_READERF_ENDOFSTREAM) != 0) break; else continue; }
                        if (ts > maxTs) maxTs = ts;
                        Marshal.ReleaseComObject(s);
                    }
                    if (maxTs > 0) total = maxTs / 1e7;
                }
                catch { }
            }
            if (total <= 0.05) total = 60.0;

            int n = fps > 0 ? Math.Max(2, (int)Math.Round(total * fps)) : Math.Max(2, count);
            if (n > 4000) n = 4000;
            int saved = 0;
            var entries = new System.Collections.Generic.List<string>();
            for (int i = 1; i <= n; i++)
            {
                double sec = total * i / (n + 1.0);
                long target = (long)(sec * 1e7);
                var pos = new PropVariant { vt = VT_I8, hVal = target };
                var nullGuid = new Guid();
                reader.SetCurrentPosition(ref nullGuid, ref pos);
                reader.Flush(videoIndex);

                IMFSample sample = null; long ts = 0;
                for (int guard = 0; guard < 200; guard++)
                {
                    uint ai, sf; IMFSample s;
                    hr = reader.ReadSample(videoIndex, 0, out ai, out sf, out ts, out s);
                    if (hr < 0) break;
                    if (s == null)
                    {
                        if ((sf & MF_SOURCE_READERF_ENDOFSTREAM) != 0) break;
                        continue;
                    }
                    if (ts + 200000 >= target) { sample = s; break; }
                    // 否则丢弃，继续找 >= target 的帧
                }
                if (sample == null) continue;

                IMFMediaBuffer buf;
                sample.ConvertToContiguousBuffer(out buf);
                IntPtr ptr; int maxLen, curLen;
                buf.Lock(out ptr, out maxLen, out curLen);
                var bmp = new Bitmap(width, height, PixelFormat.Format32bppRgb);
                var bd = bmp.LockBits(new Rectangle(0, 0, width, height), ImageLockMode.WriteOnly, PixelFormat.Format32bppRgb);
                int rowBytes = width * 4;
                // MF RGB32 为自底向上：逐行翻转拷贝（末尾不足一行的按可用长度截断）
                for (int y = 0; y < height; y++)
                {
                    IntPtr src = IntPtr.Add(ptr, (height - 1 - y) * rowBytes);
                    long offset = (long)(height - 1 - y) * rowBytes;
                    if (offset + rowBytes > curLen) break;
                    IntPtr dst = IntPtr.Add(bd.Scan0, y * bd.Stride);
                    CopyMemory(dst, src, rowBytes);
                }
                bmp.UnlockBits(bd);
                buf.Unlock();
                if (rot == 180) bmp.RotateFlip(RotateFlipType.Rotate180FlipNone);
                else if (rot == 90) bmp.RotateFlip(RotateFlipType.Rotate90FlipNone);
                else if (rot == 270) bmp.RotateFlip(RotateFlipType.Rotate270FlipNone);
                string outPath = Path.Combine(outDir, "frame_" + i.ToString("D3") + ".png");
                bmp.Save(outPath, ImageFormat.Png);
                bmp.Dispose();
                entries.Add("{\"timestamp\":" + sec.ToString("0.###", System.Globalization.CultureInfo.InvariantCulture) + ",\"imagePath\":\"" + outPath.Replace("\\", "\\\\") + "\"}");
                saved++;
            }
            File.WriteAllText(Path.Combine(outDir, "frames.json"), "[" + string.Join(",", entries) + "]", new System.Text.UTF8Encoding(false));
            return saved;
        }
        finally
        {
            if (reader != null) Marshal.ReleaseComObject(reader);
            if (attrs != null) Marshal.ReleaseComObject(attrs);
            MFShutdown();
        }
    }

    [DllImport("kernel32.dll", EntryPoint = "RtlMoveMemory")]
    static extern void CopyMemory(IntPtr dest, IntPtr src, int count);

    // Scene-keyframe extraction: sequential decode at fps; keep only changed frames.
    public static int GrabScenes(string video, string outDir, int fps, int diffBits = 8, int outWidth = 0)
    {
        if (!Directory.Exists(outDir)) Directory.CreateDirectory(outDir);
        int hr = MFStartup(MF_VERSION, 0);
        if (hr < 0) throw new Exception("MFStartup fail");
        IMFAttributes attrs = null; IMFSourceReader reader = null;
        try
        {
            MFCreateAttributes(out attrs, 1);
            attrs.SetUINT32(ref MF_SOURCE_READER_ENABLE_VIDEO_PROCESSING, 1);
            if (MFCreateSourceReaderFromURL(video, attrs, out reader) < 0) throw new Exception("reader fail");
            uint videoIndex = MF_SOURCE_READER_FIRST_VIDEO_STREAM;
            try { reader.SetStreamSelection(MF_SOURCE_READER_MEDIASOURCE, false); } catch { }
            for (uint i = 0; i < 8; i++)
            {
                IMFMediaType t;
                if (reader.GetCurrentMediaType(i, out t) < 0 || t == null) continue;
                Guid major; if (t.GetMajorType(out major) >= 0 && SameGuid(major, MFMediaType_Video)) { videoIndex = i; break; }
            }
            if (videoIndex == MF_SOURCE_READER_FIRST_VIDEO_STREAM) throw new Exception("no video stream");
            reader.SetStreamSelection(videoIndex, true);
            IMFMediaType outType; MFCreateMediaType(out outType);
            var vg = MFMediaType_Video; outType.SetGUID(ref MF_MT_MAJOR_TYPE, ref vg);
            var rg = MFVideoFormat_RGB32; outType.SetGUID(ref MF_MT_SUBTYPE, ref rg);
            if (reader.SetCurrentMediaType(videoIndex, IntPtr.Zero, outType) < 0) throw new Exception("RGB32 fail");
            IMFMediaType cur; reader.GetCurrentMediaType(videoIndex, out cur);
            ulong fs; cur.GetUINT64(ref MF_MT_FRAME_SIZE, out fs);
            int width = (int)(fs >> 32), height = (int)(fs & 0xFFFFFFFF);
            uint rot = 0; try { cur.GetUINT32(ref MF_MT_VIDEO_ROTATION, out rot); } catch { rot = 0; }

            double step = 1e7 / Math.Max(1, fps);
            long lastSample = long.MinValue;
            ulong prevHash = 0;
            int saved = 0;
            var entries = new System.Collections.Generic.List<string>();
            var inv = System.Globalization.CultureInfo.InvariantCulture;

            for (int guard = 0; guard < 5000000; guard++)
            {
                uint ai, sf; long ts; IMFSample s;
                if (reader.ReadSample(videoIndex, 0, out ai, out sf, out ts, out s) < 0) break;
                if (s == null) { if ((sf & MF_SOURCE_READERF_ENDOFSTREAM) != 0) break; else continue; }
                if (lastSample == long.MinValue || ts - lastSample >= (long)step)
                {
                    lastSample = ts;
                    IMFMediaBuffer buf; s.ConvertToContiguousBuffer(out buf);
                    IntPtr ptr; int maxLen, curLen; buf.Lock(out ptr, out maxLen, out curLen);
                    ulong h = HashFrame(ptr, curLen, width, height);
                    bool changed = diffBits < 0 || prevHash == 0 || Hamming(h, prevHash) > diffBits;
                    if (changed)
                    {
                        prevHash = h;
                        bool ok; var bmp = BuildBitmap(ptr, curLen, width, height, rot, out ok);
                        if (ok)
                        {
                            string outPath = Path.Combine(outDir, "frame_" + (saved + 1).ToString("D4") + ".jpg");
                            if (outWidth > 0 && outWidth < width) { var sb = Scale(bmp, outWidth); bmp.Dispose(); SaveJpeg(sb, outPath); sb.Dispose(); }
                            else { SaveJpeg(bmp, outPath); bmp.Dispose(); }
                            double sec = ts / 1e7;
                            entries.Add("{\"timestamp\":" + sec.ToString("0.###", inv) + ",\"imagePath\":\"" + outPath.Replace("\\", "\\\\") + "\"}");
                            saved++;
                        }
                    }
                    buf.Unlock(); Marshal.ReleaseComObject(buf);
                }
                Marshal.ReleaseComObject(s);
            }
            File.WriteAllText(Path.Combine(outDir, "frames.json"), "[" + string.Join(",", entries) + "]", new System.Text.UTF8Encoding(false));
            return saved;
        }
        finally
        {
            if (reader != null) Marshal.ReleaseComObject(reader);
            if (attrs != null) Marshal.ReleaseComObject(attrs);
            MFShutdown();
        }
    }

    static bool SameGuid(Guid a, Guid b) { return a.Data1 == b.Data1 && a.Data2 == b.Data2 && a.Data3 == b.Data3 && a.B0 == b.B0 && a.B1 == b.B1 && a.B2 == b.B2 && a.B3 == b.B3; }

    static int Hamming(ulong a, ulong b) { ulong x = a ^ b; int c = 0; while (x != 0) { c += (int)(x & 1); x >>= 1; } return c; }

    static ulong HashFrame(IntPtr ptr, int len, int width, int height)
    {
        int rowBytes = width * 4;
        ulong hash = 0;
        long sum = 0; var cell = new long[64];
        for (int cy = 0; cy < 8; cy++)
            for (int cx = 0; cx < 8; cx++)
            {
                int x0 = cx * width / 8, x1 = Math.Max(x0 + 1, (cx + 1) * width / 8);
                int y0 = cy * height / 8, y1 = Math.Max(y0 + 1, (cy + 1) * height / 8);
                long acc = 0; int n = 0;
                for (int y = y0; y < y1; y += 2)
                    for (int x = x0; x < x1; x += 2)
                    {
                        long off = (long)(height - 1 - y) * rowBytes + x * 4;
                        if (off + 3 >= len) continue;
                        byte bb = Marshal.ReadByte(ptr, (int)off);
                        byte gg = Marshal.ReadByte(ptr, (int)(off + 1));
                        byte rr = Marshal.ReadByte(ptr, (int)(off + 2));
                        acc += (rr * 3 + gg * 6 + bb) / 10; n++;
                    }
                cell[cy * 8 + cx] = n > 0 ? acc / n : 0; sum += cell[cy * 8 + cx];
            }
        long mean = sum / 64;
        for (int k = 0; k < 64; k++) if (cell[k] >= mean) hash |= (1UL << k);
        return hash;
    }

    static Bitmap BuildBitmap(IntPtr ptr, int curLen, int width, int height, uint rot, out bool ok)
    {
        ok = false;
        int rowBytes = width * 4;
        if (curLen < rowBytes) return null;
        var bmp = new Bitmap(width, height, PixelFormat.Format32bppRgb);
        var bd = bmp.LockBits(new Rectangle(0, 0, width, height), ImageLockMode.WriteOnly, PixelFormat.Format32bppRgb);
        for (int y = 0; y < height; y++)
        {
            long off = (long)(height - 1 - y) * rowBytes;
            if (off + rowBytes > curLen) break;
            CopyMemory(IntPtr.Add(bd.Scan0, y * bd.Stride), IntPtr.Add(ptr, (int)off), rowBytes);
        }
        bmp.UnlockBits(bd);
        if (rot == 180) bmp.RotateFlip(RotateFlipType.Rotate180FlipNone);
        else if (rot == 90) bmp.RotateFlip(RotateFlipType.Rotate90FlipNone);
        else if (rot == 270) bmp.RotateFlip(RotateFlipType.Rotate270FlipNone);
        ok = true;
        return bmp;
    }

    static Bitmap Scale(Bitmap src, int w)
    {
        int h = Math.Max(1, src.Height * w / src.Width);
        var dst = new Bitmap(w, h);
        using (var g = Graphics.FromImage(dst))
        {
            g.InterpolationMode = InterpolationMode.HighQualityBilinear;
            g.DrawImage(src, 0, 0, w, h);
        }
        return dst;
    }

    static void SaveJpeg(Bitmap bmp, string path)
    {
        var codec = ImageCodecInfo.GetImageEncoders().First(c => c.FormatID == ImageFormat.Jpeg.Guid);
        using (var ep = new EncoderParameters(1))
        {
            ep.Param[0] = new EncoderParameter(Encoder.Quality, 85L);
            bmp.Save(path, codec, ep);
        }
    }
}
