param(
    [string]$AwsProfile = 'oryzem',
    [string]$RoleName,
    [string]$PolicyName = 'ProgramManagementSystemManageCognitoUsers',
    [string]$PolicyDocumentPath = '.\scripts\grant-cognito-user-management-policy.json'
)

$ErrorActionPreference = 'Stop'
$aws = 'C:\Program Files\Amazon\AWSCLIV2\aws.exe'

if (-not (Test-Path $aws)) {
    throw 'AWS CLI not found at expected path.'
}

if (-not $RoleName) {
    throw 'RoleName is required.'
}

if (-not (Test-Path $PolicyDocumentPath)) {
    throw "Policy document not found: $PolicyDocumentPath"
}

& $aws iam put-role-policy `
    --profile $AwsProfile `
    --role-name $RoleName `
    --policy-name $PolicyName `
    --policy-document "file://$PolicyDocumentPath"
