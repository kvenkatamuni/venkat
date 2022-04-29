package com.paanini.jiffy.utils.jackson.filter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.option3.docube.schema.datasheet.filter.ConditionOperand;
import com.option3.docube.schema.datasheet.filter.FilterFunction;
import com.option3.docube.schema.datasheet.filter.LiteralArray;
import com.option3.docube.schema.datasheet.filter.LiteralValue;
import com.option3.docube.schema.datasheet.meta.Column;

import java.io.IOException;

public class ConditionOperandSerializer extends StdSerializer<ConditionOperand> {

  public ConditionOperandSerializer() {
    super(ConditionOperand.class);
  }

  @Override
  public void serialize(ConditionOperand operand, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
    jsonGenerator.writeStartObject();
    jsonGenerator.writeStringField("type", operand.getType().toString());
    jsonGenerator.writeFieldName("value");

    Object comp = operand.getValue();

    JsonSerializer<Object> compSerializer = null;
    switch (operand.getType()) {
      case COLUMN:
        compSerializer = serializerProvider.findValueSerializer(Column.class);
        break;
      case FILTER_FUNCTION:
        compSerializer = serializerProvider.findValueSerializer(FilterFunction.class);
        break;
      case LITERAL_ARRAY:
        compSerializer = serializerProvider.findValueSerializer(LiteralArray.class);
        break;
      case LITERAL_VALUE:
        compSerializer = serializerProvider.findValueSerializer(LiteralValue.class);
        break;
    }
    if (compSerializer != null) {
      compSerializer.serialize(comp, jsonGenerator, serializerProvider);
    }
    jsonGenerator.writeEndObject();
  }
}
