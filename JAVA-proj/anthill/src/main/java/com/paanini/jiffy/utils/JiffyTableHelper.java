package com.paanini.jiffy.utils;

import com.paanini.jiffy.vfs.files.JiffyTable;

public class JiffyTableHelper {
  public static boolean isMeta(String name) {
    if (name.endsWith(JiffyTable.ACCURACY_SUFFIX) || name.endsWith(JiffyTable.CATEGORY_SUFFIX) ||
            name.endsWith(JiffyTable.PSEUDONYMS_SUFFIX)) {
      return true;
    }
    return false;
  }
}
