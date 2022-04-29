package com.paanini.jiffy.services;

import com.paanini.jiffy.constants.App;
import com.paanini.jiffy.constants.Common;
import com.paanini.jiffy.dto.Configurations;
import com.paanini.jiffy.dto.JfsFile;
import com.paanini.jiffy.dto.ResourceCleanUpDTO;
import com.paanini.jiffy.encryption.api.CipherService;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.helper.AttachmentFile;
import com.paanini.jiffy.models.JiffyTableAttachment;
import com.paanini.jiffy.storage.DocumentStore;
import com.paanini.jiffy.utils.FileUtils;
import com.paanini.jiffy.utils.TenantHelper;
import com.paanini.jiffy.utils.VfsManager;
import com.paanini.jiffy.utils.*;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.files.FileSet;
import ai.jiffy.secure.client.auditlog.AuditLogger;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;
import java.util.*;

@Service
public class AppResourceService implements ResourceService {

  @Autowired
  VfsManager vfsManager;

  @Autowired
  Configurations configurations;

  @Autowired
  DocumentStore store;

  @Autowired
  CipherService cipherService;

  @Autowired
  FileValidationUtils fileValidationUtils;

  @Autowired
  AuditLogger auditLogger;

  private final String jfsBaseUrl;
  private final String jfsBaseLocation;

  static Logger logger = LoggerFactory.getLogger(AppResourceService.class);

  public AppResourceService(@Value("${jfs.url}") String jfsBaseUrl,
                            @Value("${jfs.base.path}") String jfsBaseLocation) {
    this.jfsBaseUrl = jfsBaseUrl;
    this.jfsBaseLocation = FileUtils.checkAndAddFileSeperator(jfsBaseLocation);
  }

  /**
   * Dowmload file from JFS and upload to fileset
   * @param appPath
   * @param jfsFile
   * @return
   */
  public AttachmentFile saveResource(String appPath, JfsFile jfsFile) {
    FileSet fileset = getStorageFileset(appPath);
    logger.debug("[Service - App resource] Uploading resource for app {} from file {}",
            appPath, jfsFile.getfId());
    try {
      AttachmentFile resource = AttachmentFile.from(jfsFile, configurations.getTempLocation(),
              jfsBaseUrl, cipherService);
      return moveAttachmentToFS(fileset, resource);
    } catch (IOException e) {
      logger.error(Common.SERVICE_APP_RESOURCE_NOT_ABLE_TO_STORE_RESOURCE, appPath,
              e.getMessage());
      throw new ProcessingException(Common.ERROR_WHILE_PROCESSING_RESOURCE);
    }
  }

  public AttachmentFile saveResource(String appPath, MultipartFile attachment) {
    if(!fileValidationUtils.isValidFileExtension(attachment.getOriginalFilename())){
      logger.error("[ARS] Invalid File extension {}, ", attachment.getOriginalFilename());
      throw new ProcessingException("[ARS] Invalid file extension");
    }
    FileSet fileset = getStorageFileset(appPath);
    logger.debug(Common.SERVICE_APP_RESOURCE_UPLOADING_RESOURCE_FOR_APP, appPath);
    try {
      AttachmentFile resource = AttachmentFile.from(attachment, configurations.getTempLocation());
      return moveAttachmentToFS(fileset, resource);
    } catch (IOException e) {
      logger.error(Common.SERVICE_APP_RESOURCE_NOT_ABLE_TO_STORE_RESOURCE, appPath,
              e.getMessage());
      throw new ProcessingException(Common.ERROR_WHILE_PROCESSING_RESOURCE);
    }
  }

  protected AttachmentFile moveAttachmentToFS(FileSet fileset, AttachmentFile resource) throws IOException {
    String refId = UUID.randomUUID().toString();
    logger.debug("[Service - App resource] generated refIf for file upload {}", refId);

    Path loc = getRefPath(refId, vfsManager.getDataPath(fileset.getId()));

    if (!Files.exists(loc)) {
      Files.createDirectories(loc);
      logger.debug("[Service - App resource] created directory for uploading file");
    }

    if(resource.getTempLoc().toFile().isDirectory()) {
      FileUtils.moveDirectory(resource.getTempLoc(), loc);
      logger.debug("[Service - App resource] Successfully moved the upload file directory");
    } else {
      Files.move(resource.getTempLoc(), loc.resolve(resource.getName()));
      logger.debug("[Service - App resource] Successfully moved the upload file");
    }

    resource.setRef(refId);
    resource.setTempRef(null);      //reset tempRef
    return resource;
  }

