$ErrorActionPreference = "Stop"

$databaseUrl = "https://final-project-app-cb2dd-default-rtdb.asia-southeast1.firebasedatabase.app"
$seedPath = Join-Path $PSScriptRoot "firebase_seed.json"

if (-not (Test-Path $seedPath)) {
    throw "Seed file not found: $seedPath"
}

$seed = Get-Content $seedPath -Raw
$payload = ($seed | ConvertFrom-Json).static | ConvertTo-Json -Depth 20

Invoke-RestMethod `
    -Method Put `
    -Uri "$databaseUrl/static.json" `
    -ContentType "application/json; charset=utf-8" `
    -Body $payload

Write-Host "Uploaded static seed to $databaseUrl/static"
