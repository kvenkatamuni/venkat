package com.paanini.jiffy.jcrquery.readers.impl;

import com.paanini.jiffy.jcrquery.readers.JCRQuery;
import com.paanini.jiffy.models.RolesV2;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.List;

public class AppPropertiesReaderQuery implements JCRQuery {
    @Override
    public NodeIterator execute(Node node, Session session) throws RepositoryException {
        return null;
    }

    @Override
    public List<RolesV2> getRoles() {
        return null;
    }
}
