$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'common.ps1')

Assert-OracleJdk
Assert-JcdkSimulator

$configurator = Join-Path $env:JC_HOME_SIMULATOR 'tools\Configurator.jar'
$backup = "$SimulatorExe.unprovisioned"
if (!(Test-Path $configurator)) {
    throw 'Configurator.jar was not found under JC_HOME_SIMULATOR.'
}
if (Test-Path $backup) {
    throw "The backup already exists: $backup. Use a newly extracted Simulator when provisioning again."
}

Copy-Item $SimulatorExe $backup
try {
    & $JavaExe '-jar' $configurator `
        '-binary' $SimulatorExe `
        '-SCP-keyset' '10' `
        '1111111111111111111111111111111111111111111111111111111111111111' `
        '2222222222222222222222222222222222222222222222222222222222222222' `
        '3333333333333333333333333333333333333333333333333333333333333333' `
        '-global-pin' '01020304050f' '03'
    if ($LASTEXITCODE -ne 0) {
        throw 'Configurator returned an error.'
    }
} catch {
    Copy-Item $backup $SimulatorExe -Force
    Remove-Item $backup -Force
    throw
}

Write-Host 'Oracle Java Card Simulator provisioning succeeded.'
Write-Host "Original binary: $backup"
