# Creating Local variable to define common tags to be used.
locals {
  common_tags = {
    Department  = var.Department
    Product     = "${var.platform_name}-${var.Product}"
    Contact     = var.Contact
    environment = var.environment
  }
}
