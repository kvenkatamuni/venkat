package com.paanini.jiffy.utils;

import com.option3.docube.schema.Dashboard;
import com.option3.docube.schema.approles.Permission;
import com.option3.docube.schema.approles.Role;
import com.option3.docube.schema.datasheet.meta.Column;
import com.option3.docube.schema.datasheet.meta.DataSheetSchema;
import com.option3.docube.schema.folder.NestingLevel;
import com.option3.docube.schema.folder.Options;
import com.option3.docube.schema.folder.View;
import com.option3.docube.schema.jiffytable.*;
import com.option3.docube.schema.layout.Card;
import com.option3.docube.schema.layout.Layout;
import com.option3.docube.schema.layout.Section;
import com.option3.docube.schema.nodes.*;
import com.paanini.jiffy.constants.Common;
import com.option3.docube.schema.nodes.Mode;
import com.option3.docube.schema.nodes.Status;
import com.option3.docube.service.SchemaService;
import com.paanini.jiffy.constants.Content;
import com.paanini.jiffy.constants.Roles;
import com.paanini.jiffy.constants.Tenant;
import com.paanini.jiffy.dto.QueryOptions;
import com.paanini.jiffy.exception.ContentRepositoryException;
import com.paanini.jiffy.exception.DataProcessingException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.exception.ValidationException;
import com.paanini.jiffy.jcrquery.QueryModel;
import com.paanini.jiffy.jcrquery.readers.JCRQuery;
import com.paanini.jiffy.jcrquery.readers.impl.AppFileTypeReaderQuery;
import com.paanini.jiffy.jcrquery.readers.impl.AppPropertiesReaderQuery;
import com.paanini.jiffy.jcrquery.readers.impl.SimpleJCRQuery;
import com.paanini.jiffy.models.RolesV2;
import com.paanini.jiffy.proc.api.IngestContext;
import com.paanini.jiffy.services.ContentSession;
import com.paanini.jiffy.services.SessionBuilder;
import com.paanini.jiffy.storage.DocumentStore;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.DataSheetProps;
import com.paanini.jiffy.vfs.api.ExtraFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.*;
import com.paanini.jiffy.vfs.io.FolderViewOption;
import com.paanini.jiffy.vfs.io.Utils;
import com.paanini.jiffy.vfs.utils.BasicFileWithPath;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VfsManager {

  static Logger logger = LoggerFactory.getLogger(VfsManager.class);

  @Autowired
  DocumentStore store;

  @Autowired
  SessionBuilder sessionBuilder;



  private String map(Exception e) {
    Throwable t  = e.getCause() != null ? e.getCause(): e;
    Class<? extends Throwable> exceptionClass = e.getCause() != null ? e.getCause().getClass() : e.getClass();
    exceptionClass = exceptionClass == null ? e.getClass() : exceptionClass;
    return JcrUtils.mapException(exceptionClass) + " " + t.getMessage();
  }

  public Persistable getFolder(String id, QueryOptions options, boolean isShared)
          throws ProcessingException {
    FolderViewOption option = new FolderViewOption(1,
            FolderViewOption.ReadAs.BASIC_FILE,
            options);
    return getFolder(id, option);
  }

  public Persistable getFolderWithoutChildren(String id, QueryOptions options, boolean isShared)
          throws ProcessingException {
    FolderViewOption option = new FolderViewOption(0,
            FolderViewOption.ReadAs.BASIC_FILE,
            options);
    return getFolder(id, option);
  }

  public Persistable getAppGroupFolder(String id, QueryOptions options, boolean isShared)
          throws ProcessingException {
    return getFolder(id, FolderViewOption.getAppGroupWithoutJCR(options));
  }

  public Persistable getFolderPrivilleged(String id, QueryOptions options, boolean isShared)
          throws ProcessingException {
    FolderViewOption option = new FolderViewOption(1,
            FolderViewOption.ReadAs.BASIC_FILE,
            options);
    return getFolderPrivilleged(id,option);
  }

  public Persistable getFolderWithoutJCR(String id, QueryOptions options) {
    return getFolder(id, FolderViewOption.getWithoutJCR(options));
  }

  public Persistable getFolderData(String id, Optional<QueryModel> model) {
    JCRQuery JCRQuery = new AppFileTypeReaderQuery(model.get());

    FolderViewOption folderViewOption = new FolderViewOption(1,
            FolderViewOption.ReadAs.BASIC_FILE, new QueryOptions(), JCRQuery);

    return getFolder(id, folderViewOption);
  }

  public Persistable getFolderDataFullRead(String id, Optional<QueryModel> model) {
    JCRQuery JCRQuery = new AppFileTypeReaderQuery(model.get());

    FolderViewOption folderViewOption = new FolderViewOption(1,
            FolderViewOption.ReadAs.DOCUBE_FILE, new QueryOptions(), JCRQuery);

    return getFolder(id, folderViewOption);
  }

  public List<Role> getAssignedRoles(String id) throws RepositoryException {
    try(ContentSession session = sessionBuilder.adminLogin()) {
      return session.getAssignedRoles(id);
    } catch (RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public List<Role> getAssignedRolesByPath(String appPath) throws RepositoryException {
    String id = getIdFromPath(appPath);
    return getAssignedRoles(id);
  }

  public Set<String> getAssignedRolesV2ByPath(String appPath) throws RepositoryException {
    String id = getIdFromPath(appPath);
    try(ContentSession session = sessionBuilder.login()) {
      return session.getAssignedRolesV2(id,TenantHelper.getUser());
    }catch(ContentRepositoryException | RepositoryException e) {
      throw new DataProcessingException(map(e), e);
    }
  }

  public void savePublishHeaders(String id, List<Column>  header) {
    //List<Role> roles = getRoles(id);
    try(ContentSession session = sessionBuilder.login()) {
      //@TODO Remove setRole
      //session.setRoles(roles);
      DataSheet ds = session.read(id);
      ds = updateHeaders(ds, header);
      session.update(ds);
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new DataProcessingException(map(e), e);
    }
  }

  public <T extends Persistable> T getFileFromRelativePath(String base, String path) {
    FolderViewOption folderViewOption = new FolderViewOption(0,
            FolderViewOption.ReadAs.DOCUBE_FILE,
            new QueryOptions(),new AppPropertiesReaderQuery());
    try(ContentSession session = sessionBuilder.login()) {
      return session.read(base,path,folderViewOption);
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new DataProcessingException(map(e), e);
    }
  }


  private DataSheet updateHeaders(DataSheet ds, List<Column>  header) {
    if(header != null) {
      DataSheetSchema schema = ds.getDatasheetSourceSchema();
      DataSheetSchema publishedSchema = new DataSheetSchema(header, schema.getDescription(), Arrays.asList(), null, null);
      ds.setDatasheetPublishedSchema(publishedSchema);
      if(Objects.isNull(ds.getDatasheetSourceSchema())) {
        //update source schema, if source schema is not defined already defined
        ds.setDatasheetSourceSchema(publishedSchema);
      }
    }
    return ds;

  }


  public Set<String> getRolesV2byId(String id){
    try (ContentSession session = sessionBuilder.login()) {
      return session.getAssignedRolesV2(id, TenantHelper.getUser());
    } catch (ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public Folder getSharedSpace(FolderViewOption folderViewOption,String tenantId){
    try(ContentSession session = sessionBuilder.login()) {
      Persistable file = session.readSharedSpaceForTenant(folderViewOption, tenantId);
      return (Folder)file;
    }catch(RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(map(e), e);
    }

  }


  private Persistable getFolder(String id, FolderViewOption folderViewOption) {
    //List<Role> roles = getRoles(id);
    try(ContentSession session = sessionBuilder.login()) {
      //session.setRoles(roles);
      BasicFileProps file = id == null ?
              session.readSharedSpace(folderViewOption) :
              session.read(id, folderViewOption);

      if (file.getType() == Type.FILESET) {
        FileSet fileset = (FileSet) file;
        String path = fileset.getPath();

        fileset.setPath(null);
        FileSet fs = (FileSet) readFileSetChildren(getDataPath(id), fileset, folderViewOption.getQueryOptions());
        fs.setPath(path);
        return fs;
      }

      if (file.getType() != Type.FOLDER) {
        throw new RepositoryException(Common.FILE_IS_NOT_A_FOLDER);
      }
      Folder folder = (Folder) file;
      if(SubType.appGroup.equals(file.getSubType())){
        for(Persistable child : folder.getChildren()){
          if(child instanceof Folder){
            Set<String> rolesApp = getRolesV2byId(((BasicFileProps)child).getId());
            ((Folder)child).setRole(rolesApp.stream().collect(Collectors.toList()));
          }
        }
      }

      return folder;
    } catch(RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  private Persistable getFolderPrivilleged(String id, FolderViewOption folderViewOption) {
    //List<Role> roles = getRoles(id);
    try(ContentSession session = sessionBuilder.adminLogin()) {

           /* BasicFileProps file = id == null ?
                (isShared ? session.readShare(option) : session.readOwned(option)) :
                session.read(id, option);*/
      //session.setRoles(roles);
      BasicFileProps file = id == null ?
              session.readSharedSpace(folderViewOption) :
              session.read(id, folderViewOption);

      if (file.getType() == Type.FILESET) {
        FileSet fileset = (FileSet) file;
        String path = fileset.getPath();

        fileset.setPath(null);
        FileSet fs = (FileSet) readFileSetChildren(getDataPath(id), fileset, folderViewOption.getQueryOptions() );
        fs.setPath(path);
        return fs;
      }

      if (file.getType() != Type.FOLDER) {
        throw new RepositoryException(Common.FILE_IS_NOT_A_FOLDER);
      }
      return (Folder) file;
    } catch(RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public Persistable getJiffyTableWithSchemas(String id, QueryOptions options,
                                              boolean isShared) throws Exception {
    try(ContentSession session = sessionBuilder.login()) {
      FolderViewOption option = new FolderViewOption(1,
              FolderViewOption.ReadAs.DOCUBE_FILE,
              options);

      BasicFileProps file = id == null ?
              session.readSharedSpace(option) :
              session.read(id, option);

      if (file.getType() == Type.FILESET) {
        FileSet fileset = (FileSet) file;
        String path = fileset.getPath();

        fileset.setPath(null);
        FileSet fs = (FileSet) readFileSetChildren(getDataPath(id), (FileSet) file,options );
        fs.setPath(path);
        return fs;
      }

      if (file.getType() != Type.FOLDER) {
        throw new RepositoryException(Common.FILE_IS_NOT_A_FOLDER);
      }
      return (Folder) file;
    } catch(RepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public Persistable getFolder(String id, QueryOptions queryOptions) {
    return getFolder(id, queryOptions, false);
  }

  public Persistable getFolderData(String id, QueryOptions options,
                                   boolean isShared){
    //List<Role> roles = getRoles(id);
    try(ContentSession session = sessionBuilder.login()) {
      FolderViewOption option = new FolderViewOption(1,
              FolderViewOption.ReadAs.DOCUBE_FILE,
              options);
      //session.setRoles(roles);
      BasicFileProps file = id == null ?
              session.readSharedSpace(option) :
              session.read(id, option);

      if (file.getType() == Type.FILESET) {
        FileSet fileset = (FileSet) file;
        String path = fileset.getPath();

        fileset.setPath(null);
        FileSet fs = (FileSet) readFileSetChildren(getDataPath(id), (FileSet) file,options );
        fs.setPath(path);
        return fs;
      }

      if (file.getType() != Type.FOLDER) {
        throw new RepositoryException(Common.FILE_IS_NOT_A_FOLDER);
      }
      return (Folder) file;
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public Persistable readFileSetChildren(Path path, FileSet file,
                                         QueryOptions opt) {
    List<Persistable> simpleFiles = new ArrayList<>();
    File fileset = new File(path.toUri());
    File[] filesets = fileset.listFiles();

    if(filesets == null){
      return file;
    }

    for (File entry : filesets) {
      String entryPath = file.getPath() == null
              ? entry.getName()
              : file.getPath() + "/" + entry.getName();

      BasicFileView simpleFile = new BasicFileView();
      simpleFile.setName(entry.getName());
      simpleFile.setId(entry.getName());
      simpleFile.setOwner(file.getOwner());
      simpleFile.setSubType(entry.isDirectory() ? SubType.filesetMember : SubType.any);
      simpleFile.setType(Type.FILE);
      simpleFile.setCreatedBy(file.getCreatedBy());
      simpleFile.setLastModified(entry.lastModified());
      simpleFile.setPrivileges(file.getPrivileges());
      simpleFile.setCreateAt(entry.lastModified());
      simpleFile.setPath(entryPath);
      simpleFiles.add(simpleFile);
    }
    Utils.sort(simpleFiles, opt);
    file.setChildren(simpleFiles);
    return file;
  }

  public int countFileSetMembers(String fileSetId) {
    Path path = this.store.getFileSystem().getPath(fileSetId).resolve("data");
    File fileset = new File(path.toUri());
    File[] filesets = fileset.listFiles();

    return filesets.length;
  }

  public void renameFile(String id, String newName) throws ProcessingException {
    //List<Role> roles = getRoles(id);
    newName =  cleanse(newName);
    try(ContentSession session = sessionBuilder.login()) {
      //session.setRoles(roles);
      Type type = session.getType(id);
      Persistable file = session.read(id);

      if (type.equals(Type.JIFFY_TABLE) &&
              ((JiffyTable) file).getTableType().equals(TableType.DOC_JDI)) {
        renameCategoryTable(session, (JiffyTable) file, newName);
      } else if (type.equals(Type.FOLDER) && isApp((Folder) file)) {
        renameMetaTables(session, (Folder) file, newName);
      }

      session.renameFile(id, newName);

      if (type.equals(Type.DATASHEET) || type.equals(Type.SQL_DATASHEET) ||
              type.equals(Type.KUDU_DATASHEET)) {
        updateDatasheetDetailsForRename(session, id, newName);
      }
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  private String cleanse(String name){
    name = name.trim();
    return name;
  }

  public void moveDocument(String sourceId,  String destId) throws ProcessingException {
    try (ContentSession session = sessionBuilder.login()) {
      session.move(sourceId, destId);
    } catch (ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public Persistable delete(String id){
    //List<Role> roles = getRoles(id);
    try(ContentSession session = sessionBuilder.login()) {
      //session.setRoles(roles);
      try {
        BasicFileProps file = session.read(id, FolderViewOption.getMinimumOption());
        final Persistable persistable = (Persistable) file;
        if(file.getType().equals(Type.JIFFY_TABLE)){
          throw new ProcessingException("Delete action denied for this type");
        }
        logger.debug("Deleting file {} by {}", file.getName(),TenantHelper.getUser());
        session.delete(persistable);
        if (file.getType() == Type.DATASHEET
                || file.getType() == Type.FILESET) {
          PhysicalStoreUtils.deleteContent(store, id);
        }
        return persistable;
      } catch(RepositoryException | ProcessingException e) {
        logger.error("Error deleting content with id " + id , e);
        session.markRollBackOnly();
        throw new DataProcessingException(map(e), e);
      }
    } catch (ContentRepositoryException | RepositoryException e ){
      throw new DataProcessingException(map(e), e);
    }
  }

  /***
   * Method to delete jiffy table..
   * Jiffy table delete is blocked from normal file delete calls
   * @return persistable File
   */
  public Persistable deleteJiffyTable(JiffyTable file){
    try(ContentSession session = sessionBuilder.login()) {
      logger.info("Deleting name {} by {}", file.getName(),TenantHelper.getUser());
      final JiffyTable jiffyTable = (JiffyTable) file;
      if(file.getType().equals(Type.JIFFY_TABLE)){
        session.delete(jiffyTable);
        logger.debug("Jiffy Table {} deleted",jiffyTable.getName());
        logger.debug("Mongo Table Name {}",jiffyTable.getTableName());
        logger.debug("Schema {} ",jiffyTable.getCurrentSchema());
        logger.debug("forms {} ",jiffyTable.getForms());
      }
      return jiffyTable;
    }catch (ContentRepositoryException | RepositoryException e ){
      throw new DataProcessingException(map(e), e);
    }
  }

  public <T extends Persistable>T getFileProperties(String id) throws ProcessingException {
    FolderViewOption folderViewOption = new FolderViewOption(0,
            FolderViewOption.ReadAs.DOCUBE_FILE,
            new QueryOptions(),new AppPropertiesReaderQuery());
    try(ContentSession session = sessionBuilder.login()) {
      return session.read(id,folderViewOption);
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public <T extends Persistable>T getFile(String id) throws ProcessingException {
    //List<Role> roles = getRoles(id);
    try(ContentSession session = sessionBuilder.login()) {
      //session.setRoles(roles);
      return session.read(id);
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public <T extends Persistable>T elevateAndGetFile(String id,boolean elavate) throws ProcessingException {
    //@TODO elevate
    //List<Role> roles = getRoles(id);
    /*if(elavate && !RoleServiceUtils.hasDesignerRole(roles))
      roles.add(RoleServiceUtils.getDesignerRole());*/
    try(ContentSession session = sessionBuilder.login()) {
      //session.setRoles(roles);
      return session.read(id);
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public <T extends Persistable>T getFile(String appPath,String name) throws ProcessingException {
    //List<Role> roles = getRolesByPath(appPath);
    try(ContentSession session = sessionBuilder.login()) {
      //session.setRoles(roles);
      Node node = session.getNodeBypath(TenantHelper.getTenantId(), Content.FOLDER_USERS,
              Content.SHARED_SPACE, appPath, name);
      return session.read(node.getIdentifier());
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public <T extends Persistable>T elevateAndGetFileByPath(String appPath,String name,boolean elevate) throws ProcessingException {

    try(ContentSession session = sessionBuilder.login()) {
      Node node = session.getNodeBypath(TenantHelper.getTenantId(), Content.FOLDER_USERS,
              Content.SHARED_SPACE, appPath, name);
      return session.read(node.getIdentifier());
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public <T extends Persistable>T getFileFromParentId(String paretnId,String name) throws ProcessingException {
    //List<Role> roles = getRoles(paretnId);
    try(ContentSession session = sessionBuilder.login()) {
      String absAappPath = session.getPathFromId(paretnId);
      Node node = session.getNodeByAbsPath(absAappPath, name);
      //session.setRoles(roles);
      return session.read(node.getIdentifier());
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public Type getType(String id) throws ProcessingException {
    try(ContentSession session = sessionBuilder.login()) {
      return session.getType(id);
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }


  public <T extends Persistable>T getFileFromPath(String path) throws
          ProcessingException {
    //List<Role> roles = getRolesByPath(path);
    try (ContentSession session = sessionBuilder.login()) {
      //session.setRoles(roles);
      return session.read(session.getId(path), FolderViewOption.getFileOptions());
    } catch (ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(map(e),e);
    }
  }

  public String getIdFromPath(String path) throws ProcessingException {
    try (ContentSession session = sessionBuilder.login()) {
      return session.getId(path);
    } catch (ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(map(e),e);
    }
  }

  public String getPathFromId(String id) throws ProcessingException {
    try (ContentSession session = sessionBuilder.login()) {
      return session.getPathFromId(id);
    } catch (ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(map(e),e);
    }
  }


  public BasicFileWithPath searchFilePath(String path) throws ProcessingException {
    try (ContentSession session = sessionBuilder.login()) {
      return session.searchPath("", path);
    } catch (RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(map(e),e);
    }
  }

  public String getFilePath(String id) {
    Path path = this.store.getFileSystem().getPath(id);
    return path.toString();
  }

  public Path getDataPath(String id) {
    return this.store.getFileSystem().getPath(id).resolve("data");
  }

  public void moveOrphanedFolder(Map<String, String> map) throws IOException {
    PhysicalStoreUtils.moveOrphanedFolder(store, map);
  }

  public void resetOrphanFolder(Map<String, String> map) throws IOException {
    PhysicalStoreUtils.resetOrphanFolder(store, map);
  }

  public Presentation savePresentation(Presentation presentation, String parentId) throws ProcessingException {
    try(ContentSession session = sessionBuilder.login()) {
      return session.create(presentation, checkId(parentId));
    } catch(RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public boolean isFilePresent(String name, String parentId) {
    try(ContentSession session = sessionBuilder.login()) {
      return session.isFilePresent(name, parentId);
    } catch(RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(map(e), e);
    }

  }

  public <T extends Persistable> T saveGeneric(T file, String parentId)
          throws ProcessingException {
    try(ContentSession session = sessionBuilder.login()) {
      return session.create(file, parentId);
    } catch(RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public <T extends Persistable> T updateGeneric(T file)
          throws ProcessingException {
    try(ContentSession session = sessionBuilder.login()) {
      return session.update(file);
    } catch(RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public <T extends Persistable> T elevateUpdateGeneric(T file)
          throws ProcessingException {
    try(ContentSession session = sessionBuilder.elevateAndLogin()) {
      return session.update(file);
    } catch(RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public <T extends Persistable> T updateGenericPrivilleged(T file)
          throws ProcessingException {
    try(ContentSession session = sessionBuilder.adminLogin()) {
      return session.update(file);
    } catch(RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }



  public String getParentId(String id) throws ProcessingException {
    try(ContentSession session = sessionBuilder.login()) {
      return session.getParentId(id);
    } catch(RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }



  public String createFolder(String name, String parentId,
                             Optional<SubType> subType,
                             Optional<Options> options) {
    try(ContentSession session = sessionBuilder.login()) {
      Folder folder = new Folder();
      folder.setSubType(subType.orElse(null));
      folder.setOptions(options.orElse(
              new Options(NestingLevel.UNLIMITED, View.LIST)));
      folder.setName(name);
      return session.create(folder, checkId(parentId)).getId();
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error("Error creating Folder", e);
      throw new DataProcessingException(map(e), e);
    }
  }

  public FileSet createFileSet(String name, String parentId) throws ProcessingException {
    try(ContentSession session = sessionBuilder.login()) {
      FileSet fileset = new FileSet();
      fileset.setName(name);
      FileSet fileSet = session.create(fileset, checkId(parentId));
      return fileSet;
    } catch(RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public void updateFileSet(String id, FileSet fileSet) throws ProcessingException {
    try(ContentSession session = sessionBuilder.login()) {
      session.update(fileSet);
    } catch(RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  private boolean validateHeader(JsonArray headerExisting, JsonArray headerNew)
          throws ValidationException {

    //todo add sanity conditions
    if(headerExisting.size() != headerNew.size()) {
      throw new ValidationException("Header element length does not match");
    }
    // Currently validates the field names
    // No comparison on the assignment compatibility
    for(int i = 0; i < headerExisting.size(); i++) {
      JsonObject o = headerExisting.getJsonObject(i);
      String name = o.getString("name");
      String type = o.getString("type");
      String nameNew = headerNew.getJsonObject(i).getString("name");
      String typeNew = headerNew.getJsonObject(i).getString("type");

      if(!Objects.equals(name, nameNew) || !type.equals(typeNew)) {
        throw new ValidationException("Headers do not match : " + name + "(" + type + ") & " +
                nameNew + "(" + typeNew + ")");
      }
    }
    return true;
  }

  public String checkId(String id) {
    return id == null ? Content.ROOTFOLDER : id;
  }

  public ColorPalette getColorPalettes() throws ProcessingException {
    try(ContentSession session = sessionBuilder.login()) {
      return session.getColorPalette();
    } catch(RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public void saveColorPalette(String content) throws ProcessingException {
    try(ContentSession session = sessionBuilder.login()) {
      ColorPalette colorPalette = session.getColorPalette();
      colorPalette.setContent(content);
      session.update(colorPalette);
    } catch(RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public Persistable getDocubeFile(String id) throws ProcessingException {
    //List<Role> roles = getRoles(id);
    try (ContentSession session = sessionBuilder.login()) {
      //session.setRoles(roles);
      return session.read(id);
    } catch (RepositoryException | ContentRepositoryException e) {
      logger.error("Error while retrieving isDataSheetQueryable  ", e);
      String message = JcrUtils.mapException(e.getClass()) + " " + e.getMessage();
      throw new ProcessingException(message, e);
    }
  }



  public SparkModelFile findOrCreateSparkModel(String modelFilePath, SparkModelFile tempModel) {
    try (ContentSession session = sessionBuilder.login()) {
      String featureSet = tempModel.getFeatureSet();
      String trainingFile = tempModel.getTrainingFile();
      String targetColumn = tempModel.getTargetColumn();

      if(session.checkPathAvailable(modelFilePath)) {
        Persistable file = session.read(session.getId(modelFilePath));
        if(!(file instanceof SparkModelFile)) {
          throw new ProcessingException("Expecting a model file");
        }

        return getSparkModelFile(session, featureSet, trainingFile, targetColumn, (SparkModelFile) file);
      } else {
        String filename = modelFilePath.substring(modelFilePath.lastIndexOf("/") + 1);
        String parent = modelFilePath.substring(0, modelFilePath.lastIndexOf("/"));
        SparkModelFile smf = new SparkModelFile();
        smf.setName(filename);
        if(featureSet != null)  smf.setFeatureSet(featureSet);
        if(trainingFile != null)  smf.setTrainingFile(trainingFile);
        if(targetColumn != null) smf.setTargetColumn(targetColumn);

        String parentId = ((BasicFileProps) getFileFromPath(parent)).getId();
        session.create(smf, parentId);

        Path filePath = PhysicalStoreUtils.createSparkModel(store, smf.getId());
        smf.setLocation(filePath.toString());

        session.update(smf);
        return smf;
      }
    } catch (RepositoryException | IOException | ContentRepositoryException e) {
      logger.error("  ", e);
      String message = JcrUtils.mapException(e.getClass()) + " " + e.getMessage();
      throw new ProcessingException(message, e);
    }
  }

  private SparkModelFile getSparkModelFile(ContentSession session, String featureSet, String trainingFile, String targetColumn, SparkModelFile file) throws RepositoryException {
    SparkModelFile model = file;
    if(featureSet != null || trainingFile != null || targetColumn != null) {
      if(featureSet != null) model.setFeatureSet(featureSet);
      if(trainingFile != null) model.setTrainingFile(trainingFile);
      if(targetColumn != null) model.setTargetColumn(targetColumn);
      session.update(model);
    }
    return model;
  }


  public Notebook createNotebook(String name, String parentId) {
    try(ContentSession session = sessionBuilder.login()) {
      Notebook notebook = new Notebook();
      notebook.setName(name);
      return session.create(notebook, checkId(parentId));
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error("Error creating Notebook", e);
      throw new DataProcessingException(map(e), e);
    }
  }

    /*public void updateNotebook(String id, Notebook notebook)
            throws ProcessingException {
        try(ContentSession session = cRepo.newTenantSession(TenantHelper.getTenantId()).loginWithCurrentToken()) {
            session.update(notebook);
        } catch(ContentRepositoryException | RepositoryException e) {
            throw new ProcessingException(map(e), e);
        }
    }*/

  public Folder createAppGroup(String name) {
    String adminGroup = new StringBuilder(TenantHelper.getTenantId())
            .append(Tenant.TENANT_USER_GROUP).toString();
    Folder appFolder = null;
    if(!isMember(adminGroup,TenantHelper.getUser())){
      throw new ProcessingException("Permission denied : Cannot create App Category");
    }
    try(ContentSession session = sessionBuilder.login()) {

      Folder sharedSpace = (Folder) getFolder(null,new QueryOptions(),false);
      if(sharedSpace.getChildren().size() >= 10){
        logger.error("Error creating App Category - Max limit Reached");
        throw new DataProcessingException("Error creating App Category - Max limit Reached");
      }
      Folder folder = new Folder();
      folder.setName(name);
      folder.setSubType(SubType.appGroup);
      Options options = new Options(NestingLevel.ONE_LEVEL, View.GRID);
      folder.setOptions(options);
      appFolder = session.createAppFolder(folder, null);
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error("Error creating App Group", e);
      throw new DataProcessingException(map(e), e);
    }
    restrictAccess(appFolder.getId(),TenantHelper.getUser());
    return appFolder;
  }


  private void restrictAccess(String id,String user){
    try(ContentSession session = sessionBuilder.adminLogin()) {
      session.restrictAccess(id,user);
    } catch (RepositoryException | ContentRepositoryException e) {
      delete(id);
      throw new DataProcessingException(map(e), e);
    }
  }

  public Folder createApp(String name, String appGroupName,String description,String thumbnail) {
    String user = TenantHelper.getUser();
    String groupName =  new StringBuilder(TenantHelper.getTenantId())
            .append(Tenant.CAN_CREATE_APPS).toString();
    String adminGroup = new StringBuilder(TenantHelper.getTenantId())
            .append(Tenant.TENANT_USER_GROUP).toString();
    Folder appFolder = null;
    if(isMember(groupName,user) || isMember(adminGroup,user)) {
      try (ContentSession session = sessionBuilder.login()) {
        String appGroupId = getIdFromPath(appGroupName);
        Folder folder = new Folder();
        folder.setName(name);
        folder.setDescription(description);
        folder.setSubType(SubType.app);
        Options options = new Options(NestingLevel.NONE, View.GRID);
        folder.setOptions(options);
        folder.setThumbnail(thumbnail);
        appFolder = session.createAppFolder(folder, checkId(appGroupId));
      } catch (RepositoryException | ContentRepositoryException e) {
        throw new ProcessingException(map(e), e);
      }
    }   else  {
      throw new ProcessingException("Permission Denied : Cannot create App ");
    }
    restrictAccess(appFolder.getId(),user);
    return appFolder;
  }

  public void createTenantVault(String path){
    try(ContentSession session = sessionBuilder.adminLogin()) {
      session.createVaultFolder(path);
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error(Common.ERROR_SECURE_VAULT_ENTRY, e);
      throw new DataProcessingException(map(e), e);
    }
  }


  public SecureVaultEntry createSecureVaultEntry(String userId,SecureVaultEntry secureVaultEntry,String... path){
    try(ContentSession session = sessionBuilder.login()) {
      Node node = session.getNodeBypath(path);
      return session.create(secureVaultEntry,node.getIdentifier());
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error(Common.ERROR_SECURE_VAULT_ENTRY, e);
      throw new DataProcessingException(map(e), e);
    }
  }

  public SecureVaultEntry createSecureVaultEntryElevated(String userId, SecureVaultEntry secureVaultEntry, String... path){
    try(ContentSession session = sessionBuilder.adminLogin()) {
      Node node = session.getNodeBypath(path);
      return session.create(secureVaultEntry,node.getIdentifier());
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error(Common.ERROR_SECURE_VAULT_ENTRY, e);
      throw new DataProcessingException(map(e), e);
    }
  }

  public void assignPrivilleges(Map<String,List<String>> data,Boolean global,String... path){
    try(ContentSession session = sessionBuilder.adminLogin()) {
      Node node = session.getNodeBypath(path);
      session.assignPrivilleges(node,data,global);
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error(Common.ERROR_SECURE_VAULT_ENTRY, e);
      throw new DataProcessingException(map(e), e);
    }
  }

  public void assignPermissionsPrivilleged(Map<String,List<String>> data,Boolean global,String... path){
    try(ContentSession session = sessionBuilder.adminLogin()) {
      Node node = session.getNodeBypath(path);
      session.assignPrivilleges(node,data,global);
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error(Common.ERROR_SECURE_VAULT_ENTRY, e);
      throw new DataProcessingException(map(e), e);
    }
  }

  public void assignRootVaultPrivilleges(String name){
    try(ContentSession session = sessionBuilder.adminLogin()) {
      session.assignRootVaultPrivilleges(name);
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error(Common.ERROR_SECURE_VAULT_ENTRY, e);
      throw new DataProcessingException(map(e), e);
    }
  }

  public void assignTenantVaultPrivilleges(String name){
    try(ContentSession session = sessionBuilder.login()) {
      session.assignTenantVaultPrivilleges(name);
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error(Common.ERROR_SECURE_VAULT_ENTRY, e);
      throw new DataProcessingException(map(e), e);
    }
  }

  public void assignPermissionTenantVault(String user){
    try(ContentSession session = sessionBuilder.adminLogin()) {
      session.assignPermissionTenantVault(user);
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error(Common.ERROR_SECURE_VAULT_ENTRY, e);
      throw new DataProcessingException(map(e), e);
    }
  }

  public void clearPrivilleges(String... path){
    try(ContentSession session = sessionBuilder.adminLogin()) {
      Node node = session.getNodeBypath(path);
      session.clearPrivilleges(node);
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error(Common.ERROR_SECURE_VAULT_ENTRY, e);
      throw new DataProcessingException(map(e), e);
    }
  }

  public void clearPrivillegesElevated(String... path){
    try(ContentSession session = sessionBuilder.adminLogin()) {
      Node node = session.getNodeBypath(path);
      session.clearPrivilleges(node);
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error(Common.ERROR_SECURE_VAULT_ENTRY, e);
      throw new DataProcessingException(map(e), e);
    }
  }

  /***
   * Reads the file properties including the external service calls which fetch data from external system
   * @param path
   * @return
   */

  public SecureVaultEntry hardReadVaultEntry(String... path){
    return getVaultEntry(false,path);
  }

  public SecureVaultEntry hardReadVaultEntryElevated(String... path){
    return getVaultEntryElevated(false,path);
  }

  /***
   * Reads the file properties skipping the external service calls  which fetch data from external system
   * @param path
   * @return
   */

  public SecureVaultEntry softReadVaultEntry(String... path){
    return getVaultEntry(true,path);
  }

  public SecureVaultEntry softReadVaultEntryElevated(String... path){
    return getVaultEntryElevated(true,path);
  }

  private SecureVaultEntry getVaultEntry(boolean isSoftRead,String... path){
    try(ContentSession session = sessionBuilder.login()) {
      return session.readVaultEntry(isSoftRead,path);
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error(Common.ERROR_SECURE_VAULT_ENTRY, e);
      throw new DataProcessingException(map(e), e);
    }
  }

  private SecureVaultEntry getVaultEntryElevated(boolean isSoftRead,String... path){
    try(ContentSession session = sessionBuilder.adminLogin()) {
      return session.readVaultEntry(isSoftRead,path);
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error(Common.ERROR_SECURE_VAULT_ENTRY, e);
      throw new DataProcessingException(map(e), e);
    }
  }

  public Map<String,String> getVaultAcl(String... path){
    try(ContentSession session = sessionBuilder.login()) {
      return session.getVaultAcl(path);
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error(Common.ERROR_SECURE_VAULT_ENTRY, e);
      throw new DataProcessingException(map(e), e);
    }
  }

  public Map<String,String> getVaultAclElevated(String... path){
    try(ContentSession session = sessionBuilder.adminLogin()) {
      return session.getVaultAcl(path);
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error(Common.ERROR_SECURE_VAULT_ENTRY, e);
      throw new DataProcessingException(map(e), e);
    }
  }

  public void deleteTenantVault() {
      String secureVault = getNodeByAbsPathElevated(TenantHelper.getTenantId(),Content.VAULT);
      deleteFile(secureVault);
      return;
  }

  public void createTenantVault() {
    try (ContentSession session = sessionBuilder.adminLogin()) {
      session.createTenantVaultFolder();
    } catch (ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(e.getMessage(), e);
    }
  }

  public void createRootVault() {
    try (ContentSession session = sessionBuilder.adminLogin()) {
      session.createRootVaultFolder();
    } catch (ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(e.getMessage(), e);
    }
  }

  public String getNodeByAbsPath(String... path){
    try(ContentSession session = sessionBuilder.login()) {
      Node node = session.getNodeBypath(path);
      return node.getIdentifier();
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error(Common.ERROR_SECURE_VAULT_ENTRY, e);
      throw new DataProcessingException(map(e), e);
    }
  }

  public String getNodeByAbsPathElevated(String... path){
    try(ContentSession session = sessionBuilder.adminLogin()) {
      Node node = session.getNodeBypath(path);
      return node.getIdentifier();
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error(Common.ERROR_SECURE_VAULT_ENTRY, e);
      throw new DataProcessingException(map(e), e);
    }
  }

  public Boolean isVaultExists(String path){
    try(ContentSession session = sessionBuilder.login()) {
      return session.isVaultExists(path);
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error(Common.ERROR_SECURE_VAULT_ENTRY, e);
      throw new DataProcessingException(map(e), e);
    }
  }

  public void deleteFile(String id){
    try(ContentSession session = sessionBuilder.login()) {
      logger.info("Deleting id {}",id);
      session.deleteNode(id);
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error(Common.ERROR_SECURE_VAULT_ENTRY, e);
      throw new DataProcessingException(map(e), e);
    }
  }

  public Map<String,Object> getAppGroupPermissions(String user,String groupName){
    try(ContentSession session = sessionBuilder.adminLogin()) {
      return session.getAppGroupPermission(user,groupName);
    } catch(RepositoryException | ContentRepositoryException e) {
      throw new DataProcessingException(map(e), e);
    }
  }

  public void setGroupPermissions(List<String> userList,String groupName){
    try(ContentSession session = sessionBuilder.adminLogin()) {
      session.setGroupPermissions(groupName, userList);
    } catch(RepositoryException | ContentRepositoryException e) {
      throw new DataProcessingException(map(e), e);
    }
  }

  public void migrateRoleGroups(String path, List<Role> roles){
    try(ContentSession session = sessionBuilder.adminLogin()) {
      String id = getIdFromPath(path);
      session.migrateRoleGroups(path, id, roles);
    } catch(RepositoryException | ContentRepositoryException e) {
      throw new DataProcessingException(map(e), e);
    }

  }

  public void migratePermissionGroups(String groupName){
    try(ContentSession session = sessionBuilder.adminLogin()) {
      session.migratePermissionGroups(groupName);
    } catch(RepositoryException | ContentRepositoryException e) {
      throw new DataProcessingException(map(e), e);
    }
  }

  public List<String> getAllAppPaths(){
    try(ContentSession session = sessionBuilder.login()) {
      return  session.getAllAppPaths();
    } catch(RepositoryException | ContentRepositoryException e) {
      return Collections.emptyList();
    }
  }

  public Map<String, Integer> getAppCount(List<Long> tenantIds) {
    Map<String, Integer> appsCount = new HashMap<>();
    try(ContentSession session = sessionBuilder.adminLogin()) {
      Date date = new Date();
      long start = date.getTime();
      for(Long tenantId : tenantIds){
        Integer appCount = session.getAppCount(tenantId);
        appsCount.put(tenantId.toString(),appCount);
      }
      Date endDate = new Date();
      long end = endDate.getTime();
      logger.info("Total time taken to fetch appCount {}",(end-start));
      return appsCount;
    } catch(RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException("Error in reading app counts",e);
    }
  }

  public Persistable getLicenseEntry(){
    try(ContentSession session = sessionBuilder.adminLogin()) {
      return session.readLicenseEntry();
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error("Error License Entry", e);
      return null;
    }
  }

  public boolean isLicenseExist() {
    try(ContentSession session = sessionBuilder.adminLogin()) {
      return session.isLicenseExist();
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error("License not found", e);
      return false;
    }
  }

  public Map<String, Object> getBotCount(List<String> tenantIds) {
    try(ContentSession session = sessionBuilder.adminLogin()) {
      return session.getBotCount(tenantIds);
    } catch (RepositoryException | ContentRepositoryException e) {
      throw new DataProcessingException(map(e), e);
    }
  }

  public void deleteSuperVisor(String id){
    try(ContentSession session = sessionBuilder.login()) {
      logger.info("Deleting id {} by user {}", id,TenantHelper.getUser());
      session.deleteNode(id);
    } catch(RepositoryException | ContentRepositoryException e) {
      throw new DataProcessingException(map(e), e);
    }
  }

  public boolean isAvailable(String path) {
    try(ContentSession session = sessionBuilder.login()) {
      return session.checkPathAvailable(path);
    } catch(RepositoryException | ContentRepositoryException e) {
      return false;
    }
  }

  public License createLicense(License license, String userId) {
    String tenantId = TenantHelper.getTenantId();
    license.setType(Type.LICENSE);
    license.setName("license");
    try(ContentSession session = sessionBuilder.adminLogin()) {
      session.createLicenseFolder();
      return session.createLicenseEntry(license, userId);
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }

  }

  public void markPublished(String id) {
    //List<Role> roles = getRoles(id);
    try(ContentSession session = sessionBuilder.login()) {
      //session.setRoles(roles);
      DataSheet ds = session.read(id);
      ds.setStatus(Status.PUBLISHED);
      //@todo Concurrency issue to be address.
      Long versionNumber = ds.getNextVersionNumber();
      ds.setVersionNumber(versionNumber);
      ds.setLastError(null);
      session.update(ds);
      if(Type.SQL_APPENDABLE_DATASHEET.equals(ds.getType())) {
        AppendableDataSheet sDs = session.read(id);
        sDs.setQueryable(true);
        Long higherBound = sDs.getHigherBound();
        if(higherBound != null) sDs.setLastQueried(higherBound);

        session.update(sDs);
      }
      session.markConsumerPresentationStale(id);
    } catch(RepositoryException | ContentRepositoryException e) {
      throw new DataProcessingException(map(e), e);
    }
  }

  public DataSheet saveSqlSource(String fileName,
                                 String parentId,
                                 String sqlQuery) {
    return saveSqlSource(fileName, parentId, sqlQuery,
            new DataSheetSchema(Arrays.asList(), "", Arrays.asList(), null, null));
  }

  public DataSheet saveSqlSource(String fileName, String parentId, String sqlQuery, DataSheetSchema sourceSchema) {
    DataSheet sheet;
    String sqlTimestamp = String.valueOf(new Date().getTime()) + ".sql";
    parentId = checkId(parentId);
    //List<Role> roles = getRoles(parentId);
    try (ContentSession session = sessionBuilder.login()) {
      //session.setRoles(roles);
      if (session.isFilePresent(fileName, parentId)) {
        sheet = session.readByName(fileName, parentId);
        //replace or append
        if (sheet.getMode().equals(Mode.REPLACE)) {
          sheet.setSourceSql(sqlQuery);
          sheet.setDatasheetSourceSchema(sourceSchema);
          session.update(sheet);
        }
      } else {
        sheet = new DataSheet(Type.SQL_DATASHEET);
        sheet.setName(fileName);
        sheet.setDatasheetSourceSchema(sourceSchema);
        sheet.setSourceType(SourceType.sql);
        sheet.setSourceSql(sqlQuery);
        sheet.setStatus(Status.UNPUBLISHED);
        DataSheet ds = session.create(sheet, parentId);
        return ds;
      }

      return sheet;
    } catch (RepositoryException | ContentRepositoryException   e) {
      logger.error("  ", e);
      String message = JcrUtils.mapException(e.getClass()) + " " + e.getMessage();
      throw new ProcessingException(message, e);
    }
  }

  public Persistable getChildFile(String name, String parentId)  {
    //List<Role> roles = getRoles(parentId);
    try(ContentSession session = sessionBuilder.elevateAndLogin()) {
      //session.setRoles(roles);
      return session.readByName(name, parentId);
    } catch(ContentRepositoryException | RepositoryException  e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public <T extends Persistable>T getFileDeep(String id) throws ProcessingException {
    //List<Role> roles = getRoles(id);
    try(ContentSession session = sessionBuilder.elevateAndLogin()) {
      //session.setRoles(roles);
      return session.read(id, new FolderViewOption(2, FolderViewOption.ReadAs.DOCUBE_FILE,
          new QueryOptions(),new SimpleJCRQuery()));
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public void assignAdminPrivilleges(String user,String path,boolean global){
    try(ContentSession session = sessionBuilder.adminLogin()) {
      session.assignAdminPrivilleges(user,path,global);
    } catch(RepositoryException | ContentRepositoryException e) {
      logger.error(Common.ERROR_SECURE_VAULT_ENTRY, e);
      throw new DataProcessingException(map(e), e);
    }
  }

  private void renameCategoryTable(ContentSession session, JiffyTable table, String newName)
          throws RepositoryException {
    String categoryTableName = table.getName().concat(JiffyTable.CATEGORY_SUFFIX);
    JiffyTable categoryTable = session.readByName(categoryTableName, table.getParentId());
    session.renameFile(categoryTable.getId(), newName.concat(JiffyTable.CATEGORY_SUFFIX));
  }

  private void renameMetaTables(ContentSession session, Folder folder, String newName)
          throws RepositoryException {
    String accuracyTableName = folder.getName().concat(JiffyTable.ACCURACY_SUFFIX);
    if(isFilePresent(accuracyTableName, folder.getId())){
      JiffyTable accuracyTable = session.readByName(accuracyTableName, folder.getId());
      session.renameFile(accuracyTable.getId(), newName.concat(JiffyTable.ACCURACY_SUFFIX));
    }

    String pseudonymsTableName = folder.getName().concat(JiffyTable.PSEUDONYMS_SUFFIX);
    if(isFilePresent(pseudonymsTableName, folder.getId())){
      JiffyTable pseudonymsTable = session.readByName(pseudonymsTableName, folder.getId());
      session.renameFile(pseudonymsTable.getId(),
              newName.concat(JiffyTable.PSEUDONYMS_SUFFIX));
    }
  }

  private boolean isApp(Folder folder) {
    return Objects.nonNull(folder.getSubType()) && folder.getSubType().equals(SubType.app);
  }

  private void updateDatasheetDetailsForRename(ContentSession session, String id, String newName)
          throws RepositoryException {
    DataSheet file = session.read(id);
    List<Presentation> presentations = session.findReferences(id);
    for(int i = 0; i< presentations.size(); i++) {
      Presentation presentation = presentations.get(i);
      Dashboard content = presentation.getContent();
      content.getDatasheets().forEach(datasheet -> {
        if(datasheet.getId().equals(id)) {
          datasheet.setName(newName);

          String path = file.getPath();
          String[] owneds = path.split("owned");
          if (path.indexOf(session.getSessionUserID()) == -1) {
            datasheet.setPath("/Shared" + owneds[1]);
          } else {
            datasheet.setPath("/Home" + owneds[1]);
          }
        }
      });
      presentation.setContent(content);
      if(session.verifyWritePermission(presentation.getId())) {
        session.update(presentation);
      }
    }
  }

  public boolean isMember(String group, String user) {
      try(ContentSession session = sessionBuilder.adminLogin()) {
        return session.isMember(group, user);
      } catch(ContentRepositoryException | RepositoryException e) {
        throw new DataProcessingException(e.getMessage());
      }
    }

  public DataSheetProps upsertDataSheet(IngestContext ctx) {
    //List<Role> roles = getRoles(ctx.getId());
    try(ContentSession session = sessionBuilder.login()) {
      //session.setRoles(roles);
      boolean existing =  session.isFilePresent(ctx.getFileName(), ctx.getParentId());
      DataSheetProps processed =  existing?
              update(ctx, session, session.readByName(ctx.getFileName(), ctx.getParentId())) :
              insert(ctx, session);
      return processed;
    } catch(RepositoryException | ContentRepositoryException e) {
      throw new DataProcessingException(map(e), e);
    }
  }

  private DataSheetProps update(IngestContext ctx, ContentSession session,
                                DataSheetProps sheet) throws RepositoryException {
    ctx.setIngestMode("UPDATE");
    sheet.setStatus(Status.UNPUBLISHED);
    if(sheet.getMode().equals(Mode.APPEND)) {
      if(ctx.getCRLFEnding() != sheet.getCRLFEnding()) {
        throw new DataProcessingException("Line endings do not match. Expected " +
                getLineEnd(sheet.getCRLFEnding()) +
                ", but the new file had" +
                getLineEnd(ctx.getCRLFEnding()));
      }
    } else if(sheet.getMode().equals(Mode.REPLACE)) {
      if(SourceType.csv.equals(ctx.getSourceType())) {
        sheet.setDatasheetSourceSchema(ctx.getIngestedSchema());
        sheet.setCRLFEnding(ctx.getCRLFEnding());
      }
    }
    if(sheet instanceof DataSheet) {
      session.update((DataSheet) sheet);
    } else if(sheet instanceof AppendableDataSheet){
      session.update((AppendableDataSheet) sheet);
    }
    return sheet;
  }

  private DataSheet insert(IngestContext ctx, ContentSession session) throws RepositoryException {
    DataSheetSchema schema = SourceType.csv.equals(ctx.getSourceType())
            ? ctx.getIngestedSchema()
            : null;
    ctx.setIngestMode("CREATE");
    DataSheet sheet = new DataSheet();
    sheet.setSourceType(ctx.getSourceType());
    sheet.setSource("");
    sheet.setDatasheetSourceSchema(schema);
    sheet.setCRLFEnding(ctx.getCRLFEnding());

    //For Any type datasheet, set subtype as ANY
    if(SourceType.any.equals(ctx.getSourceType())) {
      sheet.setSubType(SubType.any);
    }

    sheet.setName(ctx.getFileName());
    return session.create(sheet, ctx.getParentId());
  }

  private String getLineEnd(boolean crlfEnding) {
    return crlfEnding ? "\\r\\n" : "\\n";
  }

  public DataSheetProps getDataSheet(String id) throws ProcessingException {
    //List<Role> roles = getRoles(id);
    try(ContentSession session =sessionBuilder.login()) {
      //session.setRoles(roles);
      return session.read(id);
    } catch (ContentRepositoryException |RepositoryException e) {
      logger.error(e.getMessage(),e);
      throw new ProcessingException(map(e),e);
    }
  }

  public DataSheetProps appendToDataSheet(IngestContext ctx) {
    try(ContentSession session = sessionBuilder.login()) {
      DataSheetProps ds = getDataSheet(ctx.getId());
      if(ds == DataSheet.NULL) {
        throw new DataProcessingException("Datasheet not found");
      }
      update(ctx, session, ds);
      return ds;
    } catch(RepositoryException | ContentRepositoryException | ProcessingException e) {
      throw new DataProcessingException(map(e), e);
    }
  }

  public <T extends Persistable> T getFile(String... path){
    try(ContentSession session = sessionBuilder.adminLogin()) {
      return session.read(path);
    } catch (RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  public void upsertUserPreference(UserPreference preference){
    try(ContentSession session = sessionBuilder.adminLogin()) {
       if(!session.isFileExits(TenantHelper.getTenantId(),Content.FOLDER_USERS,TenantHelper.getUser())){
         session.createUserFolder(TenantHelper.getUser());
       }
       if(session.isFileExits(TenantHelper.getTenantId(),Content.FOLDER_USERS,TenantHelper.getUser()
               ,preference.getName())){
         UserPreference existingPreference = session.read(TenantHelper.getTenantId(), Content.FOLDER_USERS, TenantHelper.getUser()
                 , preference.getName());
         existingPreference.setPreference(preference.getPreference());
          session.update(existingPreference);
       }else {
         session.createUserPreference(preference,TenantHelper.getUser());
       }
    } catch (RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  public void upsertGeneralUserPreference(UserPreference preference) {
    try (ContentSession session = sessionBuilder.adminLogin()) {
      if (session.isFileExits(TenantHelper.getTenantId(), Content.FOLDER_USERS, Content.PREFERENCES
              , preference.getName())) {
        UserPreference existingPreference = session.read(TenantHelper.getTenantId(), Content.FOLDER_USERS, Content.PREFERENCES
                , preference.getName());
        existingPreference.setPreference(preference.getPreference());
        session.update(existingPreference);
      } else {
        session.createGeneralUserPreference(preference, TenantHelper.getUser());
      }
    } catch (RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  public boolean isAdmin(){
    String adminGroup = new StringBuilder(TenantHelper.getTenantId())
            .append(Tenant.TENANT_USER_GROUP).toString();
    return isMember(adminGroup,TenantHelper.getUser());
  }

  public boolean isResourceAccessibleToBusinessUser(String id) {
    //@TODO
    //List<Role> roles = getRoles(id);
    try (ContentSession session = sessionBuilder.adminLogin()) {
      //session.setRoles(roles);
      Type fileType = session.getType(id);
      if (!fileType.equals(Type.JIFFY_TABLE)) {
        logger.error("Invalid file type {} ", fileType);
        throw new ProcessingException("Invalid file type");
      }
      /*if (!roles.get(0).getFilesTypes().stream().
              filter(p -> p.getType().equals(Type.JIFFY_TABLE)).
              findAny().orElse(null).getPermission().name().equals(Permission.REFERENCE_WRITE.name())) {
        logger.error("User doesn't have permission to perform this operation, check the file permissions");
        throw new ProcessingException("User doesn't have permission");
      }*/
      return true;
    } catch (ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

   public <T extends Persistable>T getFileFromPathForAdmin(String path) throws
    ProcessingException {
     logger.info("upgrading the business user as designer to upload the attachment");
     try (ContentSession session = sessionBuilder.login()) {
       session.elevateUser();
       return session.read(session.getId(path), FolderViewOption.getFileOptions());
     } catch (ContentRepositoryException | RepositoryException e) {
       throw new ProcessingException(map(e), e);
     }
   }

  //this method will return true if role is business user or custom role
  public boolean isElevationRequired(String id) {
    List<RolesV2> roles = getRolesV2(id);
    List<String> userRoles = roles.stream().map(role -> role.getName()).collect(Collectors.toList());
    boolean isElevationRequired = true;
    if(userRoles.contains(Roles.DESIGNER.name()) || userRoles.contains(Roles.RELEASE_ADMIN.name()) ||
            userRoles.contains(Roles.SUPPORT.name())) {
      logger.debug("Roles contains Non custom and non business user, elevation is not required");
      isElevationRequired = false;
    }
    if(isElevationRequired){
      logger.debug("Role is business or custom role, elevation is required");
    }
    return isElevationRequired;
  }


  public Presentation updateCardUUID(Presentation presentation){
    List<Section> section = ((Layout)((Dashboard) presentation.getContent()).getLayout()).getSections()
            .stream().collect(Collectors.toList());
    section.forEach((k)->{
      if(k instanceof Section){
        k.getPapers().stream().forEach((p)->{
          p.getColumns().stream().forEach((column)->{
            updateStorageCardId(column.getCards());
          });
        });
      }
    });
    elevateUpdateGeneric(presentation);
    return presentation;
  }

  private void updateStorageCardId(List<Card> cards){
    List<Card> storageCards = new ArrayList<>();
    cards.stream().forEach((card)->{
      if(Objects.isNull(card.getCardUUID()) && card.getType().equalsIgnoreCase("JIFFY_TABLE")){
        card.setCardUUID(UUID.randomUUID().toString());
      }
      storageCards.add(card);
    });
  }

  public void logJiffyTableDetails(JiffyTable jt){
    logger.debug("Backing up Jiffy Table {} of {}",jt.getName(),jt.getPath());
    try {
      //String fileName = FileUtils.getFileForBackup(store.getRoot());
      //String currentSchema = schemaService.serializeNode(jt.getCurrentSchema());
      List<Form> forms = jt.getForms();
      StringBuilder stringToWrite = new StringBuilder("/****************************************************/")
              .append(System.getProperty(Common.LINE_SEPARATOR))
              .append(jt.getPath())
              .append(System.getProperty(Common.LINE_SEPARATOR))
              .append("Tenant ID -- ")
              .append(TenantHelper.getTenantId())
              .append(System.getProperty(Common.LINE_SEPARATOR))
              .append("Mongo Table Name " + jt.getTableName())
              .append(System.getProperty(Common.LINE_SEPARATOR))
              .append(jt.getCurrentSchema().toString())
              .append(System.getProperty(Common.LINE_SEPARATOR))
              .append("/***********FORMS START***********/")
              .append(System.getProperty(Common.LINE_SEPARATOR));
      for(Form form : forms){
        //String formData = schemaService.serializeNode(form);
        stringToWrite.append(form.getName())
                .append(System.getProperty(Common.LINE_SEPARATOR))
                .append(form.toString())
                .append(System.getProperty(Common.LINE_SEPARATOR));
      }
      String s = stringToWrite.append("/***********FORMS END***********/")
              .append(System.getProperty(Common.LINE_SEPARATOR))
              .append("/####################################################/")
              .append(System.getProperty(Common.LINE_SEPARATOR))
              .toString();
      JiffyTableBackUpUtils.appendToFile(s);
    } catch (Exception e) {
      logger.error("Error in Creating backup {}",e.getMessage());
    }

  }

  public void migratePermissions(String id){
    try(ContentSession session = sessionBuilder.adminLogin()) {
      AppRoles appRoleFile = session.getAppRoleFile(id);
      session.migratePermissions(id,appRoleFile.getRoles(),TenantHelper.getTenantId());
    } catch (RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }


  public  List<RolesV2> getRolesV2(String id){
    try(ContentSession session = sessionBuilder.login()) {
      return session.getRolesV2(id);
    } catch (RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public  List<RolesV2> getRolesV2ByPath(String appPath){
    try(ContentSession session = sessionBuilder.login()) {
      return session.getRolesV2Path(appPath);
    } catch (RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public List<String> getTenantIds(){
    try(ContentSession session = sessionBuilder.adminLogin()) {
      return session.getTenantIds();
    } catch (RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }

  public Set<String> getCategoryTableIds(Set<String> datasetIds,String appId){
    try(ContentSession session = sessionBuilder.adminLogin()) {
      return session.getCategoryTableIds(datasetIds,appId);
    } catch (RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(map(e), e);
    }
  }


}
