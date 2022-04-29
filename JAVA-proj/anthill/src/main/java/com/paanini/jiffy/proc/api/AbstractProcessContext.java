package com.paanini.jiffy.proc.api;

import com.paanini.jiffy.exception.DataProcessingException;
import com.paanini.jiffy.storage.DocumentStore;
import com.paanini.jiffy.utils.Permission;

import java.util.Optional;

public class AbstractProcessContext extends Context {

  private static final String IS_RAW_QUERY = "docube.query.is.raw";
  private static final String SOURCE_QUERY = "docube.query.on.source";
  private static final String BASE_DATASHEET_PERMISSION = "docube.query.base.datasheet.permission";
  private static final String QUERY_SPEC = "docube.query.spec";
  private static final String BASE_FOLDER = "docube.query.base.folder";
  private static final String RAW_QUERY = "docube.raw.source.query";
  private static final String FILTER = "docube.datasheet.filter";
  private static final String QUERY_FORCE_RESULT_LIMIT = "docube.query.force.result.limit";
  private static final String IS_CACHEABLE_QUERY = "docube.query.is.cacheable";

  public AbstractProcessContext(DocumentStore documentStore) {
    super(documentStore);
  }

  /**
   * @return true if the processing has to happen on the user specified sql
   */
  public boolean isQueriedRaw(){
    try {
      return get(IS_RAW_QUERY);
    }catch(DataProcessingException e){
      return false;
    }
  }

  public Context setQueriedRaw(boolean sourceQuery){
    set(IS_RAW_QUERY,sourceQuery);
    return this;
  }

  /**
   * @return true if the processing has to happen on the user specified sql
   */
  public boolean isQueriedOnSource(){
    try {
      return get(SOURCE_QUERY);
    }catch(DataProcessingException e){
      return false;
    }
  }

  public Context setQueriedOnSource(boolean sourceQuery){
    set(SOURCE_QUERY,sourceQuery);
    return this;
  }

  /**
   *
   * @return Permission : which permission is required to access the datasheet
   */
  public String getBaseDatasheetPermission() {
    try {
      return get(BASE_DATASHEET_PERMISSION);
    } catch(DataProcessingException e){
      //minimum view permisssion is needed to access any datasheet
      return Permission.DOCUBE_READ;
    }
  }

  /**
   * Sets permisssion to access datasheet, permission may change according to operation, ex - create query need Write permission
   * @param p Permission
   * @return
   */
  public Context setBaseDatasheetPermission(String p) {
    set(BASE_DATASHEET_PERMISSION, p);
    return this;
  }

  public String getBaseFolderId(){
    try {
      return get(BASE_FOLDER);
    }catch(DataProcessingException e) {
      return "";
    }
  }

  public AbstractProcessContext setBaseFolderId(String base){
    set(BASE_FOLDER,base);
    return this;
  }

  public String getQuerySpec(){
    return get(QUERY_SPEC);
  }

  public Optional<String> findQuerySpec(){
    try {
      return Optional.of(get(QUERY_SPEC));
    }catch(DataProcessingException e){
      return Optional.empty();
    }
  }

  public AbstractProcessContext setQuerySpec(String filter){
    set(QUERY_SPEC,filter);
    return this;
  }

  public String getRawQuery(){
    return get(RAW_QUERY);
  }

  public AbstractProcessContext setRawQuery(String table){
    set(RAW_QUERY,table);
    return this;
  }

  public AbstractProcessContext setFilter(String filter) {
    set(FILTER,filter);
    return this;
  }

  public String getFilter() {
    return get(FILTER);
  }


  /**
   * Force a limit on the number of records returned.
   * @param limit
   * @return
   */
  public AbstractProcessContext setQueryLimit(long limit){
    set(QUERY_FORCE_RESULT_LIMIT, limit);
    return this;
  }

  /**
   * Find the limit on query result
   * @return
   */
  public Optional<Long> findQueryLimit(){
    try {
      return Optional.of(get(QUERY_FORCE_RESULT_LIMIT));
    }catch(DataProcessingException e){
      return Optional.empty();
    }
  }


  /**
   * @return true if the processing has to happen on the user specified sql
   */
  public boolean isCacheableQuery(){
    try {
      return get(IS_CACHEABLE_QUERY);
    }catch(DataProcessingException e){
      return false;
    }
  }

  public Context setCacheableQuery(boolean isCacheable){
    set(IS_CACHEABLE_QUERY, isCacheable);
    return this;
  }


}
