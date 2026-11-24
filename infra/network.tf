locals {
  console_port = 7681
}

# Default VPC is used implicitly. Every open port has a reason:
#   7681  the web console: players' browsers connect here (public by design)
#   80    the version endpoint (nginx): the frontend and the pipeline read the running version
#   22    SSH, only if ssh_allowed_cidrs is set (debugging); deployments use SSM, not SSH
# Outbound traffic is open: package installs, the ttyd download, S3 and SSM need it.
resource "aws_security_group" "app" {
  name        = "${var.name_prefix}-app"
  description = "TicTacTest web console, version endpoint, optional SSH"

  ingress {
    description = "Web console (ttyd)"
    from_port   = local.console_port
    to_port     = local.console_port
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Version endpoint (nginx)"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  dynamic "ingress" {
    for_each = length(var.ssh_allowed_cidrs) > 0 ? [1] : []
    content {
      description = "SSH (debugging only)"
      from_port   = 22
      to_port     = 22
      protocol    = "tcp"
      cidr_blocks = var.ssh_allowed_cidrs
    }
  }

  egress {
    description = "All outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Fixed public address: survives the instance being stopped and started at the end of
# every Learner Lab session. Created independently of the instance, so the frontend's
# config.js can contain the address without a dependency cycle.
resource "aws_eip" "app" {
  domain = "vpc"

  tags = {
    Name = "${var.name_prefix}-eip"
  }
}
