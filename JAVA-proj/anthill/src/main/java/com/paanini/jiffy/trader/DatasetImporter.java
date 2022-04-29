package com.paanini.jiffy.trader;

import com.option3.docube.schema.jiffytable.DataSchema;
import com.option3.docube.schema.jiffytable.Form;
import com.paanini.jiffy.exception.DocubeException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.models.ImportAppOptions;
import com.paanini.jiffy.models.Summary;
import com.paanini.jiffy.models.Summary.Status;
import com.paanini.jiffy.models.TradeFile;
import com.paanini.jiffy.services.MangroveService;
import com.paanini.jiffy.utils.VfsManager;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.Folder;
import com.paanini.jiffy.vfs.files.JiffyTable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
@Service
public class DatasetImporter {

  @Autowired
  VfsManager vfsManager;

  @Autowired
  MangroveService mangroveService;

  private static Logger logger = LoggerFactory.getLogger(DatasetImporter.class);

  /**
   * JiffyTable are migrated before importing. There will be no change to
   * other children
   * @param existingApp
   * @param result
   * @param statusBiConsumer
   * @return
   */
  public Folder importDatasets(Folder existingApp, Persistable result,
      BiConsumer<Persistable, TradeFile> statusBiConsumer,
      ImportAppOptions importAppOptions, String fileId) {


    List<Persistable> children = new ArrayList<>();
    Folder importApp = (Folder) result;
    List<JiffyTable> importedTables = getJiffyTablesInApp(importApp);
    List<Persistable> otherImportedFiles = getOtherChildren(importApp);
    children.addAll(otherImportedFiles);
    if(Objects.nonNull(importAppOptions)){
      if(!importAppOptions.isFullImport() && !importAppOptions.getNewApp()){
        importApp.setChildren(children);
        return importApp;
      }
    }
    List<JiffyTable> existingTables = getJiffyTablesInApp(existingApp);
    List<String> existingTableNames = getJiffyTableNames(existingTables);

    List<JiffyTable> nonExistingTables = new ArrayList<>();
    List<JiffyTable> tablesToMigrate = getJiffyTables(statusBiConsumer, fileId, importedTables, existingTableNames, nonExistingTables);
    children.addAll(nonExistingTables);
    migrateJiffyTables(existingTables, tablesToMigrate, statusBiConsumer, fileId);
    importApp.setChildren(children);
    return importApp;
  }

  private List<JiffyTable> getJiffyTables(BiConsumer<Persistable, TradeFile> statusBiConsumer, String fileId, List<JiffyTable> importedTables, List<String> existingTableNames, List<JiffyTable> nonExistingTables) {
    List<JiffyTable> tablesToMigrate = new ArrayList<>();
    for (JiffyTable table : importedTables) {
      if (!existingTableNames.contains(table.getName())) {
        try{
          JiffyTable schemaImportedTable = importTableSchema(((Persistable)table), fileId);
          if(Objects.nonNull(schemaImportedTable.getTableName()) &&
              !schemaImportedTable.getTableName().isEmpty()){
            nonExistingTables.add(schemaImportedTable);
          }else{
            String errorMessage = String.format("Object %s has not got imported " +
                    "as the schema import has failed!", schemaImportedTable.getName());
            statusBiConsumer.accept(schemaImportedTable, new TradeFile(schemaImportedTable.getName(),
                    true , Status.Error.name(), errorMessage));
          }

        }catch (Exception e){
          logger.error("Failed to import table schema");
        }
      } else {
        tablesToMigrate.add(table);
      }
    }
    return tablesToMigrate;
  }

  private JiffyTable importTableSchema(Persistable table, String fileId) {
      try{
        return mangroveService.importTableSchema(table, fileId);
      }catch (Exception e){
        logger.error("failed to import table schema {} {}", e.getMessage(), e);
        throw new ProcessingException(e.getMessage());
      }
  }

