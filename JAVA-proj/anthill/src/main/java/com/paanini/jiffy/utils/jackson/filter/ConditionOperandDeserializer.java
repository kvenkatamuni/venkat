package com.paanini.jiffy.utils.jackson.filter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.option3.docube.schema.datasheet.filter.*;
import com.option3.docube.schema.datasheet.meta.Column;

import java.io.IOException;

public class ConditionOperandDeserializer extends StdDeserializer<ConditionOperand> {

  public ConditionOperandDeserializer() {
    super(Column.class);
  }

  protected ConditionOperandDeserializer(Class<?> vc) {
    super(vc);
  }


  protected ConditionOperandDeserializer(JavaType valueType) {
    super(valueType);
  }

  protected ConditionOperandDeserializer(StdDeserializer<?> src) {
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
  public ConditionOperand deserialize(JsonParser jsonParser,
                                      DeserializationContext deserializationContext) throws IOException {

    JsonNode node = jsonParser.getCodec().readTree(jsonParser);
    ConditionOperandTypes operandType =
            ConditionOperandTypes.valueOf(node.get("type").asText());
    JsonNode content = node.get("value");
    JsonParser compParser = this.getParser(content, jsonParser);
    Object operand;

    switch (operandType){
      case COLUMN:
        operand = deserializationContext.readValue(compParser, Column.class);
        break;
      case FILTER_FUNCTION:
        operand = deserializationContext.readValue(compParser, FilterFunction.class);
        break;
      case LITERAL_ARRAY:
        operand = deserializationContext.readValue(compParser, LiteralArray.class);
        break;
      case LITERAL_VALUE:
        operand = deserializationContext.readValue(compParser, LiteralValue.class);
        break;
      default:
        throw new RuntimeException("Cannot parse :" + operandType);
    }

    return new ConditionOperand(operandType, operand);
  }
}

