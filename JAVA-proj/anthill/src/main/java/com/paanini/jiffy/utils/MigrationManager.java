package com.paanini.jiffy.utils;

import com.option3.docube.schema.jiffytable.Column;
import com.option3.docube.schema.jiffytable.DataSchema;
import com.option3.docube.schema.jiffytable.InnerTableSchema;
import com.option3.docube.schema.jiffytable.NestedStructureSchema;
import com.option3.docube.schema.approles.Role;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.constants.Roles;
import com.paanini.jiffy.dto.QueryOptions;
import com.paanini.jiffy.exception.ContentRepositoryException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.jcrquery.QueryModel;
import com.paanini.jiffy.jcrquery.readers.JCRQuery;
import com.paanini.jiffy.jcrquery.readers.impl.AppFileTypeReaderQuery;
import com.paanini.jiffy.services.ContentSession;
import com.paanini.jiffy.services.SessionBuilder;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.AppRoles;
import com.paanini.jiffy.vfs.files.Folder;
import com.paanini.jiffy.vfs.files.JiffyTable;
import com.paanini.jiffy.vfs.io.FolderViewOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MigrationManager {

    @Autowired
    SessionBuilder sessionBuilder;


    private String map(Exception e) {
        Throwable t  = e.getCause() != null ? e.getCause(): e;
        Class<? extends Throwable> exceptionClass = e.getCause() != null ? e.getCause().getClass() : e.getClass();
        exceptionClass = exceptionClass == null ? e.getClass() : exceptionClass;
        return JcrUtils.mapException(exceptionClass) + " " + t.getMessage();
    }

    public List<String> getTenantIds(){
        try(ContentSession session = sessionBuilder.guestLogin("")) {
            return session.getTenantIds();
        } catch (RepositoryException | ContentRepositoryException e) {
            throw new ProcessingException(map(e), e);
        }
    }

    public Folder getSharedSpace(FolderViewOption folderViewOption, String tenantId){
        try(ContentSession session = sessionBuilder.guestLogin("")) {
            Persistable file = session.readSharedSpaceForTenant(folderViewOption, tenantId);
            return (Folder)file;
        }catch(RepositoryException | ContentRepositoryException e) {
            throw new ProcessingException(map(e), e);
        }

    }

    public Folder getJiffyTables(String id, Optional<QueryModel> model){
        Folder folderData = getFolderData(id, model);
        return filterMetaTables(model.get(), folderData);
    }

    public Folder getFolderData(String id, Optional<QueryModel> model){
        JCRQuery JCRQuery = new AppFileTypeReaderQuery(model.get());

        FolderViewOption folderViewOption = new FolderViewOption(1,
                FolderViewOption.ReadAs.DOCUBE_FILE, new QueryOptions(), JCRQuery);

        return getFolderData(id, folderViewOption);
    }

    public Folder getFolderData(String id, FolderViewOption option){
        try(ContentSession session = sessionBuilder.guestLogin("")) {
            session.elevate();
            BasicFileProps file = id == null ?
                    session.readSharedSpace(option) :
                    session.read(id, option);

            if (file.getType() != Type.FOLDER) {
                throw new RepositoryException("File is not a Folder");
            }
            return (Folder) file;
        } catch(ContentRepositoryException | RepositoryException e) {
            throw new ProcessingException(map(e), e);
        }
    }

    public <T extends Persistable> T updateGeneric(T file){
        try(ContentSession session = sessionBuilder.guestLogin("")) {
            session.elevate();
            return session.update(file);
        } catch(ContentRepositoryException | RepositoryException e) {
            throw new ProcessingException(map(e), e);
        }
    }

    /**
     * This is to prevent meta tables from showing, only when tableType is
     * specified
     * @param queryModel
     * @param folder
     * @return
     */
    private Folder filterMetaTables(QueryModel queryModel, Folder folder) {
        Folder dataFolder = folder;
        if (queryModel.getFilters().stream()
                .anyMatch(filter -> filter.getColumn().equals("tableType"))) {

            List<Persistable> children = folder.getChildren().stream()
                    .filter(child -> !JiffyTableHelper.isMeta(child.getValue("name").toString()))
                    .collect(Collectors.toList());

            dataFolder.setChildren(children);
        }

        return dataFolder;
    }

    public void migratePermissions(String id,String tenantId){
        try(ContentSession session = sessionBuilder.guestLogin("")) {
            session.elevate();
            AppRoles appRoleFile = session.getAppRoleFile(id);
            session.migratePermissions(id,appRoleFile.getRoles(),tenantId);
        } catch (RepositoryException | ContentRepositoryException e) {
            throw new ProcessingException(map(e), e);
        }
    }

    public JiffyTable updateSchema(JiffyTable jiffyTable) {
        JiffyTable finalTable = null;
        finalTable = updateColumnIds(jiffyTable);
        return finalTable;
    }

    public boolean isSchemaUpdatedRequired(JiffyTable jiffyTable){
        DataSchema schema = jiffyTable.getCurrentSchema();
        List<Object> columns = schema.getColumns();
        for (Object field : columns) {
            if (field instanceof InnerTableSchema) {
                String id = ((InnerTableSchema) field).getId();
                if (Objects.isNull(id) || id.trim().isEmpty()) {
                    return true;
                }
            }
            if (field instanceof NestedStructureSchema) {
                String id = ((NestedStructureSchema) field).getId();
                if (Objects.isNull(id) || id.trim().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private JiffyTable updateColumnIds(JiffyTable jiffyTable) {
        addColumnId(jiffyTable);
        int lastMigratedSchemaIndex = jiffyTable.getSchemas().size()-1;
        jiffyTable.getSchemas().remove(lastMigratedSchemaIndex);
        jiffyTable.getSchemas().add(jiffyTable.getCurrentSchema());
        return jiffyTable;
    }

    private void addColumnId(JiffyTable table){
        preProcessColumns(table.getCurrentSchema().getColumns());
    }

    private void preProcessColumns(List<Object> columnData) {
        columnData.forEach(column -> {
            if (column instanceof Column) {
                preProcessBasicColumns((Column) column);
            }else if (column instanceof InnerTableSchema) {
                InnerTableSchema table = (InnerTableSchema) column;
                if(table.getId() == null || table.getId().trim().isEmpty()){
                    table.setId(UUID.randomUUID().toString());
                }
                preProcessColumns(((InnerTableSchema) column).getColumns());
            }else if (column instanceof NestedStructureSchema) {
                NestedStructureSchema nested = (NestedStructureSchema) column;
                if(nested.getId() == null || nested.getId().trim().isEmpty()){
                    nested.setId(UUID.randomUUID().toString());
                }
                preProcessColumns(nested.getColumns());
            }
        });
    }

    private static void preProcessBasicColumns(Column column){
        if(column.getId() == null || column.getId().trim().isEmpty()){
            column.setId(UUID.randomUUID().toString());
        }
    }

    public void migrateCustomRoles(String id,String tenantId){
        Set<String> defaultRoles = Arrays.stream(Roles.values()).map(e-> e.name()).collect(Collectors.toSet());
        try(ContentSession session = sessionBuilder.guestLogin("")) {
            session.elevate();
            AppRoles appRoleFile = session.getAppRoleFile(id);
            List<Role> customRoles = appRoleFile.getRoles().stream().filter(role -> !defaultRoles.contains(role.getName()))
                    .collect(Collectors.toList());
            session.migrateCustomRoles(id,customRoles,tenantId);
        } catch (RepositoryException | ContentRepositoryException e) {
            throw new ProcessingException(map(e), e);
        }
    }

    public void migrateServiceUsers(List<String> tenantIds){
        try(ContentSession session = sessionBuilder.guestLogin("")) {
            session.migrateServiceUsers(tenantIds);
        }catch (RepositoryException | ContentRepositoryException e) {
            throw new ProcessingException(map(e), e);
        }
    }

    public Set<String> getCategoryTableIdsForMigration(Set<String> datasetIds,String appId){
        try(ContentSession session = sessionBuilder.guestLogin("")) {
            return session.getCategoryTableIds(datasetIds,appId);
        } catch (RepositoryException | ContentRepositoryException e) {
            throw new ProcessingException(map(e), e);
        }
    }

}
