## This is the provider definition where all the resources will get created. 
provider "aws" {
  profile = "root"
  region  = var.region
  
  }

## Set the AWS profile for the ROOT AWS account where the route53 hosted zone is active
provider "aws" {
  alias   = "root"
  region  = var.region
  profile = "root"
  
}

# Fetching route53 zone_id using the domain name variable. (This is not working inside a module !!!)
data "aws_route53_zone" "selected" {
  provider     = aws.root
  name         = var.route53_base_domain
  private_zone = false
}

data "aws_iam_instance_profile" "CW_SSM_Access" {
  name = "Ec2RoleForSSMAndCloudWatch"
}

data "aws_iam_instance_profile" "CW_SSM_ECR_Access" {
  name = "Ec2RoleForSSMCloudWatchAndECR"
}
## Get Availability zones
data "aws_availability_zones" "available" {
  state = "available"
}
