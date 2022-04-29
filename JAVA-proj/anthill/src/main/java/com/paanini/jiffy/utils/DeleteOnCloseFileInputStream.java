package com.paanini.jiffy.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by appmgr on 3/6/16.
 */
public class DeleteOnCloseFileInputStream extends FileInputStream {

  static Logger logger = LoggerFactory.getLogger(DeleteOnCloseFileInputStream.class);
  private final Path path;

  public DeleteOnCloseFileInputStream(File file, String folder) throws FileNotFoundException {
    super(file);
    this.path= Paths.get(folder);
  }
  public void close() throws IOException {
    logger.debug("Closing and deleting file {}" ,path);
    try {
      super.close();
    } finally {
      FileUtils.deleteDirectory(path);
      logger.debug("Deleted file {}" ,path);
    }
  }
}
