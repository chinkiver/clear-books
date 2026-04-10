#!/usr/bin/env pwsh
# Clear Books Local Build Script (Windows PowerShell)
# Usage: .\build.ps1
# Note: Can be executed from any directory

$ErrorActionPreference = "Stop"

# Get project root (two levels up from script location)
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $ScriptDir)

# Switch to project root
Set-Location $ProjectRoot

Write-Host "=== Building Clear Books ===" -ForegroundColor Cyan
Write-Host "Project: $ProjectRoot" -ForegroundColor Gray

# 1. Build frontend
Write-Host "[1/3] Building frontend..." -ForegroundColor Yellow
Set-Location frontend
& npm install
if ($LASTEXITCODE -ne 0) { throw "npm install failed" }

& npm run build
if ($LASTEXITCODE -ne 0) { throw "npm build failed" }
Set-Location ..

# 2. Copy to backend static
Write-Host "[2/3] Merging frontend & backend..." -ForegroundColor Yellow
$staticDir = "backend/src/main/resources/static"
if (!(Test-Path $staticDir)) {
    New-Item -ItemType Directory -Force -Path $staticDir | Out-Null
}
Remove-Item -Path "$staticDir/*" -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item -Path "frontend/dist/*" -Destination $staticDir -Recurse -Force

# 3. Package JAR
Write-Host "[3/3] Packaging JAR..." -ForegroundColor Yellow
Set-Location backend
& mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) { throw "Maven package failed" }
Set-Location ..

# Show result
Write-Host ""
Write-Host "=== Build Successful ===" -ForegroundColor Green
$jarFile = Get-ChildItem "backend/target/personal-accounting-*.jar" | Select-Object -First 1
if ($jarFile) {
    Write-Host "Output: $($jarFile.FullName)" -ForegroundColor Green
    Write-Host "Size: $([math]::Round($jarFile.Length/1MB, 2)) MB" -ForegroundColor Green
}

Write-Host ""
Write-Host "Upload these files to your server:" -ForegroundColor Cyan
Write-Host "  - backend/target/personal-accounting-*.jar" -ForegroundColor White
Write-Host "  - scripts/server/start.sh" -ForegroundColor White
Write-Host "  - scripts/server/stop.sh" -ForegroundColor White
Write-Host "  - .env (config file)" -ForegroundColor White
