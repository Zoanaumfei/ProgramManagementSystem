param(
    [string]$ImageUri,
    [string]$TemplatePath = '.\infra\ecs\task-definition.template.json',
    [string]$OutputPath = '.\infra\ecs\task-definition.rendered.json'
)

$ErrorActionPreference = 'Stop'

if (-not $ImageUri) {
    throw 'ImageUri is required.'
}

$content = Get-Content -Path $TemplatePath -Raw
$content = $content.Replace('__IMAGE_URI__', $ImageUri)
Set-Content -Path $OutputPath -Value $content -Encoding ascii

Get-Content $OutputPath
