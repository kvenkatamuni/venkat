package com.paanini.jiffy.vfs.utils;

import com.paanini.jiffy.jcrquery.readers.impl.AppFileTypeReaderQuery;
import com.paanini.jiffy.jcrquery.readers.impl.DefaultJCRQuery;
import com.paanini.jiffy.jcrquery.readers.impl.SimpleJCRQuery;
import com.paanini.jiffy.models.RolesV2;
import com.paanini.jiffy.vfs.io.FolderViewOption;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by
 */
public class FolderOptionsEnhancer {

  List<RolesV2> roles = new ArrayList<>();

  public void setRoles(List<RolesV2> roles) {
    this.roles = roles;
  }

  public FolderViewOption enhance(Node node, FolderViewOption option)
          throws RepositoryException {
      return getReadQueryOption(option, roles);
  }

  private FolderViewOption getReadQueryOption(FolderViewOption option,
                                              List<RolesV2> roles) {
    if (Objects.isNull(option.getJCRQuery())) {
      option.setJCRQuery(new DefaultJCRQuery().setRoles(roles));
    }

    if (option.getJCRQuery() instanceof AppFileTypeReaderQuery) {
      ((AppFileTypeReaderQuery) option.getJCRQuery())
              .setRoles(roles);
    }

    if (option.getJCRQuery() instanceof SimpleJCRQuery) {
      ((SimpleJCRQuery) option.getJCRQuery())
              .setRoles(roles);
    }
    return option;
  }
}
