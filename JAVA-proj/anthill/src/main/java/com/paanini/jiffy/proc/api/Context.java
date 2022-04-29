package com.paanini.jiffy.proc.api;

import com.option3.docube.schema.nodes.SourceType;
import com.option3.docube.schema.nodes.SubType;
import com.paanini.jiffy.constants.Common;
import com.paanini.jiffy.exception.DataProcessingException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.models.DataSheetHeader;
import com.paanini.jiffy.storage.DocumentStore;
import com.paanini.jiffy.vfs.api.DataSheetProps;
import org.springframework.beans.factory.annotation.Autowired;

import javax.print.Doc;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Context {
  public static final String ID = "docube.id";
  public static final String FILE_NAME = "docube.filename";
  public static final String INPUT_STREAM = "docube.input.stream";
  private static final String PARENT_ID = "docube.parent.id";
  private static final String HEADER = "docube.data-sheet.header";
  private static final String OUTPUT_STREAM = "docube.output.stream";
  private static final String SOURCE_TYPE = "docube.source.type";
  private static final String DATA_SHEET = "docube.data.sheet";
  private static final String TIMEZONE_OFFSET = "timezone.offset";
  private static final String DOCUBE_JAR_NAME = "docube-models-assembly-1.0";
  public static final String DOCUBE_CREATE_MODEL_JAR = "modelCreation";
  public static final String DOCUBE_GENERIC_JAR = "generic";
  public static final String DOCUBE_APPLY_MODEL_JAR = "modelApplication";
  private static final String PATH = "path";

  private final DocumentStore documentStore;
  public Context(DocumentStore documentStore){
    this.documentStore=documentStore;
  }

  private Map<String,Object> map = new HashMap<>();
  private ExecutionState state = new ExecutionState();

  <T> T get(String key){
    T t = (T) map.get(key);
    if(t == null){
      throw new DataProcessingException(key + " is null");
    }
    return t;
  }

  void set(String key, Object object){
    Object t = map.get(key);
    if(t != null){
      throw new DataProcessingException(
              "Value set already, cannot modify: " + key);
    }

    if(object == null){
      throw new DataProcessingException("Cannot set null objects into context");
    }

    map.put(key,object);
  }

  public String getId(){
    //return get(ID);
    return getDataSheet().getId();
  }
  public Optional<Long> getVersionNumber() {
    return getDataSheet().getCurrentVersionNumber();
  }

    /*public Context setId(String id){
        set(ID,id);
        return this;
    }*/

  public String getParentId(){
    return get(PARENT_ID);
  }

  public Context setParentId(String parentId){
    set(PARENT_ID,parentId);
    return this;
  }

  public String getPath() {
    return get(PATH);
  }

  public Context setPath(String fileName) {
    set(PATH,fileName);
    return this;
  }


  public String getFileName() {
    return get(FILE_NAME);
  }

  public Context setFileName(String fileName) {
    set(FILE_NAME,fileName);
    return this;
  }

  public SourceType getSourceType() {
    return SourceType.valueOf(get(SOURCE_TYPE));
  }

  public Context setSourceType(SourceType type) {
    set(SOURCE_TYPE,type.name());
    return this;
  }

  public Path getSourceFolder(){
    return getSourceFolder(getId());
  }

  public Path getSourceFolder(String id){
    return getFilePath(id).resolve("data");
  }

  // returns current parquet location
  public Path getDestinationFolder(){
    return getDestinationFolder(getId(), getVersionNumber());
  }

  // returns current parquet location
  public Path getDestinationFolder(String id, Optional<Long> versionNumber){
    //After spark execution completes, all output files will be marked with version -1
    String dest = versionNumber.isPresent() && versionNumber.get() != -1 ? String.valueOf(versionNumber.get()) : "latest";
    return getFilePath(id).resolve(Common.PARQUET).resolve(dest);
  }


  public Path getLatestFolderPath(String id) {
    return getFilePath(id).resolve(Common.PARQUET).resolve("latest");
  }
  // returns next parquet location, mainly use in publish to identify next file to write to.
  public Path getNextVersionFolder(String id) {
    return getDestinationFolder(id, Optional.of(getDataSheet().getNextVersionNumber()));
  }

  public Path getTempParquetPath(String id) {
    return getFilePath(id).resolve("temp");
  }

  public Path getDatasheetBasePath(String id) {
    return getFilePath(id);
  }

  public Path getDatasheetCurrentMonthBasePath(String id) {
    return getFilePath(id).resolve(Common.PARQUET)
            .resolve(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)))
            .resolve(String.valueOf(Calendar.getInstance().get(Calendar.MONTH)));
  }

  public Path getHistoryFolder(String id, long start, long end) {
    return getFilePath(id).resolve(Common.PARQUET)
            .resolve("history")
            .resolve(start + "-" + end);
  }


  public InputStream getInputStream(){
    return get(INPUT_STREAM);
  }

  public Context setInputStream(InputStream stream){
    set(INPUT_STREAM,stream);
    return this;
  }

  public OutputStream getOutputStream(){
    return get(OUTPUT_STREAM);
  }

  public Context setOutputStream(OutputStream stream){
    set(OUTPUT_STREAM,stream);
    return this;
  }

  public DataSheetHeader[] getHeader(){
    return get(HEADER);
  }

  public Context setHeader(DataSheetHeader[] header){
    set(HEADER,header);
    return this;
  }

  public DataSheetProps getDataSheet(){
    return get(DATA_SHEET);
  }

  public Optional<DataSheetProps> findDataSheet(){
    try{
      return Optional.of(get(DATA_SHEET));
    }catch(DataProcessingException e){
      return Optional.empty();
    }

  }

  public Context setDataSheet(DataSheetProps dataSheet){
    set(DATA_SHEET,dataSheet);
    return this;
  }

  public Path getFilePath(String path){
    return documentStore.getFileSystem().getPath(path);
  }

  public ExecutionState getState() {
    return state;
  }

  public long getTimezoneOffsetInSeconds(){
    try{
      return get(TIMEZONE_OFFSET);
    }catch(DataProcessingException e){
      return 0;
    }
  }

  public Context setTimezoneOffsetInSeconds(long offset){
    set(TIMEZONE_OFFSET,offset);
    return this;
  }

  public Path getDocubeAlgorithmJar() {
    return getDocubeAlgorithmJar("", "");
  }

  public Path getDocubeAlgorithmJar(String algorithmType) {
    return getDocubeAlgorithmJar(algorithmType, "");
  }

  public Path getDocubeAlgorithmJar(String algorithmType, String version) {
    String jarName  = DOCUBE_JAR_NAME;
    if(SubType.modelCreation.toString().equals(algorithmType)) {
      jarName = DOCUBE_CREATE_MODEL_JAR;
    } else if(SubType.modelApplication.toString().equals(algorithmType)) {
      jarName = DOCUBE_APPLY_MODEL_JAR;
    } else if(SubType.generic.toString().equals(algorithmType)) {
      jarName = DOCUBE_GENERIC_JAR;
    }

    jarName = version.isEmpty() ? jarName.concat(".jar") : jarName.concat("-").concat(version).concat(".jar");

    return Paths.get(documentStore.getSparkFilePath()).resolve(jarName);
  }

}