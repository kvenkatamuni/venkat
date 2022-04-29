package com.paanini.jiffy.jcrquery;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

public interface QueryBuilder {
  Object buildQuery(Session session, String nodePath, QueryModel queryModel) throws RepositoryException;
}
