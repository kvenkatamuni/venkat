package com.paanini.jiffy.utils;

import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.constants.Content;
import com.paanini.jiffy.constants.FileProps;
import com.paanini.jiffy.vfs.utils.ContentPath;
import com.paanini.jiffy.vfs.utils.NodeWithPath;

import javax.jcr.*;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.paanini.jiffy.constants.Content.*;

public class NodeLocator {
  private final Session session;
  private final String tenantId;

  public NodeLocator(Session session, String tenantId) {
    this.session = session;
    this.tenantId = tenantId;
  }

  public String getPath(String... folders) {
    List<String> path = new ArrayList<>();
    path.add(PREFIX);
    path.add(FOLDER_TENANTS);
    if (folders != null && folders.length > 0) {
      for (String folder : folders) {
        path.add(folder);
      }
    }
    return String.join("/", path.toArray(new String[0]));
  }

  public String concatPath(String... folders) {
    List<String> path = new ArrayList<>();
    if (folders != null && folders.length > 0) {
      for (String folder : folders) {
        path.add(folder);
      }
    }
    return String.join("/", path.toArray(new String[0]));
  }

  public Node getTenantUserHome() throws RepositoryException {
    return session.getNode(getPath(tenantId, FOLDER_USERS));
  }

  public Node getUserFolder(String username) throws RepositoryException {
    return session.getNode(getPath(tenantId, FOLDER_USERS,username));
  }

  public Node getTenantRoot(String tenantIdStr) throws RepositoryException {
    return session.getNode(getPath(tenantIdStr));
  }

  public Node getallTenantsNode() throws RepositoryException {
    return session.getNode(getPath());
  }

  public Node getLicenseFolder() throws RepositoryException {
    return session.getNode(getPath(tenantId,LICENSE));
  }

  public Node getSharedFolder() throws RepositoryException {
    return getSharedFolder(session.getUserID());
  }

  public Node getTenantSharedSpace() throws RepositoryException {
    return session.getNode(getPath(tenantId, FOLDER_USERS, SHARED_SPACE));
  }

  public Node getTenantSharedSpace(String tenantId) throws RepositoryException {
    return session.getNode(getPath(tenantId, FOLDER_USERS, SHARED_SPACE));
  }

  public Node getSharedFolder(String userId) throws RepositoryException {
    return getTenantSharedSpace();
  }

  public Node getUserHome() throws RepositoryException {
        /*String userBase = getUserBasePath(session.getUserID());
        return session.getNode(userBase + "/" + ContentConstant.OWNED_FILES);*/
    return getTenantSharedSpace();
  }

  public NodeWithPath locate(String baseId, String path) throws
          RepositoryException {
    return locate(baseId, path, false);
  }

  private NodeWithPath matchPossiblePath(Node node, String pathString) {
    Path path = Paths.get(pathString);
    int count = path.getNameCount();
    for(int i = 0; i < count ; i++) {
      try {
        node = node.getNode(path.getName(i).toString());
      } catch (RepositoryException e) {
        //Exception terminates the folder search
        return new NodeWithPath(node,path.subpath(i,count).toString());
      }
    }
    // Complete match happened no path to return
    return new NodeWithPath(node);
  }

  private String getNodePath(String locationCondition, Node node) {
    String path = null;
    try {
      path = node.getPath();
      return locationCondition + "(s, '" + path + "')";
    } catch (RepositoryException e) {
      return "";
    }
  }

  public NodeWithPath locate(String baseId, String path, boolean matchPossible)
          throws RepositoryException {
    ContentPath cp = new ContentPath(path);
    //Current implementation there should not be any absolute path
    Node root = getNode(baseId, cp);

        /* A file is shared when
           1) it starts  with '/shared' or
           2) the base folderId matches id of '/shared'
           When shared, we need to dereference on the link id and proceed
           further
         */
    if (cp.isShared() || baseId.equals(getSharedFolder().getIdentifier())) {
      final Node node = searchNodeByName(root, cp.getLink());
      if (!node.isNodeType(NodeType.NT_LINKED_FILE)) {
        throw new RepositoryException("Not a link :" + node);
      }
      Node redirected = node.getProperty(Property.JCR_CONTENT).getNode();
      Type type = Type.valueOf(
              redirected.getProperty(FileProps.TYPE).getString());

      return processRedirected(matchPossible, cp, redirected, type);
    } else {
      return matchPossible ?
              matchPossiblePath(root, cp.getPath()) :
              new NodeWithPath(root.getNode(cp.getPath()));
    }
  }

