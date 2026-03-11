param(
    [string]$AwsProfile = 'oryzem',
    [string]$AwsRegion = 'sa-east-1',
    [string]$LogGroupName = '/ecs/program-management-system',
    [string]$TargetGroupArn = 'arn:aws:elasticloadbalancing:sa-east-1:439533253319:targetgroup/program-management-system-alb-tg/1425d73086a3393d',
    [int]$MaxLogStreams = 5,
    [int]$MaxLogEventsPerStream = 50,
    [string]$RoleArn = '',
    [string]$RoleSessionName = 'program-management-system-observability-check'
)

$ErrorActionPreference = 'Stop'
$aws = 'C:\Program Files\Amazon\AWSCLIV2\aws.exe'

if (-not (Test-Path $aws)) {
    throw 'AWS CLI not found at expected path.'
}

function Invoke-AwsJson {
    param(
        [string[]]$Arguments,
        [string]$ErrorMessage
    )

    $stdoutPath = [System.IO.Path]::GetTempFileName()
    $stderrPath = [System.IO.Path]::GetTempFileName()

    try {
        $process = Start-Process `
            -FilePath $aws `
            -ArgumentList $Arguments `
            -Wait `
            -NoNewWindow `
            -PassThru `
            -RedirectStandardOutput $stdoutPath `
            -RedirectStandardError $stderrPath

        $stdout = Get-Content -Path $stdoutPath -Raw
        $stderr = Get-Content -Path $stderrPath -Raw

        if ($process.ExitCode -ne 0) {
            $details = $stderr.Trim()
            if (-not $details) {
                $details = $stdout.Trim()
            }
            throw "$ErrorMessage`n$details".Trim()
        }

        return $stdout | ConvertFrom-Json
    } finally {
        Remove-Item $stdoutPath, $stderrPath -ErrorAction SilentlyContinue
    }
}

function Get-AwsCommonArguments {
    param(
        [string]$Profile,
        [string]$Region
    )

    $arguments = @()
    if ($Profile) {
        $arguments += @('--profile', $Profile)
    }
    if ($Region) {
        $arguments += @('--region', $Region)
    }

    return $arguments
}

function Find-PatternEvidence {
    param(
        [object[]]$Entries,
        [string[]]$Patterns
    )

    foreach ($pattern in $Patterns) {
        $match = $Entries | Where-Object {
            $_.message -and $_.message.IndexOf($pattern, [System.StringComparison]::OrdinalIgnoreCase) -ge 0
        } | Select-Object -First 1

        if ($match) {
            return [PSCustomObject]@{
                pattern = $pattern
                stream  = $match.stream
                message = $match.message
            }
        }
    }

    return $null
}

$secretEvidencePatterns = @(
    'Loaded RDS datasource properties from AWS Secrets Manager secret'
)

$databaseEvidencePatterns = @(
    'HikariPool',
    'Flyway',
    'Started ProgramManagementSystemApplication'
)

