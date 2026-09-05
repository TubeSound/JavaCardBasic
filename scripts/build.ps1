$ErrorActionPreference = 'Stop'
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
. (Join-Path $PSScriptRoot 'common.ps1')

Assert-OracleJdk
Assert-JcdkTools
Assert-JcdkSimulator

$BuildDir = Join-Path $ProjectRoot 'build'
$AppletClasses = Join-Path $BuildDir 'applet-classes'
$ClientClasses = Join-Path $BuildDir 'client-classes'
$Deliverables = Join-Path $BuildDir 'deliverables'
if (Test-Path $BuildDir) {
    Remove-Item $BuildDir -Recurse -Force
}
New-Item $AppletClasses, $ClientClasses, $Deliverables -ItemType Directory -Force | Out-Null

$appletSources = @(Get-ChildItem (Join-Path $ProjectRoot 'src\applet\java') -Filter '*.java' -Recurse |
    Sort-Object FullName | ForEach-Object FullName)
$clientSources = @(Get-ChildItem (Join-Path $ProjectRoot 'src\client\java') -Filter '*.java' -Recurse |
    Sort-Object FullName | ForEach-Object FullName)

Write-Host 'Compiling Java Card applet with Oracle JDK 25...'
# Oracle Converter input requires Java 8 class files (major version 52).
& $JavacExe '-g' '-d' $AppletClasses '-cp' $CardApi `
    '--release' '8' @appletSources
if ($LASTEXITCODE -ne 0) {
    throw 'Applet compilation failed.'
}

Write-Host 'Converting and verifying CAP with Oracle JCDK Tools 26.0...'
& $Converter `
    '-classdir' $AppletClasses `
    '-d' $Deliverables `
    '-target' '3.2.0' `
    '-applet' '0xf0:0x54:0x55:0x42:0x45:0x01:0x01' `
    'io.github.tubesound.javacardbasic.card.HelloApplet' `
    'io.github.tubesound.javacardbasic.card' `
    '0xf0:0x54:0x55:0x42:0x45:0x01' `
    '1.0'
if ($LASTEXITCODE -ne 0) {
    throw 'CAP conversion failed.'
}

$capFile = Get-ChildItem $Deliverables -Filter '*.cap' -File -Recurse | Select-Object -First 1
if ($null -eq $capFile) {
    throw 'Converter completed without producing a CAP file.'
}

Write-Host 'Compiling Oracle Simulator client...'
$modulePath = "$AmDir;$ComDir"
$classPath = "$AmJar;$SocketJar"
& $JavacExe '-g' '-d' $ClientClasses '-cp' $classPath `
    '--module-path' $modulePath '--add-modules' 'ALL-MODULE-PATH' @clientSources
if ($LASTEXITCODE -ne 0) {
    throw 'Client compilation failed.'
}

Write-Host 'Build succeeded.'
Write-Host "CAP: $($capFile.FullName)"
