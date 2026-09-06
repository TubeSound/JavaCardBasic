#Requires -Version 5.1
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

try {
    if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        throw 'Set JAVA_HOME to Oracle JDK 25. See docs/00-windows-setup.md.'
    }
    if ([string]::IsNullOrWhiteSpace($env:JC_HOME_TOOLS)) {
        throw 'Set JC_HOME_TOOLS to Oracle Java Card Development Kit Tools 26.0. See docs/02-build-cap.md.'
    }
    $Javac = Join-Path $env:JAVA_HOME 'bin\javac.exe'
    $Converter = Join-Path $env:JC_HOME_TOOLS 'bin\converter.bat'
    $CardApi = Join-Path $env:JC_HOME_TOOLS 'lib\api_classic-3.0.5.jar'
    foreach ($requiredFile in @($Javac, $Converter, $CardApi)) {
        if (!(Test-Path -LiteralPath $requiredFile -PathType Leaf)) {
            throw "Required file missing: $requiredFile"
        }
    }

    $compilerVersion = & $Javac '-version'
    if ($LASTEXITCODE -ne 0 -or "$compilerVersion" -notmatch '^javac 25(?:\.|$)') {
        throw "CAP build requires the documented JDK 25 toolchain. Found: $compilerVersion"
    }

    Push-Location $ProjectRoot
    try {
        # Run the same simulator tests as the normal development loop.
        & (Join-Path $ProjectRoot 'mvnw.cmd') '--batch-mode' '--no-transfer-progress' 'clean' 'test'
        if ($LASTEXITCODE -ne 0) {
            throw 'Maven tests failed; CAP conversion was not started.'
        }

        $AppletClasses = Join-Path $ProjectRoot 'target\cap-classes'
        $OutputDir = Join-Path $ProjectRoot 'target\cap'
        New-Item -ItemType Directory -Force -Path $AppletClasses, $OutputDir | Out-Null
        $sources = @(Get-ChildItem -LiteralPath (Join-Path $ProjectRoot 'src\main\java') `
            -Recurse -File -Filter '*.java' | Sort-Object FullName | ForEach-Object FullName)
        if ($sources.Count -eq 0) {
            throw 'No applet sources found in src/main/java.'
        }

        # Compile a separate set of class files against the actual target API.
        # Never convert target/classes built against the simulator's bundled API.
        & $Javac '--release' '8' '-g' '-encoding' 'UTF-8' '-classpath' $CardApi `
            '-d' $AppletClasses @sources
        if ($LASTEXITCODE -ne 0) {
            throw 'Compilation against the Oracle Java Card 3.0.5 API failed.'
        }

        # Tools 26.0 selects its built-in platform export files from -target.
        # Keep the default input/output verification enabled.
        $converterArgs = @(
            '-classdir', $AppletClasses,
            '-d', $OutputDir,
            '-target', '3.0.5',
            '-out', 'CAP', 'EXP', 'JCA',
            '-applet', '0xf0:0x54:0x55:0x42:0x45:0x01:0x01',
            'io.github.tubesound.javacardbasic.card.SampleApplet',
            '-applet', '0xf0:0x54:0x55:0x42:0x45:0x02:0x01',
            'io.github.tubesound.javacardbasic.card.FileSystemApplet',
            'io.github.tubesound.javacardbasic.card',
            '0xf0:0x54:0x55:0x42:0x45:0x01', '1.0'
        )
        & $Converter @converterArgs
        if ($LASTEXITCODE -ne 0) {
            throw 'Oracle CAP conversion or verification failed.'
        }

        $PackageOutput = Join-Path $OutputDir 'io\github\tubesound\javacardbasic\card\javacard'
        foreach ($extension in @('cap', 'exp', 'jca')) {
            $artifact = Join-Path $PackageOutput "card.$extension"
            if (!(Test-Path -LiteralPath $artifact -PathType Leaf) -or
                    (Get-Item -LiteralPath $artifact).Length -eq 0) {
                throw "Converter did not produce a non-empty output: $artifact"
            }
            Write-Host "Created: $artifact"
        }
    } finally {
        Pop-Location
    }
} catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 1
}