try {
    if ($RoleArn) {
        if (-not $AwsProfile) {
            throw 'AwsProfile is required when RoleArn is provided.'
        }

        Write-Host "Assuming role $RoleArn..."
        $assumeRoleArguments = @(
            'sts', 'assume-role'
        ) + (Get-AwsCommonArguments -Profile $AwsProfile -Region $AwsRegion) + @(
            '--role-arn', $RoleArn,
            '--role-session-name', $RoleSessionName,
            '--duration-seconds', '3600',
            '--output', 'json'
        )
        $assumeRoleResponse = Invoke-AwsJson `
            -Arguments $assumeRoleArguments `
            -ErrorMessage "Unable to assume role '$RoleArn'."

        $env:AWS_ACCESS_KEY_ID = $assumeRoleResponse.Credentials.AccessKeyId
        $env:AWS_SECRET_ACCESS_KEY = $assumeRoleResponse.Credentials.SecretAccessKey
        $env:AWS_SESSION_TOKEN = $assumeRoleResponse.Credentials.SessionToken
        $AwsProfile = ''
    }

    Write-Host 'Checking CloudWatch Logs read access...'
    $describeLogStreamsArguments = @(
        'logs', 'describe-log-streams',
        '--log-group-name', $LogGroupName,
        '--order-by', 'LastEventTime',
        '--descending',
        '--max-items', $MaxLogStreams,
        '--output', 'json'
    ) + (Get-AwsCommonArguments -Profile $AwsProfile -Region $AwsRegion)
    $logStreamResponse = Invoke-AwsJson `
        -Arguments $describeLogStreamsArguments `
        -ErrorMessage "Unable to describe log streams for '$LogGroupName'."

    $logStreams = @($logStreamResponse.logStreams)
    if (-not $logStreams -or $logStreams.Count -eq 0) {
        throw "No log streams were found in '$LogGroupName'."
    }

    $collectedEntries = @()
    foreach ($stream in $logStreams) {
        $getLogEventsArguments = @(
            'logs', 'get-log-events',
            '--log-group-name', $LogGroupName,
            '--log-stream-name', $stream.logStreamName,
            '--start-from-head',
            '--limit', $MaxLogEventsPerStream,
            '--output', 'json'
        ) + (Get-AwsCommonArguments -Profile $AwsProfile -Region $AwsRegion)
        $eventsResponse = Invoke-AwsJson `
            -Arguments $getLogEventsArguments `
            -ErrorMessage "Unable to read log events from stream '$($stream.logStreamName)'."

        foreach ($event in @($eventsResponse.events)) {
            $collectedEntries += [PSCustomObject]@{
                stream    = $stream.logStreamName
                timestamp = $event.timestamp
                message   = $event.message
            }
        }
    }

    $secretEvidence = Find-PatternEvidence -Entries $collectedEntries -Patterns $secretEvidencePatterns
    $databaseEvidence = Find-PatternEvidence -Entries $collectedEntries -Patterns $databaseEvidencePatterns

    Write-Host "Log streams checked: $($logStreams.Count)"
    Write-Host "Log events inspected: $($collectedEntries.Count)"

    if ($secretEvidence) {
        Write-Host "Secret evidence found: [$($secretEvidence.pattern)] in stream $($secretEvidence.stream)"
        Write-Host "  $($secretEvidence.message)"
    } else {
        Write-Warning 'No explicit Secrets Manager evidence was found in the inspected log events.'
    }

    if ($databaseEvidence) {
        Write-Host "Database/runtime evidence found: [$($databaseEvidence.pattern)] in stream $($databaseEvidence.stream)"
        Write-Host "  $($databaseEvidence.message)"
    } else {
        Write-Warning 'No explicit database/runtime evidence was found in the inspected log events.'
    }

    Write-Host ''
    Write-Host 'Checking Target Health read access...'
    $describeTargetHealthArguments = @(
        'elbv2', 'describe-target-health',
        '--target-group-arn', $TargetGroupArn,
        '--output', 'json'
    ) + (Get-AwsCommonArguments -Profile $AwsProfile -Region $AwsRegion)
    $targetHealthResponse = Invoke-AwsJson `
        -Arguments $describeTargetHealthArguments `
        -ErrorMessage "Unable to describe target health for '$TargetGroupArn'."

    $targetDescriptions = @($targetHealthResponse.TargetHealthDescriptions)
    if (-not $targetDescriptions -or $targetDescriptions.Count -eq 0) {
        Write-Warning 'No targets were returned for the target group.'
    } else {
        foreach ($item in $targetDescriptions) {
            Write-Host ("Target {0}:{1} -> {2}" -f $item.Target.Id, $item.Target.Port, $item.TargetHealth.State)
        }
    }

    Write-Host ''
    Write-Host 'Observability read validation completed.'
} catch {
    Write-Host $_.Exception.Message
    exit 1
}
