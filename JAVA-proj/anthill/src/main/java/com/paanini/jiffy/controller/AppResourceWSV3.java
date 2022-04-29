package com.paanini.jiffy.controller;

import com.paanini.jiffy.services.AppResourceService;
import com.paanini.jiffy.utils.FileUtils;
import com.paanini.jiffy.utils.FileValidationUtils;
import com.paanini.jiffy.utils.ResultMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.Consumes;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@RestController
@RequestMapping("/app/resource/v3")
@Consumes(MediaType.APPLICATION_JSON_VALUE)
public class AppResourceWSV3 {

    static Logger logger = LoggerFactory.getLogger(AppResourceWSV2.class);

    @Autowired
    AppResourceService appResourceService;

    @Autowired
    FileValidationUtils fileValidationUtils;

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

    @GetMapping("/{appgroup}/{app}/{ref}/{name}/location")
    public ResponseEntity getResourcePath(@PathVariable("appgroup") String appgroup,
                                          @PathVariable("app") String app,
                                          @PathVariable("ref") String ref,
                                          @PathVariable("name") String name) {
        try {
            logger.debug("[AppResource] request values App: {} ref {} name {}", app, ref, name);
            String path = appResourceService.getResource(FileUtils.getPath(appgroup,app), ref, name).getPath();
            path = path.substring(0, (path.length() - name.length())).concat(name);
            path = path.substring(path.indexOf(appResourceService.getTenantPath()) +
                    appResourceService.getTenantPath().length());

            return ResponseEntity.ok(new ResultMap()
                    .add("location", path)
                    .build());
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
