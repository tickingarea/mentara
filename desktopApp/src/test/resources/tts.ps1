param(
    [Parameter(Mandatory = $true)][string]$Out,
    [string]$Text = "今天天气很好"
)
Add-Type -AssemblyName System.Speech
$s = New-Object System.Speech.Synthesis.SpeechSynthesizer
$s.SetOutputToWaveFile($Out)
$s.Speak($Text)
$s.Dispose()
Write-Output ("WAV_BYTES=" + (Get-Item $Out).Length)
