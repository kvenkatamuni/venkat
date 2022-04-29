package com.paanini.jiffy.services;

import com.paanini.jiffy.constants.Content;
import com.paanini.jiffy.utils.NodeLocator;
import com.paanini.jiffy.utils.NodeManager;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.security.Privilege;

public class ContentSystem {

  Session session;
  NodeManager nodeManager;
  NodeLocator nodeLocator;
  public ContentSystem(Session session, NodeLocator nodeLocator){
    this.session = session;
    this.nodeManager = new NodeManager(session);
    this.nodeLocator = nodeLocator;
  }

  public void bootstrapSystem() throws RepositoryException {
    Node node = session.getRootNode()
            .addNode(Content.FOLDER_TENANTS, "nt:folder");
    nodeManager.setUpCustomNodes();
    nodeManager.setupCustomPrivileges();
  }

  public void upgrade() throws RepositoryException {
    nodeManager.setUpCustomNodes();
  }

  public void createTenant(String tenantId) throws RepositoryException {
    Node repoRoot = session.getNode(nodeLocator.getPath());
    if (repoRoot.hasNode(tenantId)) {
      throw new RepositoryException("Tenant already exists");
    }
    Node home = repoRoot.addNode(tenantId, NodeType.NT_FOLDER);
    home.addNode(Content.FOLDER_USERS, NodeType.NT_FOLDER);
  }

  public boolean tenantExists(String tenantId) throws RepositoryException {
    Node repoRoot = session.getNode(nodeLocator.getPath());
    return repoRoot.hasNode(tenantId);
  }

  public void addUserToTenantSpace(String userId)
          throws RepositoryException {

    Node tenantSharedSpace = nodeLocator.getTenantSharedSpace();
    Node tenantUserHome = nodeLocator.getTenantUserHome();

    JackrabbitSession js = (JackrabbitSession) session;
    UserManager um = js.getUserManager();
    Authorizable u = um.getAuthorizable(userId);
    if (u == null) {
      u = um.createUser(userId, "password");
    }
    nodeManager.grantPrivilegeToUser(tenantUserHome, u.getPrincipal(), js, Privilege
            .JCR_ALL);
    nodeManager.grantPrivilegeToUser(tenantSharedSpace, u.getPrincipal(), js, Privilege
            .JCR_ALL);
  }


}
