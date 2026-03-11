param(
    [string]$AwsProfile = 'oryzem',
    [string]$AwsRegion = 'sa-east-1',
    [string]$SecretId = 'program-management-system/rds/master',
    [string]$DbHost = 'program-management-system-db.cns8u4awye4v.sa-east-1.rds.amazonaws.com',
    [int]$Port = 5432,
    [string]$Database = 'program_management_system',
    [string]$Username = 'pms_admin'
)

$ErrorActionPreference = 'Stop'

$aws = 'C:\Program Files\Amazon\AWSCLIV2\aws.exe'
if (-not (Test-Path $aws)) {
    throw 'AWS CLI not found at expected path.'
}

$secretJson = & $aws secretsmanager get-secret-value `
    --profile $AwsProfile `
    --region $AwsRegion `
    --secret-id $SecretId `
    --query SecretString `
    --output text

if ($LASTEXITCODE -ne 0 -or -not $secretJson) {
    throw "Unable to read the secret '$SecretId' in region '$AwsRegion'."
}

$secret = $secretJson | ConvertFrom-Json
$resolvedHost = if ($secret.host) { $secret.host } else { $DbHost }
$resolvedPort = if ($secret.port) { [int]$secret.port } else { $Port }
$resolvedDatabase = if ($secret.dbname) { $secret.dbname } elseif ($secret.database) { $secret.database } else { $Database }
$resolvedUsername = if ($secret.username) { $secret.username } else { $Username }
$resolvedPassword = $secret.password

if (-not $resolvedPassword) {
    throw "The secret '$SecretId' does not contain a 'password' field."
}

$jar = Get-ChildItem "$HOME\.m2\repository\org\postgresql\postgresql" -Recurse -Filter 'postgresql-*.jar' |
    Sort-Object FullName -Descending |
    Select-Object -First 1 -ExpandProperty FullName

if (-not $jar) {
    throw 'PostgreSQL JDBC driver not found in local Maven repository.'
}

@"
Class.forName("org.postgresql.Driver");
var url = "jdbc:postgresql://${resolvedHost}:${resolvedPort}/${resolvedDatabase}?sslmode=require";
try (var conn = java.sql.DriverManager.getConnection(url, "$resolvedUsername", "$resolvedPassword")) {
    try (var stmt = conn.createStatement(); var rs = stmt.executeQuery("select current_database(), current_user")) {
        while (rs.next()) {
            System.out.println("DB_OK " + rs.getString(1) + " " + rs.getString(2));
        }
    }
}
/exit
"@ | jshell --class-path $jar

Write-Host "Credential source=Secrets Manager ($SecretId)"
