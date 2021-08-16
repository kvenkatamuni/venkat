output "instanceid" {
  value = aws_instance.ec2_db.id
}
output "instance_public_ip" {
  value = aws_instance.ec2_db.public_ip
}
output "instance_private_ip" {
  value = aws_instance.ec2_db.private_ip
}

output "private_dns"{
  value = aws_instance.ec2_db.private_dns

}
