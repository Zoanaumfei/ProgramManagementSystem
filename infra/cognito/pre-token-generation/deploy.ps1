param(
    [string]$Region = "sa-east-1",
    [string]$UserPoolId = "sa-east-1_aA4I3tEmF",
    [string]$FunctionName = "program-management-system-cognito-pre-token",
    [string]$RoleName = "program-management-system-cognito-pre-token-role",
    [string]$Runtime = "nodejs20.x"
)

$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $false
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$buildDir = Join-Path $scriptDir ".build"
$zipPath = Join-Path $buildDir "pre-token-generation.zip"
$trustPolicyPath = Join-Path $buildDir "trust-policy.json"

New-Item -ItemType Directory -Path $buildDir -Force | Out-Null
@'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
'@ | Set-Content -Path $trustPolicyPath -Encoding ascii

if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
}

Compress-Archive -Path (Join-Path $scriptDir "index.mjs") -DestinationPath $zipPath -Force

try {
    $roleArn = aws iam get-role --role-name $RoleName --query "Role.Arn" --output text 2>$null
}
catch {
    $roleArn = $null
}
if (-not $roleArn) {
    $roleArn = aws iam create-role `
        --role-name $RoleName `
        --assume-role-policy-document file://$trustPolicyPath `
        --query "Role.Arn" `
        --output text

    aws iam attach-role-policy `
        --role-name $RoleName `
        --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole | Out-Null

    Start-Sleep -Seconds 10
}

try {
    $functionArn = aws lambda get-function --function-name $FunctionName --region $Region --query "Configuration.FunctionArn" --output text 2>$null
}
catch {
    $functionArn = $null
}
if (-not $functionArn) {
    $functionArn = aws lambda create-function `
        --function-name $FunctionName `
        --runtime $Runtime `
        --role $roleArn `
        --handler index.handler `
        --zip-file fileb://$zipPath `
        --region $Region `
        --timeout 10 `
        --query "FunctionArn" `
        --output text
}
else {
    aws lambda update-function-code `
        --function-name $FunctionName `
        --zip-file fileb://$zipPath `
        --region $Region | Out-Null
}

$accountId = aws sts get-caller-identity --query Account --output text
$userPoolArn = "arn:aws:cognito-idp:${Region}:${accountId}:userpool/${UserPoolId}"

aws lambda add-permission `
    --function-name $FunctionName `
    --statement-id "AllowCognitoInvoke-$UserPoolId" `
    --action lambda:InvokeFunction `
    --principal cognito-idp.amazonaws.com `
    --source-arn $userPoolArn `
    --region $Region 2>$null | Out-Null

Write-Host "Lambda pronta:" $functionArn
Write-Host "Anexe no User Pool como Pre Token Generation com LambdaVersion=V2_0."