  protected FileSet getStorageFileset(String appPath) {
    FileSet fileset;
    String appId = vfsManager.getIdFromPath(appPath);
    fileset = vfsManager.isFilePresent(App.APP_STORAGE_NAME, appId)
            ? vfsManager.getFileFromPath(appPath.concat("/").concat(App.APP_STORAGE_NAME))
            : vfsManager.createFileSet(App.APP_STORAGE_NAME, appId);

    return fileset;
  }

  public File getResource(String appPath, String ref, String name) throws IOException {
    logger.debug("[Service - App resource] accessing resource {} for app {}", name, appPath);
    String filesetID = vfsManager.getIdFromPath(
            appPath.concat("/").concat(App.APP_STORAGE_NAME));

    Path permentLocation = getRefPath(ref, vfsManager.getDataPath(filesetID))
            .resolve(name);
    File file = permentLocation.toFile();

    if(file.exists()){
      return file;
    } else {
      logger.error("[Service - App resource] not found resource {} ", file.getPath());
      throw new IOException("Requested file not found :" + appPath + "/" + ref + "/" + name);
    }
  }

  /*public JiffyTableRow migrateOld(String tablePath, JiffyTableRow row) {
    //1. iterate complete record
    //2. move attachment from sha-row to app_filestorage
    //3. update reference in row

    //return the updated Row
    return row;
  }*/

  /**
   * Saves attachment reference folder
   * Used in PDF, where one attachment holds many files
   * @param appPath
   * @param name
   * @param path
   * @return
   */
  @Override
  public JiffyTableAttachment saveResourceFolder(String appPath, String name, Path path) {
    String refId = UUID.randomUUID().toString();
    JiffyTableAttachment resource = new JiffyTableAttachment(name, null, refId);

    FileSet fileset = getStorageFileset(appPath);
    logger.debug(Common.SERVICE_APP_RESOURCE_UPLOADING_RESOURCE_FOR_APP, appPath);
    Path loc = getRefPath(refId, vfsManager.getDataPath(fileset.getId()));
    try {
      if (!Files.exists(loc)) {
        Files.createDirectories(loc);
      }
      FileUtils.moveDirectory(path, loc);
      return resource;
    } catch (IOException e) {
      logger.error("Error while moving from {} to {} : {} : {}",
              path, loc,
              e.getMessage(), e.getClass().getName());
      throw new ProcessingException(Common.ERROR_WHILE_PROCESSING_RESOURCE);
    }
  }

  @Override
  public FileOutputStream getFileOutputstream(String appPath, String ref, String fileName) {
    FileSet fileset = getStorageFileset(appPath);

    String id = FileUtils.getTempFileName();
    try {
      Files.createFile(Paths.get(id));
    } catch (IOException e) {
      throw new ProcessingException("Cannot create file",e);
    }

    try {
      return new FileOutputStream(id){
        @Override
        public void close() throws IOException {

          Path tablePath = getRefPath(ref, vfsManager.getDataPath(fileset.getId()));
          Path permLocation = tablePath.resolve(fileName);

          Files.move(Paths.get(id), permLocation,
                  StandardCopyOption.REPLACE_EXISTING);
          super.close();
        }
      };
    } catch (FileNotFoundException e) {
      throw new ProcessingException("File not found");
    }
  }

  @Override
  public JiffyTableAttachment saveResourceFolder(String appPath, String fId, String name) {
    FileSet fileset = getStorageFileset(appPath);
    logger.debug("[Service - App resource] Downloading resource for app {} from file {} {}",
            appPath, jfsBaseLocation, fId);
    Path jfsPath = Paths.get(JFSService.getFilePath(jfsBaseUrl,fId,cipherService));
    try {
      AttachmentFile resource = AttachmentFile.from(jfsPath,configurations.getTempLocation(),name);
      return moveAttachmentToFS(fileset, resource);
    } catch (IOException e) {
      logger.error(Common.SERVICE_APP_RESOURCE_NOT_ABLE_TO_STORE_RESOURCE, appPath,
              e.getMessage(), e);
      throw new ProcessingException(Common.ERROR_WHILE_PROCESSING_RESOURCE);
    }
  }


