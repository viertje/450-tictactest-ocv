# Copies the OpenTofu outputs the pipeline needs into GitHub Actions VARIABLES (not secrets:
# they are addresses and names). Run it after every `tofu apply` that created new resources,
# for example after `tofu destroy` followed by `tofu apply`.
#
# Needs: gh (signed in) and tofu. Usage:  .\scripts\set-github-variables.ps1
param(
    [string]$InfraDir = "$PSScriptRoot\..\infra",
    [string]$Repo = "viertje/450-tictactest-ocv"
)

$ErrorActionPreference = "Stop"

$json = tofu "-chdir=$InfraDir" output -json github_variables
if ($LASTEXITCODE -ne 0) { throw "tofu output failed. Did 'tofu apply' run in $InfraDir?" }

$variables = $json | ConvertFrom-Json
foreach ($entry in $variables.PSObject.Properties) {
    gh variable set $entry.Name --repo $Repo --body $entry.Value
    if ($LASTEXITCODE -ne 0) { throw "gh variable set $($entry.Name) failed" }
    Write-Host "set $($entry.Name)"
}
