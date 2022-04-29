package com.paanini.jiffy.vfs.api;

import com.paanini.jiffy.vfs.io.ContentNode;

/**
 * Created by Priyanka Bhoir on 6/8/19
 */
public interface ContentNodeVisitor {
  boolean enterFolder(ContentNode node);
  void visit(ContentNode file);
  void exitFolder(ContentNode folder);
  int getDepth();
}
