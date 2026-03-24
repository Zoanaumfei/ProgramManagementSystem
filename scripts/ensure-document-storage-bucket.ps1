param(
    [string]$AwsProfile = 'oryzem',
    [string]$AwsRegion = 'sa-east-1',
    [string]$BucketName = 'program-management-system-documents-dev-439533253319-sa-east-1',
    [string]$CorsConfigurationPath = '.\scripts\document-storage-cors.json'
)

$ErrorActionPreference = 'Stop'
$aws = 'C:\Program Files\Amazon\AWSCLIV2\aws.exe'

if (-not (Test-Path $aws)) {
    throw 'AWS CLI not found at expected path.'
}

if (-not $BucketName) {
    throw 'BucketName is required.'
}

function Invoke-Aws {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    & $aws @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "AWS CLI command failed: aws $($Arguments -join ' ')"
    }
}

function Write-JsonFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [string]$Content
    )

    Set-Content -Path $Path -Value $Content -Encoding ascii
}

$bucketExists = $false
$tempDir = Join-Path $env:TEMP 'program-management-system-documents'
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
$headBucketStdOut = Join-Path $tempDir 'head-bucket.stdout.log'
$headBucketStdErr = Join-Path $tempDir 'head-bucket.stderr.log'
$headBucketProcess = Start-Process `
    -FilePath $aws `
    -ArgumentList @(
        's3api', 'head-bucket',
        '--profile', $AwsProfile,
        '--region', $AwsRegion,
        '--bucket', $BucketName
    ) `
    -NoNewWindow `
    -PassThru `
    -Wait `
    -RedirectStandardOutput $headBucketStdOut `
    -RedirectStandardError $headBucketStdErr
if ($headBucketProcess.ExitCode -eq 0) {
    $bucketExists = $true
}

if (-not $bucketExists) {
    Write-Host "Creating S3 bucket '$BucketName' in region '$AwsRegion'..."
    if ($AwsRegion -eq 'us-east-1') {
        Invoke-Aws @(
            's3api', 'create-bucket',
            '--profile', $AwsProfile,
            '--region', $AwsRegion,
            '--bucket', $BucketName
        )
    } else {
        Invoke-Aws @(
            's3api', 'create-bucket',
            '--profile', $AwsProfile,
            '--region', $AwsRegion,
            '--bucket', $BucketName,
            '--create-bucket-configuration', "LocationConstraint=$AwsRegion"
        )
    }
} else {
    Write-Host "S3 bucket '$BucketName' already exists."
}

$ownershipPath = Join-Path $tempDir 'ownership-controls.json'
$encryptionPath = Join-Path $tempDir 'bucket-encryption.json'
$publicAccessPath = Join-Path $tempDir 'public-access-block.json'

Write-JsonFile -Path $ownershipPath -Content @'
{
  "Rules": [
    {
      "ObjectOwnership": "BucketOwnerEnforced"
    }
  ]
}
'@

Write-JsonFile -Path $encryptionPath -Content @'
{
  "Rules": [
    {
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }
  ]
}
'@

Write-JsonFile -Path $publicAccessPath -Content @'
{
  "BlockPublicAcls": true,
  "IgnorePublicAcls": true,
  "BlockPublicPolicy": true,
  "RestrictPublicBuckets": true
}
'@

Write-Host "Applying bucket ownership controls..."
Invoke-Aws @(
    's3api', 'put-bucket-ownership-controls',
    '--profile', $AwsProfile,
    '--region', $AwsRegion,
    '--bucket', $BucketName,
    '--ownership-controls', "file://$ownershipPath"
)

Write-Host "Applying default encryption (AES256)..."
Invoke-Aws @(
    's3api', 'put-bucket-encryption',
    '--profile', $AwsProfile,
    '--region', $AwsRegion,
    '--bucket', $BucketName,
    '--server-side-encryption-configuration', "file://$encryptionPath"
)

Write-Host "Blocking all public access..."
Invoke-Aws @(
    's3api', 'put-public-access-block',
    '--profile', $AwsProfile,
    '--region', $AwsRegion,
    '--bucket', $BucketName,
    '--public-access-block-configuration', "file://$publicAccessPath"
)

if (Test-Path $CorsConfigurationPath) {
    Write-Host "Applying bucket CORS from '$CorsConfigurationPath'..."
    Invoke-Aws @(
        's3api', 'put-bucket-cors',
        '--profile', $AwsProfile,
        '--region', $AwsRegion,
        '--bucket', $BucketName,
        '--cors-configuration', "file://$CorsConfigurationPath"
    )
} else {
    Write-Host "CORS configuration file not found at '$CorsConfigurationPath'. Skipping CORS setup."
}

Write-Host "Document storage bucket is ready: $BucketName"
