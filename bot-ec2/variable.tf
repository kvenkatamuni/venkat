variable "instance_type" {
  description = "Enter the instance type ID"
  type        = string
}

variable "subnet_id" {

}

variable "vpc_security_group_ids" {
  
}
//variable "root_vol_size" {
// default = "30"
//}
//variable "root_vol_type" {
//  default = "gp2"
//}
variable "delete_on_termination" {
  default = "true"
}
variable "ebs_device_name" {
  default = "/dev/sdb"
}

variable "Name" {
  description = "Name for this resource "
}

variable "Autoshutdown" {
  description = "if True this instance will be dowm automatically at 10PM IST"
  default ="false"
}
variable "associate_public_ip_address" {
  description = "If true, the EC2 instance will have associated public IP address"
  default     = "true"
}

variable "key_name" {
  description = "The name for the key pair."
  type        = string
  default     = "test"
}

variable "tags" {
  description = "A map of tags to add to key pair resource."
  default     = {}
}

variable "az-name" {

}

variable "iam_instance_profile" {}

variable "name_suffix" {}

