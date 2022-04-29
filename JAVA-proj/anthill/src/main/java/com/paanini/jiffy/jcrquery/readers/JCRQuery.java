package com.paanini.jiffy.jcrquery.readers;

import com.option3.docube.schema.approles.Role;
import com.paanini.jiffy.models.RolesV2;
import com.paanini.jiffy.vfs.io.FolderViewOption;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.List;

public interface JCRQuery {
  NodeIterator execute(Node node, Session session) throws RepositoryException ;
  default boolean hasPermission(Node node, Session session, FolderViewOption.ReadAs view)
          throws RepositoryException {
    return true;
  }

  List<RolesV2> getRoles();

}
