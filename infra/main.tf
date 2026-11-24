terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# Provider: the link between OpenTofu and AWS.
# Credentials are NOT configured here; they come from the environment
# (AWS_SHARED_CREDENTIALS_FILE / AWS_CONFIG_FILE, see aws-school).
provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project   = "TicTacTest"
      ManagedBy = "OpenTofu"
    }
  }
}

# AMI IDs differ per region, so look up the latest Amazon Linux 2023 image
# instead of hard-coding an ID. (The instance ignores later AMI changes, see compute.tf.)
data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# Only the PUBLIC key is uploaded. The private key stays on this computer.
resource "aws_key_pair" "tictactest" {
  key_name   = "${var.name_prefix}-key"
  public_key = file(pathexpand(var.public_key_path))
}
