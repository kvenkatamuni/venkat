package com.paanini.jiffy.vfs.io;

import com.option3.docube.schema.nodes.Type;
import com.option3.docube.service.SchemaService;
import com.paanini.jiffy.constants.FileProps;
import com.paanini.jiffy.dto.AccessEntry;
import com.paanini.jiffy.exception.DataProcessingException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.utils.NodeFactory;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.ExtraFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import org.apache.avro.Schema;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by Priyanka Bhoir on 6/8/19
 */
public class NodeUtils {
  private static Logger LOGGER = LoggerFactory.getLogger(NodeFactory.class);
  public static <T extends Persistable> T getFileInstance(Type type
          , FolderViewOption.ReadAs view, SchemaService schemaService) {
    boolean readAsView = view.equals(FolderViewOption.ReadAs.BASIC_FILE)
            && ((type != Type.FOLDER) && (type != Type.FILESET));

    String concreteClass = readAsView ?
            schemaService.getConcreteClass(Type.FILE) :
            schemaService.getConcreteClass(type);

    return getFile(concreteClass);
  }

  public static <T extends Persistable> T getFile(String concreteClass) {
    try {
      return (T) Class.forName(concreteClass).getConstructor()
              .newInstance();
    } catch (InstantiationException |
            IllegalAccessException | InvocationTargetException |
            NoSuchMethodException |
            NoClassDefFoundError |
            ClassNotFoundException e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  public static Type getType(Node node) {
    Type type;
    try {
      type = Type.valueOf(node.getProperty(FileProps.TYPE)
              .getString());
    } catch (RepositoryException e) {
//            logger.error(e.getMessage(), e);
      throw new ProcessingException(e.getMessage());
    }
    return type;
  }

  public static String getNodeType(Type type) {
    if (type.equals(Type.FOLDER)) return NodeType.NT_FOLDER;
    return NodeType.NT_FILE;
  }

  public static <T extends Persistable> void addFileProperties(T file, String userID, String owner) {
    Schema fileSchema = file.getFileSchema();
    BasicFileProps fileProps = (BasicFileProps) file;
    fileProps.setOwner(owner);
    fileProps.setCreatedBy(userID);
    fileProps.setLastModified(new Timestamp(new Date().getTime()).getTime());
  }

  public static <T extends Persistable> void addFilePropertiesforUpdate(T file, String userID, String owner) {
    BasicFileProps fileProps = (BasicFileProps) file;
    fileProps.setOwner(owner);
    fileProps.setCreatedBy(userID);
    fileProps.setLastModified(new Timestamp(new Date().getTime()).getTime());
  }

  public static Type getType(String fileName){
    String extention = FilenameUtils.getExtension(fileName);
    switch (extention){
      case "ds" : return Type.DATASHEET;
      case "fd" : return Type.FOLDER;
      case "ad" : return Type.SQL_APPENDABLE_DATASHEET;
      case "cg" : return Type.CONFIGURATION;
      case "cf" : return Type.CUSTOM_FILE;
      case "fs" : return Type.FILESET;
      case "ps" : return Type.PRESENTATION;
      case "sm" : return Type.SPARK_MODEL_FILE;
      case "jt" : return Type.JIFFY_TABLE;
      case "le" : return Type.LICENSE;
      case "sv" : return Type.SECURE_VAULT_ENTRY;
      case "rl" : return Type.APP_ROLES;
      default : throw new ProcessingException(
              "Unknown filetype : " + fileName);
    }
  }

  public static Node getLinkSource(Node node) {
    boolean isLinkedFile = false;
    try {
      isLinkedFile = node.isNodeType(NodeType.NT_LINKED_FILE);
    } catch (RepositoryException e) {
      throw new ProcessingException(e.getMessage(),  e);
    }
    if(isLinkedFile){
      try {
        node = node.getProperty(Property.JCR_CONTENT).getNode();
      } catch (RepositoryException e) {
        throw new DataProcessingException("Stale Link");
      }
    }
    return node;
  }

  public static <T extends Persistable> void addExtraFileProperties(T file, Node node) {
    String owner = ((BasicFileProps) file).getOwner();
    try {
      file.setValue("id", node.getIdentifier());
      if (file instanceof ExtraFileProps) {
        ExtraFileProps file1 = (ExtraFileProps) file;
        file1.setPath(Utils.getRelativePath(node.getPath(), owner));

        file1.setPrivileges(new AccessEntry[0]);
        try {
          file1.setParentId(node.getParent().getIdentifier());
        } catch (AccessDeniedException e) {
          // when current session does not have access to parent
          LOGGER.warn("Current session does not have permission for {} file's parent",
                  ((BasicFileProps) file).getName());
        }
      }
    } catch (RepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }
  }

}