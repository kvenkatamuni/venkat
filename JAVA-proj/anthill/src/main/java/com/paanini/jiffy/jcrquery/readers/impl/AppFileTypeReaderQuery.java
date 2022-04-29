package com.paanini.jiffy.jcrquery.readers.impl;

import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.jcrquery.Filter;
import com.paanini.jiffy.jcrquery.JCRQueryBuilder;
import com.paanini.jiffy.jcrquery.Operator;
import com.paanini.jiffy.jcrquery.QueryModel;
import com.paanini.jiffy.jcrquery.readers.JCRQuery;
import com.paanini.jiffy.models.RolesV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.util.*;
import java.util.stream.Collectors;

public class AppFileTypeReaderQuery implements JCRQuery {

  private QueryModel queryModel;
  private List<RolesV2> roles;
  private static final List<String> defaultRoles = Arrays.asList("DESIGNER",
          "SUPPORT", "RELEASE_ADMIN", "BUSINESS_USER");
  static Logger logger =
          LoggerFactory.getLogger(AppFileTypeReaderQuery.class);

  public AppFileTypeReaderQuery(QueryModel queryModel) {
    this.queryModel = queryModel;
    this.roles = new ArrayList<>();
  }

  public AppFileTypeReaderQuery(QueryModel queryModel, List<RolesV2> roles) {
    this.queryModel = queryModel;
    this.roles = roles;
  }

  public AppFileTypeReaderQuery setQueryModel(QueryModel queryModel) {
    this.queryModel = queryModel;
    return this;
  }

  public AppFileTypeReaderQuery setRoles(List<RolesV2> roles) {
    this.roles = roles;
    return this;
  }

  public QueryModel getQueryModel() {
    return queryModel;
  }

  public List<RolesV2> getRoles() {
    return roles;
  }

  public NodeIterator execute(Node node, Session session) throws RepositoryException {
    if (node.hasProperty("subType") && node.getProperty(
            "subType").getValue().toString().equals("app")) {
      String appId = node.getIdentifier();
      //return node.getNodes();
    }

    if (roles.isEmpty()) {
      throw new ProcessingException("User doesn't have permission to " +
              "perform this action");
    }

    QueryModel model = getPermissions(queryModel);
    Query query = new JCRQueryBuilder().buildQuery(session, node.getPath(),
            model);
    logger.debug("Query : "+ query.getStatement());

    QueryResult result = query.execute();

    return result.getNodes();
  }

  private QueryModel getPermissions(QueryModel model) {
    Set<String> fileIdentifiers = new HashSet<>();
    Set<String> fileTypes = new HashSet<>();
    List<String> roleNames = roles.stream()
            .map(role -> role.getName())
            .collect(Collectors.toList());

    roles.forEach(role -> {
      Set<String> fileIds = role.getFileIds();
      //List<FileTypePermission> types = role.getFilesTypes();

      if (!roleNames.stream().anyMatch(name ->
              defaultRoles.contains(name))) {
        fileIds.forEach(fileId -> {
          fileIdentifiers.add(fileId);
        });
      }

      Set<String> files = role.getPermissionMap().keySet();
      for(String file : files){
        if(role.getPermissionMap().get(file).contains("read")){
          fileTypes.add(file.split("_FOLDER")[0]);
        }
      }
    });

    validateFilters(model, fileTypes);

    return addPermissionFilters(model, fileIdentifiers);
  }

  private void validateFilters(QueryModel model, Set<String> fileTypes) {
    Iterator<Filter> iterator = model.getFilters()
            .stream()
            .filter(filter -> filter.getColumn().equals("type"))
            .collect(Collectors.toList())
            .iterator();

    while (iterator.hasNext()) {
      Filter filter = iterator.next();
      if (!Arrays.asList(fileTypes.toArray()).contains(
              filter.getValue().toString())) {
        throw new ProcessingException("User doesn't have permission " +
                "to view this file type :"+
                filter.getValue().toString());
      }
    }
  }

  private QueryModel addPermissionFilters(QueryModel model,
                                          Set<String> fileIdentifiers) {
    List<Filter> filters = new ArrayList<>();
    fileIdentifiers.forEach(fileIdentifier -> {
      filters.add(new Filter("id", Operator.EQUAL, fileIdentifier));
    });

    model.getFilters().addAll(filters);

    return model;
  }
}
