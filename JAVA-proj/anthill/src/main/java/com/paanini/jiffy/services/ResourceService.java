package com.paanini.jiffy.services;

import com.paanini.jiffy.models.JiffyTableAttachment;

import java.io.FileOutputStream;
import java.nio.file.Path;

public interface ResourceService {
  JiffyTableAttachment saveResourceFolder(String appPath, String name, Path path);

  FileOutputStream getFileOutputstream(String appPath, String ref, String fileName);

  JiffyTableAttachment saveResourceFolder(String appPath, String fId, String name);
}
