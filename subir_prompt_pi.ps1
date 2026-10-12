# Sube el prompt de Gemma (fallback_llm.py) a teo@192.168.0.148 y reinicia app.py.
# La clave no se guarda: PI_SSH_PASS en el entorno, o te la pide.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$py = Join-Path $root ".cursor\skills\uploading-to-raspberry-pi\scripts\upload_prompt_to_pi.py"
if (-not $env:PI_SSH_PASS) {
    $env:PI_SSH_PASS = Read-Host "Clave SSH de teo (no se guarda)"
}
python $py
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
