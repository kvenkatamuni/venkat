package com.paanini.jiffy.vfs.api;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public interface Exportable {
  default Set<String> getDependencies() {
    return Collections.<String>emptySet();
  }
  default Set<String> updateDependencies() {
    return Collections.<String>emptySet();
  }
  default void updateDependencies(List<Persistable> files) {
  }
}
