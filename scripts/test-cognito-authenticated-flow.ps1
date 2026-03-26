param(
    [string]$BaseUrl = 'http://program-management-system-alb-1082436660.sa-east-1.elb.amazonaws.com',
    [string]$AccessToken,
    [switch]$SkipHttpChecks
)

$ErrorActionPreference = 'Stop'

if (-not $AccessToken) {
    throw 'AccessToken is required.'
}

function ConvertFrom-Base64Url([string]$Value) {
    $padded = $Value.Replace('-', '+').Replace('_', '/')
    switch ($padded.Length % 4) {
        2 { $padded += '==' }
        3 { $padded += '=' }
    }
    return [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($padded))
}

function ConvertTo-RoleName([string]$GroupName) {
    if (-not $GroupName) {
        return $null
    }

    return $GroupName.Trim().Replace('-', '_').Replace(' ', '_').ToUpperInvariant()
}

function Invoke-ApiCheck {
    param(
        [string]$Url,
        [hashtable]$Headers
    )

    try {
        $response = Invoke-WebRequest -Uri $Url -Headers $Headers -Method Get -UseBasicParsing
        $body = if ($response.Content) { $response.Content | ConvertFrom-Json } else { $null }
        return [PSCustomObject]@{
            url    = $Url
            status = [int]$response.StatusCode
            body   = $body
            error  = $null
        }
    } catch {
        $statusCode = 0
        $body = $null
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $rawBody = $reader.ReadToEnd()
            if ($rawBody) {
                try {
                    $body = $rawBody | ConvertFrom-Json
                } catch {
                    $body = $rawBody
                }
            }
        }

        return [PSCustomObject]@{
            url    = $Url
            status = $statusCode
            body   = $body
            error  = $_.Exception.Message
        }
    }
}

$parts = $AccessToken.Split('.')
if ($parts.Length -lt 2) {
    throw 'AccessToken does not look like a JWT.'
}

$payloadJson = ConvertFrom-Base64Url $parts[1]
$claims = $payloadJson | ConvertFrom-Json

$groups = @()
if ($claims.PSObject.Properties.Name -contains 'cognito:groups') {
    $groups = @($claims.'cognito:groups')
}

$roles = @($groups | ForEach-Object { ConvertTo-RoleName $_ } | Where-Object { $_ } | Sort-Object -Unique)
$hasAdminRole = $roles -contains 'ADMIN'

Write-Host 'Token summary'
Write-Host "  subject: $($claims.sub)"
Write-Host "  username: $($claims.'cognito:username')"
Write-Host "  token_use: $($claims.token_use)"
Write-Host "  client_id: $($claims.client_id)"
Write-Host "  audience: $(@($claims.aud) -join ', ')"
Write-Host "  groups: $($groups -join ', ')"
Write-Host "  derived roles: $($roles -join ', ')"
Write-Host "  user_status: $($claims.user_status)"
Write-Host ''

if ($claims.token_use -and $claims.token_use -ne 'access') {
    Write-Warning "The provided JWT has token_use=$($claims.token_use). For API validation, prefer an access token."
}

if ($SkipHttpChecks) {
    Write-Host 'HTTP checks skipped.'
    exit 0
}

$normalizedBaseUrl = $BaseUrl.TrimEnd('/')
$headers = @{
    Authorization = "Bearer $AccessToken"
    Accept        = 'application/json'
}

$checks = @(
    [PSCustomObject]@{
        name = 'api ping'
        url  = "$normalizedBaseUrl/api/ping"
        expectedStatus = 200
    },
    [PSCustomObject]@{
        name = 'auth me'
        url  = "$normalizedBaseUrl/api/auth/me"
        expectedStatus = 200
    },
    [PSCustomObject]@{
        name = 'admin ping'
        url  = "$normalizedBaseUrl/api/admin/ping"
        expectedStatus = $(if ($hasAdminRole) { 200 } else { 403 })
    },
    [PSCustomObject]@{
        name = 'authz check users view'
        url  = "$normalizedBaseUrl/api/authz/check?module=USERS&action=VIEW"
        expectedStatus = 200
    }
)

$results = foreach ($check in $checks) {
    $result = Invoke-ApiCheck -Url $check.url -Headers $headers
    [PSCustomObject]@{
        name           = $check.name
        url            = $check.url
        expectedStatus = $check.expectedStatus
        actualStatus   = $result.status
        ok             = ($result.status -eq $check.expectedStatus)
        body           = $result.body
    }
}

Write-Host 'HTTP validation'
foreach ($result in $results) {
    $status = if ($result.ok) { 'OK' } else { 'FAIL' }
    Write-Host ("  [{0}] {1} -> expected {2}, got {3}" -f $status, $result.name, $result.expectedStatus, $result.actualStatus)

    if ($result.name -eq 'auth me' -and $result.body) {
        Write-Host ("    username={0}; membershipId={1}; activeTenantId={2}; activeOrganizationId={3}; roles={4}" -f $result.body.username, $result.body.membershipId, $result.body.activeTenantId, $result.body.activeOrganizationId, ($result.body.roles -join ','))
    }

    if ($result.name -eq 'authz check users view' -and $result.body) {
        Write-Host ("    allowed={0}; reason={1}" -f $result.body.allowed, $result.body.reason)
    }
}

if ($results.ok -contains $false) {
    exit 1
}

Write-Host ''
Write-Host 'Authenticated flow validation completed successfully.'
