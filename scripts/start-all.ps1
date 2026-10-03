# Builds (unless -SkipBuild) and starts all four RideLink services in the background,
# then waits until each one reports healthy. Logs go to .\logs\<service>.log
param([switch]$SkipBuild)
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Logs = Join-Path $Root "logs"
New-Item -ItemType Directory -Force -Path $Logs | Out-Null

# Start order follows the dependencies: Ride calls Driver and Fare; Driver calls Account.
$Services = @(
    @{ Name = "account-service";         Port = 8081 },
    @{ Name = "driver-vehicle-service";  Port = 8082 },
    @{ Name = "fare-payment-service";    Port = 8084 },
    @{ Name = "ride-management-service"; Port = 8083 }
)

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "Java was not found. Install JDK 21 and make sure 'java' is on PATH." -ForegroundColor Red; exit 1
}
$envFile = Join-Path $Root ".env"
if (-not (Test-Path $envFile)) {
    Copy-Item (Join-Path $Root ".env.example") $envFile
    Write-Host "Created .env from .env.example (edit it to use MongoDB Atlas)." -ForegroundColor Yellow
}

foreach ($s in $Services) {
    $dir = Join-Path $Root $s.Name
    if (-not $SkipBuild) {
        Write-Host "Building $($s.Name) ..." -ForegroundColor Cyan
        Push-Location $dir
        & .\mvnw.cmd -q -DskipTests package
        $code = $LASTEXITCODE
        Pop-Location
        if ($code -ne 0) { Write-Host "Build failed for $($s.Name)" -ForegroundColor Red; exit 1 }
    }
    $jar = Get-ChildItem (Join-Path $dir "target") -Filter "*.jar" | Where-Object { $_.Name -notlike "*.original" } | Select-Object -First 1
    if (-not $jar) { Write-Host "No jar for $($s.Name). Run without -SkipBuild." -ForegroundColor Red; exit 1 }
    $p = Start-Process -FilePath "java" -ArgumentList @("-jar", "`"$($jar.FullName)`"") -WorkingDirectory $dir `
        -RedirectStandardOutput (Join-Path $Logs "$($s.Name).log") -RedirectStandardError (Join-Path $Logs "$($s.Name).err.log") `
        -WindowStyle Hidden -PassThru
    $p.Id | Set-Content (Join-Path $Logs "$($s.Name).pid")
    Write-Host "Started $($s.Name) (pid $($p.Id)) on port $($s.Port)"
}

Write-Host "Waiting for services to become healthy ..."
foreach ($s in $Services) {
    $ok = $false
    for ($i = 0; $i -lt 90; $i++) {
        try {
            $h = Invoke-RestMethod -Uri "http://localhost:$($s.Port)/actuator/health" -TimeoutSec 3
            if ($h.status -eq "UP") { $ok = $true; break }
        } catch { }
        Start-Sleep -Seconds 2
    }
    if ($ok) { Write-Host ("  {0,-26} UP    http://localhost:{1}/swagger-ui.html" -f $s.Name, $s.Port) -ForegroundColor Green }
    else { Write-Host ("  {0,-26} NOT UP  see logs\{0}.log (is MongoDB running?)" -f $s.Name) -ForegroundColor Red }
}
Write-Host "Stop everything with: stop-all.bat"
