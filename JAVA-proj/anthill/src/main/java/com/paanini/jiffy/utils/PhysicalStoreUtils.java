package com.paanini.jiffy.utils;

import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.storage.DocumentStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * @author Priyanka Bhoir
 * @since 12/8/19
 */
public class PhysicalStoreUtils {
  private static Logger logger = LoggerFactory.getLogger(PhysicalStoreUtils.class);


  public static void deleteContent(DocumentStore documentStore,String id)
          throws ProcessingException {
    java.nio.file.Path filePath = documentStore
            .getFileSystem()
            .getPath(id);
    logger.debug("Deleting physical content of file {} at {}", id,
            filePath.toString());
    try {
      FileUtils.deleteDirectory(filePath);
    } catch (IOException e) {
      throw new ProcessingException(e.getMessage(), e);
    }
  }

  public static void moveOrphanedFolder(DocumentStore store, Map<String, String>
          map)
          throws IOException {
    String path = store.getRoot().concat("/0/");
    File file = new File(path);
    File[] files = file.listFiles();

    String backUpPath = store.getRoot().concat("/backup/");
    File backupDirectory = new File(backUpPath);
    backupDirectory.mkdir();

    for (File entry : files) {
      File[] subDirectories = entry.listFiles();
      if (subDirectories != null) {

        for (File subDirectory : subDirectories) {
          if (subDirectory.isDirectory() && !map.containsKey(subDirectory.getAbsolutePath())) {
            logger.debug("Moving file to backup folder {} "+subDirectory.getName());
            org.apache.commons.io.FileUtils.moveDirectoryToDirectory(subDirectory, backupDirectory, false);
          }
        }
      }
    }
  }

  public static void resetOrphanFolder(DocumentStore store,
                                       Map<String, String> map) throws IOException {
    String path = store.getRoot().concat("/0/");
    File file = new File(path);
    String backUpPath = store.getRoot().concat("/backup/");
    File backupDirectory = new File(backUpPath);
    //        backupDirectory.mkdir();

        /*for (java.io.File entry : backupDirectory) {
            java.io.File[] subDirectories = entry.listFiles();
            if (subDirectories != null) {

                for (File subDirectory : subDirectories) {
                    if (subDirectory.isDirectory() && !map.containsKey(subDirectory.getAbsolutePath())) {
                        logger.debug("Moving file to backup folder {} "+subDirectory.getName());
                        org.apache.commons.io.FileUtils.moveDirectoryToDirectory(subDirectory, backupDirectory, false);
                    }
                }
            }
        }*/

    File[] backedupfiles = backupDirectory.listFiles();
    Path backupDirPath = Paths.get(backUpPath);
    int i = 0;
    for(Map.Entry<String, String> entry : map.entrySet()){
      //            if(i == 5) return;

      String originalPath = entry.getKey();
      String firstPart = originalPath.substring(0, originalPath.lastIndexOf( "/"));
      String abt = originalPath.substring(originalPath.lastIndexOf("/") + 1);
      Path bkPath = backupDirPath.resolve(abt);
      if(Files.exists(bkPath)) {
        try {
          logger.debug("moving {} to {}", bkPath, firstPart);
          org.apache.commons.io.FileUtils.moveDirectoryToDirectory(bkPath.toFile(), Paths.get(firstPart).toFile(), false);
        } catch (IOException e) {
          logger.error(e.getMessage());
          throw e;
        }

      }
      i++;
    }
  }

  public static void createParquetFolder(DocumentStore store, String id)
          throws IOException {
    Path parquetPath = store.getFileSystem()
            .getPath(id)
            .resolve("parquet");
    Files.createDirectories(parquetPath);
  }

  public static Path createSparkModel(DocumentStore store, String id)
          throws IOException {
    Path filePath = store.getFileSystem().getPath(id);
    Files.createDirectories(filePath.resolve("data"));
    return filePath;
  }


}
