package com.paanini.jiffy.vfs.api;


import com.option3.docube.schema.datasheet.meta.DataSheetSchema;
import com.option3.docube.schema.nodes.Mode;
import com.option3.docube.schema.nodes.SourceType;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Priyanka Bhoir
 * @since 18/8/19
 */
public interface DataSheetProps extends BasicFileProps{
  /**
   * Gets the value of the 'mode' field.
   */
  public Mode getMode();

  /**
   * Sets the value of the 'mode' field.
   * @param value the value to set.
   */
  public void setMode(Mode value);

  /**
   * Gets the value of the 'publishSchemaVersion' field.
   */
  public java.lang.String getPublishSchemaVersion();

  /**
   * Sets the value of the 'publishSchemaVersion' field.
   * @param value the value to set.
   */
  public void setPublishSchemaVersion(java.lang.String value);

  /**
   * Gets the value of the 'sourceSchemaVersion' field.
   */
  public java.lang.String getSourceSchemaVersion();
  /**
   * Sets the value of the 'sourceSchemaVersion' field.
   * @param value the value to set.
   */
  public void setSourceSchemaVersion(java.lang.String value);

  /**
   * Gets the value of the 'datasheetSourceSchema' field.
   */
  public DataSheetSchema getDatasheetSourceSchema();

  /**
   * Sets the value of the 'datasheetSourceSchema' field.
   * @param value the value to set.
   */
  public void setDatasheetSourceSchema(DataSheetSchema value) ;

  /**
   * Gets the value of the 'datasheetPublishedSchema' field.
   */
  public DataSheetSchema getDatasheetPublishedSchema();

  /**
   * Sets the value of the 'datasheetPublishedSchema' field.
   * @param value the value to set.
   */
  public void setDatasheetPublishedSchema(DataSheetSchema value);
  /**
   * Gets the value of the 'schemaSuggestionAvailable' field.
   */
  public java.lang.Boolean getSchemaSuggestionAvailable() ;
  /**
   * Sets the value of the 'schemaSuggestionAvailable' field.
   * @param value the value to set.
   */
  public void setSchemaSuggestionAvailable(java.lang.Boolean value);

  /**
   * Gets the value of the 'sourceSql' field.
   */
  public java.lang.String getSourceSql();

  /**
   * Sets the value of the 'sourceSql' field.
   * @param value the value to set.
   */
  public void setSourceSql(java.lang.String value);
  /**
   * Gets the value of the 'tableName' field.
   */
  public java.lang.String getTableName();

  /**
   * Sets the value of the 'tableName' field.
   * @param value the value to set.
   */
  public void setTableName(java.lang.String value);

  /**
   * Gets the value of the 'CRLFEnding' field.
   */
  public java.lang.Boolean getCRLFEnding();

  /**
   * Sets the value of the 'CRLFEnding' field.
   * @param value the value to set.
   */
  public void setCRLFEnding(java.lang.Boolean value) ;

  /**
   * Gets the value of the 'sourceType' field.
   */
  public SourceType getSourceType();

  /**
   * Sets the value of the 'sourceType' field.
   * @param value the value to set.
   */
  public void setSourceType(SourceType value);

  /**
   * Gets the value of the 'versionNumber' field.
   */
  public java.lang.Long getVersionNumber();

  /**
   * Sets the value of the 'versionNumber' field.
   * @param value the value to set.
   */
  public void setVersionNumber(java.lang.Long value);

  /**
   * Gets the value of the 'source' field.
   */
  public java.lang.String getSource();

  /**
   * Sets the value of the 'source' field.
   * @param value the value to set.
   */
  public void setSource(java.lang.String value);

  public Optional<Long> getCurrentVersionNumber();

  public Long getNextVersionNumber();

  public DataSheetSchema getCurrentSchema();
}
