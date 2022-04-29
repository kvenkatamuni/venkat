package com.paanini.jiffy.vfs.api;

import com.paanini.jiffy.exception.ProcessingException;

public interface Visitable {
  default void accept(VfsVisitor visitor){
    throw new ProcessingException("Visit not supported for " +
            this.getClass().getName());
  }
}
