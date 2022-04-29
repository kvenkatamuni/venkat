package com.paanini.jiffy.vfs.api;

import com.paanini.jiffy.vfs.files.Folder;

public interface VfsVisitor {
  void enterFolder(Folder folder);
  void visit(Persistable file);
  void exitFolder(Folder folder);
}
