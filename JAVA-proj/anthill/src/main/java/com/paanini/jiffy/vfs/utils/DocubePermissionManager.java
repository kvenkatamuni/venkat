package com.paanini.jiffy.vfs.utils;

import com.paanini.jiffy.constants.FileProps;
import com.paanini.jiffy.utils.Permission;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;

public class DocubePermissionManager {
  private final Session session;

  private static Logger logger = LoggerFactory
          .getLogger(DocubePermissionManager.class);

  public DocubePermissionManager(Session session) {
    this.session = session;
  }

  public Boolean verifyReadPermission(String id) {
    try {
      return checkPermission(id, Permission.DOCUBE_READ);
    } catch (AccessDeniedException e) {
      logger.debug("Node is not found, reason may be not not exists at " + "" + "all or user does not have permission for the node");
      return false;
    }
  }

  public Boolean verifyWritePermission(String id) {
    try {
      return checkPermission(id, Permission.DOCUBE_WRITE);
    } catch (AccessDeniedException e) {
      logger.debug("Node is not found, reason may be not not exists at " +
              "" + "all or user does not have permission for the node");
      return false;
    }

  }

  public boolean checkPermission(String id, String permission)
          throws AccessDeniedException {
    try {
      Node node = session.getNodeByIdentifier(id);
      String owner = getOwner(node);

      Privilege[] privileges = AccessControlUtils.privilegesFromNames(session, permission);
      boolean hasAccess = session.getAccessControlManager()
              .hasPrivileges(node.getPath(), privileges);
      if (hasAccess || owner.equals(session.getUserID())) {
        return true;
      } else {
        throw new AccessDeniedException("Access Denied");
      }
    } catch (RepositoryException e) {
      throw new AccessDeniedException("Access Denied");
    }
  }

  private String getOwner(Node parent) throws RepositoryException {
    return parent.hasProperty(FileProps.OWNER) ?
            parent.getProperty(FileProps.OWNER).getString() :
            this.session.getUserID();
  }

}