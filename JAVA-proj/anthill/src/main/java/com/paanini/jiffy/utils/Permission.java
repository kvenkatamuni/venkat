package com.paanini.jiffy.utils;

import javax.jcr.security.Privilege;


public enum Permission {
  NONE {
    @Override
    public String[] translate() {
      return new String[]{
              DOCUBE_NONE
      };
    }

    @Override
    public String[] getPermissions() {
      return new String[0];
    }

    @Override
    public Permission[] getAllowedPermissions() {
      return new Permission[] {Permission.CLONE};
    }
  },
  VIEW {
    @Override
    public String[] translate() {
      return new String[]{
              DOCUBE_READ
      };
    }
    @Override
    public String[] getPermissions() {
      return new String[] {
              Privilege.JCR_READ,
              Privilege.JCR_READ_ACCESS_CONTROL
      };
    }

    @Override
    public Permission[] getAllowedPermissions() {
      return new Permission[] {Permission.VIEW};
    }
  },
  EDIT {
    @Override
    public String[] translate() {
      return new String[] {
              DOCUBE_WRITE
      };
    }
    @Override
    public String[] getPermissions() {
      return new String[] {
              Privilege.JCR_MODIFY_ACCESS_CONTROL,
              Privilege.JCR_NODE_TYPE_MANAGEMENT,
              Privilege.JCR_WRITE,
              Permission.DOCUBE_READ_DOWNLOAD
      };
    }

    @Override
    public Permission[] getAllowedPermissions() {
      return new Permission[] {Permission.VIEW, Permission.EDIT, Permission.CLONE};
    }
  },
  CLONE {
    @Override
    public String[] translate() {
      return new String[] {
              DOCUBE_READ_DOWNLOAD
      };
    }
    @Override
    public String[] getPermissions() {
      return new String[] {
              Permission.DOCUBE_READ,
              Permission.DOCUBE_DOWNLOAD
      };
    }

    @Override
    public Permission[] getAllowedPermissions() {
      return new Permission[] {Permission.VIEW, Permission.CLONE};
    }
  },
  SHARE {
    @Override
    public String[] translate() {
      return new String[]{
              DOCUBE_WRITE_SHARE
      };
    }
    @Override
    public String[] getPermissions() {
      return new String[] {
              Permission.DOCUBE_WRITE,
              Permission.DOCUBE_SHARE
      };
    }

    @Override
    public Permission[] getAllowedPermissions() {
      return new Permission[] {Permission.VIEW, Permission.EDIT, Permission.CLONE, Permission.SHARE};
    }
  },
  ALL {
    @Override
    public String[] translate() {
      return new String[]{
              DOCUBE_ALL
      };
    }
    @Override
    public String[] getPermissions() {
      return new String[] {
              Permission.DOCUBE_WRITE_SHARE,
              Privilege.JCR_ALL
      };
    }

    @Override
    public Permission[] getAllowedPermissions() {
      return new Permission[] {Permission.VIEW, Permission.EDIT, Permission.CLONE, Permission.SHARE};
    }
  };

  public static final String DOCUBE_WRITE_SHARE = "docube_Write_share";
  public static final String DOCUBE_READ_DOWNLOAD = "docube_read_download";
  public static final String DOCUBE_NONE = "docube_none";
  public static final String DOCUBE_READ = "docube_read";
  public static final String DOCUBE_WRITE = "docube_write";
  public static final String DOCUBE_SHARE = "docube_share";
  public static final String DOCUBE_DOWNLOAD = "docube_download";
  public static final String DOCUBE_ALL = "docube_all";
  public abstract String[] translate();
  public abstract String[] getPermissions();
  public abstract Permission[] getAllowedPermissions();
}
