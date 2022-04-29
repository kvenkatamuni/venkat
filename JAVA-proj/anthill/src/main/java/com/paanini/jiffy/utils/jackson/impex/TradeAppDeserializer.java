package com.paanini.jiffy.utils.jackson.impex;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.paanini.jiffy.models.DependencyGraph;
import com.paanini.jiffy.models.TradeApp;
import com.paanini.jiffy.models.TradeEntity;
import com.paanini.jiffy.models.TradeFile;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class TradeAppDeserializer extends StdDeserializer<TradeApp> {

  public TradeAppDeserializer() {
    super(TradeApp.class);
  }

  private JsonParser getParser(JsonNode node, JsonParser jsonParser) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    JsonFactory jsonFactory = new JsonFactory();
    String treeString = objectMapper.writeValueAsString(objectMapper.treeToValue(node, Object.class));
    JsonParser newParser = jsonFactory.createParser(treeString);
    newParser.setCodec(jsonParser.getCodec());
    newParser.nextToken();

    return newParser;
  }
  @Override
  public TradeApp deserialize(JsonParser jsonParser,
      DeserializationContext deserializationContext) throws IOException {
    ObjectMapper mapper = new ObjectMapper();

    JsonNode node = jsonParser.getCodec().readTree(jsonParser);
    if(node == null){
      throw new IOException("Unable to serialize TradeApp, node from parser is null");
    }
    String id = readStringProperty(node, "id");
    String path = readStringProperty(node, "path");
    String name = readStringProperty(node, "name");


    DependencyGraph graph = null;

    if(node.has("graph")) {
      JsonParser contentParser = this.getParser(node.get("graph"), jsonParser);
      graph = deserializationContext.readValue(contentParser, DependencyGraph.class);
    }


    JsonNode files = node.get("files");
    HashMap<String, TradeFile> tradeEntities = getTradeEntities(mapper, files);

    return new TradeApp(id, name, path, tradeEntities, graph);
  }

  private HashMap<String, TradeFile> getTradeEntities(ObjectMapper mapper, JsonNode files) {
    HashMap<String, TradeFile> fileHashMap = new HashMap<>();
    files.fieldNames().forEachRemaining(field -> {
      JsonNode jsonNode = files.get(field);
      if(jsonNode != null){
        if(jsonNode.has("list")) {
          TradeEntity tradeEntity = getTradeEntity(mapper, jsonNode);
          if(tradeEntity !=null){
            fileHashMap.put(field, tradeEntity);
          }
        } else {
          fileHashMap.put(field, mapper.convertValue(
                  jsonNode, TradeFile.class));
        }
      }
    });
    return fileHashMap;
  }

  private TradeEntity getTradeEntity(ObjectMapper mapper, JsonNode file) {
    if(file == null){
      return null;
    }
    String name = readStringProperty(file, "name");
    String type = file.has("type") ? readStringProperty(file, "type") : null;
    boolean selected = file.has("selected") && file.get("selected").asBoolean(true);

    return new TradeEntity(
        name, selected,
        getTradeEntities(mapper, file.get("list")),
        type);
  }

  /**
   * Returns a string value property if present, else empty
   */
  private String readStringProperty(JsonNode node, String property) {
    if(node != null) {
      JsonNode idNode = node.get(property);
      return idNode != null && idNode.getNodeType() == JsonNodeType.STRING ? idNode.asText() : "";
    }
    return "";
  }
}
