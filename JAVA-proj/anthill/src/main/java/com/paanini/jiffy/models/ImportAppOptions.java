package com.paanini.jiffy.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.paanini.jiffy.dto.ImportDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Athul Krishna N S
 * @since 02/11/20
 */
public class ImportAppOptions extends ImportDTO {
  private Map<String, TradeFile> importOptions;

  public ImportAppOptions() {
  }

  public ImportAppOptions(String fileId, boolean isNew, String appName,
                          Map<String, TradeFile> importOptions) {
    this.fileId = fileId;
    this.setNewApp(isNew);
    this.appName = appName;
    this.importOptions = importOptions;
  }

  public Map<String, TradeFile> getImportOptions() {
        return importOptions;
    }

  public void setImportOptions(Map<String, TradeFile> importOptions) {
    this.importOptions = importOptions;
  }

  @JsonIgnore
  public List<String> getDocubeFiles() {
        return getFiles(importOptions, null);
    }

  @JsonIgnore
  public List<String> getDatasetFiles() {
    return getFiles(importOptions, TabType.datasets.name());
  }

  @JsonIgnore
  private List<String> getFiles(Map<String, TradeFile> inputFiles, String type) {
    ArrayList<String> files = new ArrayList<>();
    inputFiles.forEach((k,v) -> {
      if (v instanceof TradeEntity) {
        TradeEntity entity = (TradeEntity) v;
        String fileType = entity.getType();
        if (entity.getType().equals(type)) {
          files.addAll(getFiles(entity.getList(), type));
        } else if ((Objects.isNull(type) && (Objects.isNull(fileType) ||
                    TabType.getDocubeTabs().contains(TabType.valueOf(fileType))))) {
          files.addAll(getFiles(entity.getList(), fileType));
        }
      } else if(v instanceof TradeFile &&  v.isSelected()) {
        files.add(v.getName());
      }
    });
    return files;
  }

  @JsonIgnore
  public boolean isFullAppImport() {
        return importOptions == null || importOptions.isEmpty();
    }

  public int getTotal() {
        return getCount(importOptions);
    }

  protected int getCount(Map<String, TradeFile> inputFiles) {
    int total = 0;
    for (Map.Entry<String, TradeFile> entry : inputFiles.entrySet()) {
      TradeFile tradeFile = entry.getValue();
      if (tradeFile instanceof TradeEntity) {
        total = total + getCount(((TradeEntity) tradeFile).getList());
      } else {
        if(tradeFile.isSelected()) {
          total++;
        }
      }
    }

    return total;
  }
}
