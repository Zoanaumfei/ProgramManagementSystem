param(
    [string]$AwsProfile = 'oryzem',
    [string]$RoleName = 'program-management-system-ecs-task-role',
    [string]$TrustPolicyPath = '.\scripts\ecs-task-role-trust-policy.json',
    [string]$PolicyName = 'ProgramManagementSystemReadRdsSecret',
    [string]$PolicyDocumentPath = '.\scripts\grant-rds-secret-read-policy.json'
)

$ErrorActionPreference = 'Stop'
$aws = 'C:\Program Files\Amazon\AWSCLIV2\aws.exe'

if (-not (Test-Path $aws)) {
    throw 'AWS CLI not found at expected path.'
}

if (-not (Test-Path $TrustPolicyPath)) {
    throw "Trust policy not found: $TrustPolicyPath"
}

if (-not (Test-Path $PolicyDocumentPath)) {
    throw "Policy document not found: $PolicyDocumentPath"
}

& $aws iam create-role `
    --profile $AwsProfile `
    --role-name $RoleName `
    --assume-role-policy-document "file://$TrustPolicyPath"

& $aws iam put-role-policy `
    --profile $AwsProfile `
    --role-name $RoleName `
    --policy-name $PolicyName `
    --policy-document "file://$PolicyDocumentPath"
