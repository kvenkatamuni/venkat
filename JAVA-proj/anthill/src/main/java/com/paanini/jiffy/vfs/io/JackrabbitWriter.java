package com.paanini.jiffy.vfs.io;

import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.utils.NodeFactory;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.api.VfsVisitor;
import com.paanini.jiffy.vfs.files.Folder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Objects;
import java.util.Stack;

/**
 * Created by
 */
public class JackrabbitWriter implements VfsVisitor {

  private final Services services;
  private final boolean isCreate;
  private Node parentNode;
  private Stack<Folder> folders = new Stack<>();

  private Persistable result;

  private static Logger LOGGER = LoggerFactory.getLogger(JackrabbitWriter.class);
  /**
   * This service is used to create a new node in jackrabbit
   * @param services
   * @param node
   * @return
   */
  public static JackrabbitWriter forCreate(Services services, Node node) {
    return new JackrabbitWriter(services, node, true);
  }

  /**
   * This service is used to do an update operation on the node.
   * @param services
   * @param node
   * @return
   */
  public static JackrabbitWriter forUpdate(Services services, Node node) {
    return new JackrabbitWriter(services, node, false);
  }

  private JackrabbitWriter(Services services, Node node, boolean isCreate) {
    this.services = services;
    this.parentNode = node;
    this.isCreate = isCreate;
  }

  @Override
  public void enterFolder(Folder folder) {
    addMissingProperties(folder);
    Node child = NodeFactory.get(folder, parentNode, services, isCreate);

    NodeUtils.addExtraFileProperties(folder, child);

    ContentNodeWriter handler = new ContentNodeWriter(folder, child, services);
    SchemaTraverser t = new SchemaTraverser(folder.getFileSchema(), handler);
    boolean success = t.traverse();

    /**
     * @TODO: what should happen if while bulk writing(ImpEx) external save fails
     */
    if(isCreate && !success) {
      try {
        child.remove();
      } catch (RepositoryException e) {
        LOGGER.error(e.getMessage(), e);
        throw new ProcessingException(e.getMessage(), e);
      }
    }

    parentNode = child;
    folders.push(folder);
  }

  @Override
  public void visit(Persistable file) {
    addMissingProperties(file);
    Node node = NodeFactory.get(file, parentNode, services, isCreate);

    NodeUtils.addExtraFileProperties(file, node);

    //saves the created node
    ContentNodeWriter handler = new ContentNodeWriter(file, node, services);
    SchemaTraverser t = new SchemaTraverser(file.getFileSchema(), handler);
    boolean success = t.traverse();

    if(isCreate && !success){
      validateNodeSaved(node, success,handler.getExternalErroMessage());
    }


    if (folders.empty()) {
      result = file;
    }
  }

  private void validateNodeSaved(Node node, boolean success,String errorMessage) {
    if(!success) {
      try {
        String name = node.getName();
        node.remove();
        if(Objects.isNull(errorMessage)){
          throw new ProcessingException("Could not create " + name);
        }else {
          throw new ProcessingException(errorMessage);
        }

      } catch (RepositoryException e) {
        LOGGER.error(e.getMessage(), e);
        throw new ProcessingException(e.getMessage(), e);
      }
    }
  }

  @Override
  public void exitFolder(Folder folder) {
    Folder pop = folders.pop();
    if (folders.empty()) {
      result = pop;
    } else {
      try {
        parentNode = parentNode.getParent();
      } catch (RepositoryException e) {
        throw new ProcessingException(e.getMessage());
      }
    }
  }

  public Persistable getResult() {
    return result;
  }

  /**
   * add properties from session which are not part of the file passed.
   * @param file
   */
  private void addMissingProperties(Persistable file) {
    if(folders.empty())
      return;
    Folder folder = folders.peek();
    NodeUtils.addFileProperties(file, folder.getCreatedBy(), folder.getOwner());
  }
}
