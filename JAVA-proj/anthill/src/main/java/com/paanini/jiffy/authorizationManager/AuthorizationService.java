package com.paanini.jiffy.authorizationManager;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.constants.App;
import com.paanini.jiffy.constants.Authenication;
import com.paanini.jiffy.constants.Roles;
import com.paanini.jiffy.db.DataBaseMongoOperations;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.models.FilterObject;
import com.paanini.jiffy.models.RolesV2;
import com.paanini.jiffy.utils.RoleServiceUtilsV2;
import com.paanini.jiffy.utils.TenantHelper;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class AuthorizationService {
    private final String authdatabaseName;
    private final String authdbCollection="authCollection";
    private final String appRoledatabaseName;
    private final String defaultAppRoles="DEFAULTAPPROLES";
    private final String tableDependencyDb ="dependencyDb";
    private final String serviceUserDb ="serviceUserDb";
    private final String serviceUserCollection ="serviceUserCollection";
    private final String tableDependencyCollection = "dependencyCollection";
    private final String jiffytableDependencyCollection = "jtdependencyCollection";
    private final String defaultTenantAdminRoles="DEFAULTTENANTADMINROLES";
    private MongoClient client;
    private final DataBaseMongoOperations dbOps;
    static Logger logger = LoggerFactory.getLogger(AuthorizationService.class);
    public AuthorizationService(MongoClient mongoClient, String sslCAFilePath, String mongoUrl,
                                String authDatabase, String appRoledatabaseName){
        this.client=mongoClient;
        this.dbOps=new DataBaseMongoOperations(mongoClient);
        this.authdatabaseName=authDatabase;
        this.appRoledatabaseName=appRoledatabaseName;
    }

    public void addUserAppPermissions(Document document){
        dbOps.insertOne(authdatabaseName,getAuthdbCollection(),document);
    }

    public void addUserAppPermissionsForMigration(Document document,String tenantId){
        String authDbCollection = new StringBuilder(authdbCollection)
                .append("_")
                .append(tenantId).toString();
        dbOps.insertOne(authdatabaseName,authDbCollection,document);
    }

    public void updateUserAppPermissions(String key, String value, Document document){
        dbOps.updateRow(key,value,getAuthdbCollection(),authdatabaseName,document);
    }

    public void updateUserAppPermissionsForMigration(String key, String value, Document document,String tenantId){
        String authDbCollection = new StringBuilder(authdbCollection)
                .append("_")
                .append(tenantId).toString();
        dbOps.updateRow(key,value,authDbCollection,authdatabaseName,document);
    }

    public Document getAppUserPermission(String key, String value){
        return dbOps.getRow(key,value,getAuthdbCollection(),authdatabaseName);
    }

    public Document getAppUserPermissionForMigration(String key, String value, String tenantId){
        String authDbCollection = new StringBuilder(authdbCollection)
                .append("_")
                .append(tenantId).toString();
        return dbOps.getRow(key,value,authDbCollection,authdatabaseName);
    }

    public Map<String, Set<String>> getAppUsers(String appId){
        Map<String, Set<String>> data = new HashMap<>();
        List<Document> rows = dbOps.getRows(Authenication.APP_ID, appId, getAuthdbCollection(), authdatabaseName);
        for (Document row : rows){
            List<String> roles = (List<String>) ((Document) row.get(Authenication.AUTH_TABLE_VALUE)).get(Authenication.ROLES);
            data.put(row.get(Authenication.AUTH_TABLE_KEY).toString().split("::")[0],
                    new HashSet<>(roles));
        }
        return data;
    }

    public void deleteUserAppPermissions(String key, String value){
        dbOps.deleteRow(key,value,getAuthdbCollection(),authdatabaseName);
    }

    public void deleteMultipleUserAppPermissions(String appId, List<String> users){
        List<String> values = users.stream().map(user -> new StringBuilder(user).append("::").append(appId).toString())
                .collect(Collectors.toList());
        dbOps.deleteMultipleRow(Authenication.AUTH_TABLE_KEY,values,getAuthdbCollection(),authdatabaseName);
    }

    public void registerApproles(List<Document> documents){
        logger.debug("registering default  roles");
        dbOps.dropCollection(appRoledatabaseName,defaultAppRoles);
        logger.debug("dropped existing default  roles");
        dbOps.insertMany(appRoledatabaseName,defaultAppRoles,documents);
        logger.debug("Default roles registered");
    }

    public void registerCustomRoles(Document document,String appId){
        logger.debug("registering custom  roles");
        dbOps.insertOne(appRoledatabaseName,getAppRoleCollection(appId),document);
        logger.debug("Custom  roles registered");
    }

    public void migrateCustomRoles(Document document,String appId,String name){
        logger.debug("registering custom  roles");
        String collection = getAppRoleCollection(appId);
        Document customRole = dbOps.getRow(Authenication.AUTH_TABLE_KEY, name, collection, appRoledatabaseName);
        if(Objects.isNull(customRole)){
            dbOps.insertOne(appRoledatabaseName,collection,document);
        }
        logger.debug("Custom  roles registered");
    }

    public void editCustomRoles(Document document,String appId,String name){
        logger.debug("Editing custom role -- {}",name);
        dbOps.updateRow(Authenication.AUTH_TABLE_KEY,name,
                getAppRoleCollection(appId),appRoledatabaseName,document);
    }

    public List<RolesV2> getCustomRoles(String appId){
        String collection = getAppRoleCollection(appId);
        Document customRoleAcl = dbOps.getRow(Authenication.AUTH_TABLE_KEY, App.CUSTOM_ROLES, defaultAppRoles, appRoledatabaseName);
        List<Document> rows = dbOps.getRows(collection, appRoledatabaseName);
        return rows.stream().map(doc -> RoleServiceUtilsV2.buildCustomRoles(doc,customRoleAcl))
                .collect(Collectors.toList());
    }

    private String getAppRoleCollection(String appId) {
        return new StringBuilder("App").append(appId).toString();
    }

    public RolesV2 getDefaultRoles(String role){
        return getDefaultRoles(role,null);
    }

    public RolesV2 getDefaultRoles(String role,String appId){
        Document defaultRoles = dbOps.getRow(Authenication.AUTH_TABLE_KEY, role, defaultAppRoles, appRoledatabaseName);
        if(Objects.isNull(defaultRoles)){
            logger.error("Defnition for default role {} not found",role);
            throw new ProcessingException("System roles are not defined, please contact system administrator");
        }
        RolesV2 rolesV2 = RoleServiceUtilsV2.buildRoles(defaultRoles);
        if(Objects.nonNull(appId) && Roles.BUSINESS_USER.name().equals(role)){
            Set<String> permittedIds = getPermittedIds(appId);
            rolesV2.setFileIds(permittedIds);
        }
        return rolesV2;
    }

    public RolesV2 getCustomRole(String appId,String name){
        String collection = getAppRoleCollection(appId);
        Document customRoleAcl = dbOps.getRow(Authenication.AUTH_TABLE_KEY, App.CUSTOM_ROLES, defaultAppRoles, appRoledatabaseName);
        Document customRole = dbOps.getRow(Authenication.AUTH_TABLE_KEY, name, collection, appRoledatabaseName);
        RolesV2 rolesV2 = RoleServiceUtilsV2.buildCustomRoles(customRole,customRoleAcl);
        Set<String> permittedIds = rolesV2.getFileIds();
        Set<String> permittedDatasetIds = getPermittedIds(appId);
        permittedIds.addAll(permittedDatasetIds);
        return rolesV2;
    }

    public List<RolesV2> getDefaultRoles(){
        List<Document> rows = dbOps.getRows(defaultAppRoles, appRoledatabaseName);
        return rows.stream().filter(doc -> !doc.getString(Authenication.AUTH_TABLE_KEY).equals(App.CUSTOM_ROLES))
                .map(doc -> RoleServiceUtilsV2.buildRoles(doc))
                .collect(Collectors.toList());
    }

    public void deleteCustomRoles(String appId,String role){
        dbOps.deleteRow(Authenication.AUTH_TABLE_KEY,role,
                getAppRoleCollection(appId),appRoledatabaseName);
    }

    /**
     * on deleting the custom role. Auth details needs to be updated
     * removing users had only that custom role from app
     * removing the custom role from the users if he had multiple roles attached
     * @param appId
     * @param role
     */
    public void removefromUsers(String appId,String role){
        List<Document> rows = dbOps.getRows(Authenication.APP_ID, appId, getAuthdbCollection(), authdatabaseName);
        if(Objects.isNull(rows) || rows.isEmpty()){
            return;
        }
        List<Document> usersTobeUpdated = new ArrayList<>();
        List<String> usersToberemoved = new ArrayList<>();
        for(Document row : rows){
            Object o = ((Document) row.get(Authenication.AUTH_TABLE_VALUE))
                    .get(Authenication.ROLES);
            if(o instanceof ArrayList){
                ArrayList roles = (ArrayList) o;
                if(roles.contains(role)){
                    if(roles.size()==1){
                        usersToberemoved.add(row.get(Authenication.AUTH_TABLE_KEY).toString());
                    }else{
                        roles.remove(role);
                        ((Document) row.get(Authenication.AUTH_TABLE_VALUE))
                                .put(Authenication.ROLES,roles);
                        usersTobeUpdated.add(row);
                    }
                }
            }

        }
        logger.debug("removing users who had only custom role -- {} in app {}",role,appId);
        dbOps.deleteMultipleRow(Authenication.AUTH_TABLE_KEY,usersToberemoved,getAuthdbCollection(),authdatabaseName);
        logger.debug("updating users who had custom role -- {} in app {}",role,appId);
        updateAccess(usersTobeUpdated);
    }

    public void updateForUsers(String appId,String oldName,String newName){
        if(oldName.equals(newName)){
            return;
        }
        List<Document> rows = dbOps.getRows(Authenication.APP_ID, appId, getAuthdbCollection(), authdatabaseName);
        if(Objects.isNull(rows) || rows.isEmpty()){
            return;
        }
        List<Document> usersTobeUpdated = new ArrayList<>();
        for(Document row : rows){
            Object o = ((Document) row.get(Authenication.AUTH_TABLE_VALUE))
                    .get(Authenication.ROLES);
            if(o instanceof ArrayList){
                ArrayList roles = (ArrayList) o;
                if(roles.contains(oldName)){
                    roles.remove(oldName);
                    roles.add(newName);
                    ((Document) row.get(Authenication.AUTH_TABLE_VALUE))
                            .put(Authenication.ROLES,roles);
                    usersTobeUpdated.add(row);
                }
            }
        }
        logger.debug("updating users who had custom role -- {} in app {}",oldName,appId);
        updateAccess(usersTobeUpdated);
    }

    private void updateAccess(List<Document> rows){
        rows.forEach(row ->
                dbOps.updateRow(Authenication.AUTH_TABLE_KEY,
                        row.get(Authenication.AUTH_TABLE_KEY).toString(),
                        getAuthdbCollection(),
                        authdatabaseName,row));
    }

    public boolean checkPermission(List<RolesV2> rolesV2,String permission, Type type){
        if(AuthorizationUtils.getAllowedFiles().contains(type) || type.equals(Type.FOLDER)) {
            return true;
        }
        for (RolesV2 role : rolesV2) {
            List<String> permissions = role.getPermissionMap().get(type.name());
            if(Objects.nonNull(permissions) && permissions.contains(permission)){
                return true;
            }
        }
        return false;
    }

    /**
     * add map of dataset ids that are used in a presentaion this is for allowing business user to
     * access the particular data sets
     * @param presentationId
     * @param datasetIds
     * @param appId
     */

    public void addTableDependency(String presentationId,Set<String> datasetIds,String appId){
        List<Document> documents = datasetIds.stream().map(dtid ->
                new Document(Authenication.PRESENTATION_ID, presentationId)
                        .append(Authenication.DATASET_ID, dtid)
                        .append(Authenication.APP_ID,appId)).collect(Collectors.toList());
        dbOps.insertMany(tableDependencyDb,getTenantSpecificCollection(tableDependencyCollection),documents);
    }

    /**
     * add map of tables used in a  jiffy table form
     * used for giving permission to the Business user if that referencing jiffy table is used in any presentation
     * @param jiffyTableId
     * @param datasetIds
     * @param appId
     */

    public void addJiffyTableDependency(String jiffyTableId,Set<String> datasetIds,String appId){
        List<Document> documents = datasetIds.stream().map(dtid ->
                new Document(Authenication.JIFFYTABLE_ID, jiffyTableId)
                        .append(Authenication.DATASET_ID, dtid)
                        .append(Authenication.APP_ID,appId)).collect(Collectors.toList());
        dbOps.insertMany(tableDependencyDb,getTenantSpecificCollection(jiffytableDependencyCollection),documents);
    }

    /**
     * update map of tables used in a  jiffy table form
     * used for giving permission to the Business user if that referencing jiffy table is used in any presentation
     * @param jiffyTableId
     * @param datasetIds
     * @param appId
     */

    public void updateJiffyTableDependency(String jiffyTableId,Set<String> datasetIds,String appId){
        String tenantSpecificCollection = getTenantSpecificCollection(jiffytableDependencyCollection);
        List<Document> rows = dbOps.getRows(Authenication.JIFFYTABLE_ID, jiffyTableId,
                tenantSpecificCollection, tableDependencyDb);
        Set<String> existingDatasetIds = rows.stream().map(r -> r.get(Authenication.DATASET_ID).toString())
                .collect(Collectors.toSet());
        datasetIds.removeAll(existingDatasetIds);
        List<Document> documents = datasetIds.stream().map(dtid ->
                new Document(Authenication.JIFFYTABLE_ID, jiffyTableId)
                        .append(Authenication.DATASET_ID, dtid)
                        .append(Authenication.APP_ID,appId)).collect(Collectors.toList());
        dbOps.insertMany(tableDependencyDb, tenantSpecificCollection,documents);
    }


    public void migrateJiffyTableDependency(String jiffyTableId,Set<String> datasetIds,String appId,String tenantId){
        String tenantSpecificCollection = new StringBuilder(jiffytableDependencyCollection)
                .append("_")
                .append(tenantId).toString();
        List<Document> rows = dbOps.getRows(Authenication.JIFFYTABLE_ID, jiffyTableId,
                tenantSpecificCollection, tableDependencyDb);
        Set<String> existingDatasetIds = rows.stream().map(r -> r.get(Authenication.DATASET_ID).toString())
                .collect(Collectors.toSet());
        datasetIds.removeAll(existingDatasetIds);
        List<Document> documents = datasetIds.stream().map(dtid ->
                new Document(Authenication.JIFFYTABLE_ID, jiffyTableId)
                        .append(Authenication.DATASET_ID, dtid)
                        .append(Authenication.APP_ID,appId)).collect(Collectors.toList());
        dbOps.insertMany(tableDependencyDb, tenantSpecificCollection,documents);
    }

    /**
     * update map of dataset ids that are used in a presentaion this is for allowing business user to
     * access the particular data sets
     * @param presentationId
     * @param datasetIds
     * @param appId
     */

    public void updateTableDependency(String presentationId,Set<String> datasetIds,String appId){
        String tenantSpecificCollection = getTenantSpecificCollection(tableDependencyCollection);
        List<Document> rows = dbOps.getRows(Authenication.PRESENTATION_ID, presentationId,
                tenantSpecificCollection, tableDependencyDb);
        Set<String> existingDatasetIds = rows.stream().map(r -> r.get(Authenication.DATASET_ID).toString())
                .collect(Collectors.toSet());
        datasetIds.removeAll(existingDatasetIds);
        List<Document> documents = datasetIds.stream().map(dtid ->
                new Document(Authenication.PRESENTATION_ID, presentationId)
                        .append(Authenication.DATASET_ID, dtid)
                        .append(Authenication.APP_ID,appId)).collect(Collectors.toList());
        dbOps.insertMany(tableDependencyDb, tenantSpecificCollection,documents);
    }

    public void migrateTableDependency(String presentationId,Set<String> datasetIds,String appId,String tenantID){
        String tenantSpecificCollection = new StringBuilder(tableDependencyCollection)
                .append("_")
                .append(tenantID).toString();
        List<Document> rows = dbOps.getRows(Authenication.PRESENTATION_ID, presentationId,
                tenantSpecificCollection, tableDependencyDb);
        Set<String> existingDatasetIds = rows.stream().map(r -> r.get(Authenication.DATASET_ID).toString())
                .collect(Collectors.toSet());
        datasetIds.removeAll(existingDatasetIds);
        List<Document> documents = datasetIds.stream().map(dtid ->
                new Document(Authenication.PRESENTATION_ID, presentationId)
                        .append(Authenication.DATASET_ID, dtid)
                        .append(Authenication.APP_ID,appId)).collect(Collectors.toList());
        dbOps.insertMany(tableDependencyDb, tenantSpecificCollection,documents);
    }

    public void deleteTableDependency(String presentationId){
        dbOps.deleteRows(Authenication.PRESENTATION_ID,presentationId,
                getTenantSpecificCollection(tableDependencyCollection),tableDependencyDb);
    }

    public void createServiceUser(String username,String key){
        Document row = dbOps.getRow(Authenication.AUTH_TABLE_VALUE, username, serviceUserCollection, serviceUserDb);
        if(Objects.nonNull(row)){
            logger.debug("Service user already present");
            return;
        }
        Document doc = new Document(Authenication.AUTH_TABLE_KEY, key).append(Authenication.AUTH_TABLE_VALUE, username);
        dbOps.insertOne(serviceUserDb,serviceUserCollection,doc);
    }

    public void migrateServiceUsers(List<Document> documents){
        dbOps.insertMany(serviceUserDb,serviceUserCollection,documents);
    }

    public Boolean isServiceUser(String user){
        Document row = dbOps.getRow(Authenication.AUTH_TABLE_VALUE, user, serviceUserCollection, serviceUserDb);
        if(Objects.isNull(row)){
            return false;
        }else{
            return true;
        }
    }

    /**
     * return set of permitted file ids to a business user by traversing through the map stored in mongodb
     * @param appId
     * @return
     */

    public Set<String> getPermittedIds(String appId){
        Set<String> dataSetIds = new HashSet<>();
        List<Document> rows = dbOps.getRows(Authenication.APP_ID, appId,
                getTenantSpecificCollection(tableDependencyCollection), tableDependencyDb);
        rows.stream().forEach(r -> dataSetIds.add(r.get(Authenication.DATASET_ID).toString()));
        List<Document> rows2 = dbOps.getRows(Authenication.APP_ID, appId,
                getTenantSpecificCollection(jiffytableDependencyCollection), tableDependencyDb);
        dataSetIds.addAll(rows2.stream().map(r -> r.get(Authenication.DATASET_ID).toString())
                .collect(Collectors.toSet()));
        return dataSetIds;
    }

    public Set<String> getPermittedIdsForTenant(String appId,String tenantId){
        String tenantCollection = new StringBuilder(tableDependencyCollection)
                .append("_")
                .append(tenantId).toString();
        Set<String> dataSetIds = new HashSet<>();
        List<Document> rows = dbOps.getRows(Authenication.APP_ID, appId,
                tenantCollection, tableDependencyDb);
        rows.stream().forEach(r -> dataSetIds.add(r.get(Authenication.DATASET_ID).toString()));
        List<Document> rows2 = dbOps.getRows(Authenication.APP_ID, appId,
                tenantCollection, tableDependencyDb);
        dataSetIds.addAll(rows2.stream().map(r -> r.get(Authenication.DATASET_ID).toString())
                .collect(Collectors.toSet()));
        return dataSetIds;
    }

    public void setTenantDefaultAdminRole(String tenantId,Roles role){
        Document document = new Document(Authenication.AUTH_TABLE_KEY,tenantId)
                .append(Authenication.AUTH_TABLE_VALUE,role.name());
        dbOps.insertOne(appRoledatabaseName,defaultTenantAdminRoles,document);
    }

    public String getTenantDefaultAdminRole(){
        Document row = dbOps.getRow(Authenication.AUTH_TABLE_KEY, TenantHelper.getTenantId()
                , defaultTenantAdminRoles, appRoledatabaseName);
        if(Objects.nonNull(row))
            return row.get(Authenication.AUTH_TABLE_VALUE).toString();
        return Roles.DESIGNER.name();
    }
    
    private String getAuthdbCollection(){
        return new StringBuilder(authdbCollection)
                .append("_")
                .append(TenantHelper.getTenantId()).toString();
    }

    private String getTenantSpecificCollection(String collectionName){
        return new StringBuilder(collectionName)
                .append("_")
                .append(TenantHelper.getTenantId()).toString();
    }
    public void addJiffyTableSchemas(String databaseName, String collectionName, List<Document> docs){
        try {
            dbOps.bulkUpset(databaseName, collectionName, docs);
        }catch (Exception e) {
            logger.error("Failed to insert into mongo {} {} {} ", collectionName, e.getMessage(), e);
            throw new ProcessingException("Failed to insert into mongo");
        }
    }
}
