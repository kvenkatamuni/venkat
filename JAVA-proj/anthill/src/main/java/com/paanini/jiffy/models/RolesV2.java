package com.paanini.jiffy.models;

import com.option3.docube.schema.nodes.Type;
import org.bson.Document;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RolesV2 {
    private Map<String,List<String>> permissionMap;
    private String name;
    Boolean customRole;
    Set<String> fileIds;

    public Map<String, List<String>> getPermissionMap() {
        return permissionMap;
    }

    public void setPermissionMap(Map<String, List<String>> permissionMap) {
        this.permissionMap = permissionMap;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getCustomRole() {
        return customRole;
    }

    public void setCustomRole(Boolean customRole) {
        this.customRole = customRole;
    }

    public Set<String> getFileIds() {
        return fileIds;
    }

    public void setFileIds(Set<String> fileIds) {
        this.fileIds = fileIds;
    }


}