  Path getRefPath(String ref, Path filesetPath) {
    //new Sha256Hash(ref).toString();
    String docSha = DigestUtils.sha256Hex(ref);
    return filesetPath.resolve(docSha.substring(0, 2))
            .resolve(docSha.substring(2));
  }


  public JiffyTableAttachment copyResource(String appPath, JiffyTableAttachment attachment) {
    String refId = UUID.randomUUID().toString();

    JiffyTableAttachment resource = new JiffyTableAttachment(attachment.getName(), null, refId);

    FileSet fileset = getStorageFileset(appPath);
    logger.debug(Common.SERVICE_APP_RESOURCE_UPLOADING_RESOURCE_FOR_APP, appPath);
    try {

      Path loc = getRefPath(refId, vfsManager.getDataPath(fileset.getId()));
      Path original = getRefPath(attachment.getRef(), vfsManager.getDataPath(fileset.getId()));

      if (!Files.exists(loc)) {
        Files.createDirectories(loc);
      }
      FileUtils.copyDirectory(original, loc);
      return resource;
    } catch (IOException e) {
      throw new ProcessingException(Common.ERROR_WHILE_PROCESSING_RESOURCE);
    }
  }

  public String getTenantPath() {
    return TenantHelper.getTenantId() + "/";
  }

