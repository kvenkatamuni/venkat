package com.paanini.jiffy.utils;


import com.google.common.collect.Sets;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.constants.App;
import com.paanini.jiffy.constants.Authenication;
import com.paanini.jiffy.constants.Common;
import com.paanini.jiffy.constants.Roles;
import com.paanini.jiffy.models.RolesV2;
import org.bson.Document;

import java.util.*;
import java.util.stream.Collectors;

import static com.option3.docube.schema.nodes.Type.*;

public class RoleServiceUtilsV2 {

    private static final String WRITE = "write";
    private static final String EXECUTE = "execute";
    private static final String IMPORT = "import";
    private static final String EXPORT = "export";

    private RoleServiceUtilsV2(List<String> fileTypes){
    }
    public static List<Document> getDefaultAppRoles() {
        return Arrays.asList(getDesignerRole(),
                getSupportRole(), getBusinessUserRole(),
                getReleaseAdminRole(),getCustomUserRoleAcl());
    }

    public static RolesV2 getDesignerApprole(){
        return RoleServiceUtilsV2.buildRoles(getDesignerRole());
    }

    public static Document getDesignerRole(){
        return new Document("key",Roles.DESIGNER.name()).append(Common.VALUE,getDesignerRolePermissions());
    }

    public static Document getSupportRole(){
        return new Document("key",Roles.SUPPORT.name()).append(Common.VALUE,getSupportRolePermission());
    }

    public static Document getBusinessUserRole(){
        return new Document("key",Roles.BUSINESS_USER.name()).append(Common.VALUE,getBusinessUserRolePermissions());
    }

    public static Document getReleaseAdminRole(){
        return new Document("key",Roles.RELEASE_ADMIN.name()).append(Common.VALUE,getReleaseAdminRolePermissions());
    }

    public static Document getCustomUserRoleAcl(){
        return new Document("key", App.CUSTOM_ROLES).append(Common.VALUE,getCustomUserRolePermissions());
    }

    public static Document getCustomUserRole(String name,List<String> identifiers){
        return new Document("key",name).append(Common.VALUE,getCustomUserRolePermissions()).append(Authenication.FILE_IDS,identifiers);
    }

