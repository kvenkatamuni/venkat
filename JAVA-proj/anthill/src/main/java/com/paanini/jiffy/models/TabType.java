package com.paanini.jiffy.models;

import com.option3.docube.schema.nodes.Type;

import java.util.Arrays;
import java.util.List;

/**
 * @author Athul Krishna N S
 * @since 02/11/20
 */
public enum TabType {
  //    importZip,
  tasks {
    @Override
    public String getDisplayName() {
      return this.name();
    }
  },

  templates {
    @Override
    public String getDisplayName() {
      return this.name();
    }
  },
  ui {
    @Override
    public String getDisplayName() {
      return "UI control(s)";
    }
  },
  uicomponents {
    @Override
    public String getDisplayName() {
      return "UI Component(s)";
    }
  },
  excelMacros {
    @Override
    public String getDisplayName() {
      return "Excel Macro(s)";
    }
  },
  functions {
    @Override
    public String getDisplayName() {
      return functions.name();
    }
  },
  vault {
    @Override
    public String getDisplayName() {
      return vault.name();
    }
  },
  presentation {
    @Override
    public String getDisplayName() {
      return presentation.name();
  }
  },
  datasets {
    @Override
    public String getDisplayName() {
      return datasets.name();
    }
  },
  userRoles {
    @Override
    public String getDisplayName() {
      return userRoles.name();
    }
  },
  model {
    @Override
    public String getDisplayName() {
      return model.name();
    }
  },
  filesets {
    @Override
    public String getDisplayName() {
      return filesets.name();
    }
  },
  configurations {
    @Override
    public String getDisplayName() {
      return configurations.name();
    }
  },
  cluster {
    @Override
    public String getDisplayName() {
      return cluster.name();
  }
  },
  flows {
    @Override
    public String getDisplayName() {
      return "Flow (s)";
    }
  };


  public static TabType from(Type type) {
    switch (type) {
      case APP_ROLES:
        return TabType.userRoles;
      case CONFIGURATION:
      case DATASHEET:
      case SQL_APPENDABLE_DATASHEET:
      case SQL_DATASHEET:
      case JIFFY_TABLE:
      case KUDU_DATASHEET:
        return TabType.datasets;
      case FILESET:
        return TabType.filesets;
      case NOTEBOOK:
      case SPARK_MODEL_FILE:
      case CUSTOM_FILE:
        return TabType.model;
      case PRESENTATION:
        return TabType.presentation;
      case SECURE_VAULT_ENTRY:
        return TabType.vault;
      case OS_FILE:
      case BOT_MANAGEMENT:
      case COLOR_PALETTE:
      case DATASHEET_RESTRICTION:
      case FILE:
      case FOLDER:
      case JIFFY_TASKS:
      case KEY_RACK:
      case LICENSE:
      case ALL:
      default:
        return null;
    }
  }

  public static List<TabType> getDocubeTabs() {
    return Arrays.asList(
      TabType.userRoles, TabType.datasets, TabType.filesets,  TabType.model,
      TabType.presentation, TabType.vault
    );
  }

  public abstract String getDisplayName();
}
