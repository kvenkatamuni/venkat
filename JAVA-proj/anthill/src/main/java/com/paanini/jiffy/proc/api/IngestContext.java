package com.paanini.jiffy.proc.api;

import com.option3.docube.schema.datasheet.meta.DataSheetSchema;
import com.paanini.jiffy.exception.DataProcessingException;
import com.paanini.jiffy.storage.DocumentStore;

import java.nio.file.Path;

public class IngestContext extends Context {
  private static final String DATASHEET_SCHEMA = "docube.ingest.schema";
  private static final String STAGED_LOCATION = "docube.ingest.staged.location";
  private static final String TIMESTAMPED_NAME = "docube.ingest.timestamp.name";
  private static final String INGEST_MODE = "docube.ingest.mode";
  private static final String CRLF_ENDING = "docube.src.crlf";

  public IngestContext(DocumentStore documentStore) {
    super(documentStore);
  }

  public DataSheetSchema getIngestedSchema(){
    return get(DATASHEET_SCHEMA);
  }

  public IngestContext setIngestedSchema(DataSheetSchema schema){
    set(DATASHEET_SCHEMA,schema);
    return this;
  }

  public Path getStagedFileLocation(){
    return get(STAGED_LOCATION);
  }

  public IngestContext setStagedFileLocation(Path path){
    set(STAGED_LOCATION,path);
    return this;
  }

  public String getTimeStampedName(){
    return get(TIMESTAMPED_NAME);
  }

  public IngestContext setTimeStampedName(String name){
    set(TIMESTAMPED_NAME,name);
    return this;
  }

  /**
   * Get the mode of ingestion, whether CREATE/UPDATE.
   * Invoke after the processing is complete.
   * @return
   */
  public String getIngestMode(){
    return get(INGEST_MODE);
  }

  /**
   * Set the ingested mode, to be used by the processor
   * @param mode
   * @return
   */
  public IngestContext setIngestMode(String mode){
    set(INGEST_MODE,mode);
    return this;
  }

  public IngestContext setCRLFEnding() {
    set(CRLF_ENDING,true);
    return this;
  }

  public boolean getCRLFEnding() {
    try {
      return get(CRLF_ENDING);
    } catch(DataProcessingException e ){
      return false;
    }
  }
}