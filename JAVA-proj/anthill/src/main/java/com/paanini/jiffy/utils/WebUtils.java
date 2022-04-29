package com.paanini.jiffy.utils;

import com.option3.docube.schema.nodes.SubType;
import com.paanini.jiffy.exception.DataProcessingException;
import com.paanini.jiffy.models.UserTimezone;
import com.paanini.jiffy.proc.api.IngestContext;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.files.SparkModelFile;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WebUtils {
  private WebUtils() {
    throw new IllegalStateException("Utility class");
  }
  public static final int LIMIT_QUERY_UPPER_BOUND = 5000;

  public static long getTimezoneOffset(){
    Object tz = null;
    if(Objects.isNull(tz)){
      throw new DataProcessingException("No time zone value found");
    }
    return Long.valueOf(tz.toString());
  }

  public static List<UserTimezone> getSupportedTimezones() {
    List<UserTimezone> result = new ArrayList<UserTimezone>();
    String[] timeZones = TimeZone.getAvailableIDs();
    if(timeZones != null){
      Stream.of(timeZones).map(TimeZone::getTimeZone).forEach(tz -> {
        Calendar c = Calendar.getInstance(tz); // omit timezone for default tz
        c.setTime(new Date()); // your date; omit this line for current date
        int offset = tz.getRawOffset() / 1000;
        int dstOffset = c.get(Calendar.DST_OFFSET) / 1000;
        result.add(new UserTimezone(tz.getID(), offset + dstOffset));
      });
    }

    return result;
  }


  /*public static LaunchProperties getSparkProperties(String id, String jobInstanceId, SecurityManager securityManager,
                                                    String user, String clientUrl, CustomFile file, CustomFileContext ctx,
                                                    VfsManager vfsManager, IngestContext ingestContext,
                                                    SparkConfigFactory sparkConfigFactory) {
    boolean useDocubeClient = file.getVersion() != null;

    if (useDocubeClient) {
      return getSparkPropsV2(id, jobInstanceId,
              user, clientUrl, file, ctx,
              vfsManager, sparkConfigFactory);
    } else {
      return getSparkPropsV1(id, jobInstanceId, user,
              file, ctx, vfsManager, ingestContext,
              sparkConfigFactory);
    }
  }

  private static LaunchProperties getSparkPropsV1(String id, String jobInstanceId,
                                                  String user, CustomFile file, CustomFileContext ctx,
                                                  VfsManager vfsManager, IngestContext ingestContext,
                                                  SparkConfigFactory sparkConfigFactory) {
    String content = file.getContent();
    SubType subType = file.getSubType();
    String configName = "";
    String parentPath = "";
    Map<String, String> map = readConfiguration(content, sparkConfigFactory, getPhysicalPathFunction(vfsManager, ingestContext));

    boolean isDocubeAlgorithm = map.containsKey(SparkProps.SPARK_MAIN_CLASS) && !map.containsKey(SparkProps.SPARK_RESOURCE_FILE);
    java.nio.file.Path path =  isDocubeAlgorithm ?
            ctx.getDocubeAlgorithmJar(subType.name()) :
            ctx.getSourceFolder()
                    .resolve(Paths.get(map.get(SparkProps.SPARK_RESOURCE_FILE)));

    String[] inputIds = getFileIds(map.get(SparkProps.SPARK_INPUT_PARAMETERS),
            vfsManager::getFileFromPath);
    String[] outputIds = getFileIds(map.get(SparkProps.SPARK_OUTPUT_PARAMETERS),
            vfsManager::getOrCreateAppedableDatasheet);

    String others = map.get(SparkProps.SPARK_OTHER_PARAMETERS);
    String driverMemory = map.get(SparkProps.DRIVER_MEMORY);
    String driverCores = map.get(SparkProps.DRIVER_CORES);
    String executorMemory = map.get(SparkProps.EXECUTOR_MEMORY);
    String executorCores = map.get(SparkProps.EXECUTOR_CORES);
    String numberOfExecutor = map.get(SparkProps.NUMBER_OF_EXECUTOR);
    String extraParameters = map.get(SparkProps.CONFIG);

    SparkModelFile sparkModelFile = new SparkModelFile();
    sparkModelFile.setFeatureSet(map.get(SparkProps.FEATURES));
    sparkModelFile.setTrainingFile(map.get(SparkProps.TRAINING_DATA));
    sparkModelFile.setTargetColumn(map.get(SparkProps.TARGET));

    Optional<SparkModelFile> inputSparkModel = getModelFile(vfsManager, map.get(SparkProps.INPUT_MODEL), sparkModelFile);
    Optional<SparkModelFile> outputSparkModel = getModelFile(vfsManager, map.get(SparkProps.OUTPUT_MODEL), sparkModelFile);
    String inputModel = null;
    String featureSet = null;
    String trainingFile = null;
    String outputModel = null;

    if(inputSparkModel.isPresent()) {
      inputModel = inputSparkModel.get().getLocation();
      featureSet = inputSparkModel.get().getFeatureSet();
      trainingFile = inputSparkModel.get().getTrainingFile();
    } else if(outputSparkModel.isPresent()) {
      outputModel = outputSparkModel.get().getLocation();
      featureSet = outputSparkModel.get().getFeatureSet();
      trainingFile = outputSparkModel.get().getTrainingFile();
    }

    String mlAlgo = map.get(SparkProps.ML_ALGORITHM);
    String modelArguments = map.get(SparkProps.SPARK_MODEL_ARGUMENT);
    String pid = map.get(SparkProps.SPARK_CLUSTER);
    String sparkHome ="";
    String master = "";
    Optional<? extends Dictionary<String ,?>> sparkConfig = Objects.isNull(pid) || pid.isEmpty() ?
            sparkConfigFactory.getDefaultProps() :
            sparkConfigFactory.getSparkConfiguration(pid);

    if(sparkConfig.isPresent()) {
      sparkHome =(String)sparkConfig.get().get("spark_home");
      master = (String)sparkConfig.get().get("spark_rest_url");

    }

    String[] otherArgs = (others == null || others.trim().equals("")) ? new String[0] : others.split(",");
    String[] inputArgs = prepareSparkArgs(ingestContext, inputIds);
    String[] outputArgs = prepareSparkArgs(ingestContext, outputIds);
    String sessionToken = "";
    String docubeUrl = "";
    return new LaunchProperties(
            id,
            jobInstanceId,
            sparkHome,
            path.toString(),
            master,
            "cluster",
            map.get(SparkProps.SPARK_APP_NAME),
            inputArgs,
            outputArgs,
            otherArgs,
            outputIds,
            map.get(SparkProps.SPARK_MAIN_CLASS),
            user,
            driverMemory,
            driverCores,
            executorMemory,
            executorCores,
            numberOfExecutor,
            extraParameters,
            inputModel,
            outputModel,
            mlAlgo,
            modelArguments,
            map.get(SparkProps.DEBUG_PROGRAM),
            getPhysicalFilePath(trainingFile, vfsManager, ingestContext),
            featureSet,
            sessionToken,
            docubeUrl,
            map.get(SparkProps.TARGET),
            parentPath,
            configName
    );
  }

  private static LaunchProperties getSparkPropsV2(String id, String jobInstanceId,
                                                  String user, String clientUrl, CustomFile file,
                                                  CustomFileContext ctx, VfsManager vfsManager,
                                                  SparkConfigFactory sparkConfigFactory) {
    String content = file.getContent();
    String configName = file.getName();
    SubType subType = file.getSubType();
    String parentId = file.getParentId();
    String parentPath = "";
    String parentPathList [] = file.getPath().split
            ("/owned");
    if(parentPathList.length > 1){
      parentPath = parentPathList[1].substring(0, parentPathList[1].lastIndexOf('/'));
    }

    Map<String, String> map = readConfiguration(content, sparkConfigFactory, dcbPath -> dcbPath);

    boolean isDocubeAlgorithm = map.containsKey(SparkProps.SPARK_MAIN_CLASS) && !map.containsKey(SparkProps.SPARK_RESOURCE_FILE);
    java.nio.file.Path path =  isDocubeAlgorithm ?
            ctx.getDocubeAlgorithmJar(subType.name(), file.getVersion() != null
                    ? Long.toString(file.getVersion())
                    : "") :
            ctx.getSourceFolder()
                    .resolve(Paths.get(map.get(SparkProps.SPARK_RESOURCE_FILE)));

    //verify if files are present
    String[] inputIds = getFileIds(map.get(SparkProps.SPARK_INPUT_PARAMETERS), vfsManager::getFileFromPath);
    String[] outputIds = getFileIds(map.get(SparkProps.SPARK_OUTPUT_PARAMETERS), vfsManager::getOrCreateAppedableDatasheet);

    String others = map.get(SparkProps.SPARK_OTHER_PARAMETERS);
    String driverMemory = map.get(SparkProps.DRIVER_MEMORY);
    String driverCores = map.get(SparkProps.DRIVER_CORES);
    String executorMemory = map.get(SparkProps.EXECUTOR_MEMORY);
    String executorCores = map.get(SparkProps.EXECUTOR_CORES);
    String numberOfExecutor = map.get(SparkProps.NUMBER_OF_EXECUTOR);
    String extraParameters = map.get(SparkProps.CONFIG);

    SparkModelFile sparkModelFile = new SparkModelFile();
    sparkModelFile.setFeatureSet(map.get(SparkProps.FEATURES));
    sparkModelFile.setTrainingFile(map.get(SparkProps.TRAINING_DATA));
    sparkModelFile.setTargetColumn(map.get(SparkProps.TARGET));


    Optional<SparkModelFile> inputSparkModel = getModelFile(vfsManager, map.get(SparkProps.INPUT_MODEL), sparkModelFile);
    Optional<SparkModelFile> outputSparkModel = getModelFile(vfsManager, map.get(SparkProps.OUTPUT_MODEL), sparkModelFile);

    String inputModel = null;
    String featureSet = null;
    String trainingFile = null;
    String outputModel = null;
    String targetColumn = null;

    if(inputSparkModel.isPresent()) {
      inputModel = map.get(SparkProps.INPUT_MODEL);
      featureSet = inputSparkModel.get().getFeatureSet();
      trainingFile = inputSparkModel.get().getTrainingFile();
      targetColumn = inputSparkModel.get().getTargetColumn();
    } else if(outputSparkModel.isPresent()) {
      outputModel = map.get(SparkProps.OUTPUT_MODEL);
      featureSet = outputSparkModel.get().getFeatureSet();
      trainingFile = outputSparkModel.get().getTrainingFile();
      targetColumn = outputSparkModel.get().getTargetColumn();
    }

    String mlAlgo = map.get(SparkProps.ML_ALGORITHM);
    String modelArguments = map.get(SparkProps.SPARK_MODEL_ARGUMENT);
    String pid = map.get(SparkProps.SPARK_CLUSTER);
    String sparkHome ="";
    String master = "";
    Optional<? extends Dictionary<String ,?>> sparkConfig = Objects.isNull(pid) || pid.isEmpty() ?
            sparkConfigFactory.getDefaultProps() :
            sparkConfigFactory.getSparkConfiguration(pid);

    if(sparkConfig.isPresent()) {
      sparkHome =(String)sparkConfig.get().get("spark_home");
      master = (String)sparkConfig.get().get("spark_rest_url");

    }

    String[] otherArgs = (others == null || others.trim().equals("")) ? new String[0] : others.split(",");

    String[] inputArgs = Objects.isNull(map.get(SparkProps.SPARK_INPUT_PARAMETERS)) ?
            new String[]{""} : map.get(SparkProps.SPARK_INPUT_PARAMETERS).split(",");
    String[] outputArgs =Objects.isNull(map.get(SparkProps.SPARK_OUTPUT_PARAMETERS)) ?
            new String[]{""} : map.get(SparkProps.SPARK_OUTPUT_PARAMETERS).split(",");
    *//**
     * Spark programs require a valid session to query docube. We create a guest login session and attach
     * the current user id so that the user privileges could be elevated for queries originating from spark sessions.
     *
     *   1) create new Subject (don't use security utils.getSubject, that is current session)
     *   2) login as guest
     *   3) set attribute "REMEMBERED_ELEVATED_USER" to current active user, this data will be use to elevate user permissions,
     *      when spark program tries to login with session id
     *//*
    Subject subject = new Subject.Builder().buildSubject();
    subject.login(new GusUsernamePasswordToken("guest", "guest".toCharArray()));
    subject.getSession().setAttribute(Constants.REMEMBERED_ELEVATED_USER_ATTR, user);

    String sessionToken = subject.getSession().getId().toString();
    String docubeUrl = clientUrl;

    return new LaunchProperties(
            id, jobInstanceId,
            sparkHome,
            path.toString(),
            master,
            "cluster",
            map.get(SparkProps.SPARK_APP_NAME),
            inputArgs,
            outputArgs,
            otherArgs,
            outputIds,
            map.get(SparkProps.SPARK_MAIN_CLASS),
            user,
            driverMemory,
            driverCores,
            executorMemory,
            executorCores,
            numberOfExecutor,
            extraParameters,
            inputModel,
            outputModel,
            mlAlgo,
            modelArguments,
            map.get(SparkProps.DEBUG_PROGRAM),
            trainingFile,
            featureSet,
            sessionToken,
            docubeUrl,
            targetColumn,
            parentPath,
            configName
    );
  }

  public static Map<String, String> readConfiguration(String content, SparkConfigFactory sparkConfigFactory, Function<String, String> getPhysicalPathFn) {
    Map<String, String> config = new HashMap<>();
    String pid = "";
    try(JsonReader reader = Json.createReader(new StringReader(content))) {
      JsonArray array = reader.readArray();
      String modelArguments = getModelArguments(array);
      if(!modelArguments.isEmpty()) {
        config.put(SparkProps.SPARK_MODEL_ARGUMENT, modelArguments);
      }

      //pid extracted from JSONObject in order to pass pid in getExtraParameters
      for(int i=0 ; i < array.size() ; i++){
        JsonObject jo = (JsonObject) array.get(i);
        String name = jo.getString("name");
        if(name.equals("spark_cluster")) {
          pid = jo.getString("value");
        }
      }

      for(int i=0 ; i < array.size() ; i++){
        JsonObject jo = (JsonObject) array.get(i);
        String name = jo.getString("name");
        switch (name){
          case SparkProps.SPARK_TITLE:
            config.put(SparkProps.SPARK_APP_NAME,jo.getString("value"));
            break;
          case SparkProps.SPARK_APP_RESOURCE:
            config.put(SparkProps.SPARK_APP_RESOURCE,jo.getString("value"));
            break;
          case SparkProps.SPARK_MAIN_CLASS:
            config.put(SparkProps.SPARK_MAIN_CLASS,jo.getString("value"));
            break;
          case SparkProps.SPARK_INPUT_PARAMETERS:
            config.put(SparkProps.SPARK_INPUT_PARAMETERS,jo.getString("value"));
            break;
          case SparkProps.SPARK_OUTPUT_PARAMETERS:
            config.put(SparkProps.SPARK_OUTPUT_PARAMETERS,jo.getString("value"));
            break;
          case SparkProps.SPARK_RESOURCE_FILE:
            config.put(SparkProps.SPARK_RESOURCE_FILE,jo.getString("value"));
            break;
          case SparkProps.DRIVER_MEMORY:
            config.put(SparkProps.DRIVER_MEMORY,jo.getString("value"));
            break;
          case SparkProps.DRIVER_CORES:
            config.put(SparkProps.DRIVER_CORES,jo.getString("value"));
            break;
          case SparkProps.EXECUTOR_MEMORY:
            config.put(SparkProps.EXECUTOR_MEMORY,jo.getString("value"));
            break;
          case SparkProps.EXECUTOR_CORES:
            config.put(SparkProps.EXECUTOR_CORES,jo.getString("value"));
            break;
          case SparkProps.NUMBER_OF_EXECUTOR:
            config.put(SparkProps.NUMBER_OF_EXECUTOR,jo.getString("value"));
            break;
          case SparkProps.SPARK_OTHER_PARAMETERS:
            config.put(SparkProps.SPARK_OTHER_PARAMETERS,jo.getString("value"));
            break;
          case SparkProps.CONFIG:
            config.put(SparkProps.CONFIG, getExtraParameters(jo.getJsonArray("value"), sparkConfigFactory, pid ) );
            break;
          case SparkProps.DEBUG_PROGRAM:
            config.put(SparkProps.DEBUG_PROGRAM, String.valueOf(jo.getBoolean("value")));
            break;
          case SparkProps.TRAINING_DATA:
            config.put(SparkProps.TRAINING_DATA, jo.getString("value"));
            break;
          case SparkProps.FEATURES:
            config.put(SparkProps.FEATURES, getFeatureArray(jo.getJsonArray("value"), getPhysicalPathFn));
            break;
          case SparkProps.OUTPUT_MODEL:
            config.put(SparkProps.OUTPUT_MODEL, jo.getString("value"));
            break;
          case SparkProps.INPUT_MODEL:
            config.put(SparkProps.INPUT_MODEL, jo.getString("value"));
            break;
          case SparkProps.SPARK_CLUSTER:
            config.put(SparkProps.SPARK_CLUSTER,jo.getString("value"));
            break;
          case SparkProps.TARGET:
            config.put(SparkProps.TARGET,jo.getString("value"));

        }
      }
    }
    return config;
  }


  private static String getExtraParameters(JsonArray jsonArray, SparkConfigFactory sparkConfigFactory, String pid) {
    Map<String, String> defaultExtraParameters = sparkConfigFactory
            .getExtraParameters(pid);
    StringBuilder extraParams = new StringBuilder();

    for (int i = 0; i < jsonArray.size(); i++) {
      JsonObject jsonObject = (JsonObject) jsonArray.get(i);
      defaultExtraParameters.put(jsonObject.getString("key"), jsonObject.getString("value"));
    }

    for (Map.Entry<String, String> entry : defaultExtraParameters.entrySet()) {
      extraParams.
              append(",").
              append("\"").append(entry.getKey()).append("\" ").
              append(":").
              append(" \"").append(entry.getValue()).append("\" ");
    }

    return extraParams.toString();
  }


  private static String getFeatureArray(JsonArray array, Function<String, String> fn) {
    JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
    array.stream()
            .map(entry -> (JsonObject) entry)
            .map(entry -> {
              JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
              String featureDatasheetPath = entry.getString("featureDatasheet");
              return objectBuilder.add("featureDatasheet", fn.apply(featureDatasheetPath))
                      .add("featureColumn", entry.getString("featureColumn")).build();
            })
            .forEach(arrayBuilder::add);

    StringWriter sw = new StringWriter();
    JsonWriter jw = Json.createWriter(sw);
    jw.writeArray(arrayBuilder.build());
    jw.close();
    return sw.toString();
  }

  private static String getPhysicalFilePath(String featureDatasheetPath, VfsManager vfsManager, IngestContext ctx) {
    if(featureDatasheetPath == null || "".equals(featureDatasheetPath)) return "";

    String id = vfsManager.getIdFromPath(featureDatasheetPath);
    return ctx.getFilePath(id).toString();
  }

  private static String[] getFileIds(String args,Function<String, BasicFileProps> fn){
    if(args == null || args.trim().equals("")){
      return new String[0];
    }
    return Stream.of(args.split(","))
            .map((arg) -> fn.apply(arg))
            .map((file)-> Objects.isNull(file) ? "" : file.getId())
            .collect(Collectors.toList())
            .toArray(new String[0]);
  }

  private static Optional<SparkModelFile> getModelFile(VfsManager vfsManager, String modelFilePath, SparkModelFile tempModel){
    if(modelFilePath == null || modelFilePath.trim().equals("")){
      return Optional.empty();
    }
    return Optional.of(vfsManager.findOrCreateSparkModel(modelFilePath, tempModel));
  }

  private static String[] prepareSparkArgs(IngestContext ingestContext, String[] args) {
    return Stream.of(args)
            .map(id -> Objects.isNull(id) || id.isEmpty() ? "" : ingestContext.getFilePath(id))
            .map(Objects::toString)
            .collect(Collectors.toList())
            .toArray(new String[0]);
  }

  private static String getModelArguments(JsonArray array) {

    JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
    List<JsonObject> arguments = array.stream()
            .filter(entry -> SparkProps.SPARK_MODEL_ARGUMENT.equals(((JsonObject) entry).getString("type")))
            .map(entry -> (JsonObject) entry)
            .collect(Collectors.toList());
    if(arguments.size() > 0) {
      arguments.stream().forEach(arrayBuilder::add);

      StringWriter sw = new StringWriter();
      JsonWriter jw = Json.createWriter(sw);
      jw.writeArray(arrayBuilder.build());
      jw.close();
      return sw.toString();
    }

    return "";


  }*/

  public static Function<String, String> getPhysicalPathFunction(VfsManager vfsManager, IngestContext ctx) {
    return new Function<String, String>() {
      @Override
      public String apply(String docubePath) {
        if(docubePath == null || "".equals(docubePath)) return "";

        String id = vfsManager.getIdFromPath(docubePath);
        return ctx.getFilePath(id).toString();
      }
    };
  }

}

