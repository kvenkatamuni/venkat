package com.paanini.jiffy.controller;


import com.paanini.jiffy.dto.JfsFile;
import com.paanini.jiffy.models.JiffyTableAttachment;
import com.paanini.jiffy.services.AppResourceService;
import com.paanini.jiffy.utils.FileUtils;
import com.paanini.jiffy.utils.FileValidationUtils;
import com.paanini.jiffy.utils.ResultMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.ws.rs.Consumes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Athul Krishna N S
 * @since 9/1/20
 */
@RestController
@RequestMapping("/app/resource")
@Consumes(MediaType.APPLICATION_JSON_VALUE)
public class AppResourceWS {

  static Logger logger = LoggerFactory.getLogger(AppResourceWS.class);

  @Autowired
  AppResourceService appResourceService;
  //protected final JiffyTableServiceImpl tableService;

  @Autowired
  FileValidationUtils fileValidationUtils;

  @PostMapping("/{appgroup}/{app}/upload")
  public ResponseEntity uploadFileSet(@RequestParam("file") MultipartFile attachment,
                                      @PathVariable("appgroup") String appgroup,
                                      @PathVariable("app") String app) {
    return ResponseEntity.ok(appResourceService.saveResource(FileUtils.getPath(appgroup,app), attachment));
  }

  @PostMapping("/{appgroup}/{app}/upload/jfs")
  public ResponseEntity uploadFromJfsFileSet(@RequestBody JfsFile file,
                                             @PathVariable("appgroup") String appgroup,
                                             @PathVariable("app") String app) {
    return ResponseEntity.ok(appResourceService.saveResource(FileUtils.getPath(appgroup,app), file));
  }

  @PostMapping("/{appgroup}/{app}/upload/folder/jfs")
  public ResponseEntity uploadFromJfsFolder(@RequestBody JfsFile file,
                                             @PathVariable("appgroup") String appgroup,
                                             @PathVariable("app") String app) {
    return ResponseEntity.ok(appResourceService.saveResourceFolder(FileUtils.getPath(appgroup,app),
            file.getfId(),
            file.getName()));
  }

  @PostMapping("/{appgroup}/{app}/upload/folder/{refId}/{name}")
  public ResponseEntity uploadFromJfsFolder(MultipartFile file,
                                            @PathVariable("appgroup") String appgroup,
                                            @PathVariable("app") String app,
                                            @PathVariable("refId") String refId,
                                            @PathVariable("name") String name) {
    appResourceService.addFileToResource(FileUtils.getPath(appgroup,app), file, refId, name);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{appgroup}/{app}/{ref}/{name}/location")
  public ResponseEntity getResourcePath(@PathVariable("appgroup") String appgroup,
                                        @PathVariable("app") String app,
                                        @PathVariable("ref") String ref,
                                        @PathVariable("name") String name) {
    try {
      logger.debug("[AppResource] request values App: {} ref {} name {}", app, ref, name);
      String path = appResourceService.getResource(FileUtils.getPath(appgroup,app), ref, name).getPath();

      path = path.substring(path.indexOf(appResourceService.getTenantPath()) +
              appResourceService.getTenantPath().length());

      return ResponseEntity.ok(new ResultMap()
                      .add("location", path)
                      .build());
    } catch (IOException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/{appgroup}/{app}/{ref}/{name}")
  public ResponseEntity getResource(@PathVariable("appgroup") String appgroup,
                              @PathVariable("app") String app,
                              @PathVariable("ref") String ref,
                              @PathVariable("name") String name) {
    try {
      logger.debug("[AppResource] request values App: {} ref {} name {}", app, ref, name);
      File file = appResourceService.getResource(FileUtils.getPath(appgroup,app), ref, name);
      String contentType = fileValidationUtils.getFileMediaType(name);
      HttpHeaders headers = new HttpHeaders();
      headers.add("Content-Disposition", "attachment;" + "filename=\"" + name + "\"");
      headers.add("Content-Description", "File Transfer");
      headers.add("content-type",contentType);

      return ResponseEntity.ok()
              .headers(headers)
          .body(new InputStreamResource(new FileInputStream(file)));

    } catch (IOException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping("/{appgroup}/{app}/copyresource")
  public ResponseEntity copyResource(@PathVariable("appgroup") String appgroup,
                                     @PathVariable("app") String app,
                                     @RequestBody JiffyTableAttachment attachment){
    return ResponseEntity.ok(appResourceService.copyResource(FileUtils.getPath(appgroup,app),attachment));
  }

  /*@PostMapping("/table/{appgroup}/{app}/{tableName}/{rowId}/migrate")
  public Response moveResources(@PathVariable("appgroup") String appgroup,
                                @PathVariable("app") String app,
                                @PathParam("tableName") String tableName,
                                @PathParam("rowId") String rowId) {

    String tablePath = FileUtils.getPath(FileUtils.getPath(appgroup,app),tableName);
    JiffyTableRow row = tableService.readOne(tablePath, JiffyTable.SelectType.PATH, rowId).getJiffyTableRow();

    JiffyTableRow migratedrow = appResourceService.migrateOld(tablePath, row);

    tableService.updateMany(tablePath, JiffyTable.SelectType.PATH,
            Arrays.asList(migratedrow), WriteOptions.withoutAtomicity());

    return Response.ok().build();
  }*/

  @PostMapping("/{appgroup}/{app}/upload/{id}")
  public ResponseEntity uploadFileSetForForms(@RequestParam("file") MultipartFile attachment,
                                      @PathVariable("appgroup") String appgroup,
                                      @PathVariable("app") String app,
                                      @PathVariable("id") String id) {
    return ResponseEntity.ok(appResourceService.saveResourceForForms(FileUtils.getPath(appgroup, app), attachment, id));
  }


}
