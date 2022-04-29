package com.paanini.jiffy.models;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class Edge {
  String startNode;
  String endNode;
  boolean isActive;

  public Edge() {
  }

  public Edge(String startNode, String endNode, boolean isActive) {
    this.startNode = startNode;
    this.endNode = endNode;
    this.isActive = isActive;
  }

  public String getStartNode() {
    return startNode;
  }

  public void setStartNode(String startNode) {
    this.startNode = startNode;
  }

  public String getEndNode() {
    return endNode;
  }

  public void setEndNode(String endNode) {
    this.endNode = endNode;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }
}
