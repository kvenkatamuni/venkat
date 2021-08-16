#! /bin/bash
sudo mongod --dbpath /opt/mongo/data --repair
sudo mongod -f /opt/mongo/conf/mongod.conf
