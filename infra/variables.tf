variable "region" {
  description = "AWS region. Learner Lab only allows us-east-1 and us-west-2."
  type        = string
  default     = "us-east-1"
}

variable "instance_type" {
  description = "EC2 instance type (keep it small to keep costs low)."
  type        = string
  default     = "t2.micro"
}

variable "name_prefix" {
  description = "Prefix for the names of all created resources."
  type        = string
  default     = "tictactest"
}

variable "public_key_path" {
  description = "Path to the PUBLIC SSH key (.pub). The private key must sit next to it, outside the repo."
  type        = string
  default     = "~/.ssh/tictactest-aws.pub"
}

variable "ssh_allowed_cidrs" {
  description = "Who may reach SSH (port 22), e.g. [\"203.0.113.7/32\"]. Empty = port 22 stays closed (deployments use SSM, not SSH)."
  type        = list(string)
  default     = []
}

variable "ttyd_version" {
  description = "Pinned ttyd release that is installed on the instance."
  type        = string
  default     = "1.7.7"
}

variable "ttyd_sha256" {
  description = "SHA-256 of ttyd.x86_64 for ttyd_version (from the release's SHA256SUMS file). Change both together."
  type        = string
  default     = "8a217c968aba172e0dbf3f34447218dc015bc4d5e59bf51db2f2cd12b7be4f55"

  validation {
    condition     = can(regex("^[0-9a-f]{64}$", var.ttyd_sha256))
    error_message = "ttyd_sha256 must be a 64-character lowercase hex SHA-256."
  }
}

variable "max_clients" {
  description = "Maximum number of simultaneous players (one JVM of about 40 MB each)."
  type        = number
  default     = 10
}