  private NodeWithPath processRedirected(boolean matchPossible, ContentPath cp, Node redirected, Type type) throws RepositoryException {
    if (type.equals(Type.FOLDER)) {
      return matchPossible ?
              matchPossiblePath(redirected, cp.getPath()) :
              new NodeWithPath(redirected.getNode(cp.getPath()));
    } else {
      return matchPossible ?
              matchPossiblePath(redirected, cp.getPath()) :
              new NodeWithPath(redirected);
    }
  }

  private Node getNode(String baseId, ContentPath cp) throws RepositoryException {
    Node root = cp.isAbsolute() ?
            //If absolute anchor on share root or owned root
            getTenantSharedSpace()/*(cp.isShared() ? getTenantSharedSpace() : getTenantUserHome())*/ :
            //Relative file, anchor on the base folder
            ("".equals(baseId) ? getTenantSharedSpace() : session.getNodeByIdentifier(baseId));
    return root;
  }

  private Node searchNodeByName(Node parent, String name) throws
          RepositoryException {
    String queryExpression = "SELECT * FROM [nt:hierarchyNode] as s" + " " +
            "where (" + getNodePath("ISCHILDNODE", parent) + ")";

    QueryManager queryManager = session.getWorkspace().getQueryManager();
    Query query = queryManager.createQuery(queryExpression, Query.JCR_SQL2);
    QueryResult result = query.execute();

    RowIterator rows = result.getRows();
    List<Node> simpleFiles = new ArrayList<>();
    while (rows.hasNext()) {
      Node linkedNode = rows.nextRow().getNode();
      if (linkedNode.getName().equals(name)) {
        simpleFiles.add(linkedNode);
      }
    }

    if (simpleFiles.size() == 0) {
      throw new RepositoryException("Node with name " + name + " was " +
              "not found ");
    } else if (simpleFiles.size() > 1) {
      throw new RepositoryException("More than one file with same name " +
              "is available " + name);
    }

    return simpleFiles.get(0);
  }

  public boolean isFileExist(String path) throws RepositoryException {
    return session.itemExists(path);
  }

  public Node getLicenseEntry() throws RepositoryException {
    String path = getPath(tenantId,LICENSE,LICENSE);
    if (isFileExist(path)) {
      return session.getNode(path);
    }

    return null;
  }

  public Node getAppVaultFolder(Node parent) throws RepositoryException {
    return searchNodeByName(parent,VAULT);
  }

  public Node getNode(String... path) throws RepositoryException {
    return session.getNode(getPath(path));
  }

  public Node getNodebyAbsPath(String... path) throws RepositoryException {
    return session.getNode(concatPath(path));
  }



  public Node getTenantVaultEntries(String name) throws RepositoryException {
    return session.getNode(getPath(tenantId,VAULT,name));
  }

  public Node getTenantVaultFolder() throws RepositoryException {
    return session.getNode(getPath(tenantId,VAULT));
  }

  public Node getRootVaultEntries(String name) throws RepositoryException {
    return session.getNode(getPath(VAULT,name));
  }

  public String getUserBasePath(String userId) {
    return getPath(tenantId, FOLDER_USERS);
  }

  public Node getPreferencesFolder() throws RepositoryException {
    String userBase = getUserBasePath(session.getUserID());
    try {
      return session.getNode(userBase + "/"
              + Content.PREFERENCES);
    } catch (PathNotFoundException e) {
      Node pref = session.getNode(userBase);
      return pref.addNode(Content.PREFERENCES, NodeType.NT_FOLDER);
    }
  }

  public Node locateNode(String parentId) throws RepositoryException {
    return parentId == Content.ROOTFOLDER ?
            getUserHome() :
            session.getNodeByIdentifier(parentId);
  }


}
