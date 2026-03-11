param(
    [string]$AwsProfile = 'oryzem',
    [string]$AwsRegion = 'sa-east-1',
    [string]$RepositoryName = 'oryzem-backend-dev'
)

$ErrorActionPreference = 'Stop'
$aws = 'C:\Program Files\Amazon\AWSCLIV2\aws.exe'

if (-not (Test-Path $aws)) {
    throw 'AWS CLI not found at expected path.'
}

& $aws ecr describe-repositories `
    --profile $AwsProfile `
    --region $AwsRegion `
    --repository-names $RepositoryName *> $null

if ($LASTEXITCODE -ne 0) {
    & $aws ecr create-repository `
        --profile $AwsProfile `
        --region $AwsRegion `
        --repository-name $RepositoryName `
        --image-scanning-configuration scanOnPush=true
}
