# Get the core instance latest ami

data "aws_ami" "Core" {
  owners      = ["self"]
  most_recent = true
  filter {
  name   = "name"
  values = ["AwsBackup_i-09c14136ec8f9a7b9*"]
}
  filter {
    name   = "architecture"
    values = ["x86_64"]
  }
  filter {
    name   = "root-device-type"
    values = ["ebs"]
  }
  filter {
    name   = "state"
    values = ["available"]
  }
}

## Get Availability zones
data "aws_availability_zones" "available" {
  state = "available"
}


resource "aws_instance" "ec2_core" {
  ami                         = data.aws_ami.Core.id
  instance_type               = var.instance_type
  subnet_id                   = var.subnet_id[var.az-name]
  associate_public_ip_address = var.associate_public_ip_address
  vpc_security_group_ids      = var.vpc_security_group_ids
  iam_instance_profile        = var.iam_instance_profile
  
//  root_block_device {
//    volume_size           = var.root_vol_size
//    volume_type           = var.root_vol_type
//    delete_on_termination = var.delete_on_termination
//  }
  //ebs_block_device {
  //  device_name           = var.ebs_device_name
  //  volume_size           = var.root_vol_size
  //  volume_type           = var.root_vol_type
  //  delete_on_termination = var.delete_on_termination
  //}
  tags = merge(var.tags, { Name = "${var.Name}-ec2-${replace(data.aws_availability_zones.available.names[var.az-name], "-", "")}-${var.name_suffix}" })

  key_name                = var.key_name
//  user_data = <<-EOT
//    sudo hostnamectl set-hostname saas-15-core
//    sudo systemctl start rabbitmq-server
//    sudo systemctl enable rabbitmq-server
//  EOT
//provisioner "remote-exec" {
//inline = ["sudo hostnamectl set-hostname saas-15-core.jiffy.ai"]
//}
  user_data = "${data.template_file.init.rendered}"

}
data "template_file" "init" {
template = "${file("${path.module}/user-data.tpl")}"
}




