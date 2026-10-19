# 获取 SenseVoice 模型（228MB + tokens），放入 Android assets（构建前执行一次）
$ErrorActionPreference = "Stop"
$dst = Join-Path $PSScriptRoot "..\androidApp\src\main\assets\sensevoice"
New-Item -ItemType Directory -Force -Path $dst | Out-Null
$base = "https://hf-mirror.com/csukuangfj/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/resolve/main/"
foreach ($f in @("model.int8.onnx", "tokens.txt")) {
    $out = Join-Path $dst $f
    if (Test-Path $out) { Write-Host "exists: $f"; continue }
    Write-Host "downloading $f ..."
    Invoke-WebRequest -Uri ($base + $f) -OutFile $out -TimeoutSec 900
}
Write-Host "done -> $dst"