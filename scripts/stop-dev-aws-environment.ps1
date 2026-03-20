param(
    [string]$AwsProfile = 'oryzem',
    [string]$AwsRegion = 'sa-east-1',
    [string]$ClusterName = 'program-management-system-cluster',
    [string]$ServiceName = 'program-management-system-service',
    [string]$DbInstanceIdentifier = 'program-management-system-db',
    [switch]$WaitForRdsStopped
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

function Get-ServiceDescription {
    return Invoke-AwsJson @(
        'ecs', 'describe-services',
        '--profile', $AwsProfile,
        '--region', $AwsRegion,
        '--cluster', $ClusterName,
        '--services', $ServiceName
    )
}

function Stop-EcsService {
    $serviceLookup = Get-ServiceDescription
    if (-not $serviceLookup.services -or $serviceLookup.services.Count -eq 0) {
        Write-Host "ECS service '$ServiceName' was not found. Skipping ECS scale-down."
        return $null
    }

    $service = $serviceLookup.services[0]
    if ($service.status -eq 'INACTIVE') {
        Write-Host "ECS service '$ServiceName' is already INACTIVE. Skipping ECS scale-down."
        return $service
    }

    Write-Host "Scaling ECS service '$ServiceName' to desired count 0..."
    & $aws ecs update-service `
        --profile $AwsProfile `
        --region $AwsRegion `
        --cluster $ClusterName `
        --service $ServiceName `
        --desired-count 0 `
        --output json | Out-Null

    if ($LASTEXITCODE -ne 0) {
        throw "Unable to scale ECS service '$ServiceName' to zero."
    }

    & $aws ecs wait services-stable `
        --profile $AwsProfile `
        --region $AwsRegion `
        --cluster $ClusterName `
        --services $ServiceName

    if ($LASTEXITCODE -ne 0) {
        throw "Timed out waiting for ECS service '$ServiceName' to stabilize after scaling to zero."
    }

    $serviceLookup = Get-ServiceDescription
    $service = $serviceLookup.services[0]
    Write-Host "ECS service stopped. desiredCount=$($service.desiredCount) runningCount=$($service.runningCount)"
    return $service
}

function Stop-RdsInstance {
    $dbLookup = Invoke-AwsJson @(
        'rds', 'describe-db-instances',
        '--profile', $AwsProfile,
        '--region', $AwsRegion,
        '--db-instance-identifier', $DbInstanceIdentifier
    )

    if (-not $dbLookup.DBInstances -or $dbLookup.DBInstances.Count -eq 0) {
        Write-Host "RDS instance '$DbInstanceIdentifier' was not found. Skipping RDS stop."
        return
    }

    $db = $dbLookup.DBInstances[0]
    if ($db.DBInstanceStatus -in @('stopped', 'stopping')) {
        Write-Host "RDS instance '$DbInstanceIdentifier' is already $($db.DBInstanceStatus)."
    } else {
        Write-Host "Stopping RDS instance '$DbInstanceIdentifier'..."
        & $aws rds stop-db-instance `
            --profile $AwsProfile `
            --region $AwsRegion `
            --db-instance-identifier $DbInstanceIdentifier `
            --output json | Out-Null

        if ($LASTEXITCODE -ne 0) {
            throw "Unable to stop RDS instance '$DbInstanceIdentifier'."
        }
    }

    if ($WaitForRdsStopped.IsPresent) {
        Write-Host "Waiting for RDS instance '$DbInstanceIdentifier' to reach 'stopped'..."
        & $aws rds wait db-instance-stopped `
            --profile $AwsProfile `
            --region $AwsRegion `
            --db-instance-identifier $DbInstanceIdentifier

        if ($LASTEXITCODE -ne 0) {
            throw "Timed out waiting for RDS instance '$DbInstanceIdentifier' to stop."
        }

        Write-Host "RDS instance '$DbInstanceIdentifier' is stopped."
    } else {
        Write-Host "RDS stop requested. Use -WaitForRdsStopped if you want the script to wait until the instance is fully stopped."
    }
}

$caller = Invoke-AwsJson @(
    'sts', 'get-caller-identity',
    '--profile', $AwsProfile,
    '--region', $AwsRegion
)

Write-Host "Using AWS account: $($caller.Account)"
Write-Host "Region: $AwsRegion"

Stop-EcsService
Stop-RdsInstance
Write-Host 'ALB was preserved intentionally. This script never deletes load balancers.'
Write-Host 'Reminder: the ALB will continue generating cost while it exists.'

Write-Host 'Done.'
