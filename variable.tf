#Common Variables

variable "platform_name" {
  description = "The name of the platform"
  #  default     = "Jiffy"
  type = string
}

variable "environment" {
  description = "Applicaiton environment"
  #  default     = "saas1"
  type = string
}

variable "Department" {
  description = "Department can be sales, training, demo, qa, dit"
  #  default     = "demo"
}
variable "Contact" {
  #  default = "devops"
}
# variable "Purpose" {
#   default = "Devops"
# }
variable "Product" {
  #  default = "automate"
}


### vpc variable

variable "region" {
  description = "aws region to deploy to"
  #  default     = "ap-south-1"
  type = string
}

variable "vpc_cidr_block" {
  type = string
  #  default     = "10.4.0.0/16"
  description = "CIDR block range for vpc"
}

variable "private_subnet_cidr_blocks" {
  type = list(string)
  #  default     = ["10.4.1.0/24", "10.4.2.0/24"]
  description = "CIDR block range for the private subnets"
}
variable "public_subnet_cidr_blocks" {
  type = list(string)
  #  default     = ["10.4.3.0/24", "10.4.4.0/24"]
  description = "CIDR block range for the public subnets"
}

### Security group variable

variable "existing_security_group" {
  description = "Leave blank to create a new security group. Otherwise it will use the VPC id to find an associated security group"
  #  default     = []
}

variable "security_group_rules_to_create" {
  description = "A slice of maps which contains the fields 'ingress_rules' and 'egress_rules'. Both of which are maps which contain the fields 'port', and 'cidr_blocks'"
   default = []

}

## RDS Variables ##

variable "instance_type" {
  #  default = "db.t3.micro"
}
variable "storage_type" {
#  default = "gp2"
}
variable "database_identifier" {
  #  default = "test"
}
variable "database_name" {
#  default = "test"
}

variable "database_port" {
#  default = "5432"
}
variable "backup_retention_period" {
#  default = "30"
}
variable "backup_window" {
#  default = "04:00-04:30"
}
variable "maintenance_window" {
#  default = "sun:04:30-sun:05:30"
}
variable "auto_minor_version_upgrade" {
#  default = "true"
}
variable "multi_availability_zone" {
#  default = false
}
variable "storage_encrypted" {
#  default = "true"
}

variable "monitoring_interval" {
#  default = "60"
}
variable "deletion_protection" {
#  default = "false"
}
//variable "database_engine" {
//#  default = "postgres"
//}

#####EC2 Variable

//variable "ami_id" {
//  description = "If no AMI ID is provided instance will use default centos image"
#  default     = ""
//}
variable "instance_type_core" {
#  default = "t3.medium"
}
variable "instance_type_ml" {
#  default = "t3.medium"
}
variable "instance_type_db" {
#  default = "t3.medium"
}
variable "instance_type_bot" {
#  default = "t3.medium"
}


//variable "root_vol_size" {
//#  default = 30
//}
//variable "root_vol_type" {
//#  default = "gp2"
//}
variable "delete_on_termination" {
#  default = "true"
}
variable "ebs_device_name" {
#  default = "/dev/sdb"
}

variable "Autoshutdown" {
  description = "change this to true to enable autoshutdown for Ec2 instances"
#  default     = "false"
}
variable "key_name_core" {
  description = "Use this along with the variable created_key_pair if a key with name exists Ec2 will use the key or it will create a new key with this name "
#  default     = "test"
}
variable "key_name_ml" {
  description = "Use this along with the variable created_key_pair if a key with name exists Ec2 will use the key or it will create a new key with this name "
#  default     = "test"
}
variable "key_name_mongo" {
  description = "Use this along with the variable created_key_pair if a key with name exists Ec2 will use the key or it will create a new key with this name "
#  default     = "test"
}

variable "key_name_bot" {
  description = "Use this along with the variable created_key_pair if a key with name exists Ec2 will use the key or it will create a new key with this name "
#  default     = "test"
}


## Create Key pair Variables
variable "created_key_pair" {
  description = "Add a public key generated from local machine to the value it will create a SSH Key in AWS."
# default     = null
}



##### ALB Variables.

variable "alb_internal" {
#  default = "false"
}
variable "lb_type" {
#  default = "application"
}

variable "enable_deletion_protection" {
#  default = "false"
}

############################################################################################################
### EFS Variables
############################################################################################################




variable "backup_life_cycle_delete" {
#  default     = "5"
  description = "Delete backup after given days"
#  type        = string
}
variable "backup_schedule" {
#  default     = "cron(0 3 ? * 2-7 *)" # every day except sundays (day index 1) at 3am UTC
  description = "Cron schedule to run backups on"
  type        = string
}

# LB Target Group Variables 

variable "domain_name" {
#  default = "saas19.jiffy.ai"
}
variable "alt_domain_name" {
#  default = "*.saas19.jiffy.ai"
}
variable "route53_base_domain" {
#  default = "jiffy.ai."
}

variable "efs_endpoint"{

 
}