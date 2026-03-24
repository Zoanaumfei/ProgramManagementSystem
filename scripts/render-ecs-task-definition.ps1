param(
    [string]$ImageUri,
    [string]$DocumentBucketName = 'program-management-system-documents-dev-439533253319-sa-east-1',
    [string]$DocumentKeyPrefix = 'portfolio',
    [string]$TemplatePath = '.\infra\ecs\task-definition.template.json',
    [string]$OutputPath = '.\infra\ecs\task-definition.rendered.json'
)

$ErrorActionPreference = 'Stop'

if (-not $ImageUri) {
    throw 'ImageUri is required.'
}

$content = Get-Content -Path $TemplatePath -Raw
$content = $content.Replace('__IMAGE_URI__', $ImageUri)
$content = $content.Replace('__DOCUMENT_BUCKET_NAME__', $DocumentBucketName)
$content = $content.Replace('__DOCUMENT_KEY_PREFIX__', $DocumentKeyPrefix)
Set-Content -Path $OutputPath -Value $content -Encoding ascii

Get-Content $OutputPath
