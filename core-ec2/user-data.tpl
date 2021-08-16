#! /bin/bash
hostnamectl set-hostname saas-15-core
sudo systemctl start rabbitmq-server
sudo systemctl enable rabbitmq-server