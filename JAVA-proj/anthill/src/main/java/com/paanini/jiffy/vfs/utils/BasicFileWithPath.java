package com.paanini.jiffy.vfs.utils;

import com.paanini.jiffy.vfs.files.BasicFileView;

import java.util.Optional;

public class BasicFileWithPath {
  private final BasicFileView file;
  private final Optional<String> path;

  public BasicFileWithPath(BasicFileView file, String path) {
    this(file, (path == null) ? Optional.empty() : Optional.of(path));
  }

  public BasicFileWithPath(BasicFileView file, Optional<String> path) {
    this.file = file;
    this.path = path;
  }

  public BasicFileView getFile() {
    return file;
  }

  public Optional<String> getPath() {
    return path;
  }
}
