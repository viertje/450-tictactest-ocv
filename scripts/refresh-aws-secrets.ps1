# Copies the current Learner Lab credentials into the GitHub Actions secrets of this repository.
# Run it after EVERY "Start Lab": the credentials end with the lab session.
#
# Needs the GitHub CLI, signed in once with:  gh auth login
# Usage:  .\scripts\refresh-aws-secrets.ps1
param(
    [string]$CredentialsFile = "$HOME\.aws-school\credentials",
    [string]$Repo = "viertje/450-tictactest-ocv"
)

$ErrorActionPreference = "Stop"

$values = @{}
foreach ($line in Get-Content $CredentialsFile) {
    if ($line -match '^\s*(aws_access_key_id|aws_secret_access_key|aws_session_token)\s*=\s*(.+?)\s*$') {
        $values[$Matches[1]] = $Matches[2]
    }
}

$secrets = @{
    AWS_ACCESS_KEY_ID     = "aws_access_key_id"
    AWS_SECRET_ACCESS_KEY = "aws_secret_access_key"
    AWS_SESSION_TOKEN     = "aws_session_token"
}

foreach ($name in $secrets.Keys) {
    $value = $values[$secrets[$name]]
    if (-not $value) { throw "$($secrets[$name]) not found in $CredentialsFile" }
    gh secret set $name --repo $Repo --body $value
    if ($LASTEXITCODE -ne 0) { throw "gh secret set $name failed" }
}

Write-Host "Updated 3 secrets in $Repo. Values are not printed."
