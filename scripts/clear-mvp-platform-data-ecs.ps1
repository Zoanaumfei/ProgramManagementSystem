param(
    [string]$AwsProfile = 'oryzem',
    [string]$AwsRegion = 'sa-east-1',
    [string]$ClusterName = 'program-management-system-cluster',
    [string]$ServiceName = 'program-management-system-service',
    [string]$ContainerName = 'program-management-system',
    [string]$SecretId = 'program-management-system/rds/master',
    [string]$BootstrapEmail = 'vanderson.verza@gmail.com',
    [string]$BootstrapDisplayName = 'Vanderson Verza',
    [string]$ConfirmToken
)

$ErrorActionPreference = 'Stop'
$requiredToken = 'RESET_MVP_PLATFORM_DATA'
$aws = 'C:\Program Files\Amazon\AWSCLIV2\aws.exe'

if ($ConfirmToken -ne $requiredToken) {
    throw "Refusing to clear MVP data. Re-run with -ConfirmToken $requiredToken after confirming this is the intended ECS/RDS environment."
}

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

$serviceLookup = Invoke-AwsJson @(
    'ecs', 'describe-services',
    '--profile', $AwsProfile,
    '--region', $AwsRegion,
    '--cluster', $ClusterName,
    '--services', $ServiceName
)

if (-not $serviceLookup.services -or $serviceLookup.services.Count -eq 0 -or $serviceLookup.services[0].status -eq 'INACTIVE') {
    throw "ECS service '$ServiceName' was not found or is inactive."
}

$service = $serviceLookup.services[0]
$network = $service.networkConfiguration.awsvpcConfiguration
if (-not $network -or -not $network.subnets -or -not $network.securityGroups) {
    throw "ECS service '$ServiceName' does not expose an awsvpc network configuration."
}

$assignPublicIp = if ($network.assignPublicIp) { $network.assignPublicIp } else { 'DISABLED' }
$networkConfiguration = @{
    awsvpcConfiguration = @{
        subnets = @($network.subnets)
        securityGroups = @($network.securityGroups)
        assignPublicIp = $assignPublicIp
    }
} | ConvertTo-Json -Depth 5 -Compress

$overrides = @{
    containerOverrides = @(
        @{
            name = $ContainerName
            environment = @(
                @{ name = 'SPRING_PROFILES_ACTIVE'; value = 'rds' },
                @{ name = 'SPRING_MAIN_WEB_APPLICATION_TYPE'; value = 'none' },
                @{ name = 'AWS_REGION'; value = $AwsRegion },
                @{ name = 'DB_SECRET_ID'; value = $SecretId },
                @{ name = 'DB_SSL_MODE'; value = 'require' },
                @{ name = 'APP_BOOTSTRAP_SEED_DATA'; value = 'false' },
                @{ name = 'APP_BOOTSTRAP_INTERNAL_ADMIN_ENABLED'; value = 'true' },
                @{ name = 'APP_BOOTSTRAP_INTERNAL_ADMIN_EMAIL'; value = $BootstrapEmail },
                @{ name = 'APP_BOOTSTRAP_INTERNAL_ADMIN_DISPLAY_NAME'; value = $BootstrapDisplayName },
                @{ name = 'APP_BOOTSTRAP_INTERNAL_ADMIN_PRUNE_OTHER_INTERNAL_USERS'; value = 'true' },
                @{ name = 'APP_MAINTENANCE_RESET_ENABLED'; value = 'true' },
                @{ name = 'APP_MAINTENANCE_RESET_CONFIRMATION'; value = 'RESET_RDS_SAFE' },
                @{ name = 'APP_MAINTENANCE_RESET_EXIT_AFTER_RESET'; value = 'true' }
            )
        }
    )
} | ConvertTo-Json -Depth 8 -Compress

$tempDirectory = Join-Path ([System.IO.Path]::GetTempPath()) "oryzem-mvp-reset-$([System.Guid]::NewGuid().ToString('N'))"
New-Item -Path $tempDirectory -ItemType Directory | Out-Null
$networkConfigurationPath = Join-Path $tempDirectory 'network-configuration.json'
$overridesPath = Join-Path $tempDirectory 'overrides.json'

$utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($networkConfigurationPath, $networkConfiguration, $utf8WithoutBom)
[System.IO.File]::WriteAllText($overridesPath, $overrides, $utf8WithoutBom)

Write-Host "Launching one-off MVP reset task in ECS service network."
Write-Host "Cluster: $ClusterName"
Write-Host "Task definition: $($service.taskDefinition)"
Write-Host "Bootstrap admin: $BootstrapEmail"

try {
    $runTask = Invoke-AwsJson @(
        'ecs', 'run-task',
        '--profile', $AwsProfile,
        '--region', $AwsRegion,
        '--cluster', $ClusterName,
        '--launch-type', 'FARGATE',
        '--task-definition', $service.taskDefinition,
        '--network-configuration', "file://$networkConfigurationPath",
        '--overrides', "file://$overridesPath",
        '--started-by', 'mvp-platform-reset'
    )
} finally {
    Remove-Item -LiteralPath $tempDirectory -Recurse -Force -ErrorAction SilentlyContinue
}

if ($runTask.failures -and $runTask.failures.Count -gt 0) {
    $failure = $runTask.failures[0]
    throw "Unable to start reset task. Reason=$($failure.reason) Arn=$($failure.arn) Detail=$($failure.detail)"
}

if (-not $runTask.tasks -or $runTask.tasks.Count -eq 0) {
    throw 'ECS did not return a reset task.'
}

$taskArn = $runTask.tasks[0].taskArn
Write-Host "Reset task started: $taskArn"
Write-Host 'Waiting for reset task to stop...'

& $aws ecs wait tasks-stopped `
    --profile $AwsProfile `
    --region $AwsRegion `
    --cluster $ClusterName `
    --tasks $taskArn

if ($LASTEXITCODE -ne 0) {
    throw "Timed out waiting for reset task '$taskArn' to stop."
}

$taskLookup = Invoke-AwsJson @(
    'ecs', 'describe-tasks',
    '--profile', $AwsProfile,
    '--region', $AwsRegion,
    '--cluster', $ClusterName,
    '--tasks', $taskArn
)

$task = $taskLookup.tasks[0]
$container = $task.containers | Where-Object { $_.name -eq $ContainerName } | Select-Object -First 1
$exitCode = if ($container -and $null -ne $container.exitCode) { [int]$container.exitCode } else { -1 }

Write-Host "Reset task stopped. Last status: $($task.lastStatus)"
Write-Host "Stopped reason: $($task.stoppedReason)"
Write-Host "Container exit code: $exitCode"

if ($exitCode -ne 0) {
    throw "MVP reset task failed with exit code $exitCode. Check CloudWatch Logs group /ecs/program-management-system."
}

Write-Host 'MVP platform data reset completed successfully.'
