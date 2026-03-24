param(
    [string]$AwsProfile = 'oryzem',
    [string]$RoleName = 'program-management-system-ecs-task-role',
    [string]$PolicyName = 'ProgramManagementSystemDocumentStorage',
    [string]$PolicyDocumentPath = '.\scripts\grant-document-storage-policy.json'
)

$ErrorActionPreference = 'Stop'
$aws = 'C:\Program Files\Amazon\AWSCLIV2\aws.exe'

if (-not (Test-Path $aws)) {
    throw 'AWS CLI not found at expected path.'
}

if (-not (Test-Path $PolicyDocumentPath)) {
    throw "Policy document not found: $PolicyDocumentPath"
}

& $aws iam put-role-policy `
    --profile $AwsProfile `
    --role-name $RoleName `
    --policy-name $PolicyName `
    --policy-document "file://$PolicyDocumentPath"

if ($LASTEXITCODE -ne 0) {
    throw "Unable to attach the document storage policy '$PolicyName' to role '$RoleName'."
}
