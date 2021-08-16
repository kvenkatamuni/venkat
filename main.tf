# # Create a new VPC for Jiffy
module "vpc_for_Jiffy" {
  source = "git::https://bitbucket.org/jiffy_bb_admin/vpc.git?ref=v16"
  #  vpc_tag_name              = "${var.platform_name}-vpc"
  number_of_private_subnets  = length(var.private_subnet_cidr_blocks)
  number_of_public_subnets   = length(var.public_subnet_cidr_blocks)
  private_subnet_cidr_blocks = var.private_subnet_cidr_blocks
  public_subnet_cidr_blocks  = var.public_subnet_cidr_blocks
  #  private_subnet_tag_name   = "${var.platform_name}-private-subnet"
  #  public_subnet_tag_name    = "${var.platform_name}-public-subnet"
  #  route_table_tag_name      = "${var.platform_name}-rt"
  environment    = var.environment
  platform_name  = var.platform_name
  Product        = var.Product
  region         = var.region
  vpc_cidr_block = var.vpc_cidr_block
  Name           = "${var.platform_name}-${var.Product}-${var.environment}"
  tags           = local.common_tags

}



#Create Security Groups This will create some default Security    used in Jiffy.
module "security_group" {
  source                    = "git::https://bitbucket.org/jiffy_bb_admin/securitygroup.git?ref=v10"
  vpc_id                    = module.vpc_for_Jiffy.vpc_id
  platform_name             = var.platform_name
  existing_security_group   = var.existing_security_group
  security_groups_to_create = var.security_group_rules_to_create
  region                    = var.region
  vpc_cidr_block            = var.vpc_cidr_block
  Name                      = "${var.platform_name}-${var.Product}-${var.environment}"
  tags                      = local.common_tags
}

### Core instance creation

module "core-instance"{

    source                          = "./core-ec2"
    name_suffix                     = "core"
    //ami_id                        = data.aws_ami.Core.id
    instance_type                   = var.instance_type_core
    subnet_id                       = module.vpc_for_Jiffy.private_subnet_ids
    associate_public_ip_address     = "false"
    vpc_security_group_ids          = [module.security_group.sg_id.core]
  //  root_vol_type                   = var.root_vol_type
    Name                            = "${var.platform_name}-${var.Product}-${var.environment}"
    Autoshutdown                    = var.Autoshutdown
    key_name                        = var.key_name_core
    //created_key_pair              = var.created_key_pair
    //region                        = var.region
    tags                            = local.common_tags
    az-name                         = 0
    iam_instance_profile            = "Ec2RoleForSSMAndCloudWatch"

   depends_on= [
     
       aws_route53_record.efs_dns

   ]
    
      
    
}

### Ml Instance Creation

module "ml-instance"{

    source                          = "./ml-ec2"
    name_suffix                     = "ml"
    //ami_id                        = data.aws_ami.ml.id
    instance_type                   = var.instance_type_ml
    subnet_id                       = module.vpc_for_Jiffy.private_subnet_ids
    associate_public_ip_address     = "false"
    vpc_security_group_ids          = [module.security_group.sg_id.ml]
    //root_vol_type                   = var.root_vol_type
    Name                            = "${var.platform_name}-${var.Product}-${var.environment}"
    Autoshutdown                    = var.Autoshutdown
    key_name                        = var.key_name_ml
    //created_key_pair              = var.created_key_pair
    //region                        = var.region
    tags                            = local.common_tags
    az-name                         = 0
    iam_instance_profile            = "Ec2RoleForSSMAndCloudWatch"

    depends_on= [
     
       aws_route53_record.efs_dns
       
   ]
}

#### DB Instance Creation

module "DB-instance"{

    source                           = "./db-ec2"
    name_suffix                      =    "db"
    //ami_id                         = data.aws_ami.db.id
    instance_type                    = var.instance_type_db
    subnet_id                        = module.vpc_for_Jiffy.private_subnet_ids
    associate_public_ip_address      = "false"
    vpc_security_group_ids           = [module.security_group.sg_id.mongodb]
    //root_vol_type                    = var.root_vol_type
    Name                             = "${var.platform_name}-${var.Product}-${var.environment}"
    Autoshutdown                     = var.Autoshutdown
    key_name                         = var.key_name_mongo
    //created_key_pair               = var.created_key_pair
    //region                         = var.region
    tags                             = local.common_tags
    az-name                          = 0
    iam_instance_profile             = "Ec2RoleForSSMAndCloudWatch"


    depends_on= [
     
       aws_route53_record.efs_dns
       
   ]
}

#### BOT Instance Creation

 module "BOT-instance"{
   
    source                              ="./bot-ec2"
    name_suffix                         ="bot"
    instance_type                       =var.instance_type_bot
    subnet_id                           =module.vpc_for_Jiffy.public_subnet_ids
    associate_public_ip_address         = "true"
    vpc_security_group_ids              = [module.security_group.sg_id.bot]
    Name                                = "${var.platform_name}-${var.Product}-${var.environment}"
    Autoshutdown                        = var.Autoshutdown
    key_name                            = var.key_name_bot
    tags                                = local.common_tags
    az-name                             = 0
    iam_instance_profile                = "Ec2RoleForSSMAndCloudWatch"

 }



