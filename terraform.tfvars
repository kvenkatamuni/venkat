############################################################################################################
##### Common Variables for all modules and tags #####
############################################################################################################

platform_name = "Jiffy"
environment   = "saas-15"
Department    = "production"
Contact       = "Sekhar"
Product       = "automate"
region        = "ap-southeast-1"

############################################################################################################
##### VPC Variables #####
############################################################################################################

vpc_cidr_block             = "10.15.0.0/16"
private_subnet_cidr_blocks = ["10.15.1.0/24", "10.15.2.0/24"]
public_subnet_cidr_blocks  = ["10.15.3.0/24", "10.15.4.0/24"]

############################################################################################################
##### Security group Variables  #####
############################################################################################################

existing_security_group        = []
//security_group_rules_to_create = []
# security_group_rules_to_create variable can have a map of values similar to below mentioned
# This will create a new security group dynamically
 security_group_rules_to_create = [
   {
     name = "bastionsg-sg"

     "ingress_rules" = [{
       "port"        = 22,
       "cidr_blocks" = ["43.254.43.186/32"]
     }],
     "egress_rules" = [{
       "port"        = 0,
       "cidr_blocks" = ["0.0.0.0/0"]
     }]
   }
 ]

############################################################################################################
##### RDS Variables #####
############################################################################################################


//engine_version             = "12.4"
instance_type              = "db.m5.large"
storage_type               = "gp2"
database_identifier        = "jiffy-rds-saas-15-test"
database_name              = "jiffy"
database_port              = "5432"
backup_retention_period    = "30"
backup_window              = "04:00-04:30"
maintenance_window         = "sun:04:30-sun:05:30"
auto_minor_version_upgrade = true
multi_availability_zone    = false
storage_encrypted          = true
monitoring_interval        = "60"
deletion_protection        = false
//database_engine            = "postgres"


############################################################################################################
##### Ec2 Variables #####
############################################################################################################

//ami_id_core = ""
//ami_id_ml = ""
//ami_id = ""
## If no AMI ID is provided  in ami_id instance will created using the default centos image"

instance_type_core = "t3a.2xlarge"
instance_type_ml  = "t3a.xlarge"
instance_type_db  = "t3a.large"
instance_type_bot = "t3a.large"
//root_vol_size         = "100"
//root_vol_type         = "gp2"
delete_on_termination = true
ebs_device_name       = "/dev/sdb"
Autoshutdown          = "false"
key_name_core         = "jiffycore_saas_15"
key_name_ml           = "jiffyml_saas_15"
key_name_mongo        = "jiffymongo_saas_15"
key_name_bot          = "jiffybot_saas_15"   
created_key_pair      = null
## set created_key_pair to null will use an existing keypair in the region,
## created_key_pair value should be a public certificate generated locally
## key_name will use the existing Key pair name in AWS if created_key_pair is set to null




############################################################################################################
##### ALB Variables #####
############################################################################################################
alb_internal               = false # Setting this to true will create an internal ELB
lb_type                    = "application"
enable_deletion_protection = false

##### LB Target Group and ACM Variables  #####
domain_name         = "saas-15.jiffy.ai"
alt_domain_name     = "*.saas-15.jiffy.ai"
route53_base_domain = "jiffy.ai."   # put "." after the domain name
###################################################################################

############################################################################################################
##### EFS Variables #####
############################################################################################################
backup_life_cycle_delete = 5
backup_schedule          = "cron(0 3 ? * 2-7 *)" # every day except sundays (day index 1) at 3am UTC



efs_endpoint= "fs-69b80a29.efs.ap-southeast-1.amazonaws.com"
