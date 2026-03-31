param(
    [string]$AwsProfile = 'oryzem',
    [string]$AwsRegion = 'sa-east-1',
    [string]$SecretId = 'program-management-system/rds/master',
    [string]$BootstrapEmail = 'vanderson.verza@gmail.com',
    [string]$BootstrapDisplayName = 'Vanderson Verza'
)

$ErrorActionPreference = 'Stop'

$aws = 'C:\Program Files\Amazon\AWSCLIV2\aws.exe'
if (-not (Test-Path $aws)) {
    throw 'AWS CLI not found at expected path.'
}

& $aws secretsmanager get-secret-value `
    --profile $AwsProfile `
    --region $AwsRegion `
    --secret-id $SecretId `
    --query Name `
    --output text | Out-Null

if ($LASTEXITCODE -ne 0) {
    throw "Unable to read the secret '$SecretId' in region '$AwsRegion'."
}

$credentialExportJson = & $aws configure export-credentials `
    --profile $AwsProfile `
    --format process

if ($LASTEXITCODE -ne 0 -or -not $credentialExportJson) {
    throw "Unable to export AWS credentials for profile '$AwsProfile'."
}

$awsCredentials = $credentialExportJson | ConvertFrom-Json
if (-not $awsCredentials.AccessKeyId -or -not $awsCredentials.SecretAccessKey) {
    throw "The exported AWS credentials for profile '$AwsProfile' are incomplete."
}

$env:AWS_ACCESS_KEY_ID = $awsCredentials.AccessKeyId
$env:AWS_SECRET_ACCESS_KEY = $awsCredentials.SecretAccessKey
if ($awsCredentials.SessionToken) {
    $env:AWS_SESSION_TOKEN = $awsCredentials.SessionToken
} elseif ($env:AWS_SESSION_TOKEN) {
    Remove-Item Env:AWS_SESSION_TOKEN
}

if ($env:AWS_PROFILE) {
    Remove-Item Env:AWS_PROFILE
}

$env:AWS_REGION = $AwsRegion
$env:SPRING_PROFILES_ACTIVE = 'rds'
$env:SPRING_MAIN_WEB_APPLICATION_TYPE = 'none'
$env:DB_SECRET_ID = $SecretId
$env:DB_SSL_MODE = 'require'
$env:APP_BOOTSTRAP_SEED_DATA = 'false'
$env:APP_BOOTSTRAP_INTERNAL_ADMIN_ENABLED = 'true'
$env:APP_BOOTSTRAP_INTERNAL_ADMIN_EMAIL = $BootstrapEmail
$env:APP_BOOTSTRAP_INTERNAL_ADMIN_DISPLAY_NAME = $BootstrapDisplayName
$env:APP_BOOTSTRAP_INTERNAL_ADMIN_PRUNE_OTHER_INTERNAL_USERS = 'true'
$env:APP_MAINTENANCE_RESET_ENABLED = 'true'
$env:APP_MAINTENANCE_RESET_CONFIRMATION = 'RESET_RDS_SAFE'
$env:APP_MAINTENANCE_RESET_EXIT_AFTER_RESET = 'true'

Write-Host 'RDS maintenance reset environment loaded.'
Write-Host "AWS_PROFILE_SOURCE=$AwsProfile"
Write-Host "AWS_REGION=$($env:AWS_REGION)"
Write-Host "DB_SECRET_ID=$($env:DB_SECRET_ID)"
Write-Host "BOOTSTRAP_EMAIL=$($env:APP_BOOTSTRAP_INTERNAL_ADMIN_EMAIL)"
Write-Host 'The application will reset all application data and recreate only the minimal bootstrap.'
Write-Host 'Nothing will happen unless the confirmation token and maintenance flags are present.'

& .\mvnw.cmd spring-boot:run
