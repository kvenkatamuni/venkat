output "instanceid" {
  value = aws_instance.ec2_BOT.id
}
output "instance_public_ip" {
  value = aws_instance.ec2_BOT.public_ip
}
output "instance_private_ip" {
  value = aws_instance.ec2_BOT.private_ip
}

output "private_dns"{
  value = aws_instance.ec2_BOT.private_dns
}


