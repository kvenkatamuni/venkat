package com.paanini.jiffy.storage;

import com.paanini.jiffy.utils.TenantHelper;
import org.springframework.stereotype.Service;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

/**
 * Created by rahul on 19/10/15.
 */
public class DocumentStore {

  private final FileSystem fileSystem;
  private final FileMapStrategy mapper;
  private final String sparkFilePath;

  private final String root;

  public DocumentStore(FileMapStrategy fsMap, String root, String sparkFilePath) {
    this(FileSystems.getDefault(), root, sparkFilePath, fsMap);
  }

  public DocumentStore(FileSystem fileSystem, String root, String sparkFilePath, FileMapStrategy mapper) {
    this.fileSystem = fileSystem;
    this.root = root;
    this.sparkFilePath = sparkFilePath;
    this.mapper = mapper;
  }

  public FileSystem getFileSystem() {
    // this is now generated for individual tenants
    return new WrappedFileSystem(this.fileSystem, root, TenantHelper.getTenantId(), this.mapper);
  }

  public String getRoot() {
    return root;
  }

  public String getSparkFilePath() {
    return sparkFilePath;
  }

}