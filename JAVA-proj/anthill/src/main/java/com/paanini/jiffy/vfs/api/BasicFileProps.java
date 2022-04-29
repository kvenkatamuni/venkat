package com.paanini.jiffy.vfs.api;

import com.option3.docube.schema.nodes.SubType;

public interface BasicFileProps {

  /**
   * Gets the value of the 'subType' field.
   */
  SubType getSubType();

  /**
   * Sets the value of the 'subType' field.
   * @param value the value to set.
   */
  void setSubType(SubType value);

  /**
   * Gets the value of the 'type' field.
   */
  com.option3.docube.schema.nodes.Type getType();

  /**
   * Sets the value of the 'type' field.
   * @param value the value to set.
   */
  void setType(com.option3.docube.schema.nodes.Type value);

  /**
   * Gets the value of the 'color' field.
   */
  String getColor();

  /**
   * Sets the value of the 'color' field.
   * @param value the value to set.
   */
  void setColor(String value);

  /**
   * Gets the value of the 'createdBy' field.
   */
  String getCreatedBy();

  /**
   * Sets the value of the 'createdBy' field.
   * @param value the value to set.
   */
  void setCreatedBy(String value);

  /**
   * Gets the value of the 'owner' field.
   */
  String getOwner();

  /**
   * Sets the value of the 'owner' field.
   * @param value the value to set.
   */
  void setOwner(String value);

  /**
   * Gets the value of the 'status' field.
   */
  com.option3.docube.schema.nodes.Status getStatus();

  /**
   * Sets the value of the 'status' field.
   * @param value the value to set.
   */
  void setStatus(com.option3.docube.schema.nodes.Status value);

  /**
   * Gets the value of the 'scheduled' field.
   */
  Boolean getScheduled();

  /**
   * Sets the value of the 'scheduled' field.
   * @param value the value to set.
   */
  void setScheduled(Boolean value);

  /**
   * Gets the value of the 'encrypted' field.
   */
  Boolean getEncrypted();

  /**
   * Sets the value of the 'encrypted' field.
   * @param value the value to set.
   */
  void setEncrypted(Boolean value);

  /**
   * Gets the value of the 'description' field.
   */
  String getDescription();

  /**
   * Sets the value of the 'description' field.
   * @param value the value to set.
   */
  void setDescription(String value);

  /**
   * Gets the value of the 'lastError' field.
   */
  String getLastError();

  /**
   * Sets the value of the 'lastError' field.
   * @param value the value to set.
   */
  void setLastError(String value);

  /**
   * Gets the value of the 'createdAt' field.
   */
  Long getCreateAt();

  /**
   * Sets the value of the 'createdAt' field.
   * @param value the value to set.
   */
  void setCreateAt(Long value);

  /**
   * Gets the value of the 'lastModified' field.
   */
  Long getLastModified();

  /**
   * Sets the value of the 'lastModified' field.
   * @param value the value to set.
   */
  void setLastModified(Long value);

  /**
   * Gets the value of the 'name' field.
   */
  String getName();

  /**
   * Sets the value of the 'name' field.
   * @param value the value to set.
   */
  void setName(String value);

  /**
   * Gets the value of the 'id' field.
   */
  String getId();

  /**
   * Sets the value of the 'id' field.
   * @param value the value to set.
   */
  void setId(String value);
}