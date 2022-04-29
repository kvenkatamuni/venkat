package com.paanini.jiffy.models;

import com.paanini.jiffy.dto.ImportDTO;

import java.util.ArrayList;

/**
 * @author Priyanka Bhoir
 * @since 03/11/20
 */
public class DatasetImportOptions extends ImportDTO {
  ArrayList<String> datasets;

  public ArrayList<String> getDatasets() {
    return datasets;
  }

  public void setDatasets(ArrayList<String> datasets) {
    this.datasets = datasets;
  }
}
