variable "database_identifier" {
  type        = string
  description = "Identifier for RDS instance"
}

variable "instance_type" {
  default     = "db.t3.micro"
  type        = string
  description = "Instance type for database instance"
}


variable "vpc_id" {
  type        = string
  description = "ID of VPC meant to house database"
}

//variable "database_name" {
//  type        = string
//  description = "Name of database inside storage engine"
//}

variable "backup_retention_period" {
  default     = 30
  type        = number
  description = "Number of days to keep database backups"
}

variable "backup_window" {
  # 12:00AM-12:30AM ET
  default     = "04:00-04:30"
  type        = string
  description = "30 minute time window to reserve for backups"
}

variable "maintenance_window" {
  # SUN 12:30AM-01:30AM ET
  default     = "sun:04:30-sun:05:30"
  type        = string
  description = "60 minute time window to reserve for maintenance"
}

variable "auto_minor_version_upgrade" {
  default     = true
  type        = bool
  description = "Minor engine upgrades are applied automatically to the DB instance during the maintenance window"
}

variable "final_snapshot_identifier" {
  default     = "terraform-aws-postgresql-rds-snapshot"
  type        = string
  description = "Identifier for final snapshot if skip_final_snapshot is set to false"
}

variable "skip_final_snapshot" {
  default     = true
  type        = bool
  description = "Flag to enable or disable a snapshot if the database instance is terminated"
}

variable "copy_tags_to_snapshot" {
  default     = false
  type        = bool
  description = "Flag to enable or disable copying instance tags to the final snapshot"
}

variable "multi_availability_zone" {
  default     = false
  type        = bool
  description = "Flag to enable hot standby in another availability zone"
}

//variable "copy_tags_to_snapshot" {
//  default     = false
//  type        = bool
//  description = "Flag to enable or disable copying instance tags to the final snapshot"
//}

variable "storage_encrypted" {
  default     = false
  type        = bool
  description = "Flag to enable storage encryption"
}

variable "database_port" {
  default     = 5432
  type        = number
  description = "Port on which database will accept connections"
}
variable "monitoring_interval" {
  default     = 0
  type        = number
  description = "The interval, in seconds, between points when Enhanced Monitoring metrics are collected"
}

variable "deletion_protection" {
  default     = false
  type        = bool
  description = "Flag to protect the database instance from deletion"
}

variable "vpc_security_group_ids" {
  type = list(any)
}


variable "region" {}

variable "Name" {}

variable "tags" {}


variable "environment" {
  default     = "Unknown"
  type        = string
  description = "Name of environment this VPC is targeting"
}

variable "private_subnet_ids" {
  type        = list(string)
  description = "IDs for private subnets"
}

variable "cloudwatch_logs_exports" {
  default     = ["postgresql", "upgrade"]
  type        = list(any)
  description = "List of logs to publish to CloudWatch Logs"
}
