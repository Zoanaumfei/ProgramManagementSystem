param(
    [string]$AwsProfile = 'oryzem',
    [string]$AwsRegion = 'sa-east-1',
    [string]$SecretId = 'program-management-system/rds/master',
    [string]$BootstrapEmail = 'vanderson.verza@gmail.com',
    [string]$BootstrapDisplayName = 'Vanderson Verza',
    [string]$ConfirmToken
)

$ErrorActionPreference = 'Stop'
$requiredToken = 'RESET_MVP_PLATFORM_DATA'

if ($ConfirmToken -ne $requiredToken) {
    throw "Refusing to clear MVP data. Re-run with -ConfirmToken $requiredToken after confirming this is the intended RDS-backed environment."
}

Write-Host 'Clearing MVP platform data.'
Write-Host 'This keeps the schema, baseline catalogs/templates, the internal-core organization, and the configured INTERNAL ADMIN.'
Write-Host "Bootstrap admin: $BootstrapEmail"

powershell -ExecutionPolicy Bypass -File "$PSScriptRoot\reset-rds-safe.ps1" `
    -AwsProfile $AwsProfile `
    -AwsRegion $AwsRegion `
    -SecretId $SecretId `
    -BootstrapEmail $BootstrapEmail `
    -BootstrapDisplayName $BootstrapDisplayName
