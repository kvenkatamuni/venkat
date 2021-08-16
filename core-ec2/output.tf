output "instanceid" {
  value = aws_instance.ec2_core.id
}
output "instance_public_ip" {
  value = aws_instance.ec2_core.public_ip
}
output "instance_private_ip" {
  value = aws_instance.ec2_core.private_ip
}

output "private_dns"{
  value = aws_instance.ec2_core.private_dns
}



