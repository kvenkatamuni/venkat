package com.paanini.jiffy.utils.jackson.datasheet;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.option3.docube.schema.datasheet.function.repository.Type;
import com.option3.docube.schema.datasheet.meta.*;

import java.io.IOException;
import java.util.Objects;

public class ColumnSerializer extends StdSerializer<Column> {

  public ColumnSerializer() {
    super(Column.class);
  }

  @Override
  public void serialize(Column column, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
    jsonGenerator.writeStartObject();

    jsonGenerator.writeObjectFieldStart("source");
    Object columnSource = column.getSource();
    Integer sourceIndex = null;
    if(columnSource instanceof Expression) {
      processExpression(jsonGenerator, (Expression) columnSource);
    } else if(columnSource instanceof ColumnName) {
      sourceIndex = processColumnName(jsonGenerator, (ColumnName) columnSource, sourceIndex);
    } else if(columnSource instanceof IfExpression) {
      processIfExpression(jsonGenerator, (IfExpression) columnSource);
    }
    jsonGenerator.writeEndObject();

    String originalType;
    String type = Objects.isNull(column.getType()) ? null : column.getType().toString();
    if(column.getOriginalType() == null) {
      if (Objects.isNull(sourceIndex)) {
        //non-csv file
        originalType = type;
      } else {
        //csv file
        originalType = Type.STRING.toString();
      }
    } else {
      originalType = column.getOriginalType().toString();
    }

    jsonGenerator.writeStringField("description", column.getDescription());
    jsonGenerator.writeStringField("type", type);
    jsonGenerator.writeStringField("originalType", originalType);
    jsonGenerator.writeStringField("alias", column.getAlias());
    jsonGenerator.writeBooleanField("nullable", column.getNullable());
    jsonGenerator.writeBooleanField("hide", column.getHide());

    if(Objects.isNull(column.getExtraction())) {
      jsonGenerator.writeObjectFieldStart("extraction");
      jsonGenerator.writeArrayFieldStart("functions");
      jsonGenerator.writeEndArray();
      jsonGenerator.writeEndObject();
    } else {
      jsonGenerator.writeFieldName("extraction");
      JsonSerializer<Object> contentSerializer = serializerProvider.findValueSerializer(Extraction.class);
      contentSerializer.serialize(column.getExtraction(), jsonGenerator, serializerProvider);
    }

    if(column.getFlatExpression() != null && !column.getFlatExpression().isEmpty()) {
      jsonGenerator.writeStringField("flatExpression", column.getFlatExpression());
    }
    if(column.getTableAlias() != null && !column.getTableAlias().isEmpty()) {
      jsonGenerator.writeStringField("tableAlias", column.getTableAlias());
    }

    jsonGenerator.writeEndObject();
  }

  private void processIfExpression(JsonGenerator jsonGenerator, IfExpression columnSource) throws IOException {
    IfExpression expression = columnSource;
    jsonGenerator.writeStringField("condition", expression.getCondition());
    jsonGenerator.writeStringField("thenClause", expression.getThenClause());
    jsonGenerator.writeStringField("elseClause", expression.getElseClause());
  }

  private Integer processColumnName(JsonGenerator jsonGenerator, ColumnName columnSource, Integer sourceIndex) throws IOException {
    ColumnName columnName = columnSource;
    jsonGenerator.writeStringField("name", columnName.getName());
    if(!Objects.isNull(columnName.getSourceIndex())){
      sourceIndex = columnName.getSourceIndex();
      jsonGenerator.writeNumberField("sourceIndex", columnName.getSourceIndex());
    }
    return sourceIndex;
  }

  private void processExpression(JsonGenerator jsonGenerator, Expression columnSource) throws IOException {
    Expression source = columnSource;
    jsonGenerator.writeStringField("expression", source.getExpression());
  }
}
