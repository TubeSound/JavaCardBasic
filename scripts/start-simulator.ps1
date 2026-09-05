$ErrorActionPreference = 'Stop'
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
. (Join-Path $PSScriptRoot 'common.ps1')

Assert-JcdkSimulator

$config = Join-Path $ProjectRoot 'config\simulator.conf'
& $SimulatorExe "-f=$config"
exit $LASTEXITCODE
