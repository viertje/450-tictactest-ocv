# Why the buckets are created with the AWS CLI and not with `aws_s3_bucket`:
# after creating a bucket, the provider's `aws_s3_bucket` resource always reads its object
# lock configuration. Learner Lab's service control policy explicitly denies exactly that call
# (s3:GetBucketObjectLockConfiguration), so every `aws_s3_bucket` fails with AccessDenied.
# Creating a bucket is allowed, and the other S3 resources below do not make that call.
# Bucket names must be globally unique: the account ID makes them unique but not random.
data "aws_caller_identity" "current" {}

locals {
  frontend_bucket_name = "${var.name_prefix}-frontend-${data.aws_caller_identity.current.account_id}"
  artifact_bucket_name = "${var.name_prefix}-artifacts-${data.aws_caller_identity.current.account_id}"
  # us-east-1 is the only region that must not be given a location constraint.
  location_constraint = var.region == "us-east-1" ? "" : " --create-bucket-configuration LocationConstraint=${var.region}"

  # Reading `.output` (not the input) makes everything below wait until the bucket exists.
  frontend_bucket = terraform_data.frontend_bucket.output.name
  artifact_bucket = terraform_data.artifacts_bucket.output.name
}

# --- Frontend: public static website -----------------------------------------------------
# NEVER put keys, state files or .tfvars into this bucket: everything in it is public.
resource "terraform_data" "frontend_bucket" {
  input = {
    name   = local.frontend_bucket_name
    region = var.region
  }

  provisioner "local-exec" {
    command = "aws s3api create-bucket --bucket ${self.input.name} --region ${self.input.region}${local.location_constraint}"
  }

  # --force empties the bucket first, so `tofu destroy` works after releases were deployed.
  provisioner "local-exec" {
    when    = destroy
    command = "aws s3 rb s3://${self.input.name} --force"
  }
}

resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket = local.frontend_bucket

  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

resource "aws_s3_bucket_website_configuration" "frontend" {
  bucket = local.frontend_bucket

  index_document {
    suffix = "index.html"
  }
}

resource "aws_s3_bucket_policy" "frontend_public_read" {
  bucket = local.frontend_bucket

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "PublicRead"
      Effect    = "Allow"
      Principal = "*"
      Action    = "s3:GetObject"
      Resource  = "arn:aws:s3:::${local.frontend_bucket}/*"
    }]
  })

  depends_on = [aws_s3_bucket_public_access_block.frontend]
}

locals {
  # The only environment-specific file of the frontend. OpenTofu owns it: it is written here
  # and the frontend deployment syncs with --exclude config.js, so it is never overwritten.
  config_js = <<-EOT
    window.TICTACTEST_CONFIG = {
      consoleUrl: "${local.console_url}",
      versionUrl: "${local.version_url}"
    };
  EOT
}

resource "aws_s3_object" "config" {
  bucket       = local.frontend_bucket
  key          = "config.js"
  content      = local.config_js
  content_type = "application/javascript"
  etag         = md5(local.config_js)
}

# --- Artifacts: private bucket the pipeline copies release jars into ----------------------
resource "terraform_data" "artifacts_bucket" {
  input = {
    name   = local.artifact_bucket_name
    region = var.region
  }

  provisioner "local-exec" {
    command = "aws s3api create-bucket --bucket ${self.input.name} --region ${self.input.region}${local.location_constraint}"
  }

  provisioner "local-exec" {
    when    = destroy
    command = "aws s3 rb s3://${self.input.name} --force"
  }
}

resource "aws_s3_bucket_public_access_block" "artifacts" {
  bucket = local.artifact_bucket

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
