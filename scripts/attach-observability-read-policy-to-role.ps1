param(
    [string]$AwsProfile = 'oryzem',
    [string]$RoleName,
    [string]$PolicyName = 'ProgramManagementSystemObservabilityRead',
    [string]$PolicyDocumentPath = '.\scripts\grant-observability-read-policy.json'
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
