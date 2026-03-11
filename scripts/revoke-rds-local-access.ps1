param(
    [string]$AwsProfile = 'oryzem',
    [string]$AwsRegion = 'sa-east-1',
    [string]$SecurityGroupId = 'sg-044b773a38b2e5b4c',
    [string]$Cidr = '189.78.70.24/32',
    [int]$Port = 5432,
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
$aws = 'C:\Program Files\Amazon\AWSCLIV2\aws.exe'

if (-not (Test-Path $aws)) {
    throw 'AWS CLI not found at expected path.'
}

$command = @(
    'ec2', 'revoke-security-group-ingress',
    '--profile', $AwsProfile,
    '--region', $AwsRegion,
    '--group-id', $SecurityGroupId,
    '--protocol', 'tcp',
    '--port', "$Port",
    '--cidr', $Cidr
)

if ($DryRun) {
    $command += '--dry-run'
}

& $aws @command
