package com.paanini.jiffy.vfs.files;

import com.option3.docube.schema.nodes.CustomFileSchema;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.dto.AccessEntry;
import com.paanini.jiffy.utils.SchemaUtils;
import com.paanini.jiffy.vfs.CustomFileUploadedFileReference;
import com.paanini.jiffy.vfs.api.*;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class CustomFile extends CustomFileSchema implements ExtraFileProps, BasicFileProps,
        Persistable, Exportable {

  static Logger logger = LoggerFactory.getLogger(CustomFile.class);

  Optional<Long> versionNumber = Optional.empty();
  private String path;
  private AccessEntry[] privileges;
  private String parentId;
  public CustomFile(){
    setType(Type.CUSTOM_FILE);
  }

  public CustomFile(final SpecificRecordBase schema) {
    CustomFileSchema cp = (CustomFileSchema) schema;
    SchemaUtils.copy(cp, this);
  }

  //Used to capture the uploaded file references
  //This information is not persisted on JackRabbit
  CustomFileUploadedFileReference[] uploadedFiles;

  public CustomFileUploadedFileReference[] getUploadedFiles() {
    return uploadedFiles;
  }

  public void setUploadedFiles(CustomFileUploadedFileReference[] uploadedFiles) {
    this.uploadedFiles = uploadedFiles;
  }

  private void resetVersion() {
    this.versionNumber = Optional.empty();
  }

  @Override
  public Schema getFileSchema() {
    return this.getSchema();
  }

  @Override
  public void setValue(int field, Object value) {
    this.put(field, value);
  }

  @Override
  public void accept(VfsVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public Object getValue(String fieldName) {
    return this.get(fieldName);
  }

  @Override
  public void setValue(String fieldName, Object value) {
    this.put(fieldName, value);
  }

  @Override
  public String getPath() {
    return this.path;
  }

  @Override
  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public String getParentId() {
    return parentId;
  }

  @Override
  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  @Override
  public AccessEntry[] getPrivileges() {
    return privileges;
  }

  @Override
  public void setPrivileges(AccessEntry[] privileges) {
    this.privileges = privileges;
  }

  //ToDO: Commented this below get and update dependencies
  //we need to handle dependencies dataset if not present also
    /*@Override
    public Set<String> getDependencies() {
        if (this.getSubType().equals(SubType.modelApplication) ||
                this.getSubType().equals(SubType.spark) ||
                this.getSubType().equals(SubType.modelCreation)) {
            try {
                Set<String> dependenciesList = new HashSet<>();
                List<SparkData> sparkData = ObjectMapperFactory.getInstance()
                        .readValue(this.getContent(), new TypeReference<List<SparkData>>() {
                        });
                List<SparkData> filteredOutput = sparkData.stream()
                        .filter(this::sparkDataFilter).collect(Collectors.toList());
                filteredOutput.forEach(data -> {
                    if (data.getType().equalsIgnoreCase("modelFeatures")) {
                        List<SparkModelFeatures> values = (List<SparkModelFeatures>) data.getValue();
                        for (SparkModelFeatures val : values) {
                            if (!isNullOrEmpty(val.getFeatureDatasheet())) {
                                dependenciesList.add("@path:" + lastToken(val.getFeatureDatasheet(),
                                        "/"));
                            }
                        }
                    }
                    if (data.getType().equalsIgnoreCase("sparkModel") ||
                            data.getType().equalsIgnoreCase("sparkTainingDatasheet")) {
                        String str = (String) data.getValue();
                        if (!isNullOrEmpty(str)) {
                            dependenciesList.add("@path:" + lastToken(str, "/"));
                        }
                    }
                });
                logger.info("Got the Spark dependencies files");
                return dependenciesList;
            } catch (IOException e) {
                logger.error("Failed to get spark dependencies data {}", e.getMessage());
            }
        }
        return Collections.<String>emptySet();
    }

    private boolean sparkDataFilter(SparkData sparkData1) {
        return sparkData1.getType().equalsIgnoreCase("sparkTainingDatasheet")
                || sparkData1.getType().equalsIgnoreCase("modelFeatures")
                || sparkData1.getType().equalsIgnoreCase("sparkModel");
    }


    @Override
    public void updateDependencies(List<Persistable> files) {

        Map<String, Persistable> lookup = files.stream()
                .collect(Collectors.toMap(file -> ((BasicFileProps) file).getName(), file -> file));
        if (this.getSubType().equals(SubType.modelApplication) ||
                this.getSubType().equals(SubType.spark) ||
                this.getSubType().equals(SubType.modelCreation)) {

            try {
                List<SparkData> sparkData = ObjectMapperFactory.getInstance()
                        .readValue(this.getContent(), new TypeReference<List<SparkData>>() {
                        });
                sparkData.forEach(data -> {
                    if (data.getType().equalsIgnoreCase("sparkModel") ||
                            data.getType().equalsIgnoreCase("sparkTainingDatasheet")) {
                        String str = (String) data.getValue();
                        if (!isNullOrEmpty(str)) {
                            String key = lastToken(str, "/");
                            if (lookup.containsKey(key)) {
                                Persistable persistable = lookup.get(key);
                                String path = ((ExtraFileProps) persistable).getPath();
                                data.setValue(path);
                            }
                        }
                    }
                    if (data.getType().equalsIgnoreCase("modelFeatures")) {
                        List<SparkModelFeatures> values = (List<SparkModelFeatures>) data.getValue();
                        for (SparkModelFeatures val : values) {
                            String featureDatasheet = val.getFeatureDatasheet();
                            if (!isNullOrEmpty(featureDatasheet)) {
                                String key = lastToken(featureDatasheet, "/");
                                if (lookup.containsKey(key)) {
                                    Persistable persistable = lookup.get(key);
                                    String path = ((ExtraFileProps) persistable).getPath();
                                    val.setFeatureDatasheet(path);
                                }
                            }
                        }
                    }
                });
                String updatedContent = ObjectMapperFactory.getInstance().writeValueAsString(sparkData);
                this.setContent(updatedContent);
                logger.info("Spark update dependencies completed");
            } catch (IOException e) {
                logger.error("Failed to update spark dependencies {}", e.getMessage());
            }
        }

    }*/
}