  public void addFileToResource(String appPath, MultipartFile attachment, String refId, String name) {
    logger.debug("[Service - App resource] updating resource {} for app {}", name, appPath);
    String filesetID = vfsManager.getIdFromPath(
            appPath.concat("/").concat(App.APP_STORAGE_NAME));

    Path permentLocation = getRefPath(refId, vfsManager.getDataPath(filesetID));

    try {
      InputStream inStream = attachment.getInputStream();

      Path tempfilePath = Paths.get(FileUtils.getTempFileName());
      Files.copy(inStream, tempfilePath, StandardCopyOption.REPLACE_EXISTING);
      Files.move(tempfilePath, permentLocation.resolve(name), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      logger.error("Error while moving resource {} : {}", name, e, e.getMessage(), e);
      throw new ProcessingException("Error while moving resource " + name);
    }
  }

  public AttachmentFile saveResourceForForms(String appPath, MultipartFile attachment, String id) {
    logger.debug("[ARS] uploading file for user {}", TenantHelper.getUser());
    if(!fileValidationUtils.isValidFileExtension(attachment.getOriginalFilename())) {
      logger.error("[ARS] Invalid File extension {}, ", attachment.getOriginalFilename());
      throw new ProcessingException("Invalid file extension");
    }
    if(!fileValidationUtils.isValidFileName(attachment.getOriginalFilename())){
      logger.error("[ARS] Invalid File name {}, ", attachment.getOriginalFilename());
      throw new ProcessingException("Invalid file Name");
    }
    FileSet fileset = new FileSet();
    if(vfsManager.isElevationRequired(id)){
      logger.debug("[ARS] started uploading file for business user");
      if (vfsManager.isResourceAccessibleToBusinessUser(id)) {
        logger.debug("[ARS] validated the access control, uploading the file");
        fileset = getStorageFilesetForAdmin(appPath);
      } else {
        logger.error("Permission denied to perform this operation");
        throw new ProcessingException("Permission denied to perform this action");
      }
    } else {
      logger.debug("[ARS] started uploading attachment");
      fileset = getStorageFileset(appPath);
    }
    try {
      String[] pathArray = appPath.split("/");
      AttachmentFile resource = AttachmentFile.from(attachment, configurations.getTempLocation());
      logger.debug("moving the attachment file to docube FS");
      AttachmentFile attachmentFile = moveAttachmentToFS(fileset, resource);
      BasicFileProps file = vfsManager.getFile(id);
      try{
        auditLogger.log("Jiffy Table",
                "Update",
                new StringBuilder("Updation of Jiffy Table : ")
                        .append(file.getName())
                        .append(" in the App ")
                        .append(pathArray[1])
                        .append(" under the App Group : ")
                        .append(pathArray[0])
                        .append(" Attachement added")
                        .toString(),
                "Success",
                Optional.empty()
        );
      }catch (Exception e ){
        logger.error("Failed to write auditlog for upload attachment {}",e.getMessage());
      }

      return attachmentFile;
    } catch (IOException e) {
      logger.error(Common.SERVICE_APP_RESOURCE_NOT_ABLE_TO_STORE_RESOURCE, appPath,
              e.getMessage());
      throw new ProcessingException(Common.ERROR_WHILE_PROCESSING_RESOURCE);
    }
  }

  protected FileSet getStorageFilesetForAdmin(String appPath) {
    FileSet fileset;
    String appId = vfsManager.getIdFromPath(appPath);
    fileset = vfsManager.isFilePresent(App.APP_STORAGE_NAME, appId)
            ? vfsManager.getFileFromPathForAdmin(appPath.concat("/").concat(App.APP_STORAGE_NAME))
            : vfsManager.createFileSet(App.APP_STORAGE_NAME, appId);

    return fileset;
  }

  public Map<Object, Object> deleteResources(String appPath, List<ResourceCleanUpDTO>resourceCleanUpDTOs){
    logger.info("[ARS] deleting resources for app {} ", appPath);
    Map<Object, Object> deletionStatus = new HashMap<>();
    List<String> deletionFailureRefs = new ArrayList<>();
    String filesetID = vfsManager.getIdFromPath(
            appPath.concat("/").concat(App.APP_STORAGE_NAME));
    long start = System.currentTimeMillis();
    for(ResourceCleanUpDTO ref : resourceCleanUpDTOs){
      Path permentLocation = getRefPath(ref.getRef(), vfsManager.getDataPath(filesetID))
              .resolve(ref.getName());
      try {
        Files.delete(permentLocation);
      } catch (IOException e) {
        logger.error("Failed to delete the resource {} {} {} {} ",permentLocation,  ref.getRef(),
                ref.getName(), e.getMessage());
        deletionFailureRefs.add(ref.getRef());
      }
    }
    long end = System.currentTimeMillis();
    logger.debug("Time taken for clean up {} ms", (end - start));
    deletionStatus.put("failedRefIds", deletionFailureRefs);
    return deletionStatus;

  }

    public Map duplicateResources(String appPath, List<Map<String, Object>> attachments) {
        logger.info("[ARS] duplicating resources for appPath {}",appPath);
        Map<String, Map<String, String>> duplicates = new HashMap<>();
        String filesetID = vfsManager.getIdFromPath(
              appPath.concat("/").concat(App.APP_STORAGE_NAME));
        attachments.forEach(att -> {
          if(!att.containsKey("ref") || !att.containsKey("name")){
            logger.error("Failed to duplicate, invalid input : ref or name of attachment missing {}", att);
            throw new ProcessingException("Error while duplicating resource, invalid input ");
          }
            Map<String, String> duplicate = new HashMap<>();
            String ref = att.get("ref").toString();
            String name = att.get("name").toString();
            String newRefId = getDuplicateId(appPath, ref, name, filesetID);
            duplicate.put("ref", newRefId);
            duplicate.put("name", name);
            duplicates.put(ref, duplicate);
        });
        logger.info("[ARS] successfully duplicated the resources");
        return duplicates;
    }

    private String getDuplicateId(String appPath, String ref, String name, String filesetID) {
        logger.debug("[ARS] duplicating resource, ref: {}, name: {}", ref, name);
        File existingFile = null;
        String newRefId = UUID.randomUUID().toString();
        logger.debug("[ARS] duplicated refId: {}", newRefId);
        try {
            existingFile = getResource(appPath, ref, name);
            Path newPath = getRefPath(newRefId, vfsManager.getDataPath(filesetID));
            if (!Files.exists(newPath)) {
                Files.createDirectories(newPath);
                logger.debug("created directory {} ", newPath);
            }
            newPath = newPath.resolve(name);
            Files.createLink(newPath, existingFile.toPath());
            return newRefId;
        } catch (Exception e) {
            logger.error("Failed to duplicate {} {} ", e.getMessage(), e);
            throw new ProcessingException("Error while duplicating resource " + name);
        }
    }

}
