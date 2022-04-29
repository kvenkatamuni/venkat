package com.paanini.jiffy.proc.api;

import com.paanini.jiffy.storage.DocumentStore;

public class CustomFileContext extends Context {
  public static final String CUSTOM_ID = "docube.custom.id";

  public CustomFileContext(DocumentStore documentStore) {
    super(documentStore);
  }

  public String getCustomId(){
    return get(CUSTOM_ID);
  }

  public Context setCustomId(String id){
    set(CUSTOM_ID,id);
    return this;
  }

  public String getId(){
    return getCustomId();
  }
}
