package com.paanini.jiffy.utils.jackson.chart;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.option3.docube.schema.Bin;
import com.option3.docube.schema.DataSeries;
import com.option3.docube.schema.DataTypes;
import com.option3.docube.schema.LimitTypes;
import com.paanini.jiffy.constants.Common;
import com.paanini.jiffy.exception.DataProcessingException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Created by appmgr on 25/8/16.
 */
public class DataSeriesDeserializer extends StdDeserializer<DataSeries> {

  public DataSeriesDeserializer() {
    this(DataSeries.class);
  }

  protected DataSeriesDeserializer(Class<?> vc) {
    super(vc);
  }

  protected DataSeriesDeserializer(JavaType valueType) {
    super(valueType);
  }

  protected DataSeriesDeserializer(StdDeserializer<?> src) {
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
  public DataSeries deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();

    JsonNode node = jsonParser.getCodec().readTree(jsonParser);
    String name = node.get("name").asText();
    String seriesName = node.get("seriesName").asText();
    String type = node.get("type").asText();
    DataTypes dataType = getDataType(node);
    String aggregateFunction = node.get("aggregateFunction").asText();
    String expression = getStringValue(node, "expression");
    String columnExpression = getStringValue(node, "columnExpression");
    Object val = getVal(node, "value");
    Integer scale = getScale(node, "scale");
    Integer noOfBins = getNoOfBins(node);
    Integer limitCount = getScale(node, "limitCount");
    LimitTypes limitType = getLimitType(node);
    List<Object> bins = getBins(jsonParser, deserializationContext, node);

    List<String> customSort = getCustomSort(node);

    JsonNode content = node.get("values");
    Iterator<JsonNode> elements = content.elements();

    List<Object> values = new ArrayList<>();

    while(elements.hasNext()) {
      processElements(dataType, elements, values);

    }

    return new DataSeries(name, seriesName, type, dataType, aggregateFunction, expression, columnExpression, values, val, scale, limitCount, limitType, noOfBins, bins, customSort);
  }

  private void processElements(DataTypes dataType, Iterator<JsonNode> elements, List<Object> values) {
    JsonNode value = elements.next();

    if(dataType != null) {
      try {
        switch (dataType) {
          case STRING:
            values.add(getName(value));
            break;
          case DECIMAL:
            values.add(value.asDouble());
            break;
          case INTEGER:
            values.add(value.asInt());
            break;
          case BIGINT:
            values.add(value.asLong());
            break;
          case BOOLEAN:
            values.add(value.asBoolean());
            break;
          case TIMESTAMP:
            values.add(value.asLong());
            break;
          case DOUBLE:
            Double d = value.isNull() ? Double.NaN : Double.parseDouble(value.asText());
            values.add(d.isNaN() ? 0 : d);
            break;
        }
      }catch (Exception e) {
//                    values.add(value.asText());
        throw new DataProcessingException(e.getMessage());
      }
    }
  }

  private LimitTypes getLimitType(JsonNode node) {
    return (!node.has(Common.LIMIT_TYPE) || node.get(Common.LIMIT_TYPE).asText().equals("") || node.get(Common.LIMIT_TYPE).asText().equals("null")) ? null :  LimitTypes.valueOf(node.get(Common.LIMIT_TYPE).asText());
  }

  private Integer getNoOfBins(JsonNode node) {
    return node.has(Common.NO_OF_BINS) && node.get(Common.NO_OF_BINS) != null ? node.get(Common.NO_OF_BINS).asInt() : null;
  }

  private Integer getScale(JsonNode node, String scale) {
    return node.has(scale) ? node.get(scale).asInt() : null;
  }

  private DataTypes getDataType(JsonNode node) {
    return (!node.has(Common.VALUE_TYPE) || node.get(Common.VALUE_TYPE).asText().equals("") || node.get(Common.VALUE_TYPE).asText().equals("null")) ? null : DataTypes.valueOf(node.get(Common.VALUE_TYPE).asText());
  }

  private Object getVal(JsonNode node, String value) {
    if((node.has(value)) && (Objects.nonNull(node.get(value)))){
        return node.get(value).asText();
    }
    return null;
  }

  private Object getName(JsonNode node) {
    if(!node.isNull()){
      return node.asText();
    }
    return null;
  }

  private List<Object> getBins(JsonParser jsonParser, DeserializationContext deserializationContext, JsonNode node) throws IOException {
    List<Object> bins = new ArrayList<>();
    if(node.has("bins")) {
      Iterator<JsonNode> binsJson = node.get("bins").elements();
      while(binsJson.hasNext()) {
        bins.add(deserializationContext.readValue(this.getParser(binsJson.next(), jsonParser), Bin.class));
      }
    } else {
      bins = null;
    }
    return bins;
  }

  private List<String> getCustomSort(JsonNode node) {
    List<String> customSort = new ArrayList<>();
    if(node.has("customSort")) {
      Iterator<JsonNode> customSortJson = node.get("customSort").elements();
      while(customSortJson.hasNext()) {
        JsonNode value = customSortJson.next();
        customSort.add(value.isNull() ? null : value.asText());
      }
    } else {
      customSort = null;
    }
    return customSort;
  }

  private String getStringValue(JsonNode node, String fieldName) {
    return node.has(fieldName) ?
            (node.get(fieldName).isNull() ? null : node.get(fieldName).asText()) :
            null;
  }
}
