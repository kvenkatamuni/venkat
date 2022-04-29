package com.paanini.jiffy.proc.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.paanini.jiffy.exception.DataProcessingException;
import com.paanini.jiffy.storage.DocumentStore;
import com.paanini.jiffy.utils.VfsManager;
import org.springframework.beans.factory.annotation.Autowired;

import javax.json.JsonArray;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by appmgr on 28/4/16.
 */
public class QueryContext extends  AbstractProcessContext{

  private static final String JSON_HEADER = "docube.json.header";
  private static final String JSON_GENERATOR = "docube.json.generator";
  private static final String REFERRER = "docube.query.referrer";
  private static final String HISTORY_START_TIME = "docube.history.start";
  private static final String HISTORY_END_TIME = "docube.history.end";
  private static final String JIFFY_TABLE = "docube.jiffy.table";
  private static final String JIFFY_TABLE_DB = "docube.jiffy.db";
  private static final String JIFFY_TABLE_PLUGIN = "docube.jiffy.drill.plugin";

  private List<String> splitBy;
  private List<String> userRoles;
  private boolean isAggregatePublishedDatasheet = false;
  private String configName;
  private String schema;

  public QueryContext(DocumentStore documentStore) {
    super(documentStore);
  }


  public JsonGenerator getJsonGenerator(){
    return get(JSON_GENERATOR);
  }

  public Optional<JsonGenerator> findJsonGenerator(){
    try {
      return Optional.of(get(JSON_GENERATOR));
    }catch(DataProcessingException e){
      return Optional.empty();
    }
  }

  public QueryContext setJsonGenerator(JsonGenerator generator){
    set(JSON_GENERATOR,generator);
    return this;
  }
  //There is another Json Header in the base class. Required?
  @Deprecated
  public JsonArray getJsonHeader(){
    return get(JSON_HEADER);
  }

  //There is another Json Header in the base class. Required?
  @Deprecated
  public QueryContext setJsonHeader(JsonArray ary){
    set(JSON_HEADER,ary);
    return this;
  }


  //=============================== Pivot integration =================================

  public Optional<String> getReferrer(){
    try{
      return Optional.of(get(REFERRER));
    } catch(DataProcessingException e){
      return Optional.empty();
    }
  }

  public QueryContext setReferrer(String ref){
    set(REFERRER,ref);
    return this;
  }

  public void setSplitBy(List<String> splitBy) {
    this.splitBy = splitBy;
  }

  public Optional<List<String>> getSplitBy(){
    try{
      if(splitBy == null) {
        return Optional.empty();
      }
      return Optional.of(splitBy);
    }catch(DataProcessingException e){
      return Optional.empty();
    }

  }

  public void setUserRoles(List<String> userRoles) {
    this.userRoles = userRoles;
  }

  public List<String> getUserRoles(){
    try{
      if(userRoles == null) {
        return new ArrayList<>();
      }
      return userRoles;
    }catch(DataProcessingException e){
      return new ArrayList<>();
    }

  }

  public void setPublishAggregation(boolean aggregatePublishedDatasheet) {
    this.isAggregatePublishedDatasheet = aggregatePublishedDatasheet;
  }

  public boolean isPublishAggregation() {
    return isAggregatePublishedDatasheet;
  }

  public void setHistoricDataStart(long time) {
    this.set(HISTORY_START_TIME, time);
  }

  public void setHistoricDataEnd(long time) {
    this.set(HISTORY_END_TIME, time);
  }

  public Optional<Long> getHistoricDataStart() {
    try{
      return Optional.of(get(HISTORY_START_TIME));
    } catch(DataProcessingException e){
      return Optional.empty();
    }
  }

  public Optional<Long> getHistoricDataEnd() {
    try{
      return Optional.of(get(HISTORY_END_TIME));
    } catch(DataProcessingException e){
      return Optional.empty();
    }
  }

  public String getConfigName() {
    return configName;
  }

  public void setConfigName(String configName) {
    this.configName = configName;
  }

  public String getSchema() {
    return schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  public String getMongoPluginName() {
    return get(JIFFY_TABLE_PLUGIN);
  }

  public QueryContext setMongoPluginName(String pluginName) {
    set(JIFFY_TABLE_PLUGIN, pluginName);
    return this;
  }

  public String getJiffyTableDb() {
    return get(JIFFY_TABLE_DB);
  }

  public QueryContext setJiffyTableDb(String dbName) {
    set(JIFFY_TABLE_DB, dbName);
    return this;
  }

}