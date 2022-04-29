#!/bin/sh

start_anthill(){
cd $JIFFY_HOME
echo "Starting ANTHILL..."
java -jar -Xms512m -Xmx6g -Dlogging.config=$JIFFY_HOME/deploy/anthill/conf/logback.xml -Dspring.config.location=$JIFFY_HOME/properties/anthill.properties $JIFFY_HOME/deploy/anthill/bin/anthill*-exec.jar
echo "Started.."
}

if [ "$1" = "anthill" ]
then
  echo "Selected option : $1"
  start_anthill
else
  echo "Invalid option!.. \n Use 'anthill'"
fi

