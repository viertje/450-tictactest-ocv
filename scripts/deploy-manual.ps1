# Deploys one published release BY HAND, with exactly the steps the pipeline (deploy.yml)
# runs later. Needs: the lab running, `aws-school` active in this terminal, gh (signed in), tofu.
#
# Usage:  .\scripts\deploy-manual.ps1 -Version 1.1.0
#         .\scripts\deploy-manual.ps1 -Version 1.2.0 -SkipBackend      (frontend only)
param(
    [Parameter(Mandatory = $true)][string]$Version,
    [string]$InfraDir = "$PSScriptRoot\..\infra",
    [string]$Repo = "viertje/450-tictactest-ocv",
    [switch]$SkipBackend,
    [switch]$SkipFrontend
)

$ErrorActionPreference = "Stop"

function Invoke-Checked {
    $command = $args[0]
    $arguments = @($args | Select-Object -Skip 1)
    & $command @arguments
    if ($LASTEXITCODE -ne 0) { throw "Command failed: $command $arguments" }
}

if ($Version -notmatch '^\d+\.\d+\.\d+$') { throw "Version must look like 1.2.0 (no leading v)" }

# 1. Addresses and names come from the infrastructure (OpenTofu outputs).
$out = tofu "-chdir=$InfraDir" output -json | ConvertFrom-Json
$instanceId = $out.instance_id.value
$artifactBucket = $out.artifact_bucket.value
$frontendBucket = $out.frontend_bucket.value
$versionUrl = $out.version_url.value
$frontendUrl = "http://" + ($out.game_url.value -replace '^http://', '').TrimEnd('/')

# 2. The deployed files are the release assets, never a local build.
$assets = Join-Path $env:TEMP "tictactest-$Version"
if (Test-Path $assets) { Remove-Item $assets -Recurse -Force }
Invoke-Checked gh release download "v$Version" --repo $Repo --dir $assets

if (-not $SkipBackend) {
    # 3. Jar -> private bucket, then the instance fetches it and restarts the service.
    Invoke-Checked aws s3 cp "$assets\tictactest-$Version.jar" "s3://$artifactBucket/releases/$Version/tictactest-$Version.jar"

    $params = Join-Path $env:TEMP "ssm-params.json"
    ('{"commands":["/usr/local/bin/tictactest-deploy ' + $Version + '"]}') | Set-Content $params -Encoding ascii
    $commandId = aws ssm send-command --instance-ids $instanceId --document-name AWS-RunShellScript `
        --parameters "file://$params" --query Command.CommandId --output text
    if ($LASTEXITCODE -ne 0) { throw "send-command failed (is the instance registered in SSM?)" }

    aws ssm wait command-executed --command-id $commandId --instance-id $instanceId
    $waitFailed = ($LASTEXITCODE -ne 0)
    aws ssm get-command-invocation --command-id $commandId --instance-id $instanceId `
        --query "{status: Status, output: StandardOutputContent, errors: StandardErrorContent}" --output yaml
    if ($waitFailed) { throw "The deploy script failed on the instance (see output above)." }

    # 4. Check that exactly this version is running.
    $running = ""
    for ($i = 1; $i -le 24; $i++) {
        try { $running = (Invoke-RestMethod $versionUrl -TimeoutSec 5).ToString().Trim() } catch { $running = "" }
        if ($running -eq $Version) { break }
        Start-Sleep -Seconds 5
    }
    if ($running -ne $Version) { throw "Backend reports '$running', expected $Version" }
    Write-Host "Backend reports $running"
}

if (-not $SkipFrontend) {
    # 5. Frontend: replace the bucket content, but keep config.js (written by OpenTofu).
    $site = Join-Path $assets "site"
    Expand-Archive "$assets\tictactest-frontend-$Version.zip" -DestinationPath $site -Force
    Invoke-Checked aws s3 sync $site "s3://$frontendBucket" --delete --exclude "config.js"

    $served = (Invoke-RestMethod "$frontendUrl/version.txt" -TimeoutSec 10).ToString().Trim()
    if ($served -ne $Version) { throw "Frontend serves '$served', expected $Version" }
    Write-Host "Frontend serves $served"
}

Write-Host "Done. Play at: $($out.game_url.value)"