  private void migrateJiffyTables(List<JiffyTable> existingTables, List<JiffyTable> importedTables,
      BiConsumer<Persistable, TradeFile> statusBiConsumer, String fileId) {
    for (JiffyTable currentTable : existingTables) {
      JiffyTable updatedTable = getUpdatedJiffyTable(
          currentTable.getName(), importedTables);
      if (Objects.nonNull(updatedTable)) {
        if (!isConflict(currentTable, updatedTable)) {
          //if schema ids are same
          statusBiConsumer.accept(currentTable, new TradeFile(currentTable.getName(),
              true , Status.OverWritten.name(), ""));
        }else if (mangroveService.isSchemasValid(currentTable, updatedTable, fileId)) {
          migrateJiffyTable(currentTable, updatedTable, statusBiConsumer);
        } else {
          String errorMessage = String.format("Object %s has not got imported due " +
              "to schema conflict", currentTable.getName());
          statusBiConsumer.accept(currentTable, new TradeFile(currentTable.getName(),
              true , Summary.Status.Error.name(), errorMessage));
        }
      }
    }
  }

  private void migrateJiffyTable(JiffyTable current, JiffyTable updated,
      BiConsumer<Persistable, TradeFile> statusBiConsumer) {

    try {
      DataSchema currentSchema = updated.getCurrentSchema();
      currentSchema.setStatus(com.option3.docube.schema.jiffytable.Status.DRAFT);
      current.setCurrentSchema(currentSchema);
      mangroveService.migrate(current);
    } catch (DocubeException e) {
      statusBiConsumer.accept(current, new TradeFile(current.getName(), true ,
          Status.Error.name(), e.getMessage()));
      return;
    }

    JiffyTable table = vfsManager.getFile(current.getId());
    table.setForms(getMigratedForms(current, updated));
    try{
      //comparing the form version of existing and new table and setting the latest version of form to the imported table
      logger.debug("[DI] updating form version for table {}", current.getName());
      table.setFormsVersion(getLatestFormsVersion(current.getFormsVersion(), updated.getFormsVersion()));
    }catch (Exception e){
      logger.warn("[DI] Error occured while extracting form version number, Setting to updated form's version {}", e);
      table.setFormsVersion(updated.getFormsVersion());
    }
    vfsManager.updateGeneric(table);

    //======================= Fancy ==========================
    statusBiConsumer.accept(current, new TradeFile(current.getName(), true ,
        Status.OverWritten.name(), ""));
    //========================================================
  }

  private String getLatestFormsVersion(String existingFormVersion, String updateFormVersion) {
    logger.debug("[DI] Fetching latest forms version");
    String latestFormVersion = null;
    if(getFormVersionNumber(existingFormVersion) > getFormVersionNumber(updateFormVersion)){
      latestFormVersion= existingFormVersion;
    }else {
      latestFormVersion = updateFormVersion;
    }
    logger.debug("[DI] Setting latest form version to {} ", latestFormVersion);
    return latestFormVersion;
  }

  private int getFormVersionNumber(String existingFormVersion) {
    return Integer.parseInt(existingFormVersion.substring(1,existingFormVersion.length()));
  }

  /**
   * returns new imported table for migration if the table with same name is present
   * in the existing App
   * @param existingTableName
   * @param importedTables
   * @return
   */
  private JiffyTable getUpdatedJiffyTable(String existingTableName,
      List<JiffyTable> importedTables) {
    for (JiffyTable table : importedTables) {
      if (table.getName().equals(existingTableName)) {
        return table;
      }
    }
    return null;
  }


  /**
   * If the current schema Id is same or the table is meta table there wont be conflicts
   * @param current
   * @param updated
   * @return
   */
  private boolean isConflict(JiffyTable current, JiffyTable updated) {
    //Migration should not be done if currentschema id is same
    if (current.getCurrentSchema().getId().equals(
        updated.getCurrentSchema().getId())) {
      return false;
    }

    //Meta tables : category, pseudonyms, accuracy cannot be migrated
    if (isMeta(current.getName())) {
      return false;
    }

    return true;
  }


