package com.paanini.jiffy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.option3.docube.schema.nodes.SourceType;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.constants.Common;
import com.paanini.jiffy.dto.Configurations;
import com.paanini.jiffy.exception.ContentRepositoryException;
import com.paanini.jiffy.exception.DataProcessingException;
import com.paanini.jiffy.jcrquery.QueryModel;
import com.paanini.jiffy.proc.api.CustomFileContext;
import com.paanini.jiffy.proc.api.IngestContext;
import com.paanini.jiffy.services.DocumentService;
import com.paanini.jiffy.services.PresentationService;
import com.paanini.jiffy.storage.DocumentStore;
import com.paanini.jiffy.tfo.ApiResponse;
import com.paanini.jiffy.utils.*;
import com.paanini.jiffy.utils.validator.InputValidator;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.Presentation;
import com.paanini.jiffy.vfs.io.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.jcr.RepositoryException;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping("/documents")
@Consumes(MediaType.APPLICATION_JSON_VALUE)
public class DocumentsWS implements WebResponseHandler {
  public static final String DELETE = "delete/";
  public static final String STORAGE_POST = "storage/";
  public static final String FORMAT = "format";
  public static final String TYPE = "type";
  public static final String NAME = "name";
  private static final int STOP_AT = 1024;
  private static final int START_AT = 0;
  private static final int REC_NUMBER = 10;
  static Logger logger = LoggerFactory.getLogger(DocumentsWS.class);
  SecureRandom sr = new SecureRandom();

  static String cookie;
  private boolean encryptionEnabled;

  @Autowired
  Configurations config;

  @Autowired
  private VfsManager vfsManager;

  @Autowired
  DocumentStore documentStore;

  @Autowired
  DocumentService documentService;

  @Autowired
  PresentationService presentationService;

  public boolean isEncryptionEnabled() {
    return encryptionEnabled;
  }

  public void setEncryptionEnabled(boolean encryptionEnabled) {
    this.encryptionEnabled = encryptionEnabled;
  }

  @GetMapping("/")
  public Persistable readFolder(@RequestParam(required = false) String type,
                                @RequestParam(required = false) String sortBy,
                                @RequestParam(required = false) String order,
                                @RequestParam(required = false) String status,
                                @RequestParam(required = false) String id,
                                @RequestParam(defaultValue = "false") boolean isNested,
                                @RequestParam(defaultValue = "false") Boolean shared) {
    return documentService.readFolderService(type, sortBy, order, status, id, isNested, shared);
  }

  @GetMapping("/fileset/{id}/{member:.+}")
  public Persistable readFileset(
          @PathVariable("id") String id,
          @PathVariable("member") String member,
          @RequestParam(name ="sortby") String sortBy,
          @RequestParam(name ="order") String order,
          @RequestParam(name ="status", required = false) String status) {
    return documentService.readFileSetService(id, member);
  }

  @GetMapping("/removeOrphanedFolder")
  public ResponseEntity removeOrphanedFolder() throws IOException {
    try {
      documentService.removeOrphanedFolderService();
    } catch (IOException e) {
      return errorResponseEntity(e.getMessage());
    }
    return ResponseEntity.ok().build();
  }

  @GetMapping("/revert/removeOrphanedFolder")
  public ResponseEntity revertRemoveOrphanedFolder() throws IOException {
    try {
      documentService.revertRemovedOrphanedFolderService();
    } catch (IOException e) {
      return errorResponseEntity(e.getMessage());
    }
    return ResponseEntity.ok().build();
  }

