package com.paanini.jiffy.utils;

import com.option3.docube.schema.nodes.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum FileType {
  PRESENTATIONS {
    @Override
    public List<Type> getVfsFileTypes(){
      return Arrays.asList(Type.PRESENTATION);
    }
  },
  FILESETS{
    @Override
    public List<Type> getVfsFileTypes(){
      return Arrays.asList(Type.FILESET);
    }
  },
  DATASETS{
    @Override
    public List<Type> getVfsFileTypes(){
      List<Type> fileTypes = new ArrayList<>();
      fileTypes.add(Type.DATASHEET);
      fileTypes.add(Type.SQL_DATASHEET);
      fileTypes.add(Type.SQL_APPENDABLE_DATASHEET);
      fileTypes.add(Type.KUDU_DATASHEET);
      fileTypes.add(Type.DATASHEET_RESTRICTION);
      fileTypes.add(Type.JIFFY_TABLE);
      fileTypes.add(Type.CONFIGURATION);
      return fileTypes;
    }
  },
  MODELS{
    @Override
    public List<Type> getVfsFileTypes(){
      List<Type> fileTypes = new ArrayList<>();
      fileTypes.add(Type.NOTEBOOK);
      fileTypes.add(Type.SPARK_MODEL_FILE);
      fileTypes.add(Type.CUSTOM_FILE);
      return fileTypes;
    }
  },
  SECUREVAULT{
    @Override
    public List<Type> getVfsFileTypes(){
      List<Type> fileTypes = new ArrayList<>();
      fileTypes.add(Type.SECURE_VAULT_ENTRY);
      return fileTypes;
    }
  },
  ALL{
    @Override
    public List<Type> getVfsFileTypes(){
      List<Type> fileTypes = new ArrayList<>();
      fileTypes.addAll(MODELS.getVfsFileTypes());
      fileTypes.addAll(DATASETS.getVfsFileTypes());
      fileTypes.addAll(PRESENTATIONS.getVfsFileTypes());
      fileTypes.addAll(FILESETS.getVfsFileTypes());
      return fileTypes;
    }
  };

  public abstract List<Type> getVfsFileTypes();
}
