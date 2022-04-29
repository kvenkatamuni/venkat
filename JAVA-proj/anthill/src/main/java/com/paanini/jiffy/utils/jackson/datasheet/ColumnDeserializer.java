package com.paanini.jiffy.utils.jackson.datasheet;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.option3.docube.schema.datasheet.function.repository.Type;
import com.option3.docube.schema.datasheet.meta.*;
import com.paanini.jiffy.constants.Common;

import java.io.IOException;
import java.util.Objects;

public class ColumnDeserializer extends StdDeserializer<Column> {

  public ColumnDeserializer() {
    super(Column.class);
  }

  protected ColumnDeserializer(Class<?> vc) {
    super(vc);
  }


  protected ColumnDeserializer(JavaType valueType) {
    super(valueType);
  }

  protected ColumnDeserializer(StdDeserializer<?> src) {
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
  public Column deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
    JsonNode node = jsonParser.getCodec().readTree(jsonParser);
    JsonNode sourceNode = node.get("source");

    Integer index = sourceNode.has(Common.SOURCE_INDEX) && !sourceNode.get(Common.SOURCE_INDEX).isNull() ?
            sourceNode.get(Common.SOURCE_INDEX).asInt() :
            null;

    Object sourceObj = getSourceObj(sourceNode, jsonParser, deserializationContext);

    String description = node.get("description").isNull() ? null : node.get("description").asText();
    Type type = isNotAvailable(node, "type") ? null : Type.valueOf(node.get("type").asText());
    Type originalType = isNotAvailable(node, "originalType") ?
            Objects.isNull(index) ? type : Type.valueOf("STRING") :
            Type.valueOf(node.get("originalType").asText());
    String alias = node.get("alias").isNull() ? null : node.get("alias").asText();
    boolean nullable = node.get("nullable").asBoolean(false);
    boolean hide = node.get("hide").asBoolean(false);

    Extraction extraction = null;
    if(node.has(Common.EXTRACTION) && !node.get(Common.EXTRACTION).isNull()) {
      JsonParser contentParser = this.getParser(node.get(Common.EXTRACTION), jsonParser);
      extraction = deserializationContext.readValue(contentParser, Extraction.class);
    }

    String flatExpression = null;
    if(node.has(Common.FLAT_EXPRESSION) && !node.get(Common.FLAT_EXPRESSION).isNull()) {
      flatExpression = node.get(Common.FLAT_EXPRESSION).asText();
    }

    String tableAlias = null;
    if(node.has(Common.TABLE_ALIAS) && !node.get(Common.TABLE_ALIAS).isNull()) {
      tableAlias = node.get(Common.TABLE_ALIAS).asText();
    }
    return new Column(sourceObj, description, type, originalType, alias, nullable, hide, extraction,flatExpression, tableAlias);
  }

  private Object getSourceObj(JsonNode sourceNode, JsonParser jsonParser,  DeserializationContext deserializationContext) throws IOException {

    if(sourceNode.has("expression")) {
      return  new Expression(sourceNode.get("expression").asText());
    } else if(sourceNode.has("condition") && sourceNode.has("thenClause")) {

      return new IfExpression(sourceNode.get("condition").asText(),
              sourceNode.get("thenClause").asText(),
              sourceNode.get("elseClause").asText());
    } else {
      Integer index = sourceNode.has(Common.SOURCE_INDEX) && !sourceNode.get(Common.SOURCE_INDEX).isNull() ?
              sourceNode.get(Common.SOURCE_INDEX).asInt() :
              null;
      return new ColumnName(sourceNode.get("name").asText(), index);
    }
  }

  private boolean isNotAvailable(JsonNode node, String fieldName) {
    return !node.has(fieldName) ||
            node.get(fieldName).asText().equals("") ||
            node.get(fieldName).asText().equals("null") ||
            node.get(fieldName).asText().equals("VOID");

  }
}
