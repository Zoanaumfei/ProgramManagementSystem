param(
    [string]$AwsProfile = 'oryzem',
    [string]$AwsRegion = 'sa-east-1',
    [Parameter(Mandatory = $true)]
    [string]$LoadBalancerArn,
    [Parameter(Mandatory = $true)]
    [string]$CertificateArn,
    [Parameter(Mandatory = $true)]
    [string]$TargetGroupArn,
    [int]$HttpPort = 80,
    [int]$HttpsPort = 443,
    [string]$SslPolicy = 'ELBSecurityPolicy-2016-08'
)

$ErrorActionPreference = 'Stop'
$aws = 'C:\Program Files\Amazon\AWSCLIV2\aws.exe'

if (-not (Test-Path $aws)) {
    throw 'AWS CLI not found at expected path.'
}

function Invoke-AwsJson {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $output = & $aws @Arguments --output json
    if ($LASTEXITCODE -ne 0) {
        throw "AWS CLI command failed: aws $($Arguments -join ' ')"
    }

    if ([string]::IsNullOrWhiteSpace($output)) {
        return $null
    }

    return $output | ConvertFrom-Json
}

function Get-ListenerByPort {
    param(
        [Parameter(Mandatory = $true)]
        [object[]]$Listeners,
        [Parameter(Mandatory = $true)]
        [int]$Port,
        [Parameter(Mandatory = $true)]
        [string]$Protocol
    )

    return $Listeners | Where-Object {
        $_.Port -eq $Port -and $_.Protocol -eq $Protocol
    } | Select-Object -First 1
}

Write-Host "Validating load balancer '$LoadBalancerArn'..."
$lb = Invoke-AwsJson @(
    'elbv2', 'describe-load-balancers',
    '--profile', $AwsProfile,
    '--region', $AwsRegion,
    '--load-balancer-arns', $LoadBalancerArn
)

if (-not $lb.LoadBalancers -or $lb.LoadBalancers.Count -eq 0) {
    throw "Load balancer '$LoadBalancerArn' was not found."
}

$listeners = Invoke-AwsJson @(
    'elbv2', 'describe-listeners',
    '--profile', $AwsProfile,
    '--region', $AwsRegion,
    '--load-balancer-arn', $LoadBalancerArn
)

$existingListeners = @()
if ($listeners.Listeners) {
    $existingListeners = @($listeners.Listeners)
}

$httpListener = Get-ListenerByPort -Listeners $existingListeners -Port $HttpPort -Protocol 'HTTP'
$httpsListener = Get-ListenerByPort -Listeners $existingListeners -Port $HttpsPort -Protocol 'HTTPS'

if ($httpsListener) {
    Write-Host "Updating existing HTTPS listener on port $HttpsPort..."
    & $aws elbv2 modify-listener `
        --profile $AwsProfile `
        --region $AwsRegion `
        --listener-arn $httpsListener.ListenerArn `
        --certificates "CertificateArn=$CertificateArn" `
        --ssl-policy $SslPolicy `
        --default-actions "Type=forward,TargetGroupArn=$TargetGroupArn" `
        --output json | Out-Null

    if ($LASTEXITCODE -ne 0) {
        throw "Unable to update HTTPS listener '$($httpsListener.ListenerArn)'."
    }
} else {
    Write-Host "Creating HTTPS listener on port $HttpsPort..."
    & $aws elbv2 create-listener `
        --profile $AwsProfile `
        --region $AwsRegion `
        --load-balancer-arn $LoadBalancerArn `
        --port $HttpsPort `
        --protocol HTTPS `
        --certificates "CertificateArn=$CertificateArn" `
        --ssl-policy $SslPolicy `
        --default-actions "Type=forward,TargetGroupArn=$TargetGroupArn" `
        --output json | Out-Null

    if ($LASTEXITCODE -ne 0) {
        throw "Unable to create HTTPS listener on port $HttpsPort."
    }
}

if ($httpListener) {
    Write-Host "Updating existing HTTP listener on port $HttpPort to redirect to HTTPS..."
    & $aws elbv2 modify-listener `
        --profile $AwsProfile `
        --region $AwsRegion `
        --listener-arn $httpListener.ListenerArn `
        --default-actions "Type=redirect,RedirectConfig={Protocol=HTTPS,Port=$HttpsPort,StatusCode=HTTP_301}" `
        --output json | Out-Null

    if ($LASTEXITCODE -ne 0) {
        throw "Unable to update HTTP listener '$($httpListener.ListenerArn)'."
    }
} else {
    Write-Host "Creating HTTP listener on port $HttpPort with redirect to HTTPS..."
    & $aws elbv2 create-listener `
        --profile $AwsProfile `
        --region $AwsRegion `
        --load-balancer-arn $LoadBalancerArn `
        --port $HttpPort `
        --protocol HTTP `
        --default-actions "Type=redirect,RedirectConfig={Protocol=HTTPS,Port=$HttpsPort,StatusCode=HTTP_301}" `
        --output json | Out-Null

    if ($LASTEXITCODE -ne 0) {
        throw "Unable to create HTTP redirect listener on port $HttpPort."
    }
}

Write-Host ''
Write-Host 'ALB HTTPS configuration updated.'
Write-Host "Load balancer: $LoadBalancerArn"
Write-Host "Certificate ARN: $CertificateArn"
Write-Host "Target group: $TargetGroupArn"
Write-Host "HTTP -> HTTPS redirect: enabled on port $HttpPort"
Write-Host "HTTPS listener: enabled on port $HttpsPort"
