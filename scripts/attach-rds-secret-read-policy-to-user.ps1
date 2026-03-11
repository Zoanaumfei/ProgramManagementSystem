param(
    [string]$AwsProfile = 'oryzem',
    [string]$UserName = 'oryzem_admin',
    [string]$PolicyName = 'ProgramManagementSystemReadRdsSecret',
    [string]$PolicyDocumentPath = '.\scripts\grant-rds-secret-read-policy.json'
)

$ErrorActionPreference = 'Stop'
$aws = 'C:\Program Files\Amazon\AWSCLIV2\aws.exe'

if (-not (Test-Path $aws)) {
    throw 'AWS CLI not found at expected path.'
}

if (-not (Test-Path $PolicyDocumentPath)) {
    throw "Policy document not found: $PolicyDocumentPath"
}

& $aws iam put-user-policy `
    --profile $AwsProfile `
    --user-name $UserName `
    --policy-name $PolicyName `
    --policy-document "file://$PolicyDocumentPath"
