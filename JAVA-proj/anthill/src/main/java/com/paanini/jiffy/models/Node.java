package com.paanini.jiffy.models;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class Node {
  String name;
  boolean selected;

  public Node() {
  }

  public Node(String name, boolean selected) {
    this.name = name;
    this.selected = selected;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isSelected() {
    return selected;
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
  }
}
