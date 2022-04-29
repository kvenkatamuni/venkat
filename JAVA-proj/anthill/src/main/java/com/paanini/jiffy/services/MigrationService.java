package com.paanini.jiffy.services;

import com.option3.docube.schema.jiffytable.DataSchema;
import com.option3.docube.schema.jiffytable.TableType;
import com.option3.docube.schema.nodes.SubType;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.authorizationManager.AuthorizationService;
import com.paanini.jiffy.dto.QueryOptions;
import com.paanini.jiffy.jcrquery.QueryModel;
import com.paanini.jiffy.utils.MigrationManager;
import com.paanini.jiffy.utils.VfsManager;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.ExtraFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.Folder;
import com.paanini.jiffy.vfs.files.JiffyTable;
import com.paanini.jiffy.vfs.files.Presentation;
import com.paanini.jiffy.vfs.io.FolderViewOption;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MigrationService {

    private static final String MIGRATE_SHARED_SPACE = "[MIGRATE] shared space -- {} ";
    static Logger logger = LoggerFactory.getLogger(MigrationService.class);

    @Autowired
    VfsManager vfsManager;

    @Autowired
    RoleService roleService;

    @Autowired
    MigrationManager migrationManager;

    @Autowired
    AuthorizationService authorizationService;

    public void migratePermissions(){
        List<String> tenantIds = migrationManager.getTenantIds();
        QueryOptions options = new QueryOptions();
        FolderViewOption folderViewOption = FolderViewOption.getAppGroupWithoutJCR(options);
        Integer appCount = 0;
        long start=System.currentTimeMillis();
        for (String tenant : tenantIds){
            Folder sharesSpace = null;
            try {
                sharesSpace = migrationManager.getSharedSpace(folderViewOption,tenant);
            }catch (Exception e){
                logger.error(MIGRATE_SHARED_SPACE,e.getMessage());
                continue;
            }
            List<Persistable> appgroups = sharesSpace.getChildren();
            appCount = migratePermissions(appCount, tenant, appgroups);
        }
    }
    private boolean isTenantIdsFound(List<String> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            logger.info("No tenant found to migrate");
            return true;
        }
        return false;
    }

    private boolean isAppGroupsFound(String tenant, List<Persistable> appgroups) {
        if (appgroups == null || appgroups.isEmpty()) {
            logger.info("No app groups found for tenant {}", tenant);
            return true;
        }
        return false;
    }

    private Integer migratePermissions(Integer appCount, String tenant, List<Persistable> appgroups) {
        for(Persistable child : appgroups){
            if(SubType.appGroup.equals(((BasicFileProps)child).getSubType())){
                for(Persistable app : ((Folder)child).getChildren()){
                    logger.debug("Migrating  APP {}",((ExtraFileProps) app).getPath());
                    try{
                        migrationManager.migratePermissions(((BasicFileProps)app).getId(), tenant);
                        appCount++;
                    }catch (Exception e){
                        logger.debug("failed to migrate the app {}",((ExtraFileProps) app).getPath());
                        logger.error("[MIGRATE] -- {} ",e.getMessage());
                    }
                }
            }
       }
       return appCount;
    }

    public void migrateCustomRoles(){
        List<String> tenantIds = migrationManager.getTenantIds();
        QueryOptions options = new QueryOptions();
        FolderViewOption folderViewOption = FolderViewOption.getAppGroupWithoutJCR(options);
        for (String tenant : tenantIds){
            Folder sharesSpace = null;
            try {
                sharesSpace = migrationManager.getSharedSpace(folderViewOption,tenant);
            }catch (Exception e){
                logger.error(MIGRATE_SHARED_SPACE,e.getMessage());
                continue;
            }
            List<Persistable> appgroups = sharesSpace.getChildren();
            migrateCustomRoles(tenant, appgroups);
        }
    }

    private void migrateCustomRoles(String tenant, List<Persistable> appgroups) {
        for(Persistable child : appgroups){
            if(SubType.appGroup.equals(((BasicFileProps)child).getSubType())){
                for(Persistable app : ((Folder)child).getChildren()){
                    logger.debug("Migrating Tenant {} APP {}", tenant,((ExtraFileProps) app).getPath());
                    try{
                        migrationManager.migrateCustomRoles(((BasicFileProps)app).getId(), tenant);
                    }catch (Exception e){
                        logger.debug("failed to migrate the app {}",((ExtraFileProps) app).getPath());
                        logger.error("[MIGRATE] -- {} ",e.getMessage());
                    }
                }
            }
        }
    }


    public List<String> getTenantIds(){
        return migrationManager.getTenantIds();
    }


    public Map<String, String> migrateAutoPopulate() {
        QueryModel queryModel = new QueryModel();
        queryModel.addFilter("tableType", TableType.JDI.name());
        Integer appCount = 0;
        long start=System.currentTimeMillis();
        List<String> tenantIds = migrationManager.getTenantIds();
        QueryOptions options = new QueryOptions();
        Map<String,String> path = new HashMap<>();
        FolderViewOption folderViewOption = FolderViewOption.getAppGroupWithoutJCR(options);
        appCount = migrateAutoPopulate(queryModel, appCount, tenantIds, path, folderViewOption);
        long end=System.currentTimeMillis();
        logger.debug("[MIGRATE] migrateAutoPopulate {} took {}",appCount,end-start);
        return path;
    }

    private Integer migrateAutoPopulate(QueryModel queryModel, Integer appCount, List<String> tenantIds, Map<String, String> path, FolderViewOption folderViewOption) {
        for (String tenant : tenantIds) {
            Folder sharesSpace = null;
            try {
                sharesSpace = migrationManager.getSharedSpace(folderViewOption, tenant);
            } catch (Exception e) {
                logger.error(MIGRATE_SHARED_SPACE, e.getMessage());
                continue;
            }
            List<Persistable> appgroups = sharesSpace.getChildren();
            appCount = migrateAutoPopulate(queryModel, appCount, path, tenant, appgroups);
        }
        return appCount;
    }

    private Integer migrateAutoPopulate(QueryModel queryModel, Integer appCount, Map<String, String> path, String tenant, List<Persistable> appgroups) {
        for (Persistable child : appgroups) {
            if (SubType.appGroup.equals(((BasicFileProps) child).getSubType())) {
                for (Persistable c : ((Folder) child).getChildren()) {
                    logger.debug("Backing up jiffy table from APP {}", ((ExtraFileProps) c).getPath());
                    try {
                        Folder folderData = migrationManager.getJiffyTables(((BasicFileProps) c).getId(), Optional.of(queryModel));
                        folderData.getChildren().stream()
                                .forEach(c2 -> {
                                    roleService.migrateAutopopulate((JiffyTable) c2, tenant);
                                    path.put(((JiffyTable) c2).getPath(), tenant);
                                });
                        appCount++;
                    } catch (Exception e) {
                        logger.debug("failed to fetch the Jiffy table from the app {}", ((ExtraFileProps) c).getPath());
                        logger.error("[MIGRATE] jiffy table -- {} ", e.getMessage());
                    }
                }
            }
        }
        return appCount;
    }

    public void migrateServiceUsers(){
        List<String> tenantIds = migrationManager.getTenantIds();
        migrationManager.migrateServiceUsers(tenantIds);
        return ;
    }

    public Set<String> getPermittedIds(String appId,String tenantId){
        return authorizationService.getPermittedIdsForTenant(appId,tenantId);
    }

    public void migrateTableDependency(){
        QueryModel queryModel = new QueryModel();
        queryModel.addFilter("type", Type.PRESENTATION.name());
        Integer appCount = 0;
        long start=System.currentTimeMillis();
        List<String> tenantIds = migrationManager.getTenantIds();
        QueryOptions options = new QueryOptions();
        Map<String,String> path = new HashMap<>();
        FolderViewOption folderViewOption = FolderViewOption.getAppGroupWithoutJCR(options);
        for (String tenant : tenantIds) {
            Folder sharesSpace = null;
            try {
                sharesSpace = migrationManager.getSharedSpace(folderViewOption, tenant);
            } catch (Exception e) {
                logger.error(MIGRATE_SHARED_SPACE, e.getMessage());
                continue;
            }
            List<Persistable> appgroups = sharesSpace.getChildren();
            appCount = migrateTableDependency(queryModel, appCount, tenant, appgroups);
        }
        long end=System.currentTimeMillis();
        logger.debug("[MIGRATE] migrateTableDependency {} took {}",appCount,end-start);
    }

    private Integer migrateTableDependency(QueryModel queryModel, Integer appCount, String tenant, List<Persistable> appgroups) {
        for (Persistable child : appgroups) {
            if (SubType.appGroup.equals(((BasicFileProps) child).getSubType())) {
                for (Persistable c : ((Folder) child).getChildren()) {
                    logger.debug("Finding presentation in APP {}", ((ExtraFileProps) c).getPath());
                    try {
                        Folder folderData = migrationManager.getFolderData(((BasicFileProps) c).getId(), Optional.of(queryModel));
                        folderData.getChildren().stream()
                                .forEach(c2 -> {
                                    Presentation presentation = (Presentation) c2;
                                    Set<String> datasetIds = presentation.getContent().getDatasheets().stream()
                                            .map(datasheet -> datasheet.getId()).collect(Collectors.toSet());
                                    Set<String> categoryTableIdsForMigration = migrationManager.getCategoryTableIdsForMigration(datasetIds, presentation.getParentId());
                                    datasetIds.addAll(categoryTableIdsForMigration);
                                    authorizationService.migrateTableDependency(presentation.getId(), datasetIds, presentation.getParentId(), tenant);
                                });
                        appCount++;
                    } catch (Exception e) {
                        logger.debug("Failed to fetch the Jiffy tables from the app {}", ((ExtraFileProps) c).getPath());
                        logger.error("[MIGRATE] presentation -- {} ", e.getMessage());
                    }
                }
            }
        }
        return appCount;
    }

    public Map<String, String> updateJiffyTableSchemas() {
        QueryModel queryModel = new QueryModel();
        queryModel.addFilter("type", Type.JIFFY_TABLE.name());
        List<String> tenantIds = migrationManager.getTenantIds();
        QueryOptions options = new QueryOptions();
        Map<String,String> path = new HashMap<>();
        FolderViewOption folderViewOption = FolderViewOption.getAppGroupWithoutJCR(options);
        for (String tenant : tenantIds) {
            logger.info("Migrating jiffy table schema for tenant {} ", tenant);
            Folder sharesSpace = null;
            try {
                sharesSpace = migrationManager.getSharedSpace(folderViewOption, tenant);
            } catch (Exception e) {
                logger.error(MIGRATE_SHARED_SPACE, e.getMessage());
                continue;
            }
            List<Persistable> appgroups = sharesSpace.getChildren();
            updateJiffyTableSchemas(queryModel, path, tenant, appgroups);
        }
        return path;
    }

    private void updateJiffyTableSchemas(QueryModel queryModel, Map<String, String> path, String tenant, List<Persistable> appgroups) {
        for (Persistable child : appgroups) {
            if (SubType.appGroup.equals(((BasicFileProps) child).getSubType())) {
                for (Persistable c : ((Folder) child).getChildren()) {
                    logger.debug("Finding jiffy tables for schema update in APP {}", ((ExtraFileProps) c).getPath());
                    try {
                        Folder folderData = migrationManager.getFolderData(((BasicFileProps) c).getId(), Optional.of(queryModel));
                        folderData.getChildren().stream()
                                .forEach(c2 -> {
                                            migrateSchema(path, tenant, (JiffyTable) c2);
                                });
                    } catch (Exception e) {
                        logger.debug("Failed to fetch the Jiffy tables from the app {}", ((ExtraFileProps) c).getPath());
                        logger.error("[MIGRATE] jiffy table -- {} ", e.getMessage());
                    }
                }
            }
        }
    }

    private void migrateSchema(Map<String, String> path, String tenant, JiffyTable c2) {
        JiffyTable jiffyTable = c2;
        logger.info("Checking jiffy table {} for schema update", jiffyTable.getPath());
        if(migrationManager.isSchemaUpdatedRequired(jiffyTable)) {
            logger.trace("[MIGRATE] Updating jiffy table schema for {}", jiffyTable.getPath() );
            migrationManager.updateSchema(jiffyTable);
        }

        logger.debug("Checking jiffy table {} for schema migration", jiffyTable.getPath());
        if(!jiffyTable.getSchemas().isEmpty()){
            List<Document> dataSchemas = null;
            dataSchemas = jiffyTable.getSchemas().stream()
                    .map(dataSchema -> preProcessDocument(dataSchema))
                    .collect(Collectors.toList());
            try{
                authorizationService.addJiffyTableSchemas("jiffytablesSchemas", jiffyTable.getTableName(), dataSchemas);
                jiffyTable.setSchemas(Collections.emptyList());
                migrationManager.updateGeneric(jiffyTable);
                path.put(c2.getPath(), tenant);
                logger.info("successfully inserted to mongo");
            }catch (Exception e){
                logger.error("failed to add jiffy tables schemas into mongo DB for table {} {} ", jiffyTable.getPath() , e.getMessage());
            }
        }else{
            logger.debug("Jiffy table schema history is empty {} for schema migration", jiffyTable.getPath());
        }
    }

    private Document preProcessDocument(DataSchema dataSchema) {
        Document mongoDTO = new Document();
        mongoDTO.put("_id", dataSchema.getId());
        mongoDTO.append("schema", dataSchema.toString());
        return mongoDTO;
    }

}
