package com.paanini.jiffy.utils.jackson.chart;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.option3.docube.schema.CardStorageType;
import com.option3.docube.schema.CardTypes;
import com.option3.docube.schema.General;
import com.option3.docube.schema.Legends;
import com.option3.docube.schema.axis.Axes;
import com.option3.docube.schema.graph.*;
import com.paanini.jiffy.constants.Common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by appmgr on 25/8/16.
 */
public class ChartContentDeserializer extends StdDeserializer<ChartContent> {

  public ChartContentDeserializer() {
    this(ChartContent.class);
  }

  protected ChartContentDeserializer(Class<?> vc) {
    super(vc);
  }

  protected ChartContentDeserializer(JavaType valueType) {
    super(valueType);
  }

  protected ChartContentDeserializer(StdDeserializer<?> src) {
    super(src);
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
  public ChartContent deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
    JsonNode node = jsonParser.getCodec().readTree(jsonParser);
    ChartTypes chartType = ChartTypes.valueOf(node.get("chartType").asText());
    CardTypes cardType = node.has("type") ? CardTypes.valueOf(node.get("type").asText()) : null;
    String title = node.has("title") ? node.get("title").asText() : null;
    String groupName = node.has("groupName") ? node.get("groupName").asText() : null;
    Integer bucketCount = node.has(Common.BUCKET_COUNT) && node.get(Common.BUCKET_COUNT) != null ? node.get(Common.BUCKET_COUNT).asInt() : null;

    CardStorageType storageType = node.has("cardStorageType") ?
            CardStorageType.valueOf(node.get("cardStorageType").asText()) :
            CardStorageType.UPDATABLE_STORAGE;

    Boolean forceUpdateData = node.has("forceUpdateData") ?
            node.get("forceUpdateData").asBoolean() :
            false;

    Axes axes = null;
    if(node.has("axes") && node.get("axes") != null){
      JsonParser axesParser = this.getParser(node.get("axes"), jsonParser);
      axes = deserializationContext.readValue(axesParser, Axes.class);
    }

    Legends legends = null;
    if(node.has(Common.LEGENDS) && node.get(Common.LEGENDS) != null) {
      JsonParser legendParser = this.getParser(node.get(Common.LEGENDS), jsonParser);
      legends = deserializationContext.readValue(legendParser, Legends.class);
    }

    JsonParser generalParser = this.getParser(node.get("general"), jsonParser);
    General general = deserializationContext.readValue(generalParser, General.class);


    JsonNode content = node.get("dataComponent");
    Iterator<JsonNode> elements = content.elements();

    List<Object> dataComponents = new ArrayList<>();
    while(elements.hasNext()) {
      JsonNode comp = elements.next();
      JsonParser compParser = this.getParser(comp, jsonParser);
      GraphTypes eleType = GraphTypes.valueOf(comp.get("type").asText());
      switch (eleType) {
        case RECTANGLE:
          dataComponents.add(deserializationContext.readValue(compParser, Rectangle.class));
          break;
        case TRIANGLE:
          dataComponents.add(deserializationContext.readValue(compParser, Triangle.class));
          break;
        case ARC:
          dataComponents.add(deserializationContext.readValue(compParser, Arc.class));
          break;
        case LINE:
          dataComponents.add(deserializationContext.readValue(compParser, Line.class));
          break;
        case AREA:
          dataComponents.add(deserializationContext.readValue(compParser, Area.class));
          break;
        case PIE:
          dataComponents.add(deserializationContext.readValue(compParser, Pie.class));
          break;
        case TREEMAP:
          dataComponents.add(deserializationContext.readValue(compParser, TreeMap.class));
          break;
        case DONUT:
          dataComponents.add(deserializationContext.readValue(compParser, Pie.class));
          break;
        case CONSTANT_LINE:
          dataComponents.add(deserializationContext.readValue(compParser, ConstantLine.class));
          break;
        case CALENDER:
          dataComponents.add(deserializationContext.readValue(compParser, Calender.class));
          break;
      }

    }

    return new ChartContent(chartType, cardType, dataComponents, axes, legends, general, title, bucketCount, groupName, storageType, forceUpdateData);
  }
}
