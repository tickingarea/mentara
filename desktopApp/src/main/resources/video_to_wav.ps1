param(
    [Parameter(Mandatory = $true)][string]$Video,
    [Parameter(Mandatory = $true)][string]$Wav
)

# 用 Windows 内置 MediaTranscoder（WinRT）把视频音轨提取为
# 16kHz / 单声道 / 16-bit PCM WAV，供 System.Speech 转写。
# 全程离线，无需 ffmpeg。

Add-Type -AssemblyName System.Runtime.WindowsRuntime | Out-Null

$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
})[0]
$asTaskActionProgress = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncActionWithProgress`1'
})[0]

function AwaitOp($op, $t) {
    $task = $asTaskGeneric.MakeGenericMethod($t).Invoke($null, @($op))
    $task.Wait(-1) | Out-Null
    $task.Result
}
function AwaitActionProgress($act, $t) {
    $task = $asTaskActionProgress.MakeGenericMethod($t).Invoke($null, @($act))
    $task.Wait(-1) | Out-Null
}

[Windows.Storage.StorageFile, Windows.Storage, ContentType = WindowsRuntime] | Out-Null
[Windows.Storage.StorageFolder, Windows.Storage, ContentType = WindowsRuntime] | Out-Null
[Windows.Media.Transcoding.MediaTranscoder, Windows.Media.Transcoding, ContentType = WindowsRuntime] | Out-Null
[Windows.Media.MediaProperties.MediaEncodingProfile, Windows.Media.MediaProperties, ContentType = WindowsRuntime] | Out-Null
[Windows.Media.MediaProperties.AudioEncodingProperties, Windows.Media.MediaProperties, ContentType = WindowsRuntime] | Out-Null

if (-not (Test-Path $Video)) {
    Write-Error "Video not found: $Video"
    exit 2
}

$inFile = AwaitOp ([Windows.Storage.StorageFile]::GetFileFromPathAsync($Video)) ([Windows.Storage.StorageFile])
$wavDir = [System.IO.Path]::GetDirectoryName($Wav)
$wavName = [System.IO.Path]::GetFileName($Wav)
$folder = AwaitOp ([Windows.Storage.StorageFolder]::GetFolderFromPathAsync($wavDir)) ([Windows.Storage.StorageFolder])
$outFile = AwaitOp ($folder.CreateFileAsync($wavName, [Windows.Storage.CreationCollisionOption]::ReplaceExisting)) ([Windows.Storage.StorageFile])

$profile = [Windows.Media.MediaProperties.MediaEncodingProfile]::CreateWav([Windows.Media.MediaProperties.AudioEncodingQuality]::Medium)
$pcm = [Windows.Media.MediaProperties.AudioEncodingProperties]::CreatePcm(16000, 1, 16)
$profile.Audio = $pcm

$transcoder = [Windows.Media.Transcoding.MediaTranscoder]::new()
$prep = AwaitOp ($transcoder.PrepareFileTranscodeAsync($inFile, $outFile, $profile)) ([Windows.Media.Transcoding.PrepareTranscodeResult])

if (-not $prep.CanTranscode) {
    Write-Output ("CANNOT_TRANSCODE reason=" + $prep.FailureReason)
    exit 3
}

AwaitActionProgress ($prep.TranscodeAsync()) ([Double])
Write-Output ("WAV_BYTES=" + (Get-Item $Wav).Length)
