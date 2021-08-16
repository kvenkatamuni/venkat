output "endpoint" {
  value       = aws_db_instance.postgresql.endpoint
  description = "Public DNS name and port separated by a colon"
}

output "endpoint-url"{
  value = aws_db_instance.postgresql.address
}
