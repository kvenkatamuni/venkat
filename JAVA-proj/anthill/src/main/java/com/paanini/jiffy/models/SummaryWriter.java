package com.paanini.jiffy.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.paanini.jiffy.constants.Common;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class SummaryWriter {

  public static final String TOTAL_NUMBER_OF_FILES_TEXT = "Total number of Files - ";
  public static final String NUMBER_OF_FILES_FAILED_TO_IMPORT_TEXT =
      "Number of files Failed to import - ";
  public static final String NUMBER_OF_FILES_IMPORTED_SUCCESSFULLY_TEXT =
      "Number of files imported successfully - ";
  public static final String NUMBER_OF_FILES_OVERWRITTEN_TEXT = "Number of files overwritten - ";
  static Logger logger = LoggerFactory.getLogger(SummaryWriter.class);

  File summaryFile;
  Summary summary;

  public SummaryWriter(Summary summary, File summaryFile) {
    this.summary = summary;
    this.summaryFile = summaryFile;
  }

  public void writeToFile() throws IOException, ParseException {
    int totalFileSelected = 0;
    int failedFiles = 0;
    int importedFiles = 0;
    int overwrittenFiles = 0;
    int totalNewFile = 0;
    String excludeSelected = "true";

    //this value will filter the list of files based on the value of the 'selected' field.
    //If the full app is imported then 'selected' field is of the file is ignored for tur value
//        if(summary.isFullAppImport()){
//            excludeSelected = "false";
//        }else{
//            excludeSelected = "true";
//        }

    String sumJson = new ObjectMapper().writeValueAsString(summary.getDetailedSummary());
    JSONParser parser = new JSONParser();
    logger.debug("Parsing the JSON {}", sumJson);
    JSONObject summaryJson = (JSONObject) parser.parse(sumJson);
    logger.debug("Iterating through the summary json");
    List<Map<String, String>> detailedList = flatterndata(summaryJson,"");
    String newLine;
    try (FileWriter file = new FileWriter(summaryFile)) {
      newLine = System.getProperty("line.separator");
      logger.debug("Writing the summary details into the file");

      file.append(newLine + TOTAL_NUMBER_OF_FILES_TEXT + totalFileSelected + newLine);
      file.append(NUMBER_OF_FILES_FAILED_TO_IMPORT_TEXT + failedFiles + newLine);
      file.append(NUMBER_OF_FILES_IMPORTED_SUCCESSFULLY_TEXT + importedFiles + newLine);
      file.append(NUMBER_OF_FILES_OVERWRITTEN_TEXT + overwrittenFiles + newLine);

      for (Map<String, String> filedetails : detailedList) {
        if (!filedetails.isEmpty()) {
          String selected = filedetails.get(Common.SELECTED);

          if (selected.equalsIgnoreCase(excludeSelected) && !filedetails.get(Common.STATUS).isEmpty()) {
            file.append(filedetails.get("name") + "  :  " + filedetails.get(Common.STATUS) +
                "  " + filedetails.get(Common.ERROR) +newLine);
            switch (filedetails.get(Common.STATUS)) {

              case "Done":
                totalFileSelected++;
                importedFiles++;
                break;
              case "OverWritten":
                totalFileSelected++;
                overwrittenFiles++;
                importedFiles++;
                break;
              case "Error":
                failedFiles++;
                totalFileSelected++;
                break;
              case "New":
                totalFileSelected++;
                totalNewFile++;
                importedFiles++;
                break;
              default:
                totalFileSelected++;
            }
          }
        }
      }
      file.flush();
    }
    FileReader readFile = new FileReader(summaryFile);
    String oldContent;
    try (BufferedReader reader = new BufferedReader(readFile)) {
      String line = reader.readLine();
      oldContent = "";
      while (line != null) {
        oldContent = oldContent + line + newLine;
        line = reader.readLine();
      }
    }
    String newContent = oldContent.replaceAll("Total number of Files - 0" + newLine,
        TOTAL_NUMBER_OF_FILES_TEXT+ totalFileSelected + newLine);
    newContent = newContent.replace("Number of files Failed to import - 0" + newLine,
        NUMBER_OF_FILES_FAILED_TO_IMPORT_TEXT + failedFiles + newLine);
    newContent = newContent.replace(
        "Number of files imported successfully - 0" + newLine,
        NUMBER_OF_FILES_IMPORTED_SUCCESSFULLY_TEXT + importedFiles + newLine);
    newContent = newContent.replace("Number of files overwritten - 0" + newLine,
        NUMBER_OF_FILES_OVERWRITTEN_TEXT + overwrittenFiles + newLine);

    try (FileWriter writefile = new FileWriter(summaryFile)) {
      writefile.write(newContent);
    }
    logger.info("Successfully completed with writing in to summary file");
  }

  private List<Map<String, String>> flatterndata(JSONObject object, String path) {
    Set<String> keys = object.keySet();
    List<Map<String, String>> detailedList = new ArrayList<>();
    for (String key : keys) {
      Map<String, String> map = new HashMap<>();
      Object value = object.get(key);
      JSONObject files = ((JSONObject) value);
      JSONObject list = new JSONObject();
      if (files.keySet().contains("list")) {
        Object fileList = files.get("list");
        list = ((JSONObject) fileList);
      }
      if (!list.isEmpty()){
        detailedList.addAll(flatterndata(list, String.format("%s/%s",path,key)));
      } else {
        map.put("name", String.format("%s/%s",path,key));
        map.put(Common.SELECTED,
            !Objects.isNull(files.get(Common.SELECTED)) ? files.get(Common.SELECTED).toString() : "unknown");
        map.put(Common.STATUS, !Objects.isNull(files.get(Common.STATUS)) ?
            files.get(Common.STATUS).toString() : "Done");
        String error = Objects.nonNull(files.get(Common.ERROR)) ?
            files.get(Common.ERROR).toString() : "";
        map.put(Common.ERROR, error);
      }

      detailedList.add(map);
    }

    return detailedList;

  }

}
