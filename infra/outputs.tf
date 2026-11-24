# None of these values is a secret (no keys, no tokens). They are addresses and names that
# anyone who visits the site can see anyway. Account-internal values (bucket names, instance
# ID) are not secret either but there is no reason to publish them; they are used only by
# the pipeline and live in GitHub Variables, not Secrets.

output "game_url" {
  description = "Where people play (the frontend)."
  value       = "http://${aws_s3_bucket_website_configuration.frontend.website_endpoint}/"
}

output "console_url" {
  description = "The web console (ttyd), embedded by the frontend."
  value       = local.console_url
}

output "version_url" {
  description = "Version of the deployed backend."
  value       = local.version_url
}

output "elastic_ip" {
  description = "Fixed public address of the instance."
  value       = aws_eip.app.public_ip
}

output "instance_id" {
  description = "EC2 instance ID."
  value       = aws_instance.app.id
}

output "frontend_bucket" {
  description = "Bucket that serves the frontend."
  value       = local.frontend_bucket
}

output "artifact_bucket" {
  description = "Private bucket for release jars."
  value       = local.artifact_bucket
}

output "ssh_command" {
  description = "SSH command (works only if ssh_allowed_cidrs contains your IP)."
  value       = "ssh -i ${trimsuffix(var.public_key_path, ".pub")} ec2-user@${aws_eip.app.public_ip}"
}

# Everything the pipeline needs as GitHub Actions Variables (see scripts/set-github-variables.ps1).
output "github_variables" {
  description = "Values for GitHub Actions Variables."
  value = {
    AWS_REGION      = var.region
    INSTANCE_ID     = aws_instance.app.id
    ARTIFACT_BUCKET = local.artifact_bucket
    FRONTEND_BUCKET = local.frontend_bucket
    VERSION_URL     = local.version_url
    FRONTEND_URL    = "http://${aws_s3_bucket_website_configuration.frontend.website_endpoint}"
  }
}
