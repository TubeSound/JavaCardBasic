$ErrorActionPreference = 'Stop'

function Assert-OracleJdk {
    if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        throw 'JAVA_HOME must point to Oracle JDK 25.'
    }
    $script:JavaExe = Join-Path $env:JAVA_HOME 'bin\java.exe'
    $script:JavacExe = Join-Path $env:JAVA_HOME 'bin\javac.exe'
    if (!(Test-Path $script:JavaExe) -or !(Test-Path $script:JavacExe)) {
        throw 'JAVA_HOME must point to a 64-bit Oracle JDK 25 installation.'
    }

    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $settings = (& $script:JavaExe '-XshowSettings:properties' '-version' 2>&1 | Out-String)
        $javaExitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    if ($javaExitCode -ne 0 -or $settings -notmatch 'java\.vendor\s*=\s*Oracle Corporation') {
        throw "This project requires Oracle JDK. Detected Java settings:`n$settings"
    }
    if ($settings -notmatch 'java\.specification\.version\s*=\s*25') {
        throw 'This project requires Oracle JDK 25.'
    }
    if ($settings -notmatch 'sun\.arch\.data\.model\s*=\s*64') {
        throw 'This project requires the 64-bit Oracle JDK.'
    }
}

function Assert-JcdkTools {
    if ([string]::IsNullOrWhiteSpace($env:JC_HOME_TOOLS)) {
        throw 'JC_HOME_TOOLS must point to Oracle JCDK Tools 26.0.'
    }
    $script:CardApi = Join-Path $env:JC_HOME_TOOLS 'lib\api_classic-3.2.0.jar'
    $script:Converter = Join-Path $env:JC_HOME_TOOLS 'bin\converter.bat'
    if (!(Test-Path $script:CardApi) -or !(Test-Path $script:Converter)) {
        throw 'Required Oracle JCDK Tools files were not found under JC_HOME_TOOLS.'
    }
}

function Assert-JcdkSimulator {
    if ([string]::IsNullOrWhiteSpace($env:JC_HOME_SIMULATOR)) {
        throw 'JC_HOME_SIMULATOR must point to Oracle JCDK Simulator 26.0.'
    }
    $script:AmDir = Join-Path $env:JC_HOME_SIMULATOR 'client\AMService'
    $script:ComDir = Join-Path $env:JC_HOME_SIMULATOR 'client\COMService'
    $script:AmJar = Join-Path $script:AmDir 'amservice.jar'
    $script:SocketJar = Join-Path $script:ComDir 'socketprovider.jar'
    $script:SimulatorExe = Join-Path $env:JC_HOME_SIMULATOR 'runtime\bin\jcsw.exe'
    if (!(Test-Path $script:AmJar) -or !(Test-Path $script:SocketJar) -or !(Test-Path $script:SimulatorExe)) {
        throw 'Required Oracle Simulator files were not found under JC_HOME_SIMULATOR.'
    }
}
