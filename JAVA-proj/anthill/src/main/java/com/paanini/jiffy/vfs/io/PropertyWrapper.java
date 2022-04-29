package com.paanini.jiffy.vfs.io;


import javax.jcr.Property;

/**
 * Wrapper class for JCR property.
 * Adds information if property is alternative/old
 *
 * Alternative : this is fallback property and to be read if original does
 * not exists
 *
 * @author Priyanka Bhoir
 * @since 20/8/19
 */
public class PropertyWrapper {
  private Property property;
  private boolean isAlternate;

  public PropertyWrapper(Property property) {
    this.property = property;
    this.isAlternate = false;
  }

  public PropertyWrapper(Property property, boolean isAlternate) {
    this.property = property;
    this.isAlternate = isAlternate;
  }

  public Property getProperty() {
    return property;
  }

  public boolean isAlternate() {
    return isAlternate;
  }
}
