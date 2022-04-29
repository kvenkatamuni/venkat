package com.paanini.jiffy.dto;

/**
 * @author Athul Krishna N S
 * @since 02/11/20
 */
public class ImportDTO {
    protected String fileId;
    protected boolean newApp;
    protected String appName;
    protected boolean isFullImport;

    public boolean isFullImport() {
        return isFullImport;
    }

    public void setFullImport(boolean fullImport) {
        isFullImport = fullImport;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public boolean getNewApp() {
        return newApp;
    }

    public void setNewApp(boolean aNew) {
        newApp = aNew;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }
}
