param(
    [string]$AwsProfile = 'oryzem',
    [string]$UserName = 'oryzem_admin',
    [string]$PolicyName = 'ProgramManagementSystemEcsResetRunTask',
    [string]$PolicyDocumentPath = '.\scripts\grant-ecs-reset-run-task-policy.json'
)

$ErrorActionPreference = 'Stop'
$aws = 'C:\Program Files\Amazon\AWSCLIV2\aws.exe'
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path $scriptRoot -Parent

if (-not [System.IO.Path]::IsPathRooted($PolicyDocumentPath)) {
    $resolvedPolicyDocumentPath = Resolve-Path -Path $PolicyDocumentPath -ErrorAction SilentlyContinue
    if ($resolvedPolicyDocumentPath) {
        $PolicyDocumentPath = $resolvedPolicyDocumentPath.Path
    } else {
        $trimmedPolicyDocumentPath = $PolicyDocumentPath -replace '^[.\\/]+', ''
        $PolicyDocumentPath = Join-Path $repoRoot $trimmedPolicyDocumentPath
    }
}

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
