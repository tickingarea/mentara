param(
    [Parameter(Mandatory = $true)][string]$Wav,
    [string]$Out,
    [string]$Culture = "zh-CN"
)

# 使用 Windows 内置语音识别（System.Speech）进行本地转写，全程离线。
# 自行解析 WAV 的 fmt/data 块并以原始 PCM 喂给识别引擎，兼容带 JUNK 等额外块的 WAV。
# 输出：List<TranscriptSegment> 的 JSON，字段 start/end/text/speaker。

Add-Type -AssemblyName System.Speech

if (-not (Test-Path $Wav)) {
    Write-Error "WAV not found: $Wav"
    exit 2
}

$bytes = [System.IO.File]::ReadAllBytes($Wav)
if ($bytes.Length -lt 44) {
    Write-Error "WAV too small: $Wav"
    exit 2
}

$pos = 12
$audioFormat = $null
$channels = 0
$sampleRate = 0
$bitsPerSample = 0
$dataOffset = -1
$dataLength = 0

while ($pos -le $bytes.Length - 8) {
    $id = [System.Text.Encoding]::ASCII.GetString($bytes, $pos, 4)
    $size = [BitConverter]::ToInt32($bytes, $pos + 4)
    if ($id -eq 'fmt ') {
        $audioFormat = [BitConverter]::ToInt16($bytes, $pos + 8)
        $channels = [BitConverter]::ToInt16($bytes, $pos + 10)
        $sampleRate = [BitConverter]::ToInt32($bytes, $pos + 12)
        $bitsPerSample = [BitConverter]::ToInt16($bytes, $pos + 22)
    } elseif ($id -eq 'data') {
        $dataOffset = $pos + 8
        $dataLength = $size
        break
    }
    $pos += 8 + $size + ($size % 2)
}

if ($null -eq $audioFormat -or $dataOffset -lt 0) {
    Write-Error "WAV missing fmt/data chunk: $Wav"
    exit 2
}
if ($audioFormat -ne 1) {
    Write-Error ("Unsupported audio format " + $audioFormat + " (only PCM). Convert to 16kHz mono PCM WAV.")
    exit 3
}

$bitsEnum = if ($bitsPerSample -eq 8) { [System.Speech.AudioFormat.AudioBitsPerSample]::Eight } else { [System.Speech.AudioFormat.AudioBitsPerSample]::Sixteen }
$chEnum = if ($channels -eq 1) { [System.Speech.AudioFormat.AudioChannel]::Mono } else { [System.Speech.AudioFormat.AudioChannel]::Stereo }
$audioFormatInfo = New-Object System.Speech.AudioFormat.SpeechAudioFormatInfo($sampleRate, $bitsEnum, $chEnum)

$data = New-Object byte[] $dataLength
[Array]::Copy($bytes, $dataOffset, $data, 0, $dataLength)
$stream = New-Object System.IO.MemoryStream
$stream.Write($data, 0, $data.Length)
$stream.Position = 0

$ci = New-Object System.Globalization.CultureInfo($Culture)
try {
    $engine = New-Object System.Speech.Recognition.SpeechRecognitionEngine($ci)
} catch {
    Write-Error "No System.Speech recognizer for culture '$Culture': $($_.Exception.Message)"
    exit 4
}

$engine.LoadGrammar((New-Object System.Speech.Recognition.DictationGrammar))
$engine.SetInputToAudioStream($stream, $audioFormatInfo)

$segments = New-Object System.Collections.ArrayList
$cursor = 0.0
$i = 0
while ($i -lt 10000) {
    $r = $null
    try { $r = $engine.Recognize() } catch { break }
    if ($null -eq $r) { break }

    $dur = 0.0
    try { $dur = $r.Audio.Duration.TotalSeconds } catch { }

    $null = $segments.Add([pscustomobject]@{
        start   = $cursor
        end     = $cursor + $dur
        text    = $r.Text
        speaker = $null
    })
    $cursor += $dur
    $i++
}
$engine.Dispose()
$stream.Dispose()

$json = if ($segments.Count -eq 0) { "[]" } else { $segments | ConvertTo-Json -Depth 4 }
if (-not $json.TrimStart().StartsWith("[")) { $json = "[$json]" }

$utf8NoBom = New-Object System.Text.UTF8Encoding $false
if ($Out) {
    [System.IO.File]::WriteAllText($Out, $json, $utf8NoBom)
} else {
    Write-Output $json
}
