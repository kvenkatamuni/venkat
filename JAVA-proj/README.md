Prerequisite Software Java 8 apache-maven-3.5.0 Build Steps 1. cd zeus 2. mvn clean package

Output zeus/target/anthill-*-exec.jar

Deployment Steps 1. anthill jar to $JIFFY_HOME/deploy/anthill/bin/ 2. Copy conf to $JIFFY_HOME/deploy/anthill/

Command to Run java -jar -Dlogging.config=$JIFFY_HOME/deploy/anthill/conf/logback.xml -Dspring.config=$JIFFY_HOME/properties/anthill.properties $JIFFY_HOME/deploy/anthill/bin/anthill-*-exec.jar
Shell Script to start ZEUS $JIFFY_HOME/deploy/anthill/conf/start_engines.sh zeus

