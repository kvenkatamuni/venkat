package com.paanini.jiffy.controller;

import com.paanini.jiffy.dto.ResourceCleanUpDTO;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.helper.AttachmentFile;
import com.paanini.jiffy.models.ApiResponse;
import com.paanini.jiffy.services.AppResourceService;
import com.paanini.jiffy.utils.FileUtils;
import com.paanini.jiffy.utils.FileValidationUtils;
import com.paanini.jiffy.utils.MessageCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.ws.rs.Consumes;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/app/resource/v2")
@Consumes(MediaType.APPLICATION_JSON_VALUE)
public class AppResourceWSV2 {

    static Logger logger = LoggerFactory.getLogger(AppResourceWSV2.class);

    @Autowired
    AppResourceService appResourceService;

    @Autowired
    FileValidationUtils fileValidationUtils;

    @PostMapping("/{appgroup}/{app}/upload")
    public ResponseEntity uploadFileSet(@RequestParam("file") MultipartFile attachment,
                                        @PathVariable("appgroup") String appgroup, @PathVariable("app") String app) {
        try{
            AttachmentFile attachmentFile = appResourceService.saveResource(FileUtils.getPath(appgroup,app), attachment);
            return ResponseEntity.ok(new ApiResponse
                    .Builder()
                    .setData(attachmentFile)
                    .build());
        }catch (Exception e){
            logger.error("Failed to upload attachment {} {}",e.getMessage(), e);
            return ResponseEntity.ok(new ApiResponse
                    .Builder()
                    .addError(MessageCode.ANTHILL_ERR_FILE_UPLOAD)
                    .build());
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

        } catch (Exception e){
            logger.error("Failed to download attachment {} {}",e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{appgroup}/{app}/upload/{id}")
    public ResponseEntity uploadFileSetForForms(@RequestParam("file") MultipartFile attachment,
                                                @PathVariable("appgroup") String appgroup,
                                                @PathVariable("app") String app,
                                                @PathVariable("id") String id) {
        try{
            logger.debug("[AppResource] uploading the file for table {}", id);
            AttachmentFile attachmentFile = appResourceService.saveResourceForForms(FileUtils.getPath(appgroup, app),
                    attachment, id);
            return ResponseEntity.ok(new ApiResponse
                    .Builder()
                    .setData(attachmentFile)
                    .build());
        }catch (ProcessingException e){
            logger.error("Failed to upload attachment for table {} {} {}",id ,e.getMessage(), e);
            return ResponseEntity.ok(new ApiResponse
                    .Builder()
                    .addError("ANTHILL_ERR_FILE_UPLOAD", e.getMessage())
                    .build());
        }catch (Exception e){
            logger.error("Failed to upload attachment for table {} {} {}",id ,e.getMessage(), e);
            return ResponseEntity.ok(new ApiResponse
                    .Builder()
                    .addError(MessageCode.ANTHILL_ERR_FILE_UPLOAD)
                    .build());
        }

    }

    @PostMapping("/{appgroup}/{app}/delete")
    public ResponseEntity deleteResource(@RequestBody List<ResourceCleanUpDTO> resourceCleanUpDTOs,
                                        @PathVariable("appgroup") String appgroup, @PathVariable("app") String app) {
        try{
            logger.debug("[AppResource] Deleting the resource in app, under appGroup {}} {}", app, appgroup);
            Map<Object, Object> resourceCleanUpMap = appResourceService.deleteResources
                    (FileUtils.getPath(appgroup,app), resourceCleanUpDTOs);
            return ResponseEntity.ok(new ApiResponse
                    .Builder()
                    .setData(resourceCleanUpMap)
                    .build());
        }catch (Exception e){
            logger.error("Failed to upload attachment {} {}",e.getMessage(), e);
            return ResponseEntity.ok(new ApiResponse
                    .Builder()
                    .addError(MessageCode.ANTHILL_ERR_FILE_DELETE)
                    .build());
        }
    }

    @PostMapping("/{appgroup}/{app}/duplicate")
    public ResponseEntity duplicateResource(@PathVariable("appgroup") String appgroup,
                                            @PathVariable("app") String app,
                                            @RequestBody List<Map<String, Object>> attachments) {
        try{
            logger.debug("[AppResource] duplicating the resource in app, under appGroup {} {}", app, appgroup);
            String appPath = FileUtils.getPath(appgroup,app);
            return ResponseEntity.ok(new ApiResponse
                    .Builder()
                    .setData(appResourceService.duplicateResources(appPath, attachments))
                    .build());
        }catch (Exception e){
            logger.error("Failed to duplicate resource {} {}",e.getMessage(), e);
            return ResponseEntity.ok(new ApiResponse
                    .Builder()
                    .addError(MessageCode.ANTHILL_ERR_FILE_DUPLICATE)
                    .build());
        }
    }
}