  /**
   * Check if the jiffytable is valid for migration
   * @param current
   * @param updated
   * @return
   */
  private boolean isValidSchemas(JiffyTable current, JiffyTable updated) {

    List<String> updatedSchemaIds = updated.getSchemas()
        .stream()
        .map(schema -> schema.getId())
        .collect(Collectors.toList());
    for (String schemaId : updatedSchemaIds) {
      if (Objects.isNull(schemaId) || schemaId.trim().isEmpty()) {
        return false;
      }
    }

    List<String> existingSchemaIds = current.getSchemas().stream()
        .map(schema -> schema.getId())
        .collect(Collectors.toList());

    //if the current schema id of the existing table is present in list of schema ids
    //of the updated table(table which is getting imported) then do the migration.
     return updatedSchemaIds.contains(existingSchemaIds.get(existingSchemaIds.size()-1));
  }

  private List<DataSchema> getSchemasToAdd(JiffyTable current, JiffyTable updated) {
    List<DataSchema> schemas = new ArrayList<>();
    String existingSchemaId = current.getCurrentSchema().getId();
    List<String> jiffySchemaIds = current.getSchemas().stream()
        .map(dataSchema -> dataSchema.getId())
        .collect(Collectors.toList());

    List<DataSchema> currentSchemas = current.getSchemas();
    schemas.addAll(currentSchemas);

    List<DataSchema> updatedSchemas = updated.getSchemas()
        .stream()
        .skip(jiffySchemaIds.indexOf(existingSchemaId) + 1)
        .collect(Collectors.toList());
    schemas.addAll(updatedSchemas);

    return schemas.subList(0, schemas.size()-1);
  }

  private List<Persistable> getOtherChildren(Folder folder) {
    return folder.getChildren()
        .stream()
        .filter(child -> !(child instanceof JiffyTable))
        .collect(Collectors.toList());
  }

  private List<JiffyTable> getJiffyTablesInApp(Folder folder) {
    return folder.getChildren()
        .stream()
        .filter(child -> child instanceof JiffyTable)
        .map(child -> (JiffyTable) child)
        .collect(Collectors.toList());
  }

  private List<String> getJiffyTableNames(List<JiffyTable> tables) {
    return tables.stream()
        .map(child -> child.getName())
        .collect(Collectors.toList());
  }

  private boolean isMeta(String name) {
    if (name.endsWith(JiffyTable.ACCURACY_SUFFIX) || name.endsWith(JiffyTable.CATEGORY_SUFFIX) ||
        name.endsWith(JiffyTable.PSEUDONYMS_SUFFIX)) {
      return true;
    }
    return false;
  }

  /**
   * Current JiffyTable is destination & Incoming Jiffytable is source
   * @param current
   * @param incoming
   * @return
   */
  private List<Form> getMigratedForms(JiffyTable current, JiffyTable incoming) {
    List<Form> currentForms = current.getForms();
    List<Form> incomingForms = incoming.getForms();
    List<Form> updatedForms = new ArrayList<>();
    updatedForms.addAll(updateCurrentForms(currentForms, incomingForms));
    updatedForms.addAll(getNewForms(incomingForms, updatedForms));
    return updatedForms;
  }
  /**
   * Get the new forms
   * @param incomingForms
   * @param updatedForms
   * @return
   */
  private List<Form> getNewForms(List<Form> incomingForms, List<Form> updatedForms) {
    List<Form> newForms = new ArrayList<>();
    List<String> updatedFormNames = updatedForms
        .stream()
        .map(form -> form.getName())
        .collect(Collectors.toList());
    incomingForms.forEach(form -> {
      if (!updatedFormNames.contains(form.getName())) {
        newForms.add(form);
      }
    });
    return newForms;
  }
  /**
   * Get the updated forms
   * @param currentForms
   * @param incomingForms
   * @return
   */
  private List<Form> updateCurrentForms(List<Form> currentForms, List<Form> incomingForms) {
    List<Form> updatedForms =  new ArrayList<>();
    currentForms.forEach(form -> {
      Optional<Form> updateForm = incomingForms
          .stream()
          .filter(incomingForm -> incomingForm.getName().equals(form.getName()))
          .findFirst();
      if (updateForm.isPresent()) {
        updatedForms.add(updateForm.get());
      } else {
        updatedForms.add(form);
      }
    });
    return updatedForms;
  }


}
