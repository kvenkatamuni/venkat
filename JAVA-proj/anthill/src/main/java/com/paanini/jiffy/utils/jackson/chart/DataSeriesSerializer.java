package com.paanini.jiffy.utils.jackson.chart;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.option3.docube.schema.Bin;
import com.option3.docube.schema.DataSeries;

import java.io.IOException;
import java.util.List;

/**
 * Created by appmgr on 8/9/16.
 */
public class DataSeriesSerializer  extends StdSerializer<DataSeries> {
  public DataSeriesSerializer() {
    super(DataSeries.class);
  }

  protected DataSeriesSerializer(JavaType type) {
    super(type);
  }

  @Override
  public void serialize(DataSeries dataSeries, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
    jsonGenerator.writeStartObject();
    jsonGenerator.writeStringField("name", dataSeries.getName());
    jsonGenerator.writeStringField("seriesName", dataSeries.getSeriesName());
    jsonGenerator.writeStringField("type", dataSeries.getType());
    String valuetype = dataSeries.getValueType() != null ? dataSeries.getValueType().toString() : "";
    jsonGenerator.writeStringField("valueType", valuetype);
    jsonGenerator.writeStringField("aggregateFunction", dataSeries.getAggregateFunction());
    String columnExpression = dataSeries.getColumnExpression();
    String sanitizeColExpression = "null".equals(columnExpression) ? null : (columnExpression == null ? null : columnExpression);

    String expression = dataSeries.getExpression();
    String sanitizeExpression = "null".equals(expression) ? null : (expression == null ? null : expression);

    jsonGenerator.writeStringField("expression", sanitizeExpression);
    jsonGenerator.writeStringField("columnExpression", sanitizeColExpression);
    jsonGenerator.writeFieldName("values");
    jsonGenerator.writeStartArray();
    for(Object obj : dataSeries.getValues()){
      writeValue(dataSeries, jsonGenerator, obj);
    }
    jsonGenerator.writeEndArray();

    if(dataSeries.getValue() != null) {
      jsonGenerator.writeFieldName("value");
      writeValue(dataSeries, jsonGenerator, dataSeries.getValue());
    }
    if(dataSeries.getScale() != null) {
      jsonGenerator.writeNumberField("scale",dataSeries.getScale());
    }
    if(dataSeries.getNoOfBins() != null) {
      jsonGenerator.writeNumberField("noOfBins",dataSeries.getNoOfBins());
    }
    if(dataSeries.getLimitType() != null) {
      jsonGenerator.writeStringField("limitType", dataSeries.getLimitType().toString());
    }
    if(dataSeries.getLimitCount() != null) {
      jsonGenerator.writeNumberField("limitCount",dataSeries.getLimitCount());
    }

    processBins(dataSeries, jsonGenerator, serializerProvider);

    processCustomSort(dataSeries, jsonGenerator);

    jsonGenerator.writeEndObject();
  }

  private void processBins(DataSeries dataSeries, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
    if(dataSeries.getBins() != null) {
      jsonGenerator.writeArrayFieldStart("bins");

      List<Object> content = dataSeries.getBins();
      JsonSerializer<Object> contentSerializer = serializerProvider.findValueSerializer(Bin.class);
      for(int i = 0; i < content.size(); i++) {
        Object bin = content.get(i);
        contentSerializer.serialize(bin, jsonGenerator, serializerProvider);
      }
      jsonGenerator.writeEndArray();
    }
  }

  private void processCustomSort(DataSeries dataSeries, JsonGenerator jsonGenerator) throws IOException {
    if(dataSeries.getCustomSort() != null) {
      jsonGenerator.writeArrayFieldStart("customSort");

      List<String> content = dataSeries.getCustomSort();
      for(int i = 0; i < content.size(); i++) {
        if(content.get(i) != null) jsonGenerator.writeString(content.get(i));
      }
      jsonGenerator.writeEndArray();
    }
  }

  private void writeValue(com.option3.docube.schema.DataSeries dataSeries, JsonGenerator jsonGenerator, Object obj) throws IOException {
    try {
      switch (dataSeries.getValueType()) {
        case STRING:
          String value = getString(obj);
          jsonGenerator.writeString(value);
          break;
        case DECIMAL:
          Double decValue = getaDouble(obj);
          jsonGenerator.writeNumber(decValue);
          break;
        case INTEGER:
          int intVal = obj == null ? null : (int) obj;
          jsonGenerator.writeNumber(intVal);
          break;
        case TIMESTAMP:
          Long longVal = getaLong(obj);
          jsonGenerator.writeNumber(longVal);
          break;
        case DOUBLE:
          Double doubleVal = getaDouble(obj);
          jsonGenerator.writeNumber(doubleVal);

          break;
        case BIGINT:
          Long bigIntVal = getaLong(obj);
          jsonGenerator.writeNumber(bigIntVal);
          break;

        case BOOLEAN:
          Boolean booleanVal = obj == null ? null : (Boolean) obj;
          jsonGenerator.writeBoolean(booleanVal);
          break;
      }
    } catch(Exception e) {
      String stringVal = getString(obj);
      jsonGenerator.writeString(stringVal);
    }
  }

  private Long getaLong(Object obj) {
    return obj == null ? null : (Long) obj;
  }

  private Double getaDouble(Object obj) {
    return obj == null ? null : (Double) obj;
  }

  private String getString(Object obj) {
    return obj == null ? null : (String) obj;
  }
}