  @PutMapping("/{id}")
  public ResponseEntity renameDocument(@PathVariable("id") String id,
                             @RequestParam(name ="newName") String newName)
  {
    InputValidator.validateFileName(newName);
    documentService.renameFile(id, newName);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/move/{sourceId}/{dest}")
  public ResponseEntity moveDocument(@PathVariable("sourceId") String sourceId,
                           @PathVariable("dest") String destId) {
    vfsManager.moveDocument(sourceId, destId);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{id}")
  public ResponseEntity deleteFile(@PathVariable("id") String id) {
    documentService.deleteService(id);
    return ResponseEntity.ok().build();
  }


  @PostMapping("/duplicate/{id}")
  public ResponseEntity duplicateFile(@PathVariable("id") String id) throws RepositoryException {
    String id_dup = documentService.duplicatFileService(id);
    return okResponseEntity("id", id_dup);
  }

  @GetMapping("/presentation/{id}")
  public ResponseEntity<StreamingResponseBody> getDocument(@PathVariable("id") String id) {
    return getPresentation(id);
  }

  private ResponseEntity<StreamingResponseBody> getPresentation(String id) {
    Presentation ppt = vfsManager.getFile(id);
    StreamingDocumentResponse.JsonConsumer c = (g, os) -> {
      ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
      g.writeFieldName("content");
      try {
        objectMapper.writeValue(g, ppt.getContent());
      } catch (Exception e) {
        throw e;
      }
    };
    StreamingResponseBody responseBody = newStreamingResponseDocument(ppt,c);
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(responseBody);
  }


  @PostMapping("/folderByPath/")
  public ResponseEntity createFolderByPath(@RequestParam(name ="name") String name,
                                     @RequestParam(name ="parentPath") String parentPath) {
    try {
      documentService.createFolderByPathService(name, parentPath);
      return okResponseEntity("name", name);
    } catch (ProcessingException e) {
      logger.error("Error creating Folder", e);
      return errorResponseEntity(e.getMessage());
    }
  }

  @PostMapping("/folder/")
  public ResponseEntity createFolder(@RequestParam(name ="name") String name,
                               @RequestParam(name ="parentId") String parentId) {
    try {
      documentService.createFolderService(name, parentId);
      return okResponseEntity("name", name);
    } catch (DataProcessingException e) {
      logger.error("Error creating Folder", e);
      return errorResponseEntity(e.getMessage());
    }

  }

  @PostMapping("/fileset")
  public ResponseEntity createFileSet(@RequestParam(name ="name") String name,
                                @RequestParam(name ="parentId") String parentId) throws IOException {
    documentService.createFileSetService(name, parentId);
    return okResponseEntity("name", name);
  }

  @PostMapping("/fileset/upload")
  @Consumes(MediaType.MULTIPART_FORM_DATA_VALUE)
  @Deprecated
  public ResponseEntity uploadFileSet(@RequestParam("file") MultipartFile attachment,
                                @RequestParam(name ="parentId") String parentId) {
    try {
      Map<String, Object> result = documentService.uploadFileSetService(attachment, parentId);
      return ResponseEntity.ok(result);
    } catch (DataProcessingException e) {
      return errorResponseEntity(e.getMessage());
    }
  }

  @PostMapping("/fileset/{appgroup}/{app}/{fileSet}/upload")
  @Consumes(MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity uploadFileSetByPath(@RequestParam("file") MultipartFile attachment,
                                            @PathVariable("appgroup") String appgroup,
                                            @PathVariable("app") String app,
                                            @PathVariable("fileSet") String fileSet) {
    try {
      Map<String, Object> result = documentService.uploadFileSetService(attachment,
              vfsManager.getIdFromPath(FileUtils.getPath(appgroup,app,fileSet)));
      return ResponseEntity.ok(result);
    } catch (DataProcessingException e) {
      return errorResponseEntity(e.getMessage());
    }
  }

  @DeleteMapping("/fileset/{filesetId}/{fileName}")
  @Deprecated
  public ResponseEntity deleteFileSetEntry(@PathVariable("filesetId") String filesetId,
                                 @PathVariable("fileName") String fileName) {
    documentService.deleteDileSetEntryService(filesetId, fileName);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/fileset/{appgroup}/{app}/{fileSet}/{fileName}")
  public ResponseEntity deleteFileSetEntryByPath(@PathVariable("appgroup") String appgroup,
                                       @PathVariable("app") String app,
                                       @PathVariable("fileSet") String fileSet,
                                       @PathVariable("fileName") String fileName) {
    documentService.deleteDileSetEntryService(vfsManager.getIdFromPath(FileUtils.getPath(appgroup,app,fileSet)), fileName);
    return ResponseEntity.noContent().build();
  }


  @DeleteMapping("/fileset/clear/{id}")
  @Deprecated
  public ResponseEntity deleteFilesetContent(@PathVariable("id") String id) {
    documentService.deleteFilesetContentService(id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/fileset/{appgroup}/{app}/{fileSet}/clear")
  public ResponseEntity deleteFilesetContentByPath(@PathVariable("appgroup") String appgroup,
                                         @PathVariable("app") String app,
                                         @PathVariable("fileSet") String fileSet) {
    documentService.deleteFilesetContentService(vfsManager.getIdFromPath(FileUtils.getPath(appgroup,app,fileSet)));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/fileset/memberCount/{id}")
  public ResponseEntity countMembers(@PathVariable("id") String id) {
    ResultMap count = documentService.countMemberService(id);
    return okResponseEntity(count);
  }

  @PostMapping("/presentation")
  public Map<String, Object> saveDashboard(@RequestBody Presentation presentation,
                                           @RequestParam(name ="parentId") String parentId,
                                            @RequestParam(name ="path") String parentPath) {
    String id = presentationService.saveDashboardService(presentation, parentId);
    Map<String, Object> map = new ResultMap().add("id", id).build();
    return map;
  }


  @PutMapping("/presentation/{id}")
  @Consumes(MediaType.APPLICATION_JSON_VALUE)
  public void updateDashboard(@RequestBody Presentation presentation,
      @PathVariable("id") String id) {
    presentationService.updateDashboardService(presentation);
  }


  /**
   * API to upload any file other than datasheet
   *
   * @param attachment
   * @param parentId
   * @return
   */
  @PostMapping("/upload")
  public ResponseEntity uploadFile(@RequestParam("file") MultipartFile attachment,
                             @RequestParam(name ="parentId") String parentId) {
    try {
      Map<String, Object> result = documentService.uploadFileService(attachment, parentId);
      return ResponseEntity.ok(result);
    } catch (DataProcessingException e) {
      return errorResponseEntity(e.getMessage());
    }
  }

  @PostMapping("/datasheet/directUpload/{id}")
  public ResponseEntity directUpload(@RequestParam("file") MultipartFile attachment,
                               @PathVariable("id") String id) {
    Map<String, Object> result = documentService.directUploadService(attachment, id);
    return ResponseEntity.ok(result);
  }

  @PostMapping
  @Path("/file/upload")
  @Consumes(MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity uploadFile(@RequestParam("file") MultipartFile attachment) {
    ResultMap rm = uploadFileService(attachment);
    return okResponseEntity(rm);
  }

  private ResultMap uploadFileService(MultipartFile attachment) {
    IngestContext ctx = new IngestContext(documentStore);
    ctx.setTimezoneOffsetInSeconds(Utils.getTimezoneOffset());
    documentService.upload(ctx, attachment);
    ResultMap rm = new ResultMap().add("name", ctx.getFileName()).add("location",
            ctx.getStagedFileLocation().toString());
    return rm;
  }



  @GetMapping("/sparkConfigNames")
  public ResponseEntity getSparkConfigNames() {
    List<Map<String, String>> hashMap =  new ArrayList<>();// sparkConfigFactory.getSparkConfigNames();
    return ResponseEntity.ok(hashMap);
  }


  @PostMapping("/spark")
  public ResponseEntity sparkJobByPath(@RequestParam(name ="path") String path) {
    try {
      Map<String, Object> map = documentService.submitSparkJobService(vfsManager.getIdFromPath(path));
      return ResponseEntity.ok(map);
    } catch (ProcessingException e) {
      logger.error(Common.ERROR_WHILE_MAPPING_FILE_NAME_TO_ID, e);
      return errorResponseEntity(e.getMessage());
    }
  }


  @PostMapping("/spark/{id}")
  @Deprecated
  public ResponseEntity submitSparkJob(@PathVariable("id") String id) {
    try {
      Map<String, Object> map = documentService.submitSparkJobService(id);
      return ResponseEntity.ok(map);
    } catch (ProcessingException e) {
      logger.error("Error while starting Spark job", e);
      return errorResponseEntity(e.getMessage());
    }
  }

  @GetMapping("/sparkstatus/{id}")
  public ResponseEntity sparkStatus(@PathVariable("id") String id) {
    /*String status = this.sparkService.status(id);*/
    /*return okResponse("status", status);*/
    return ResponseEntity.ok().build();
  }

 /* @PostMapping("/spark/{path:.+}/run")
  @Deprecated
  public ResponseEntity submitSparkJobByPath(@PathVariable("path") String path) {
    return submitSparkJob(vfsManager.getIdFromPath(path));
  }

  @GetMapping("/spark/{path:.+}/status")
  public ResponseEntity checkJobStatus(@PathVariable("path") String path) {
    return sparkStatus(vfsManager.getIdFromPath(path));
  }*/




    /*@GetMapping
    @Path("/sharedetails")

    public ResponseEntity getShareDetails(@RequestParam(name ="id") String id) {
        actorService.publishAccessLog(
                System.currentTimeMillis(),
                getUser(),
                "shareDetails", id, null);
        return ResponseEntity.ok().entity(vfsManager.getShareDetails(id)).build();
    }*/

  @GetMapping
  @Path("/datasheet/download/{id}")
  @Deprecated
  @Produces("text/csv")
  public ResponseEntity download(@PathVariable("id") String id) throws RepositoryException, ContentRepositoryException, SQLException, IOException {
    throw new ProcessingException("TODO");
    /*try (ContentSession session =
                 cRepo.newTenantSession(TenantHelper.getTenantId()).loginWithCurrentToken()) {
      if (!session.checkPermission(id, Permission.DOCUBE_READ_DOWNLOAD)) {
        return errorResponse("User does not have permission to " + "download");
      }

      DataSheet ds = documentService.getDataSheetByPath(session, id);
      String tempFileName = "t" + Math.abs(sr.nextLong());
      PrepareContext pc = new PrepareContext(this.cRepo, this.drill, this.documentStore);
      pc.setTimezoneOffsetInSeconds(Utils.getTimezoneOffset());
      pc.setDataSheet(ds);
      pc.setDownloadName(tempFileName);
      pc.setTempFolder(config.getTempLocation());
      pc.setEncodeDownloadedUTF8Files(config.isEncodeDownloadedUTF8Files());
      pc.setDownloadedFileEncoding(config.getEncodeDownloadedUTF8FilesAs());
      //--Adding userRole restrictions to downloadable file.
      pc.setVfsManager(vfsManager);
      String attribute =
              (String) SecurityUtils.getSubject().getSession()
                      .getAttribute(LoginInterceptor.DATA_ROLES);
      pc.setUserRoles(SerializerUtils.deserializeArray(attribute));

      Preparable preparable =
              ds.getSourceType().equals(SourceType.xlsx)
                      || !Status.PUBLISHED.equals(ds.getStatus())
                      ? new SourceDownloadableFile() : new SqlDownloadableFile();
      preparable.prepare(pc);

      String folder = config.getTempLocation() + tempFileName;
      String fileLocation = folder + "/0_0_0." + preparable.getOutputType();
      logger.debug("Serving file from {}", fileLocation);
      java.io.File file = new File(fileLocation);
      DeleteOnCloseFileInputStream stream = new DeleteOnCloseFileInputStream(file, folder);

      String name = "zip".equals(preparable.getOutputType())
              ? ds.getName() + ".zip" : ds.getName();
      ResponseEntity res = ResponseEntity.ok(stream, preparable.getOutputMimeType())
              .header("Content-Disposition", "attachment; filename=\"" + name + "\"")
              .header("Content-Description", "File Transfer")
              .header("Content-Type", preparable.getOutputMimeType())
              .build();

      return res;
    }*/
  }


  /*@GetMapping("/datasheet/{path:.+}/download")
  @Deprecated
  @Produces("text/csv")
  public ResponseEntity downloadDatasheetByPath(@PathVariable("path") String path) throws RepositoryException,
          SQLException, IOException, ContentRepositoryException {
    return download(vfsManager.getIdFromPath(path));
  }*/

  /**
   * Downloads the file-set members as a zip file
   *
   * @param id
   * @return
   * @throws IOException
   */

  @GetMapping("/fileset/download/{id}")
  @Deprecated
  @Produces("text/csv")
  public ResponseEntity<StreamingResponseBody> downloadFileSet(@PathVariable("id") String id) throws IOException {
    BasicFileProps basicFileProps = vfsManager.getFile(id);
    CustomFileContext sourceContext = new CustomFileContext(documentStore);
    sourceContext.setCustomId(id);
    String sourcePath = sourceContext.getDatasheetBasePath(id).toUri().getPath().concat("data/");
    return prepareToDownload(basicFileProps.getName(), sourcePath);
  }

  private ResponseEntity<StreamingResponseBody> prepareToDownload(String name, String sourcePath) throws IOException {
    String tempFileName = "t" + sr.nextLong();
    String folder = config.getTempLocation() + tempFileName;

    StreamingResponseBody stream = out -> {
      FileOutputStream fileOutputStream = new FileOutputStream(folder);
      ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
      File directory = new File(sourcePath);

      zipDirectory(zipOutputStream, directory, sourcePath);

      zipOutputStream.close();
      fileOutputStream.close();

      File file = new File(folder);
    };

    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Disposition", "attachment; filename=\"" + name.concat(".zip") + "\"");
    headers.add("Content-Description", "File Transfer");
    headers.add("Content-Type", "");
    return new ResponseEntity(stream,headers, HttpStatus.OK);
  }

  private void zipDirectory(ZipOutputStream zipOutputStream, File directory, String sourcePath)
          throws IOException {
    File files[] = directory.listFiles();
    byte[] buffer = new byte[1024];

    for (File file : files) {
      if (file.isDirectory()) {
        zipDirectory(zipOutputStream, file, sourcePath);
        continue;
      }
      try (FileInputStream fis = new FileInputStream(file)) {
        zipOutputStream.putNextEntry(new ZipEntry(file.getPath().replace(sourcePath, "")));
        int length;

        while ((length = fis.read(buffer)) > 0) {
          zipOutputStream.write(buffer, 0, length);
        }

        zipOutputStream.closeEntry();
      }
    }
  }


  /**
   * Generates a temporary token for the spark program
   *
   * @return
   */
  @GetMapping("/getSparkSessionToken")
  public ResponseEntity getSparkSessionToken() {
    String value = documentService.getSparkSessionTokenService();
    return okResponseEntity("token", value);
  }


  /**
   * Uses display-path of a file to acquire its id
   *
   * @param path
   * @return
   */
  @GetMapping("/pathId")
  public ResponseEntity getIdForPath(@RequestParam(name ="path") String path) {
    try {
      BasicFileProps file = vfsManager.getFileFromPath(path);
      return okResponseEntity("id", file.getId());
    } catch (ProcessingException e) {
      logger.error(Common.ERROR_WHILE_MAPPING_FILE_NAME_TO_ID, e);
      return errorResponseEntity(e.getMessage());
    }
  }

  /**
   * Uses id of a file to acquire its physical directory (parent directory of
   * data and parquet folder)
   *
   * @param id
   * @return
   */
  @GetMapping("/idPath")
  public ResponseEntity getPathForId(@RequestParam(name ="id") String id) {
    try {
      String path = vfsManager.getFilePath(id);
      return okResponseEntity("path", path);
    } catch (ProcessingException e) {
      logger.error("Error while mapping file id to name", e);
      return errorResponseEntity(e.getMessage());
    }
  }

  /**
   * Gets the physical-path of a file from its display-path. The actual
   * folder normally varies based on "destination". READ destination =>
   * current parquet folder; WRITE destination => newly created parquet
   * folder; DATA destination => data folder. However, for file like
   * notebook (which does not have parquet structure), all three
   * destination would lead to data folder
   *
   * @param path - display-path of a file
   * @return Response containing physical-folder associated with file
   */
  @GetMapping("/physicalPath")
  public ResponseEntity getPhysicalPath(@RequestParam(name ="path") String path,
                                  @RequestParam(name ="destination") String destination) {
    try {
      BasicFileProps file = vfsManager.getFileFromPath(path);
      String physicalPathFromFile = documentService.getPhysicalPathFromFile(file, destination);
      return okResponseEntity("path", physicalPathFromFile);
    } catch (ProcessingException e) {
      logger.error(Common.ERROR_WHILE_MAPPING_FILE_NAME_TO_ID, e);
      return errorResponseEntity(e.getMessage());
    }
  }

  /**
   * Gets the physical-path of a file from its id. The actual
   * folder normally varies based on "destination". READ destination =>
   * current parquet folder; WRITE destination => newly created parquet
   * folder; DATA destination => data folder. However, for file like
   * notebook (which does not have parquet structure), all three
   * destination would lead to data folder
   *
   * @param id
   * @return Response containing physical-folder associated with file
   */
  @GetMapping("/idPhysicalPath")
  public ResponseEntity getPhysicalPathFromId(@RequestParam(name ="id") String id,
                                        @RequestParam(name ="destination") String destination) {
    try {
      BasicFileProps file = vfsManager.getFile(id);
      return okResponseEntity("path", documentService.getPhysicalPathFromFile(file, destination));
    } catch (ProcessingException e) {
      logger.error(Common.ERROR_WHILE_MAPPING_FILE_NAME_TO_ID, e);
      return errorResponseEntity(e.getMessage());
    }
  }

  @GetMapping("/downloadfilesetmember/{fileSetId}")
  @Deprecated
  public ResponseEntity<StreamingResponseBody> downloadFileSetMember(@PathVariable("fileSetId") String id,
                                        @RequestParam(name ="name") String name,
                                        @RequestParam(name ="member",required = false) String member) {
    java.nio.file.Path path = documentStore.getFileSystem().getPath(id).resolve("data");

    if (member != null) {
      path = path.resolve(member);
    }

    path = path.resolve(name);

    if (path.toFile().isDirectory()) {
      try {
        return prepareToDownload(name, path.toString());
      } catch (IOException e) {
        throw new ProcessingException("Unable to download a file");
      }
    }

    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Disposition", "attachment;" + "filename=\"" + name + "\"");
    headers.add("Content-Description", "File Transfer");
    headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
    return new ResponseEntity(path.toFile(),headers, HttpStatus.OK);
  }

  @GetMapping("/path/{appgroup}/{app}")
  public Persistable getDocumentsByType(@PathVariable("appgroup") String appgroup,
      @PathVariable("app") String app,
                      @RequestParam(name ="type",required = false) String type,
                      @RequestParam(name ="fileType",required = false) String docubeFileType) {

    if (!(Objects.isNull(docubeFileType) || docubeFileType.trim().isEmpty())) {
      return documentService.getFiles(FileUtils.getPath(appgroup,app), Type.valueOf(docubeFileType));
    }

    FileType fileType = documentService.getFileTypeService(type);
    return documentService.getAndFilterFiles(FileUtils.getPath(appgroup,app), fileType);
  }


  @GetMapping("/path/{appgroup}/{app}/{fileSet}")
  public Persistable getDocumentsByType(@PathVariable("appgroup") String appgroup,
                                        @PathVariable("app") String app,
                                        @PathVariable("fileSet") String fileSet,
                                        @RequestParam(name ="type",required = false) String type,
                                        @RequestParam(name ="fileType",required = false) String docubeFileType) {

    if (!(Objects.isNull(docubeFileType) || docubeFileType.trim().isEmpty())) {
      return documentService.getFiles(FileUtils.getPath(appgroup,app,fileSet), Type.valueOf(docubeFileType));
    }

    FileType fileType = documentService.getFileTypeService(type);
    return documentService.getAndFilterFiles(FileUtils.getPath(appgroup,app), fileType);
  }


  /**
   * Get files by type
   *
   * @param type
   * @return
   */
  @GetMapping("/{appgroup}/{app}/files")
  public Persistable getFilesByType(@PathVariable("appgroup") String appgroup,
      @PathVariable("app") String app,
                                    @RequestParam(name ="type") String type) {
    Type fileType = documentService.getFileTypeWithNull(type);
    if (Objects.isNull(fileType)) {
      throw new ProcessingException("Please mention the file type");
    }
    return documentService.getFiles(FileUtils.getPath(appgroup,app), fileType);
  }

  /**
   * Get files by query
   *
   * @param type
   * @return
   */
  @PostMapping("/{appgroup}/{app}/files")
  public Persistable getFilesByType(@PathVariable("appgroup") String appgroup,
                                    @PathVariable("app") String app,
                                    @RequestParam(name ="type",required = false) String type,
                                    @RequestBody QueryModel queryModel) {
    Type fileType = documentService.getFileTypeWithNull(type);
    return documentService.getFiles(FileUtils.getPath(appgroup,app), Optional.of(queryModel), Arrays.asList(fileType));
  }

  /**
   * Get files by query
   *
   * @return
   */
  @PostMapping("/{appgroup}/{app}/filesData")
  public ResponseEntity getFilesDataByType(@PathVariable("appgroup") String appgroup,
                                    @PathVariable("app") String app,
                                    @RequestBody QueryModel queryModel) {
    return ResponseEntity.ok(documentService.getFilesDataByQuery(FileUtils.getPath(appgroup,app),
            Optional.of(queryModel)));
  }

  @PostMapping("/fileset/{appgroup}/{app}/uploadFiles")
  @Consumes(MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity uploadFilesByPath(@RequestParam("file") List<MultipartFile> attachments,
      @PathVariable("appgroup") String appgroup,
      @PathVariable("app") String app) {
    List<Map> data = uploadFilesByPathService(attachments, FileUtils.getPath(appgroup,app));
    return ResponseEntity.ok(data);
  }

  protected List<Map> uploadFilesByPathService(List<MultipartFile> attachments, String path) {
    String parentId = vfsManager.getIdFromPath(path);
    List<Map> data = new ArrayList<>();
    for (MultipartFile attachment : attachments) {
      IngestContext ctx = new IngestContext(documentStore);
      ctx.setTimezoneOffsetInSeconds(Utils.getTimezoneOffset());
      ctx.setParentId(vfsManager.checkId(parentId));
      ctx.setSourceType(SourceType.filesetMember);

      data.add(documentService.handleFilesetSetUpload(ctx, attachment));
    }
    return data;
  }


  @GetMapping("/{appgroup}/{app}/permission")
  public boolean hasPermissionForFiles(@PathVariable("appgroup") String appgroup,
                                       @PathVariable("app") String app,
                                       @RequestParam(name ="type") String type,
                                       @RequestParam(name = "permission",required = false) String permission) {
    Type fileType = documentService.getFileTypeWithNull(type);
    if (Objects.isNull(fileType)) {
      throw new ProcessingException("Please mention the file type");
    }
    return documentService.hasPermissionForFilesV2(FileUtils.getPath(appgroup,app), fileType,permission);
  }

  /**
   * Get files by fileType
   *
   * @param appgroup
   * @param type
   * @return
   * @return
   */
  @PostMapping("/{appgroup}/{app}/fileType")

  public Persistable getFilesByFileType(@PathVariable("appgroup") String appgroup,
                                        @PathVariable("app") String app,
                                        @RequestParam(name ="type") String type,
                                        QueryModel queryModel) {
    FileType fileType = documentService.getFileTypeService(type);
    return documentService.getFilesByFileType(FileUtils.getPath(appgroup,app),
        Optional.of(queryModel), fileType.getVfsFileTypes());
  }

  @RequestMapping(method = GET, value = {"/exists/{appgroup}/{app}/{file}","/exists/{appgroup}/{app}"})

  public ResponseEntity isAppExists(
      @PathVariable("appgroup") String appgroup,
      @PathVariable("app") String app,
      @PathVariable(value = "file", required = false) String file) {
    String path = file == null
        ? String .format("%s/%s", appgroup,app)
        : String .format("%s/%s/%s", appgroup,app, file);
    Map<String, Object> resultMap = new ResultMap()
            .add("path", path)
            .add("exists", documentService.isFileAvailable(path))
            .build();
    return ResponseEntity.ok( new ApiResponse.Builder().setData(resultMap).build());
  }
}
