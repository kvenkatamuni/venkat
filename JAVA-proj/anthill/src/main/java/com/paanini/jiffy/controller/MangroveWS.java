package com.paanini.jiffy.controller;

import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.services.ContentService;
import com.paanini.jiffy.utils.FileUtils;
import com.paanini.jiffy.vfs.files.Folder;
import com.paanini.jiffy.vfs.files.JiffyTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.paanini.jiffy.services.AppService;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping("/antman")
@Consumes(MediaType.APPLICATION_JSON)
public class MangroveWS {

  @Autowired
  private AppService appService;

  @Autowired
  private ContentService contentService;

  //Get Jiffy Table without schema history by path
  @GetMapping("/{appgroup}/{app}/file/{name}")
  public ResponseEntity getJiffyTableByPath(@PathVariable("appgroup") String appgroup,
                                @PathVariable("app") String app,
                                @PathVariable("name") String name){
    return ResponseEntity.ok(appService.getJiffyTableWithoutHistoryByPath(FileUtils.getPath(appgroup,app),name));
  }

  //Get Jiffy Table without schema history by id
  @GetMapping("/file/{id}")
  public ResponseEntity getJiffyTableById(@PathVariable("id") String id) {
    return ResponseEntity.ok(appService.getJiffyTableWithoutHistoryById(id));
  }

}
