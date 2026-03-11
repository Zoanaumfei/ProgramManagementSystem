param(
    [string]$TaskDefinitionArn,
    [string]$TemplatePath = '.\infra\ecs\service-definition.template.json',
    [string]$OutputPath = '.\infra\ecs\service-definition.rendered.json'
)

$ErrorActionPreference = 'Stop'

if (-not $TaskDefinitionArn) {
    throw 'TaskDefinitionArn is required.'
}

$content = Get-Content -Path $TemplatePath -Raw
$content = $content.Replace('__TASK_DEFINITION_ARN__', $TaskDefinitionArn)
Set-Content -Path $OutputPath -Value $content -Encoding ascii

Get-Content $OutputPath
