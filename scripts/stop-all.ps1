# Stops the RideLink services started by start-all.ps1 (and anything still listening on 8081-8084).
$Root = Split-Path -Parent $PSScriptRoot
$Logs = Join-Path $Root "logs"
Get-ChildItem $Logs -Filter "*.pid" -ErrorAction SilentlyContinue | ForEach-Object {
    $procId = Get-Content $_.FullName
    Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
    Remove-Item $_.FullName
    Write-Host "Stopped $($_.BaseName) (pid $procId)"
}
foreach ($port in 8081, 8082, 8083, 8084) {
    Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | ForEach-Object {
        Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
        Write-Host "Stopped process on port $port"
    }
}
