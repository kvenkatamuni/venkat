package com.paanini.jiffy.utils;

import com.option3.docube.schema.approles.*;
import com.option3.docube.schema.approles.Permission;
import com.paanini.jiffy.vfs.files.AppRoles;

import java.util.*;
import java.util.stream.Collectors;

import static com.option3.docube.schema.approles.Permission.*;
import static com.option3.docube.schema.approles.Permission.REFERENCE_WRITE;
import static com.option3.docube.schema.nodes.Type.*;
import static com.option3.docube.schema.nodes.Type.JIFFY_TABLE;
import static com.paanini.jiffy.constants.Roles.*;

public class RoleServiceUtils {

  private RoleServiceUtils() {
    // do nothing
  }

  /*public static List<Role> getDefaultAppRoles() {
    return Arrays.asList(getDesignerRole(), getSupportRole(), getBusinessUserRole(), getReleaseAdminRole());
  }

  public static Role getDesignerRole() {
    return new Role(DESIGNER.name(), getAllFileTypesPermission(Permission.WRITE), Collections.emptyList(),
            Arrays.asList(new Configuration(MODIFY_USERS, true), new Configuration(IMPORT_EXPORT, true)));
  }

  private static Role getReleaseAdminRole() {
    return new Role(RELEASE_ADMIN.name(), getAllFileTypesPermission(Permission.LIST), Collections.emptyList(),
            Arrays.asList(new Configuration(MODIFY_USERS, true), new Configuration(IMPORT_EXPORT, true)));
  }

  private static List<FileTypePermission> getAllFileTypesPermission(Permission permission) {
    List<FileTypePermission> permissions = new ArrayList<>();
    permissions.addAll(getAllFileTypePermissions(permission));
    permissions.addAll(Arrays
            .asList(new FileTypePermission(SECURE_VAULT_ENTRY, WRITE),
                    new FileTypePermission(CONFIGURATION, WRITE),
                    new FileTypePermission(BOT_MANAGEMENT, WRITE)));
    return permissions;
  }

  private static Role getSupportRole() {
    List<FileTypePermission> permissions = new ArrayList<>();
    permissions.addAll(getAllFileTypePermissions(Permission.LIST));
    permissions.add(new FileTypePermission(BOT_MANAGEMENT, WRITE));
    permissions.add(new FileTypePermission(CONFIGURATION, LIST));
    return new Role(SUPPORT.name(), permissions, Collections.emptyList(),
            Arrays.asList(new Configuration(MODIFY_USERS, true), new Configuration(IMPORT_EXPORT, false)));
  }

  private static List<FileTypePermission> getAllFileTypePermissions(Permission permission) {
    return Arrays.asList(new FileTypePermission(DATASHEET, permission), new FileTypePermission(JIFFY_TABLE, permission),
            new FileTypePermission(PRESENTATION, permission), new FileTypePermission(SQL_APPENDABLE_DATASHEET, permission),
            new FileTypePermission(SQL_APPENDABLE_DATASHEET, permission), new FileTypePermission(KUDU_DATASHEET, permission),
            new FileTypePermission(CUSTOM_FILE, permission), new FileTypePermission(SQL_DATASHEET, permission),
            new FileTypePermission(COLOR_PALETTE, permission), new FileTypePermission(DATASHEET_RESTRICTION, permission),
            new FileTypePermission(SPARK_MODEL_FILE, permission), new FileTypePermission(NOTEBOOK, permission),
            new FileTypePermission(KEY_RACK, permission), new FileTypePermission(OS_FILE, permission),
            new FileTypePermission(FILESET, permission),
            new FileTypePermission(JIFFY_TASKS, permission));
  }

  public static Role getBusinessUserRole() {
    return new Role(BUSINESS_USER.name(), Arrays.asList(
            new FileTypePermission(DATASHEET, REFERENCE_WRITE),
            new FileTypePermission(JIFFY_TABLE, REFERENCE_WRITE),
            new FileTypePermission(PRESENTATION, READ),
            new FileTypePermission(SECURE_VAULT_ENTRY, WRITE)),
            Collections.emptyList(),
            Arrays.asList(new Configuration(IMPORT_EXPORT, false)));
  }

  public static Role getPresentationCustomRole(String name, List<String> identifiers) {
    List<FileIdentifierPermission> ids = identifiers.stream().map(s -> new FileIdentifierPermission(s, READ))
            .collect(Collectors.toList());
    return new Role(name, Arrays.asList(new FileTypePermission(DATASHEET, REFERENCE_WRITE),
            new FileTypePermission(JIFFY_TABLE, REFERENCE_WRITE), new FileTypePermission(PRESENTATION, READ)), ids,
            Arrays.asList(new Configuration(IMPORT_EXPORT, false)));
  }

  public static Role decorateCustomRole(Role role) {
    if(Objects.isNull(role.getFilesIdentifiers())) {
      role.setFilesIdentifiers(Collections.emptyList());
    }

    if(Objects.isNull(role.getFilesTypes())) {
      role.setFilesIdentifiers(Collections.emptyList());
    } else {
      boolean hasPresentation = role.getFilesTypes().stream()
              .anyMatch(fileTypePermission -> fileTypePermission.equals(PRESENTATION));
      if(hasPresentation) { // adding derived roles
        if(!role.getFilesTypes().contains(DATASHEET)) {
          role.getFilesTypes().add(new FileTypePermission(DATASHEET, REFERENCE_WRITE));
        }
        if(!role.getFilesTypes().contains(JIFFY_TABLE)) {
          role.getFilesTypes().add(new FileTypePermission(JIFFY_TABLE, REFERENCE_WRITE));
        }
      }
    }
    role.setConfigurations(getDefaultConfigurations(role.getConfigurations(), getDefaultConfigMap()));
    return role;
  }

  private static Map<String, Boolean> getDefaultConfigMap() {
    Map<String, Boolean> datamap = new HashMap<>();
    datamap.put(IMPORT_EXPORT, false);
    datamap.put(CUSTOM_ROLE, true);
    return datamap;
  }

  private static List<Configuration> getDefaultConfigurations(List<Configuration> existing,
                                                              Map<String, Boolean> config) {
    List<Configuration> configurations = new ArrayList<>(existing);
    config.entrySet().forEach(e -> configurations.add(new Configuration(e.getKey(), e.getValue().booleanValue())));
    return configurations;
  }

  public static boolean isExisting(Role role, AppRoles roles) {
    return isExisting(role, roles.getRoles());
  }

  public static boolean isExisting(Role role, List<Role> roles) {
    return roles.stream().anyMatch(role1 -> role1.getName().equalsIgnoreCase(role.getName()));
  }

  public static boolean isDefaultRole(String role) {
    return getDefaultAppRoles().stream().anyMatch(role1 -> role1.getName().equalsIgnoreCase(role));
  }

  public static boolean hasDesignerRole(List<Role> roles) {
    return roles.stream().anyMatch(role1 -> role1.getName().equalsIgnoreCase(DESIGNER.name()));
  }

  public static boolean hasReleaseAdmin(List<Role> roles) {
    return roles.stream().anyMatch(role1 -> role1.getName().equalsIgnoreCase(RELEASE_ADMIN.name()));
  }

  public static boolean hasSupport(List<Role> roles) {
    return roles.stream().anyMatch(role1 -> role1.getName().equalsIgnoreCase(SUPPORT.name()));
  }

  public static boolean hasDefaultRole(List<Role> roles) {
    return roles.stream().anyMatch(role1 -> isDefaultRole(role1.getName()));
  }*/
}
