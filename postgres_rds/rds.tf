# IAM resources
#
data "aws_iam_policy_document" "enhanced_monitoring" {
  statement {
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["monitoring.rds.amazonaws.com"]
    }

    actions = ["sts:AssumeRole"]
  }
}

resource "aws_iam_role" "enhanced_monitoring" {
  name               = "rds${var.environment}EnhancedMonitoringRole"
  assume_role_policy = data.aws_iam_policy_document.enhanced_monitoring.json
}

resource "aws_iam_role_policy_attachment" "enhanced_monitoring" {
  role       = aws_iam_role.enhanced_monitoring.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}

resource "aws_db_subnet_group" "default" {
  name       = lower("${var.Name}-${replace(var.region, "-", "")}-DB-subnetGroup")
  subnet_ids = var.private_subnet_ids

  tags = merge(var.tags, { Name = "${var.Name}-${replace(var.region, "-", "")}-DB-subnetGroup" })

}
# get the latest rds snapshot

data "aws_db_snapshot" "rds_snapshot" {
  db_instance_identifier = "dr-test-rds"
  most_recent            = true
}

##Create RDS instance from snapshot

resource "aws_db_instance" "postgresql" {

  identifier_prefix               = var.database_identifier
  snapshot_identifier             = data.aws_db_snapshot.rds_snapshot.id
  instance_class                  = var.instance_type
  vpc_security_group_ids          = var.vpc_security_group_ids
  //name                            = var.database_name
  backup_retention_period         = var.backup_retention_period
  backup_window                   = var.backup_window
  maintenance_window              = var.maintenance_window
  auto_minor_version_upgrade      = var.auto_minor_version_upgrade
  final_snapshot_identifier       = var.final_snapshot_identifier
  skip_final_snapshot             = var.skip_final_snapshot
  copy_tags_to_snapshot           = var.copy_tags_to_snapshot
  multi_az                        = var.multi_availability_zone
  port                            = var.database_port
  db_subnet_group_name            = aws_db_subnet_group.default.name
  storage_encrypted               = var.storage_encrypted
  monitoring_interval             = var.monitoring_interval
  monitoring_role_arn             = var.monitoring_interval > 0 ? aws_iam_role.enhanced_monitoring.arn : ""
  deletion_protection             = var.deletion_protection
  enabled_cloudwatch_logs_exports = var.cloudwatch_logs_exports

  tags = merge(var.tags, { Name = "${var.Name}-${replace(var.region, "-", "")}-RDS" })
  
}







