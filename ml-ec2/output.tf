output "instanceid" {
  value = aws_instance.ec2_ml.id
}
output "instance_public_ip" {
  value = aws_instance.ec2_ml.public_ip
}
output "instance_private_ip" {
  value = aws_instance.ec2_ml.private_ip
}

output "private_dns"{
  value = aws_instance.ec2_ml.private_dns

}

