package com.paanini.jiffy.jcrquery.readers.impl;

import com.option3.docube.schema.approles.Role;
import com.paanini.jiffy.jcrquery.readers.JCRQuery;
import com.paanini.jiffy.models.RolesV2;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.util.List;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class SimpleJCRQuery implements JCRQuery {

  private List<RolesV2> roles;
  private String fiterCharacter;
  public SimpleJCRQuery(String filterCharacter){
    this.fiterCharacter = filterCharacter;
  }
  public SimpleJCRQuery(){
    this.fiterCharacter="";
  };

  @Override
  public NodeIterator execute(Node node, Session session) throws RepositoryException {
    if(isValidSubType(node, "appGroup")){
      String queryString = getBaseQuery(node.getPath());
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
      QueryResult execute = query.execute();
      return execute.getNodes();
    } else {
      return node.getNodes();
    }
  }

  private boolean isValidSubType(Node node, String type) throws RepositoryException {
    return (node.hasProperty("subType") && node.getProperty(
            "subType").getValue().toString().equals(type));
  }

  @Override
  public List<RolesV2> getRoles() {
    return roles;
  }
  public SimpleJCRQuery setRoles(List<RolesV2> roles) {
    this.roles = roles;
    return this;
  }

  private String getBaseQuery(String nodePath) {
    String baseQuery = "SELECT * FROM [nt:base] AS node WHERE ISCHILDNODE" +
            "(node,["+ nodePath + "])";
    if(!fiterCharacter.isEmpty()){
      baseQuery = baseQuery.concat(" AND [jcr:title] not like '" + fiterCharacter + "%'");
    }
    return baseQuery;
  }
}
