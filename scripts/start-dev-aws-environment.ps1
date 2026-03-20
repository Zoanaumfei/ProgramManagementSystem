param(
    [string]$AwsProfile = 'oryzem',
    [string]$AwsRegion = 'sa-east-1',
    [string]$ClusterName = 'program-management-system-cluster',
    [string]$ServiceName = 'program-management-system-service',
    [string]$DbInstanceIdentifier = 'program-management-system-db',
    [int]$DesiredCount = 1,
    [switch]$WaitForRdsAvailable
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

function Start-RdsInstance {
    $dbLookup = Invoke-AwsJson @(
        'rds', 'describe-db-instances',
        '--profile', $AwsProfile,
        '--region', $AwsRegion,
        '--db-instance-identifier', $DbInstanceIdentifier
    )

    if (-not $dbLookup.DBInstances -or $dbLookup.DBInstances.Count -eq 0) {
        throw "RDS instance '$DbInstanceIdentifier' was not found."
    }

    $db = $dbLookup.DBInstances[0]
    if ($db.DBInstanceStatus -in @('available', 'starting', 'backing-up', 'configuring-enhanced-monitoring')) {
        Write-Host "RDS instance '$DbInstanceIdentifier' is already in state '$($db.DBInstanceStatus)'."
    } else {
        Write-Host "Starting RDS instance '$DbInstanceIdentifier'..."
        & $aws rds start-db-instance `
            --profile $AwsProfile `
            --region $AwsRegion `
            --db-instance-identifier $DbInstanceIdentifier `
            --output json | Out-Null

        if ($LASTEXITCODE -ne 0) {
            throw "Unable to start RDS instance '$DbInstanceIdentifier'."
        }
    }

    if ($WaitForRdsAvailable.IsPresent) {
        Write-Host "Waiting for RDS instance '$DbInstanceIdentifier' to reach 'available'..."
        & $aws rds wait db-instance-available `
            --profile $AwsProfile `
            --region $AwsRegion `
            --db-instance-identifier $DbInstanceIdentifier

        if ($LASTEXITCODE -ne 0) {
            throw "Timed out waiting for RDS instance '$DbInstanceIdentifier' to become available."
        }

        Write-Host "RDS instance '$DbInstanceIdentifier' is available."
    } else {
        Write-Host "RDS start requested. Use -WaitForRdsAvailable if you want the script to wait until the instance is fully available."
    }
}

function Start-EcsService {
    $serviceLookup = Invoke-AwsJson @(
        'ecs', 'describe-services',
        '--profile', $AwsProfile,
        '--region', $AwsRegion,
        '--cluster', $ClusterName,
        '--services', $ServiceName
    )

    if (-not $serviceLookup.services -or $serviceLookup.services.Count -eq 0) {
        throw "ECS service '$ServiceName' was not found."
    }

    $service = $serviceLookup.services[0]
    if ($service.status -eq 'INACTIVE') {
        throw "ECS service '$ServiceName' is INACTIVE and cannot be started by scaling desired count."
    }

    Write-Host "Scaling ECS service '$ServiceName' to desired count $DesiredCount..."
    & $aws ecs update-service `
        --profile $AwsProfile `
        --region $AwsRegion `
        --cluster $ClusterName `
        --service $ServiceName `
        --desired-count $DesiredCount `
        --output json | Out-Null

    if ($LASTEXITCODE -ne 0) {
        throw "Unable to scale ECS service '$ServiceName' to desired count $DesiredCount."
    }

    & $aws ecs wait services-stable `
        --profile $AwsProfile `
        --region $AwsRegion `
        --cluster $ClusterName `
        --services $ServiceName

    if ($LASTEXITCODE -ne 0) {
        throw "Timed out waiting for ECS service '$ServiceName' to stabilize."
    }

    $serviceLookup = Invoke-AwsJson @(
        'ecs', 'describe-services',
        '--profile', $AwsProfile,
        '--region', $AwsRegion,
        '--cluster', $ClusterName,
        '--services', $ServiceName
    )
    $service = $serviceLookup.services[0]

    Write-Host "ECS service is stable. desiredCount=$($service.desiredCount) runningCount=$($service.runningCount)"
    Write-Host "Task definition: $($service.taskDefinition)"
}

$caller = Invoke-AwsJson @(
    'sts', 'get-caller-identity',
    '--profile', $AwsProfile,
    '--region', $AwsRegion
)

Write-Host "Using AWS account: $($caller.Account)"
Write-Host "Region: $AwsRegion"

Start-RdsInstance
Start-EcsService

Write-Host 'ALB is preserved by design. If it already exists, the public endpoint should come back when the ECS task becomes healthy.'
Write-Host 'Done.'
