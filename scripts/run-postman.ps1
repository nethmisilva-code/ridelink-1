# Runs the full Postman collection with Newman (needs Node.js) and saves a JUnit report in .\reports
$Root = Split-Path -Parent $PSScriptRoot
New-Item -ItemType Directory -Force -Path (Join-Path $Root "reports") | Out-Null
Push-Location $Root
npx --yes newman run postman/RideLink.postman_collection.json -e postman/RideLink-Local.postman_environment.json `
    --reporters cli,junit --reporter-junit-export reports/newman-results.xml
Pop-Location
