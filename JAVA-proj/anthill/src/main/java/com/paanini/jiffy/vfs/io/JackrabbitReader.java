package com.paanini.jiffy.vfs.io;

import com.paanini.jiffy.constants.Content;
import com.paanini.jiffy.dto.AccessEntry;
import com.paanini.jiffy.dto.QueryOptions;
import com.paanini.jiffy.jcrquery.readers.JCRQuery;
import com.paanini.jiffy.jcrquery.readers.impl.AppFileTypeReaderQuery;
import com.paanini.jiffy.models.RolesV2;
import com.paanini.jiffy.utils.Permission;
import com.paanini.jiffy.utils.TenantHelper;
import com.paanini.jiffy.vfs.api.ContentNodeVisitor;
import com.paanini.jiffy.vfs.api.ExtraFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.Folder;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The JackrabbitReader class visits Jackrabbit node and builds
 * a corresponding VFS structure
 *
 * @author  Priyanka Bhoir
 * @since   5/8/19
 * */
public class JackrabbitReader implements ContentNodeVisitor {

  private final Services services;
  private final QueryOptions sort;
  private Stack<Folder> folders = new Stack<>();
  private Persistable result;
  private int depth;
  private final FolderViewOption.ReadAs view;
  private final JCRQuery jcrQuery;
  private boolean softRead;

  public JackrabbitReader(Services services) {
    this(services, FolderViewOption.getDefaultOptions());
  }

  public JackrabbitReader(Services services, FolderViewOption options) {
    this.services = services;
    this.depth = options.getDepth();
    this.view = options.getView();
    this.jcrQuery = options.getJCRQuery();
    this.sort = options.getQueryOptions();
    this.softRead = options.isSoftread();
  }

  /**
   * Tries to read a Folder node,
   * returns true if reads successfully
   * @param node
   * @return boolean value representing success of reading
   */
  @Override
  public boolean enterFolder(ContentNode node) {
    Node folderNode = node.getNode();
    //Limit the depth of traversal
    if(depth < 0) return false;

    Folder folder = getFileInstance(folderNode);
    ContentNodeReader reader = new ContentNodeReader(folder, folderNode, services,softRead);
    SchemaTraverser t = new SchemaTraverser(folder.getFileSchema(), reader);
    t.traverse();
    addBasicFileProperties(folder, node);

    folders.push(folder);
    depth = depth - 1;

    return true;
  }

  /**
   * Read a VFS file.
   *
   * @param node
   */
  @Override
  public void visit(ContentNode node) {
    if(depth < 0) return;

    Node fileNode = node.getNode();

    Persistable file = getFileInstance(fileNode);
    ContentNodeReader reader = new ContentNodeReader(file, fileNode, services,softRead);
    SchemaTraverser t = new SchemaTraverser(file.getFileSchema(), reader);
    t.traverse();

    addBasicFileProperties(file, node);
    if(folders.empty()){
      result = file;  //Direct file visit
    } else {
      //Add file to the existing children of recent folder
      folders.peek().addChild(file);
    }
  }

  /**
   * Marks folder as read
   *
   * @param folder
   */
  @Override
  public void exitFolder(ContentNode folder) {
    depth = depth + 1;

    //Pop out the current visited folder
    Folder pop = folders.pop();
    if (!(jcrQuery instanceof AppFileTypeReaderQuery)) {
      Utils.sort(pop.getChildren(), sort);
    }
    if(folders.empty()) {
      //Empty stack represents traversal completion, set result.
      result = pop;
    } else {
      //Add the visited folder to it's parent.
      folders.peek().addChild(pop);
    }
  }

  @Override
  public int getDepth() {
    return depth;
  }

  public Persistable getResult() {
    return result;
  }


  /**
   * Adds properties which are not part of Avro schema, But required as a
   * part of any VFS file and should be read from Node
   * @param file
   * @param cNode
   */
  private void addBasicFileProperties(Persistable file, ContentNode cNode) {
    Node node = cNode.getNode();
    NodeUtils.addExtraFileProperties(file, cNode.getNode());
    if (file instanceof ExtraFileProps) {
      try {
        ((ExtraFileProps) file).setPrivileges(getShareDetails(node, cNode.getSession()));
      } catch (RepositoryException e) {
        // when current session does not have access to parent
        ((ExtraFileProps) file).setPrivileges(new AccessEntry[0]);
      }
    } else if(file instanceof Folder){
      ((Folder) file).setRole(getRolesAssinged(cNode));
    }
  }

  private <T extends Persistable>T getFileInstance(Node fileNode) {
    return NodeUtils.getFileInstance(NodeUtils.getType(fileNode),
            view, services.getSchemaService());
  }


  /**
   * Reads sharing details like users to whom file is shared and with choch
   * privileges
   * @param node
   * @param session
   * @return
   * @throws RepositoryException
   */
  private AccessEntry[] getShareDetails(Node node, Session session)
          throws RepositoryException {
    if(node.getName().equals(Content.SHARED_FILES)) {
      return null;
    }
    List<AccessEntry> privileges = new ArrayList<>();
    String[] allPermissions = Stream.of(Permission.values())
            .filter(p -> !p.equals(Permission.NONE))
            .map(Enum::toString)
            .toArray(String[]::new);

    try{
      if(node.getPath().equals(
              "/alltenants/" + TenantHelper.getTenantId() +"/users/shared-space")) {
        privileges.add(new AccessEntry(session.getUserID(), allPermissions));
      } else {
        privileges.add(new AccessEntry(session.getUserID(),
                getAllPermissions(getPrivileges(session, node))));
      }
    }catch (Exception e){
      /**
       * to catch Exception thrown in migration calls and provide no privilleges
       */
    }

    return privileges.toArray(new AccessEntry[0]);
  }

  private String[] getAllPermissions(Privilege[] privileges) {
    Set<String> set = Stream.of(privileges)
            .map(Privilege::getName)
            .collect(Collectors.toSet());

    return Stream.of(Permission.values())
            .filter(p -> set.containsAll(Arrays.asList(p.translate())))
            .map(Permission::getAllowedPermissions)
            .flatMap(Stream::of)
            .map(Enum::toString)
            .toArray(String[]::new);
  }

  public Privilege[] getPrivileges(Session session, Node node)
          throws RepositoryException {
    return session.getAccessControlManager()
            .getPrivileges(node.getPath());

  }

  private List<String> getRolesAssinged(ContentNode cNode) {
    JCRQuery query = cNode.getQuery();
      List<RolesV2> roles = query.getRoles();
      if(Objects.isNull(roles)){
        return null;
      }
      return roles.stream().map(e -> e.getName()).collect(Collectors.toList());
  }

  public FolderViewOption.ReadAs getView() {
    return view;
  }
}

