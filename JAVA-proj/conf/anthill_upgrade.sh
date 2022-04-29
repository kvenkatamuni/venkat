#!/bin/sh

LINE_SEP="-------------------------------------------------------------------------------------------------"

echo $LINE_SEP
echo
echo "`date` Running upgrade"
echo
curl -v --request GET 'http://localhost:8079/setup/upgrade'
echo
echo "`date` Completed running upgrade"
echo

echo $LINE_SEP
echo
echo "`date` Registering app roles"
echo 
curl -v --request GET 'http://localhost:8079/setup/registerApproles'
echo 
echo "`date` Completed registering app roles"
echo 

echo $LINE_SEP
echo
echo "`date` Running migration for Jiffy tables schema update"
echo
curl -v --request GET 'http://localhost:8079/migration/migrateJiffyTableSchemas'
echo 
echo "`date` Completed migration for Jiffy tables schema update"
echo

echo $LINE_SEP
echo
echo "`date` Running migration for permissions"
echo 
curl -v --request GET 'http://localhost:8079/migration/permissionMigration'
echo 
echo "`date` Completed migration for permissions"
echo 

echo $LINE_SEP
echo
echo "`date` Running migration for auto populate"
echo 
curl -v --request GET 'http://localhost:8079/migration/migrateAutoPopulate'
echo 
echo "`date` Completed migration for auto populate"
echo 

echo $LINE_SEP
echo
echo "`date` Running migration for service users"
echo 
curl -v --request GET 'http://localhost:8079/migration/migrateServiceUsers'
echo 
echo "`date` Completed migration for service users"
echo 

echo $LINE_SEP
echo
echo "`date` Running migration for table dependency"
echo 
curl -v --request GET 'http://localhost:8079/migration/migrateTableDependency'
echo 
echo "`date` Completed migration for table dependency"
echo 
echo $LINE_SEP
