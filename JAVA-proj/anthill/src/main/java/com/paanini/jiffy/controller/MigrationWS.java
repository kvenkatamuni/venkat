package com.paanini.jiffy.controller;

import com.paanini.jiffy.services.AppService;
import com.paanini.jiffy.services.MigrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@RestController
@RequestMapping("/migration")
@Consumes(MediaType.APPLICATION_JSON)
public class MigrationWS {

    static Logger logger = LoggerFactory.getLogger(MigrationWS.class);

    @Autowired
    MigrationService migrationService;

    @Autowired
    AppService appService;

    @GetMapping("/permissionMigration")
    public ResponseEntity migratePermissions(){
        migrationService.migratePermissions();
        return ResponseEntity.ok().build();
    }


    @GetMapping("/migrateAutoPopulate")
    public ResponseEntity migrateAutoPopulate(){
        migrationService.migrateAutoPopulate();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tenantIds")
    public ResponseEntity getTenantIds(){
        return ResponseEntity.ok(migrationService.getTenantIds());
    }

    @GetMapping("/migrateCustomRoles")
    public ResponseEntity migrateCustomRoles(){
        migrationService.migrateCustomRoles();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/migrateServiceUsers")
    public ResponseEntity migrateServiceUsers(){
        migrationService.migrateServiceUsers();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/getPermittedIds/{tenantId}/{appId}")
    public ResponseEntity getPermittedIds(@PathVariable("appId") String appId,
                                          @PathVariable("tenantId") String tenantId){
        return ResponseEntity.ok(migrationService.getPermittedIds(appId,tenantId));
    }

    @GetMapping("/migrateTableDependency")
    public ResponseEntity migrateTableDependency(){
        migrationService.migrateTableDependency();
        return ResponseEntity.ok().build();
    }

    /*
     * This API should be used to upgrade schemas of all JiffyTable.
     * If InnerTable/NestedStructure in JiffyTable do not have id,
     * this API will add id to them and update jiffyTables
     * This API should be used only during upgrade
     */

    @GetMapping("/migrateJiffyTableSchemas")
    public ResponseEntity migrateJiffyTableSchemas(){
        Map<String, String> migrationSummary = migrationService.updateJiffyTableSchemas();
        logger.info("Migrated file details : : {}", migrationSummary);
        logger.info("Total Files migrated : : {}", migrationSummary.size());
        appService.evictCache();
        return ResponseEntity.ok().build();
    }
}
