param(
    [string]$AwsProfile = 'oryzem',
    [string]$AwsRegion = 'sa-east-1',
    [string]$SecretId = 'program-management-system/rds/master',
    [switch]$SkipMaven,
    [string]$MavenGoal = 'spring-boot:run'
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
$env:DB_SECRET_ID = $SecretId
$env:DB_SSL_MODE = 'require'

Write-Host 'RDS environment loaded.'
Write-Host "AWS_PROFILE_SOURCE=$AwsProfile"
Write-Host "AWS_REGION=$($env:AWS_REGION)"
Write-Host "DB_SECRET_ID=$($env:DB_SECRET_ID)"
Write-Host 'Datasource password will be resolved by the Spring Boot application from Secrets Manager.'

if (-not $SkipMaven) {
    & .\mvnw.cmd $MavenGoal
}
