param(
    [string]$CognitoDomainBaseUrl,
    [string]$AppClientId = 'rv7hk9nkugspb3i4p269sv828',
    [string]$CallbackUrl = 'http://localhost:3000/callback',
    [string]$LogoutUrl = 'http://localhost:3000/logout',
    [string[]]$Scopes = @('openid'),
    [switch]$IncludeImplicitFlow
)

$ErrorActionPreference = 'Stop'

if (-not $CognitoDomainBaseUrl) {
    throw 'CognitoDomainBaseUrl is required. Example: https://your-domain.auth.sa-east-1.amazoncognito.com'
}

function ConvertTo-Base64Url([byte[]]$Bytes) {
    return [Convert]::ToBase64String($Bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

function ConvertTo-QueryString([hashtable]$Parameters) {
    return ($Parameters.GetEnumerator() |
        Where-Object { $_.Value -ne $null -and "$($_.Value)".Length -gt 0 } |
        Sort-Object Key |
        ForEach-Object {
            '{0}={1}' -f [uri]::EscapeDataString($_.Key), [uri]::EscapeDataString([string]$_.Value)
        }) -join '&'
}

function New-RandomUrlSafeString([int]$ByteLength = 32) {
    $bytes = New-Object byte[] $ByteLength
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    return ConvertTo-Base64Url $bytes
}

$normalizedDomain = $CognitoDomainBaseUrl.TrimEnd('/')
$scopeValue = ($Scopes | Where-Object { $_ -and -not [string]::IsNullOrWhiteSpace($_) }) -join ' '

$codeVerifier = New-RandomUrlSafeString 64
$sha256 = [System.Security.Cryptography.SHA256]::Create()
$codeChallenge = ConvertTo-Base64Url ($sha256.ComputeHash([System.Text.Encoding]::ASCII.GetBytes($codeVerifier)))
$state = New-RandomUrlSafeString 24
$nonce = New-RandomUrlSafeString 24

$codeAuthorizeParams = @{
    client_id = $AppClientId
    response_type = 'code'
    scope = $scopeValue
    redirect_uri = $CallbackUrl
    state = $state
    nonce = $nonce
    code_challenge_method = 'S256'
    code_challenge = $codeChallenge
}

$logoutParams = @{
    client_id = $AppClientId
    logout_uri = $LogoutUrl
}

$codeAuthorizeUrl = '{0}/oauth2/authorize?{1}' -f $normalizedDomain, (ConvertTo-QueryString $codeAuthorizeParams)
$tokenUrl = '{0}/oauth2/token' -f $normalizedDomain
$logoutUrlFinal = '{0}/logout?{1}' -f $normalizedDomain, (ConvertTo-QueryString $logoutParams)

Write-Host 'Cognito Hosted UI URLs'
Write-Host "Code authorize URL: $codeAuthorizeUrl"
if ($IncludeImplicitFlow) {
    $implicitAuthorizeParams = @{
        client_id = $AppClientId
        response_type = 'token'
        scope = $scopeValue
        redirect_uri = $CallbackUrl
        state = $state
        nonce = $nonce
    }

    $implicitAuthorizeUrl = '{0}/oauth2/authorize?{1}' -f $normalizedDomain, (ConvertTo-QueryString $implicitAuthorizeParams)
    Write-Host "Implicit authorize URL: $implicitAuthorizeUrl"
}
Write-Host "Token URL: $tokenUrl"
Write-Host "Logout URL: $logoutUrlFinal"
Write-Host ''
Write-Host 'PKCE values for code flow'
Write-Host "code_verifier: $codeVerifier"
Write-Host "code_challenge: $codeChallenge"
Write-Host "state: $state"
Write-Host "nonce: $nonce"
Write-Host ''
Write-Host 'Token exchange example'
Write-Host ("curl -X POST ""{0}"" -H ""Content-Type: application/x-www-form-urlencoded"" -d ""grant_type=authorization_code&client_id={1}&code=<AUTH_CODE>&redirect_uri={2}&code_verifier={3}""" -f $tokenUrl, $AppClientId, $CallbackUrl, $codeVerifier)
