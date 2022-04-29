package com.paanini.jiffy.services;

import com.option3.docube.schema.datasheet.meta.Column;
import com.option3.docube.schema.jiffytable.AutoPopulateSettings;
import com.option3.docube.schema.jiffytable.ColumnDetails;
import com.option3.docube.schema.jiffytable.Form;
import com.option3.docube.schema.jiffytable.FormColumn;
import com.option3.docube.schema.nodes.DatasheetSchema;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.models.ContextDetails;
import com.paanini.jiffy.models.SqlSource;
import com.paanini.jiffy.utils.RoleManager;
import com.paanini.jiffy.utils.VfsManager;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.DataSheet;
import com.paanini.jiffy.vfs.files.JiffyTable;
import com.paanini.jiffy.vfs.files.Presentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ContentService {

  @Autowired
  private VfsManager vfsManager;

  @Autowired
  private RoleManager roleManager;

  public <T extends Persistable> T getFileFromParent(String parentId,String name){
    return vfsManager.getFileFromParentId(parentId,name);
  }

  public <T extends Persistable> T getFileFromRelativePath(String baseId, String path){
    return vfsManager.getFileFromRelativePath(baseId,path);
  }

  public <T extends Persistable> T getFile(String id){
    return vfsManager.getFileProperties(id);
  }

  public <T extends Persistable> T getReferencedFileFromId(String id,String presentationId){
    Presentation presentation = vfsManager.getFile(presentationId);
    boolean filePresent = presentation.getContent().getDatasheets()
            .stream()
            .anyMatch(e -> e.getId().equals(id));
      return vfsManager.elevateAndGetFile(id,filePresent);
  }

  public void markPublished(String id){
    vfsManager.markPublished(id);
  }

  public void deleteFile(String id){
    vfsManager.deleteFile(id);
  }

  public <T extends Persistable> T updateFile(T file){
    T t = vfsManager.updateGeneric(file);
    if(((BasicFileProps) t).getType().equals(Type.JIFFY_TABLE)) {
      roleManager.upsertJiffyTableDependency((JiffyTable) file);
    }
    return t;
  }

  public <T extends Persistable> T createFile(T file,String parentId){
    T t = vfsManager.saveGeneric(file, parentId);
    if(((BasicFileProps) t).getType().equals(Type.JIFFY_TABLE)) {
      vfsManager.logJiffyTableDetails((JiffyTable) t);
    }
    return t;
  }

  public <T extends Persistable> T upsertFile(T file,String parentId){
    if(vfsManager.isFilePresent(file.getValue("name").toString(),parentId)){
      T name = vfsManager.getFileFromParentId(parentId, file.getValue("name").toString());
      ((BasicFileProps)file).setId(((BasicFileProps)name).getId());
      Type type = ((BasicFileProps) name).getType();
      if(type.equals(Type.DATASHEET) || type.equals(Type.SQL_DATASHEET)){
        ((DatasheetSchema)file).setVersionNumber(((DatasheetSchema)name).getVersionNumber());
      }
      Type incomingtype = ((BasicFileProps) file).getType();
      if(incomingtype.equals(type))
        return vfsManager.updateGeneric(file);
      else
        return vfsManager.saveGeneric(file,parentId);
    }else{
      T t = vfsManager.saveGeneric(file, parentId);
      if(((BasicFileProps) t).getType().equals(Type.JIFFY_TABLE)){
        vfsManager.logJiffyTableDetails((JiffyTable)t);
      }
      return t;
    }
  }

  public DataSheet saveSqlSource(SqlSource sqlSource){
    return vfsManager.saveSqlSource(sqlSource.getName(),
            sqlSource.getParentId(),sqlSource.getQueryString());
  }

  public void savePublishHeaders(String id, List<Column> header){
    vfsManager.savePublishHeaders(id,header);
  }

  public String getDataPath(String id){
    return vfsManager.getDataPath(id).toString();
  }

  public <T extends Persistable>T getReferencedFile(String appPath,String name,String pId) throws ProcessingException {
    return vfsManager.elevateAndGetFileByPath(appPath,name,false);
  }

  public <T extends Persistable> T getJT(String appPath, ContextDetails contextDetails){
    JiffyTable referencedFile = getReferencedFile(appPath,contextDetails.getTableName(),contextDetails.getPresentationId());
    Optional<Form> first = referencedFile.getForms().stream()
            .filter(form -> form.getName().equals(contextDetails.getFormName())).findFirst();
    if(first.isPresent()){
      Form form = first.get();
      Optional<FormColumn> formColumn = form.getColumnSettings().stream()
              .filter(cs -> cs.getTableReference().equals(contextDetails.getTableReference())).findFirst();
      if(formColumn.isPresent()){
        Optional<ColumnDetails> columnDe = formColumn.get().getColumnDetails().stream()
                .filter(columnDetails -> columnDetails.getName().equals(contextDetails.getColumnName())).findFirst();
        if(columnDe.isPresent()){
          AutoPopulateSettings autoPopulateSettings = columnDe.get().getAutoPopulateSettings();
          boolean equals = autoPopulateSettings.getLookupTableName().equals(contextDetails.getLookupTableName());
          return vfsManager.elevateAndGetFileByPath(appPath,contextDetails.getLookupTableName(),equals);
        }else{
          throw new ProcessingException("Column Details not present in Jiffy Table");
        }
      }else {
        throw new ProcessingException("Table Reference"+contextDetails.getTableReference()+"not present in jiffy table");
      }
    }else {
      throw new ProcessingException("form "+contextDetails.getFormName()+" not present in the Jiffy table");
    }
  }
}
