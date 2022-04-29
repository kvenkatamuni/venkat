package com.paanini.jiffy.vfs.utils;

import javax.jcr.Node;
import java.util.Optional;

public class NodeWithPath {
  private final Node node;
  private final Optional<String> path;

  public NodeWithPath(Node node) {
    this(node, null);
  }

  public NodeWithPath(Node node, String path) {
    this.node = node;
    this.path =
            (path == null) ? Optional.empty() : Optional.of(path);
  }

  public Node getNode() {
    return node;
  }

  public Optional<String> getPath() {
    return path;
  }
}