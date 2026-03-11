param(
    [string]$AwsProfile = 'oryzem',
    [string]$RoleName = 'program-management-system-ecs-execution-role',
    [string]$TrustPolicyPath = '.\scripts\ecs-execution-role-trust-policy.json'
)

$ErrorActionPreference = 'Stop'
$aws = 'C:\Program Files\Amazon\AWSCLIV2\aws.exe'
$managedPolicyArn = 'arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy'

if (-not (Test-Path $aws)) {
    throw 'AWS CLI not found at expected path.'
}

if (-not (Test-Path $TrustPolicyPath)) {
    throw "Trust policy not found: $TrustPolicyPath"
}

& $aws iam create-role `
    --profile $AwsProfile `
    --role-name $RoleName `
    --assume-role-policy-document "file://$TrustPolicyPath"

& $aws iam attach-role-policy `
    --profile $AwsProfile `
    --role-name $RoleName `
    --policy-arn $managedPolicyArn
