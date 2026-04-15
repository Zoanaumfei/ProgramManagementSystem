param(
    [string]$AwsProfile = '',
    [string]$AwsRegion = 'sa-east-1',
    [string]$BucketName = 'oryzem-pms-documents-sa-east-1-439533253319',
    [string]$TaskRoleName = 'program-management-system-ecs-task-role',
    [string]$RolePolicyName = 'ProgramManagementSystemDocumentStorageS3'
)

$ErrorActionPreference = 'Stop'
$aws = 'C:\Program Files\Amazon\AWSCLIV2\aws.exe'

if (-not (Test-Path $aws)) {
    throw 'AWS CLI not found at expected path.'
}

function Invoke-AwsCli {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $resolvedArgs = @()
    if ($AwsProfile) {
        $resolvedArgs += @('--profile', $AwsProfile)
    }
    $resolvedArgs += $Arguments

    & $aws @resolvedArgs

    if ($LASTEXITCODE -ne 0) {
        throw "AWS CLI command failed: $($resolvedArgs -join ' ')"
    }
}

$bucketEncryption = @'
{
  "Rules": [
    {
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "aws:kms"
      },
      "BucketKeyEnabled": true
    }
  ]
}
'@
$bucketCors = @'
{
  "CORSRules": [
    {
      "AllowedOrigins": [
        "http://localhost:3000",
        "https://oryzem.com",
        "https://www.oryzem.com"
      ],
      "AllowedMethods": [
        "GET",
        "HEAD",
        "POST"
      ],
      "AllowedHeaders": [
        "*"
      ],
      "ExposeHeaders": [
        "ETag",
        "x-amz-request-id",
        "x-amz-id-2"
      ],
      "MaxAgeSeconds": 3000
    }
  ]
}
'@
$ownershipControls = @'
{
  "Rules": [
    {
      "ObjectOwnership": "BucketOwnerEnforced"
    }
  ]
}
'@
$documentsPolicy = @"
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowProgramManagementSystemDocumentStorageBucketMetadata",
      "Effect": "Allow",
      "Action": [
        "s3:GetBucketLocation",
        "s3:ListBucket"
      ],
      "Resource": "arn:aws:s3:::$BucketName"
    },
    {
      "Sid": "AllowProgramManagementSystemDocumentStorageObjects",
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::$BucketName/*"
    }
  ]
}
"@

$tempDirectory = Join-Path $env:TEMP "pms-document-storage-$([guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Path $tempDirectory | Out-Null

$ownershipControlsPath = Join-Path $tempDirectory 'ownership-controls.json'
$bucketEncryptionPath = Join-Path $tempDirectory 'bucket-encryption.json'
$bucketCorsPath = Join-Path $tempDirectory 'bucket-cors.json'
$documentsPolicyPath = Join-Path $tempDirectory 'documents-policy.json'

Set-Content -Path $ownershipControlsPath -Value $ownershipControls -Encoding ascii
Set-Content -Path $bucketEncryptionPath -Value $bucketEncryption -Encoding ascii
Set-Content -Path $bucketCorsPath -Value $bucketCors -Encoding ascii
Set-Content -Path $documentsPolicyPath -Value $documentsPolicy -Encoding ascii

$bucketExists = $true
try {
    Invoke-AwsCli @('s3api', 'head-bucket', '--bucket', $BucketName, '--region', $AwsRegion) *> $null
} catch {
    $bucketExists = $false
}

if (-not $bucketExists) {
    Invoke-AwsCli @(
        's3api', 'create-bucket',
        '--bucket', $BucketName,
        '--region', $AwsRegion,
        '--create-bucket-configuration', "LocationConstraint=$AwsRegion"
    )
}

try {
    Invoke-AwsCli @(
        's3api', 'put-public-access-block',
        '--bucket', $BucketName,
        '--region', $AwsRegion,
        '--public-access-block-configuration', 'BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true'
    )

    Invoke-AwsCli @(
        's3api', 'put-bucket-versioning',
        '--bucket', $BucketName,
        '--region', $AwsRegion,
        '--versioning-configuration', 'Status=Enabled'
    )

    Invoke-AwsCli @(
        's3api', 'put-bucket-ownership-controls',
        '--bucket', $BucketName,
        '--region', $AwsRegion,
        '--ownership-controls', "file://$ownershipControlsPath"
    )

    Invoke-AwsCli @(
        's3api', 'put-bucket-encryption',
        '--bucket', $BucketName,
        '--region', $AwsRegion,
        '--server-side-encryption-configuration', "file://$bucketEncryptionPath"
    )

    Invoke-AwsCli @(
        's3api', 'put-bucket-cors',
        '--bucket', $BucketName,
        '--region', $AwsRegion,
        '--cors-configuration', "file://$bucketCorsPath"
    )

    Invoke-AwsCli @(
        'iam', 'put-role-policy',
        '--role-name', $TaskRoleName,
        '--policy-name', $RolePolicyName,
        '--policy-document', "file://$documentsPolicyPath"
    )

    Write-Host 'Document storage S3 configuration is ready.'
    Write-Host "Bucket: $BucketName"
    Write-Host "Region: $AwsRegion"
    Write-Host "Task role policy: $TaskRoleName / $RolePolicyName"
} finally {
    if (Test-Path $tempDirectory) {
        Remove-Item -LiteralPath $tempDirectory -Recurse -Force
    }
}
