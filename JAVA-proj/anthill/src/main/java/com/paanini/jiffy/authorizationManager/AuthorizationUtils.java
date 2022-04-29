package com.paanini.jiffy.authorizationManager;

import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.constants.JiffyTable;
import com.paanini.jiffy.constants.Roles;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AuthorizationUtils {
    private AuthorizationUtils(){
        throw new IllegalStateException("Utility class");
    }
    public static List<Type> getAllowedFiles() {
        return Arrays.asList(Type.FILE, Type.COLOR_PALETTE,
                Type.APP_ROLES, Type.JIFFY_TASKS, Type.SECURE_VAULT_ENTRY,
                Type.LICENSE,Type.USERPREFERENCE);
    }

    public static List<String> getAllowedFilesNames(){
        return Arrays.asList(
                "shared-space", "vault");
    }
    public static List<String> getAllowedSuffixes(){
        return Arrays.asList(JiffyTable.ACCURACY_SUFFIX,JiffyTable.PSEUDONYMS_SUFFIX);
    }


    public static List<String> getDefaultRoles(){
        return Arrays.stream(Roles.values()).map(e -> e.name()).collect(Collectors.toList());
    }

    public static List<Type> cacheSkippedFiles(){
        return Arrays.asList(Type.FOLDER,Type.SECURE_VAULT_ENTRY,Type.LICENSE,Type.CONFIGURATION,
                Type.COLOR_PALETTE,Type.USERPREFERENCE);
    }
}