### RDS Creation

module "postgres_rds"{
    
  source                            ="./postgres_rds"
  vpc_id                            = module.vpc_for_Jiffy.vpc_id
  database_identifier               = var.database_identifier
  //snapshot_identifier             = data.aws_db_snapshot.rds_snapshot.id
  instance_type                     = var.instance_type
 vpc_security_group_ids             = [module.security_group.sg_id.rds]
  //database_name                     = var.database_name
  backup_retention_period           = var.backup_retention_period
  backup_window                     = var.backup_window
  maintenance_window                = var.maintenance_window
  auto_minor_version_upgrade        = var.auto_minor_version_upgrade
  private_subnet_ids                = module.vpc_for_Jiffy.private_subnet_ids
  //final_snapshot_identifier       = var.final_snapshot_identifier
  //skip_final_snapshot             = var.skip_final_snapshot
  //copy_tags_to_snapshot           = var.copy_tags_to_snapshot
  multi_availability_zone           = var.multi_availability_zone
  database_port                     = var.database_port
  //db_subnet_group_name            = aws_db_subnet_group.default.name
  storage_encrypted                 = var.storage_encrypted
  monitoring_interval               = var.monitoring_interval
  //monitoring_role_arn             = var.monitoring_interval > 0 ? aws_iam_role.enhanced_monitoring.arn : ""
  deletion_protection               = var.deletion_protection
  region      = var.region
  Name        = "${var.platform_name}-${var.Product}-${var.environment}"
  tags        = local.common_tags
  environment = var.environment
    
}

#### ALB Module
module "alb_module" {
  source =  "git::https://bitbucket.org/jiffy_bb_admin/alb.git?ref=v8"
## source = "../alb_module"
  providers = {
    aws.root-account = aws.root
    aws              = aws
  }
  region                     = var.region
  tags                       = local.common_tags
  Name                       = var.environment #ALB name has a character limit of 32  "${var.platform_name}-${var.Product}-${var.environment}"
  alb_internal               = var.alb_internal
  lb_type                    = var.lb_type
  alb_access_sg              = [module.security_group.sg_id.lb]
  pub_subnets                = module.vpc_for_Jiffy.public_subnet_ids
  enable_deletion_protection = var.enable_deletion_protection
  aws_lb_target_group_vpc    = module.vpc_for_Jiffy.vpc_id
  domain_name                = var.domain_name
  alt_domain_name            = var.alt_domain_name
  target_instance_id         = module.core-instance.instanceid
  route53_zone_id            = data.aws_route53_zone.selected.id
}

module "efs_module" {
  source = "git::https://bitbucket.org/jiffy_bb_admin/efs.git?ref=v6"
  # name                     = "efs_file"
  subnets                  = module.vpc_for_Jiffy.private_subnet_ids
  efs_security_group_id    = [module.security_group.sg_id.efs]
  region                   = var.region
  tags                     = local.common_tags
  Name                     = "${var.platform_name}-${var.Product}-${var.environment}"
  backup_life_cycle_delete = var.backup_life_cycle_delete
  backup_schedule          = var.backup_schedule
}



resource "aws_route53_record" "ml_dns" {

  provider = aws.root
  zone_id  = data.aws_route53_zone.selected.zone_id
  name     = "cognitive.${var.domain_name}"
  type     = "CNAME"
  ttl      = 60
  allow_overwrite = true
  records = [module.ml-instance.private_dns]
}


resource "aws_route53_record" "db_dns" {

  provider = aws.root
  zone_id  = data.aws_route53_zone.selected.zone_id
  name     = "mongodb.${var.domain_name}"
  type     = "CNAME"
  ttl      = 60
  allow_overwrite = true
  records = [module.DB-instance.private_dns]
}

resource "aws_route53_record" "rds_dns" {

  provider = aws.root
  zone_id  = data.aws_route53_zone.selected.zone_id
  name     = "postgres.${var.domain_name}"
  type     = "CNAME"
  ttl      = 60
  allow_overwrite = true
  records = [module.postgres_rds.endpoint-url]
}

resource "aws_route53_record" "efs_dns" {

   provider = aws.root
 zone_id  = data.aws_route53_zone.selected.zone_id
  name     = "efs.${var.domain_name}"
  type     = "CNAME"
  ttl      = 60
  allow_overwrite = true
  records = [var.efs_endpoint]
}
resource "aws_route53_record" "bot_dns" {

  provider = aws.root
  zone_id  = data.aws_route53_zone.selected.zone_id
  name     = "bot1.${var.domain_name}"
  type     = "CNAME"
  ttl      = 60
  allow_overwrite = true
  records = [module.ml-instance.private_dns]
}


//output "testoutput"{
//  value = module.postgres_rds.endpoint.address
//}

