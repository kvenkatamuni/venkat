package com.paanini.jiffy.storage;

public interface FileMapStrategy {
  class Direct implements FileMapStrategy {
    @Override
    public String[] map(String path) {
      return new String[]{path};
    }
  }

  String[] map(String path);
}
