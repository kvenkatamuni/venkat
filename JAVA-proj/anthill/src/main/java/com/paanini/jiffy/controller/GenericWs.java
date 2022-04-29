package com.paanini.jiffy.controller;


import com.option3.docube.schema.datasheet.meta.Column;
import com.paanini.jiffy.models.ContextDetails;
import com.paanini.jiffy.models.RefernceType;
import com.paanini.jiffy.models.SqlSource;
import com.paanini.jiffy.services.AppService;
import com.paanini.jiffy.services.ContentService;
import com.paanini.jiffy.utils.FileUtils;
import com.paanini.jiffy.utils.StringUtils;
import com.paanini.jiffy.vfs.api.Persistable;
import java.util.List;
import java.util.Objects;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/generic")
@Consumes(MediaType.APPLICATION_JSON)
public class GenericWs {

  @Autowired
  private AppService appService;

  @Autowired
  private ContentService contentService;

  @GetMapping("/{appgroup}/{app}")
  public ResponseEntity getProperty(@PathVariable("appgroup") String appgroup,
                                    @PathVariable("app") String app,
                                    @RequestParam(name = "property") String property){
    return ResponseEntity.ok(appService.getProperty(FileUtils.getPath(appgroup,app),property));
  }

  @DeleteMapping("/{appgroup}/{app}/file/{name}")
  public ResponseEntity deleteFile(@PathVariable("appgroup") String appgroup,
                                   @PathVariable("app") String app,
                                   @PathVariable String name){
    appService.deleteFile(FileUtils.getPath(appgroup,app),name);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{appgroup}/{app}/file/{name}")
  public ResponseEntity getFile(@PathVariable("appgroup") String appgroup,
                                @PathVariable("app") String app,
                                @PathVariable("name") String name){
    return ResponseEntity.ok(appService.getFile(FileUtils.getPath(appgroup,app),name));
  }

  @GetMapping("/{appgroup}/{app}/file/{name}/presentation/{presentationId}")
  public ResponseEntity getReferencedFile(@PathVariable("appgroup") String appgroup,
                                @PathVariable("app") String app,
                                @PathVariable("name") String name,
                                          @PathVariable("presentationId") String presentationId){
    return ResponseEntity.ok(appService.getReferencedFile(FileUtils.getPath(appgroup,app),name,presentationId));
  }

  @PutMapping("/{appgroup}/{app}/{name}")
  public ResponseEntity updateProperty(@PathVariable("appgroup") String appgroup,
                                       @PathVariable("app") String app,
                                       @PathVariable(value = "name") String name,
                                       @RequestParam(name = "property") String property,
                                       @RequestParam(name = "value") Object value){
    appService.updateProperty(FileUtils.getPath(appgroup,app,name),property,value);
    return ResponseEntity.ok().build();
  }

  @PutMapping("/{appgroup}/{app}")
  public ResponseEntity updateAppProperty(@PathVariable("appgroup") String appgroup,
                                       @PathVariable("app") String app,
                                       @RequestParam(name = "property") String property,
                                       @RequestParam(name = "value") Object value){
    appService.updateProperty(FileUtils.getPath(appgroup,app),property,value);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{parentId}/filename/{name}")
  public ResponseEntity getFileFromId(@PathVariable String parentId,@PathVariable String name){
   return  ResponseEntity.ok(contentService.getFileFromParent(parentId,name));
  }

  @GetMapping("/get/file/relative/path")
  public ResponseEntity getFileFromRelativePath(@RequestParam(value = "baseId", required = false) String parentId,
                                                @RequestParam("path") String name){
    String path = StringUtils.getPath(name);
    return  ResponseEntity.ok(contentService.getFileFromRelativePath(parentId, path));
  }


  @PostMapping("/file/upsert/{parentId}")
  public ResponseEntity upsert(@PathVariable String parentId,@RequestBody Persistable file){
    return  ResponseEntity.ok(contentService.upsertFile(file,parentId));
  }

  @GetMapping("/file/{id}")
  public ResponseEntity getFileFromId(@PathVariable String id){
    return  ResponseEntity.ok(contentService.getFile(id));
  }

  @GetMapping("/presentation/{presentationId}/file/{id}")
  public ResponseEntity getReferencedFileFromId(@PathVariable String id
          ,@PathVariable String presentationId){
    return  ResponseEntity.ok(contentService.getReferencedFileFromId(id,presentationId));
  }

  @PostMapping("/markPublish/{id}")
  public ResponseEntity markPublished(@PathVariable String id){
    contentService.markPublished(id);
    return  ResponseEntity.ok().build();
  }

  @DeleteMapping("/file/{id}")
  public ResponseEntity deleteFile(@PathVariable String id){
    contentService.deleteFile(id);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/file/{parentId}")
  public  ResponseEntity createFile(@PathVariable String parentId,@RequestBody Persistable file){
    return ResponseEntity.ok(contentService.createFile(file, parentId));
  }

  @PutMapping("/file")
  public ResponseEntity updateFile(@RequestBody Persistable file){
    return ResponseEntity.ok(contentService.updateFile(file));
  }

  @PostMapping("/saveSqlSource")
  public ResponseEntity saveSqlSource(@RequestBody SqlSource sqlSource){
    return ResponseEntity.ok(contentService.saveSqlSource(sqlSource));
  }

  @PostMapping("/savePublishHeaders/{id}")
  public ResponseEntity savePublishHeaders(@PathVariable String id,
                                           @RequestBody List<Column> header){
    contentService.savePublishHeaders(id,header);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/datapath/{id}")
  public ResponseEntity getDataPath(@PathVariable String id){
    return ResponseEntity.ok(contentService.getDataPath(id));
  }

  @GetMapping("/{appgroup}/{app}/roles")
  public ResponseEntity getRoles(@PathVariable("appgroup") String appgroup,
                                 @PathVariable("app") String app){
    return ResponseEntity.ok(appService.getAssignedRolesV2(FileUtils.getPath(appgroup,app)));
  }

  @GetMapping("/{appgroup}/{app}/folderdata")
  public ResponseEntity getFolderData(@PathVariable("appgroup") String appgroup,
                                 @PathVariable("app") String app){
    return ResponseEntity.ok(appService.getFolderData(FileUtils.getPath(appgroup,app)));
  }

  @GetMapping("/{appgroup}/{app}/exists")
  public ResponseEntity isAvailable(@PathVariable("appgroup") String appgroup,
                                      @PathVariable("app") String app,
                                    @RequestParam(name = "fileName",required = false) String file){
    String path = FileUtils.getPath(appgroup,app);
    if(!Objects.isNull(file)){
      path = FileUtils.getPath(path,file);
    }
    return ResponseEntity.ok(appService.isAvailable(path));
  }

  @PostMapping("/{appgroup}/{app}/autopopulate/getJiffyTable")
  public ResponseEntity getJiffyTableForAutoPopulate(@PathVariable("appgroup") String appgroup,
                                                     @PathVariable("app") String app,
                                                     @RequestBody ContextDetails contextDetails){
    String path = FileUtils.getPath(appgroup,app);
    return ResponseEntity.ok(contentService.getJT(path,contextDetails));
  }

  @GetMapping("/validateAccess")
  public ResponseEntity validateAccess(@RequestParam(name = "entityRef") String entityRef,
                                       @RequestParam(name = "refType") RefernceType refType,
                                       @RequestParam(name = "type") String type,
                                       @RequestParam(name = "permission") String permission){
    return ResponseEntity.ok(appService.hasPermissionForFiles(entityRef,refType,type,permission.toLowerCase()));
  }

  @GetMapping("/cacheEvict")
  public ResponseEntity evictCache(){
    appService.evictCache();
    return ResponseEntity.ok().build();
  }

}
