param(
    [string]$AwsProfile = 'oryzem',
    [string]$AwsRegion = 'sa-east-1',
    [string]$ClusterName = 'program-management-system-cluster',
    [string]$ServiceName = 'program-management-system-service',
    [string]$DbInstanceIdentifier = 'program-management-system-db',
    [string]$HealthCheckUrl = 'http://program-management-system-alb-1082436660.sa-east-1.elb.amazonaws.com/public/ping'
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

function Get-EcsServiceStatus {
    $serviceLookup = Invoke-AwsJson @(
        'ecs', 'describe-services',
        '--profile', $AwsProfile,
        '--region', $AwsRegion,
        '--cluster', $ClusterName,
        '--services', $ServiceName
    )

    if (-not $serviceLookup.services -or $serviceLookup.services.Count -eq 0) {
        return $null
    }

    return $serviceLookup.services[0]
}

function Get-RunningTaskArn {
    $taskLookup = Invoke-AwsJson @(
        'ecs', 'list-tasks',
        '--profile', $AwsProfile,
        '--region', $AwsRegion,
        '--cluster', $ClusterName,
        '--service-name', $ServiceName,
        '--desired-status', 'RUNNING'
    )

    if (-not $taskLookup.taskArns -or $taskLookup.taskArns.Count -eq 0) {
        return $null
    }

    return $taskLookup.taskArns[0]
}

function Get-RdsStatus {
    $dbLookup = Invoke-AwsJson @(
        'rds', 'describe-db-instances',
        '--profile', $AwsProfile,
        '--region', $AwsRegion,
        '--db-instance-identifier', $DbInstanceIdentifier
    )

    if (-not $dbLookup.DBInstances -or $dbLookup.DBInstances.Count -eq 0) {
        return $null
    }

    return $dbLookup.DBInstances[0]
}

function Test-HealthEndpoint {
    try {
        $response = Invoke-RestMethod -Uri $HealthCheckUrl -Method Get -TimeoutSec 15
        return @{
            Ok = $true
            Body = ($response | ConvertTo-Json -Compress)
        }
    } catch {
        return @{
            Ok = $false
            Body = $_.Exception.Message
        }
    }
}

$caller = Invoke-AwsJson @(
    'sts', 'get-caller-identity',
    '--profile', $AwsProfile,
    '--region', $AwsRegion
)

$service = Get-EcsServiceStatus
$taskArn = Get-RunningTaskArn
$db = Get-RdsStatus
$health = Test-HealthEndpoint

Write-Host "AWS account: $($caller.Account)"
Write-Host "Region: $AwsRegion"
Write-Host ''

if ($service -eq $null) {
    Write-Host "ECS service: '$ServiceName' not found"
} else {
    $primaryDeployment = $service.deployments | Where-Object { $_.status -eq 'PRIMARY' } | Select-Object -First 1
    Write-Host "ECS cluster: $ClusterName"
    Write-Host "ECS service: $($service.serviceName)"
    Write-Host "ECS status: $($service.status)"
    Write-Host "Desired count: $($service.desiredCount)"
    Write-Host "Running count: $($service.runningCount)"
    Write-Host "Pending count: $($service.pendingCount)"
    Write-Host "Task definition: $($service.taskDefinition)"
    if ($primaryDeployment) {
        Write-Host "Primary rollout: $($primaryDeployment.rolloutState)"
    }
}

Write-Host "Running task: $(if ($taskArn) { $taskArn } else { 'none' })"
Write-Host ''

if ($db -eq $null) {
    Write-Host "RDS instance: '$DbInstanceIdentifier' not found"
} else {
    Write-Host "RDS instance: $($db.DBInstanceIdentifier)"
    Write-Host "RDS status: $($db.DBInstanceStatus)"
    Write-Host "RDS endpoint: $($db.Endpoint.Address)"
}

Write-Host ''
Write-Host "Health URL: $HealthCheckUrl"
if ($health.Ok) {
    Write-Host "Health check: OK"
    Write-Host "Health body: $($health.Body)"
} else {
    Write-Host "Health check: FAILED"
    Write-Host "Health detail: $($health.Body)"
}
