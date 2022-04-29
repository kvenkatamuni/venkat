package com.paanini.jiffy.trader;

import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.storage.DocumentStore;
import com.paanini.jiffy.utils.FileCopyNameGenerator;
import com.paanini.jiffy.utils.VfsManager;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.BasicFileView;
import com.paanini.jiffy.vfs.files.Folder;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

/**
 * Provides functions to iterate through the file hierarchy and copy
 * Created by Nidhin francis 13/08/19
 */
@Service
public class Duplicator {

  @Autowired
  DocumentStore documentStore;

  @Autowired
  VfsManager vfsManager;

  SecureRandom sr = new SecureRandom();

  private final int ATTEMPTS = 20;

  static Logger LOGGER = LoggerFactory.getLogger(Duplicator.class);


  public <T extends Persistable> void copyFiles(T source, T dest) {
    String sId =  ((BasicFileProps) source).getId();
    String dId =  ((BasicFileProps) dest).getId();
    if(((BasicFileProps) source).getType() == Type.FOLDER){
      List<Persistable> sourceChildren = ((Folder) source).getChildren();
      List<Persistable> destChildren = ((Folder) dest).getChildren();
      for(int i = 0 ; i < sourceChildren.size(); i++){
        copyFiles(sourceChildren.get(i),destChildren.get(i));
      }
    } else {
      copyPhysicalFile(sId,dId);
    }
  }

  /**
   * Takes folder/file and return the copy of entire file/folder with
   * children(deep copy)
   * @param file
   * @return
   * @throws IOException
   * @throws ClassNotFoundException
   */

  public Persistable copyPersistable(Persistable file) {
    if(file instanceof Folder){

      Folder folder = cloneFolder(file);
      List<Persistable> sourceChildren = ((Folder) file).getChildren();
      for(Persistable p : sourceChildren){
        folder.addChild(copyPersistable(p));
      }
      return folder;
    } else {
      cleanse(file);
      return  clone(file);
    }
  }

  /**
   * Clones folder with minimal fields required for duplicate
   * @param file
   * @return
   */

  private Folder cloneFolder(Persistable file){
    Folder folder = new Folder();
    folder.setId(((BasicFileProps) file).getId());
    folder.setType(((BasicFileProps) file).getType());
    return folder;
  }

  /**
   * Cloning persistable file object with minimal fields for duplicate
   * @param file
   * @return
   * @throws IOException
   * @throws ClassNotFoundException
   */

  private Persistable clone(Persistable file){
    BasicFileView fileView = new BasicFileView();
    fileView.setId(((BasicFileProps) file).getId());
    fileView.setType(((BasicFileProps) file).getType());
    return fileView;
  }

  /**
   * Copies files from data folder of the file of sourceId to of
   * destinationId
   * @param sourceId
   * @param destinationId
   */

  private void copyPhysicalFile(String sourceId, String destinationId) {
    Path sourcePath = documentStore.getFileSystem().getPath(sourceId);
    Path dest = documentStore.getFileSystem().getPath(destinationId);
    try {
      FileUtils.copyDirectory(sourcePath.toFile(), dest.toFile());
    } catch (IOException e) {
      LOGGER.error("Exception while copying the Folder ", e.getMessage());
    }
  }

  /**
   * Cleansing of concrete file objects
   * @param file
   * @return
   */
  private Persistable cleanse(Persistable file){
    /*if(file instanceof Config){
      ((Config) file).setConfigName("");
    }*/

    if(file instanceof BasicFileProps) {
      ((BasicFileProps) file).setScheduled(false);
    }

    if (file.getValue("type").equals(Type.JIFFY_TABLE)) {
      file.setValue("tableName", UUID.randomUUID().toString());
    }

    return file;
  }


  /***
   * Returns new name for duplicate file
   * @param name
   * @param parentId
   * @return
   * @throws RepositoryException
   */
  public String getCopiedFileName(String name, String parentId) throws RepositoryException {

    for (int i = 0; i < ATTEMPTS; i++) {
      String nextName = FileCopyNameGenerator.getNext(name);
      if (vfsManager.isFilePresent(nextName, parentId)) {
        name = nextName;
        continue;
      }
      return nextName;
    }
    //could not acquire a name to copy to; trying a random number
    return name + sr.nextInt();
  }
}
