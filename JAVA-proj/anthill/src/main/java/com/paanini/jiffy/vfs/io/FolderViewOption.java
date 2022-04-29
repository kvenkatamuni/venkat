package com.paanini.jiffy.vfs.io;

import com.paanini.jiffy.dto.QueryOptions;
import com.paanini.jiffy.jcrquery.readers.JCRQuery;
import com.paanini.jiffy.jcrquery.readers.impl.SimpleJCRQuery;


/**
 * Created by Priyanka Bhoir on 31/7/19
 */
public class FolderViewOption {
  private static final int DEFAULT_DEPTH = 1;
  private final QueryOptions queryOptions;
  private JCRQuery JCRQuery;
  private boolean softread = true;

  public enum ReadAs {
    DOCUBE_FILE, BASIC_FILE
  }
  private int depth;
  private ReadAs view;

  public FolderViewOption(int depth, ReadAs view) {
    this(depth, view, new QueryOptions(), null);
  }

  public FolderViewOption(int depth, ReadAs view, QueryOptions options) {
    this(depth, view, options, null);
  }

  public FolderViewOption(int depth, ReadAs view, QueryOptions options, JCRQuery JCRQuery) {
    this.depth = depth;
    this.view = view;
    this.queryOptions = options;
    this.JCRQuery = JCRQuery;
  }

  public FolderViewOption(int depth) {
    this(depth, ReadAs.DOCUBE_FILE, new QueryOptions(), null);
  }

  public static FolderViewOption getDefaultOptions(){
    return new FolderViewOption(DEFAULT_DEPTH, ReadAs.DOCUBE_FILE);
  }

  /**
   * Reads just current file
   * @return
   */
  public static FolderViewOption getFileOptions(){
    return new FolderViewOption(0, ReadAs.DOCUBE_FILE);
  }

  public static FolderViewOption getMinimumOption(){
    return new FolderViewOption(1, ReadAs.BASIC_FILE);
  }

  /**
   * returns a option to view app without JQL query
   * @return
   */
  public static FolderViewOption getWithoutJCR(QueryOptions queryOptions){
    return new FolderViewOption(1, ReadAs.BASIC_FILE, queryOptions,
            new SimpleJCRQuery(queryOptions.getFilterCharacter()));
  }

  public static FolderViewOption getAppGroupWithoutJCR(QueryOptions queryOptions){
    return new FolderViewOption(2, ReadAs.BASIC_FILE, queryOptions,
            new SimpleJCRQuery(queryOptions.getFilterCharacter()));
  }

  public void setSoftread(boolean softread) {
    this.softread = softread;
  }

  public boolean isSoftread() {
    return softread;
  }

  public int getDepth() {
    return depth;
  }

  public FolderViewOption setDepth(int depth) {
    this.depth = depth;
    return this;
  }

  public ReadAs getView() {
    return view;
  }

  public FolderViewOption setView(ReadAs view) {
    this.view = view;
    return this;
  }

  public QueryOptions getQueryOptions() {
    return queryOptions;
  }

  public JCRQuery getJCRQuery() {
    return JCRQuery;
  }

  public FolderViewOption setJCRQuery(JCRQuery JCRQuery) {
    this.JCRQuery = JCRQuery;
    return this;
  }
}
