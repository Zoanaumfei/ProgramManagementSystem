param(
    [string]$AwsProfile = 'oryzem',
    [string]$AwsRegion = 'sa-east-1',
    [string]$RepositoryName = 'oryzem-backend-dev',
    [string]$ImageTag = 'latest',
    [string]$ClusterName = 'program-management-system-cluster',
    [string]$ServiceName = 'program-management-system-service',
    [switch]$ForceNewDeployment
)

$ErrorActionPreference = 'Stop'
$aws = 'C:\Program Files\Amazon\AWSCLIV2\aws.exe'

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker CLI not found. Install Docker before running the ECS deploy script.'
}

if (-not (Test-Path $aws)) {
    throw 'AWS CLI not found at expected path.'
}

$identity = & $aws sts get-caller-identity --profile $AwsProfile --output json | ConvertFrom-Json
$accountId = $identity.Account
$repositoryUri = "$accountId.dkr.ecr.$AwsRegion.amazonaws.com/$RepositoryName"
$imageUri = "${repositoryUri}:$ImageTag"

powershell -ExecutionPolicy Bypass -File .\scripts\ensure-ecr-repository.ps1 -AwsProfile $AwsProfile -AwsRegion $AwsRegion -RepositoryName $RepositoryName
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to validate the target ECR repository.'
}

& $aws ecr get-login-password --profile $AwsProfile --region $AwsRegion | docker login --username AWS --password-stdin "$accountId.dkr.ecr.$AwsRegion.amazonaws.com"
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to authenticate Docker against Amazon ECR.'
}

docker build --provenance=false -t $imageUri .
if ($LASTEXITCODE -ne 0) {
    throw "Docker build failed for image '$imageUri'."
}

docker push $imageUri
if ($LASTEXITCODE -ne 0) {
    throw "Docker push failed for image '$imageUri'."
}

powershell -ExecutionPolicy Bypass -File .\scripts\render-ecs-task-definition.ps1 -ImageUri $imageUri *> $null
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to render the ECS task definition.'
}

$taskRegistration = & $aws ecs register-task-definition `
    --profile $AwsProfile `
    --region $AwsRegion `
    --cli-input-json file://infra/ecs/task-definition.rendered.json `
    --output json | ConvertFrom-Json

if ($LASTEXITCODE -ne 0 -or -not $taskRegistration -or -not $taskRegistration.taskDefinition -or -not $taskRegistration.taskDefinition.taskDefinitionArn) {
    throw 'Unable to register the ECS task definition.'
}

$taskDefinitionArn = $taskRegistration.taskDefinition.taskDefinitionArn

$clusterLookup = & $aws ecs describe-clusters `
    --profile $AwsProfile `
    --region $AwsRegion `
    --clusters $ClusterName `
    --output json | ConvertFrom-Json

if ($LASTEXITCODE -ne 0) {
    throw "Unable to describe the ECS cluster '$ClusterName'."
}

if (-not $clusterLookup.clusters -or $clusterLookup.clusters.Count -eq 0) {
    & $aws ecs create-cluster `
        --profile $AwsProfile `
        --region $AwsRegion `
        --cluster-name $ClusterName *> $null

    if ($LASTEXITCODE -ne 0) {
        throw "Unable to create the ECS cluster '$ClusterName'."
    }
}

powershell -ExecutionPolicy Bypass -File .\scripts\render-ecs-service-definition.ps1 -TaskDefinitionArn $taskDefinitionArn *> $null
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to render the ECS service definition.'
}

$serviceLookup = & $aws ecs describe-services `
    --profile $AwsProfile `
    --region $AwsRegion `
    --cluster $ClusterName `
    --services $ServiceName `
    --output json | ConvertFrom-Json

if ($LASTEXITCODE -ne 0) {
    throw "Unable to describe the ECS service '$ServiceName'."
}

if ($serviceLookup.services -and $serviceLookup.services.Count -gt 0 -and $serviceLookup.services[0].status -ne 'INACTIVE') {
    $updateArgs = @(
        'ecs', 'update-service',
        '--profile', $AwsProfile,
        '--region', $AwsRegion,
        '--cluster', $ClusterName,
        '--service', $ServiceName,
        '--task-definition', $taskDefinitionArn,
        '--desired-count', '1'
    )

    if ($ForceNewDeployment.IsPresent) {
        $updateArgs += '--force-new-deployment'
    }

    & $aws @updateArgs

    if ($LASTEXITCODE -ne 0) {
        throw "Unable to update the ECS service '$ServiceName'."
    }
} else {
    & $aws ecs create-service `
        --profile $AwsProfile `
        --region $AwsRegion `
        --cli-input-json file://infra/ecs/service-definition.rendered.json

    if ($LASTEXITCODE -ne 0) {
        throw "Unable to create the ECS service '$ServiceName'."
    }
}

& $aws ecs wait services-stable `
    --profile $AwsProfile `
    --region $AwsRegion `
    --cluster $ClusterName `
    --services $ServiceName

if ($LASTEXITCODE -ne 0) {
    throw "Timed out waiting for the ECS service '$ServiceName' to reach a steady state."
}

$serviceStatus = & $aws ecs describe-services `
    --profile $AwsProfile `
    --region $AwsRegion `
    --cluster $ClusterName `
    --services $ServiceName `
    --output json | ConvertFrom-Json

if ($LASTEXITCODE -ne 0 -or -not $serviceStatus.services -or $serviceStatus.services.Count -eq 0) {
    throw "Unable to describe the ECS service '$ServiceName' after deployment."
}

$service = $serviceStatus.services[0]
$primaryDeployment = $service.deployments | Where-Object { $_.status -eq 'PRIMARY' } | Select-Object -First 1

Write-Host "ECS service is stable."
Write-Host "Cluster: $ClusterName"
Write-Host "Service: $ServiceName"
Write-Host "Task definition: $($service.taskDefinition)"
if ($primaryDeployment) {
    Write-Host "Primary rollout state: $($primaryDeployment.rolloutState)"
}
