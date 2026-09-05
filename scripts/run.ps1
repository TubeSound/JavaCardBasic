param(
    [string] $Connection = 'socket:localhost:9025'
)

$ErrorActionPreference = 'Stop'
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
. (Join-Path $PSScriptRoot 'common.ps1')

Assert-OracleJdk
Assert-JcdkSimulator

$deliverables = Join-Path $ProjectRoot 'build\deliverables'
if (!(Test-Path $deliverables)) {
    throw 'CAP file is missing. Run .\scripts\build.ps1 first.'
}
$capFile = Get-ChildItem $deliverables -Filter '*.cap' -File -Recurse |
    Select-Object -First 1
if ($null -eq $capFile) {
    throw 'CAP file is missing. Run .\scripts\build.ps1 first.'
}

$clientClasses = Join-Path $ProjectRoot 'build\client-classes'
$mainClass = 'io.github.tubesound.javacardbasic.client.HelloClient'
$modulePath = "$AmDir;$ComDir"
$classPath = "$clientClasses;$AmJar;$SocketJar"
$properties = Join-Path $ProjectRoot 'config\client.config.properties'

& $JavaExe '-cp' $classPath `
    '--module-path' $modulePath '--add-modules' 'ALL-MODULE-PATH' `
    $mainClass `
    "--cap=$($capFile.FullName)" `
    "--props=$properties" `
    "--host=$Connection"
exit $LASTEXITCODE
