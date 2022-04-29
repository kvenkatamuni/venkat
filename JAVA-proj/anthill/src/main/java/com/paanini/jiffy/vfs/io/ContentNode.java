package com.paanini.jiffy.vfs.io;

import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.jcrquery.readers.JCRQuery;
import com.paanini.jiffy.vfs.api.ContentNodeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import java.util.Objects;

/**
 * Created by Priyanka Bhoir on 6/8/19
 */
public class ContentNode {
  private final Node node;
  private final Session session;
  private final JCRQuery query;
  static Logger logger = LoggerFactory.getLogger(ContentNode.class);

  public ContentNode(Node node, Session session, JCRQuery query) {
    this.node = NodeUtils.getLinkSource(node);
    this.session = session;
    this.query = query;
  }

  public Node getNode() {
    return node;
  }

  public JCRQuery getQuery() {
    return query;
  }

  public Session getSession() {
    return session;
  }

  public void accept(ContentNodeVisitor visitor) {
    try {
      boolean isFolder = node.isNodeType(NodeType.NT_FOLDER);
      boolean isFile = node.isNodeType(NodeType.NT_FILE);

      if(!(isFile || isFolder)) return;

      if(isFolder) {
        processFolder(visitor);
      } else {
        hasPermission(visitor, session);
      }
    } catch (RepositoryException e) {
      throw new ProcessingException(e.getMessage(), e);
    }
  }

  private void processFolder(ContentNodeVisitor visitor) {
    if(visitor.enterFolder(this)) {
      if(visitor.getDepth()>=0){
        try {
          NodeIterator nodes = query.execute(node, session);
          if (Objects.nonNull(nodes)) {
            while (nodes.hasNext()) {
              ContentNode contentNode = new ContentNode(nodes.nextNode(), session, query);
              contentNode.accept(visitor);
            }
          }
        } catch (RepositoryException e) {
          throw new ProcessingException(e.getMessage(), e);
        }
      }
      visitor.exitFolder(this);
    }
  }


  /**
   * For direct file reads(PRESENTATION, JIFFYTABLE, FILESET etc there won't
   * be permission check) and the query type will be DefaultJCRQuery. If
   * the file type is only AppFileTypeReadQuery the nodes we get will be
   * validated (Hence check fot AppFileTypeReadQuery is not required here).
   *
   * @param visitor
   * @throws RepositoryException
   */
  private void hasPermission(ContentNodeVisitor visitor, Session session) throws RepositoryException {

    if (query.hasPermission(this.getNode(), session,((JackrabbitReader)visitor).getView())) {
      visitor.visit(this);
    } else {
      logger.error("User doesn't have permission for :" + this.getNode().getPath());
      throw new ProcessingException("User doesn't have " +
              "permission to perform this action");
    }

  }

}