    private static  Document getDesignerRolePermissions(){
        Document doc = new Document();
        doc.append(PRESENTATION.name(),new Document(Authenication.PERMISSIONS,Arrays.asList("read", WRITE, EXECUTE, IMPORT, EXPORT)))
            .append(JIFFY_TABLE.name(),new Document(Authenication.PERMISSIONS,Arrays.asList("read", WRITE, EXECUTE, IMPORT, EXPORT)))
            .append(JIFFY_TASKS.name(),new Document(Authenication.PERMISSIONS,Arrays.asList("read", WRITE, EXECUTE, IMPORT, EXPORT)))
            .append(DATASHEET.name(),new Document(Authenication.PERMISSIONS,Arrays.asList("read", WRITE, EXECUTE, IMPORT, EXPORT)))
            .append(SQL_DATASHEET.name(),new Document(Authenication.PERMISSIONS,Arrays.asList("read", WRITE, EXECUTE, IMPORT, EXPORT)))
            .append(SECURE_VAULT_ENTRY.name(),new Document(Authenication.PERMISSIONS,Arrays.asList("read", WRITE, EXECUTE, IMPORT, EXPORT)))
            .append(CONFIGURATION.name(),new Document(Authenication.PERMISSIONS,Arrays.asList("read", WRITE, EXECUTE, IMPORT, EXPORT)))
            .append(FILESET.name(),new Document(Authenication.PERMISSIONS,Arrays.asList("read", WRITE, EXECUTE, IMPORT, EXPORT)))
            .append(BOT_MANAGEMENT.name(),new Document(Authenication.PERMISSIONS,Arrays.asList("read", WRITE, EXECUTE, IMPORT, EXPORT)))
            .append(getFoldeKey(PRESENTATION.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
            .append(getFoldeKey(JIFFY_TABLE.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
            .append(getFoldeKey(JIFFY_TASKS.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
            .append(getFoldeKey(DATASHEET.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
            .append(getFoldeKey(SQL_DATASHEET.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
            .append(getFoldeKey(SECURE_VAULT_ENTRY.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
            .append(getFoldeKey(CONFIGURATION.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
            .append(getFoldeKey(BOT_MANAGEMENT.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
            .append(getFoldeKey(FILESET.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")));

        return doc;
    }

    private static Document getSupportRolePermission(){
        Document document = new Document();
        document.append(BOT_MANAGEMENT.name(),new Document(Authenication.PERMISSIONS,Arrays.asList("read", WRITE, EXECUTE)))
                .append(getFoldeKey(PRESENTATION.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
                .append(getFoldeKey(JIFFY_TABLE.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
                .append(getFoldeKey(JIFFY_TASKS.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
                .append(getFoldeKey(DATASHEET.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
                .append(getFoldeKey(SQL_DATASHEET.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
                .append(getFoldeKey(SECURE_VAULT_ENTRY.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
                .append(getFoldeKey(CONFIGURATION.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
                .append(getFoldeKey(FILESET.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
                .append(getFoldeKey(BOT_MANAGEMENT.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")));

        return document;
    }

    private static Document getBusinessUserRolePermissions(){
        Document document = new Document();
        document.append(PRESENTATION.name(),new Document(Authenication.PERMISSIONS,Arrays.asList("read", EXECUTE)))
                .append(JIFFY_TABLE.name(),new Document(Authenication.PERMISSIONS,Arrays.asList(EXECUTE)))
                .append(JIFFY_TASKS.name(),new Document(Authenication.PERMISSIONS,Arrays.asList(EXECUTE)))
                .append(DATASHEET.name(),new Document(Authenication.PERMISSIONS,Arrays.asList(EXECUTE)))
                .append(SQL_DATASHEET.name(),new Document(Authenication.PERMISSIONS,Arrays.asList(EXECUTE)))
                .append(SECURE_VAULT_ENTRY.name(),new Document(Authenication.PERMISSIONS,Arrays.asList("read", WRITE, EXECUTE)))
                .append(CONFIGURATION.name(),new Document(Authenication.PERMISSIONS,Arrays.asList(EXECUTE)))
                .append(FILESET.name(),new Document(Authenication.PERMISSIONS,Arrays.asList(EXECUTE)))
                .append(BOT_MANAGEMENT.name(),new Document(Authenication.PERMISSIONS,Arrays.asList(EXECUTE)))
                .append(getFoldeKey(SECURE_VAULT_ENTRY.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")));
        return document;
    }

    private static Document getCustomUserRolePermissions(){
        Document document = new Document();
        document.append(PRESENTATION.name(),new Document(Authenication.PERMISSIONS,Arrays.asList("read", EXECUTE)))
                .append(JIFFY_TABLE.name(),new Document(Authenication.PERMISSIONS,Arrays.asList(EXECUTE)))
                .append(JIFFY_TASKS.name(),new Document(Authenication.PERMISSIONS,Arrays.asList(EXECUTE)))
                .append(DATASHEET.name(),new Document(Authenication.PERMISSIONS,Arrays.asList(EXECUTE)))
                .append(SQL_DATASHEET.name(),new Document(Authenication.PERMISSIONS,Arrays.asList(EXECUTE)))
                .append(CONFIGURATION.name(),new Document(Authenication.PERMISSIONS,Arrays.asList(EXECUTE)))
                .append(FILESET.name(),new Document(Authenication.PERMISSIONS,Arrays.asList(EXECUTE)))
                .append(BOT_MANAGEMENT.name(),new Document(Authenication.PERMISSIONS,Arrays.asList(EXECUTE)));
        return document;
    }

    private static Document getReleaseAdminRolePermissions(){
        Document document = new Document();


        document.append(PRESENTATION.name(),new Document(Authenication.PERMISSIONS,Arrays.asList(IMPORT, EXPORT)))
                .append(JIFFY_TABLE.name(),new Document(Authenication.PERMISSIONS,Arrays.asList(IMPORT, EXPORT)))
                .append(DATASHEET.name(),new Document(Authenication.PERMISSIONS,Arrays.asList(IMPORT, EXPORT)))
                .append(SQL_DATASHEET.name(),new Document(Authenication.PERMISSIONS,Arrays.asList(IMPORT, EXPORT)))
                .append(FILESET.name(),new Document(Authenication.PERMISSIONS,Arrays.asList(IMPORT, EXPORT)))
                .append(SECURE_VAULT_ENTRY.name(),new Document(Authenication.PERMISSIONS,Arrays.asList("read", WRITE, EXECUTE, IMPORT, EXPORT)))
                .append(CONFIGURATION.name(),new Document(Authenication.PERMISSIONS,Arrays.asList("read", WRITE, EXECUTE, IMPORT, EXPORT)))
                .append(BOT_MANAGEMENT.name(),new Document(Authenication.PERMISSIONS,Arrays.asList("read", WRITE, EXECUTE, IMPORT, EXPORT)))
                .append(JIFFY_TASKS.name(),new Document(Authenication.PERMISSIONS,Arrays.asList("read", WRITE, IMPORT, EXPORT)))
                .append(getFoldeKey(PRESENTATION.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
                .append(getFoldeKey(JIFFY_TABLE.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
                .append(getFoldeKey(JIFFY_TASKS.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
                .append(getFoldeKey(DATASHEET.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
                .append(getFoldeKey(SQL_DATASHEET.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
                .append(getFoldeKey(SECURE_VAULT_ENTRY.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
                .append(getFoldeKey(CONFIGURATION.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
                .append(getFoldeKey(FILESET.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")))
                .append(getFoldeKey(BOT_MANAGEMENT.name()),new Document(Authenication.PERMISSIONS,Arrays.asList("read")));
        return document;
    }

    public static String getFoldeKey(String type){
        return new StringBuilder(type).append("_").append("FOLDER").toString();
    }

    public static RolesV2 buildRoles(Document document){
        RolesV2 rolesV2 = new RolesV2();
        rolesV2.setName(document.getString(Authenication.AUTH_TABLE_KEY));
        Map<String, List<String>> permissionMap = new HashMap<>();
        Document document1 = (Document) document.get(Common.VALUE);
        Set<String> keySet = document1.keySet();
        keySet.forEach(key -> {
            Object perms = ((Document) document1.get(key)).get(Authenication.PERMISSIONS);
            permissionMap.put(key, (List<String>)perms);
        });
        rolesV2.setPermissionMap(permissionMap);
        rolesV2.setCustomRole(false);
        List<String> o = (List<String>)document.get(Authenication.FILE_IDS);
        if(Objects.nonNull(o)){
            rolesV2.setFileIds((new HashSet<String>(o)));
            rolesV2.setCustomRole(true);
        }
        return rolesV2;
    }

    public static RolesV2 buildCustomRoles(Document document,Document acl){
        RolesV2 rolesV2 = new RolesV2();
        rolesV2.setName(document.getString(Authenication.AUTH_TABLE_KEY));
        Map<String, List<String>> permissionMap = new HashMap<>();
        Document document1 = (Document) acl.get(Common.VALUE);
        Set<String> keySet = document1.keySet();
        keySet.forEach(key -> {
            Object perms = ((Document) document1.get(key)).get(Authenication.PERMISSIONS);
            permissionMap.put(key, (List<String>)perms);
        });
        rolesV2.setPermissionMap(permissionMap);
        rolesV2.setCustomRole(false);
        List<String> o = (List<String>)document.get(Authenication.FILE_IDS);
        if(Objects.nonNull(o)){
            rolesV2.setFileIds((new HashSet<String>(o)));
            rolesV2.setCustomRole(true);
        }
        return rolesV2;
    }

    public static boolean isDefaultRole(String role) {
        return getDefaultAppRoles().stream()
                .anyMatch(roleV2 -> roleV2.get(Authenication.AUTH_TABLE_KEY).toString().equalsIgnoreCase(role));
    }

    public static boolean hasRole(List<RolesV2> rolesV2s,String role){
        return rolesV2s.stream().anyMatch(rolesV2 -> rolesV2.getName().equals(role));
    }

    public static boolean hasDefaultRoles(Set<String> roles){
        Set<String> defaultRoles = getDefaultAppRoles()
                .stream().map(role -> role.get(Authenication.AUTH_TABLE_KEY).toString())
                .collect(Collectors.toSet());
        Sets.SetView<String> intersection = Sets.intersection(roles, defaultRoles);
        return !intersection.isEmpty();
    }
}
