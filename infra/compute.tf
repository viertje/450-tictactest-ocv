# Learner Lab forbids creating IAM roles: use the profile that already exists.
# It lets the instance call S3 and SSM without any access keys on the machine.
data "aws_iam_instance_profile" "lab" {
  name = "LabInstanceProfile"
}

locals {
  console_url     = "http://${aws_eip.app.public_ip}:${local.console_port}"
  version_url     = "http://${aws_eip.app.public_ip}/version"
  frontend_origin = "http://${aws_s3_bucket_website_configuration.frontend.website_endpoint}"
}

resource "aws_instance" "app" {
  ami                         = data.aws_ami.al2023.id
  instance_type               = var.instance_type
  key_name                    = aws_key_pair.tictactest.key_name
  vpc_security_group_ids      = [aws_security_group.app.id]
  associate_public_ip_address = true
  iam_instance_profile        = data.aws_iam_instance_profile.lab.name

  # First-boot setup (runtime, service, nginx, deploy script). It contains NO application
  # version: new releases are deployed by running the deploy script, never by changing this.
  user_data = templatefile("${path.module}/user_data.sh.tftpl", {
    ttyd_version    = var.ttyd_version
    ttyd_sha256     = var.ttyd_sha256
    max_clients     = var.max_clients
    console_port    = local.console_port
    artifact_bucket = local.artifact_bucket
    frontend_origin = local.frontend_origin
  })
  # Default (false): changing user_data does not recreate the instance. Set to true and the
  # instance is destroyed and rebuilt on every user_data change. See docs/deployment.md.
  user_data_replace_on_change = false

  # IMDSv2 only: a process on the machine cannot fetch the instance credentials with a
  # simple GET request.
  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
  }

  tags = {
    Name = "${var.name_prefix}-ec2"
  }

  lifecycle {
    # `most_recent` picks a newer Amazon Linux image over time. Without this, a later
    # `tofu plan` would want to replace the instance (and lose the deployed version).
    ignore_changes = [ami]
  }
}

resource "aws_eip_association" "app" {
  instance_id   = aws_instance.app.id
  allocation_id = aws_eip.app.id
}
