package com.paanini.jiffy.utils;

import com.option3.docube.schema.nodes.Type;
import com.option3.docube.service.SchemaService;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.ExtraFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.io.NodeUtils;
import com.paanini.jiffy.vfs.io.Services;
import com.paanini.jiffy.vfs.io.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.util.List;

/**
 * Created by Adarsh V on 12/08/2019
 * Returns the node based on the operation type passed to it.
 */

public class NodeFactory {

  private static Logger LOGGER = LoggerFactory.getLogger(NodeFactory.class);

  /**
   * Returns the node based on the type of operation. If the operation is
   * create, a new node is returned, else the existing node is returned.
   * The file id is made null to handle the import scenario wherein the
   * passed file already has an id.
   *
   * @param file
   * @param node
   * @param services
   * @param isCreate
   * @return
   */

  public static Node get(Persistable file, Node node, Services services,
                         boolean isCreate) {
    if (isCreate) {
      file.setValue("id", null);
      return new NodeFactory().createNode(node, file, services);
    }
    /**
     * During the upsert (importing existing app, if the new file is not available
     * it is created using the below code
     */
    try {
      if((!isCreate)&&(!node.hasNode((String) file.getValue("name")))){
        file.setValue("id", null);
        return new NodeFactory().createNode(node, file, services);
      }
    } catch (RepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }
    try {
      return node.getNode(file.getValue("name").toString());
    } catch (RepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }

  }

  /**
   * creates a node for the given file.
   *
   * @param parent
   * @param file
   * @return
   */
  private Node createNode(Node parent, Persistable file, Services services) {
    Type type = ((BasicFileProps) file).getType();
    String nodeName = ((BasicFileProps) file).getName();
    String owner = ((BasicFileProps) file).getOwner();
    try {

      checkNodeNameExists(parent, nodeName);
      Node child = parent.addNode(nodeName, NodeUtils.getNodeType(type));
      addDefaultMixins(child);
      addTypeMixins(child, type, services.getSchemaService());
      file.setValue("id", child.getIdentifier());
      if (file instanceof ExtraFileProps) {
        ExtraFileProps file1 = (ExtraFileProps) file;
        file1.setPath(Utils.getRelativePath(child.getPath(),
                owner));
        file1.setParentId(parent.getIdentifier());
      }
      return child;
    } catch (RepositoryException e) {
      LOGGER.error(e.getMessage(), e);
      throw new ProcessingException(e.getMessage(), e);
    }
  }


  private void checkNodeNameExists(Node parent, String nodeName) throws
          RepositoryException {
    if (parent.hasNode(nodeName)) {
      throw new ProcessingException("Item already exists : " + nodeName);
    }
  }

  /**
   * add mixins to the node created.these mixins will be provided through
   * the schema service
   *
   * @param node
   * @param type
   * @param schemaService
   */
  private void addTypeMixins(Node node, Type type, SchemaService
          schemaService) {
    List<String> mixins = schemaService.getMixins(type);
    mixins.forEach(m -> {
      try {
        node.addMixin(m);
      } catch (RepositoryException e) {
        LOGGER.error(e.getMessage(), e);
        throw new ProcessingException(e.getMessage(), e);
      }
    });
  }

  private void addDefaultMixins(Node node) throws RepositoryException {
    node.addMixin(NodeType.MIX_CREATED);
    node.addMixin(NodeType.MIX_LAST_MODIFIED);
    node.addMixin(NodeType.MIX_TITLE);
    if (node.canAddMixin(NodeType.MIX_REFERENCEABLE)) {
      node.addMixin(NodeType.MIX_REFERENCEABLE);
    }
  }
}

