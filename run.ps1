
$workspace = Split-Path -Parent $MyInvocation.MyCommand.Definition
$bin = Join-Path $workspace 'bin'

$javaPath = 'C:\Program Files\Java\jdk-25\bin\java.exe'
if (-Not (Test-Path $javaPath)) {
    Write-Error "Java executable not found at $javaPath. Update the path in run.ps1 or install JDK 25."
    exit 1
}

$cp = "$bin"
& $javaPath -cp $cp main.MainClass
