package com.paanini.jiffy.utils.jackson.chart;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.option3.docube.schema.General;
import com.option3.docube.schema.Legends;
import com.option3.docube.schema.axis.Axes;
import com.option3.docube.schema.graph.*;
import com.paanini.jiffy.services.AppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created by appmgr on 9/9/16.
 */
public class ChartContentSerializer extends StdSerializer<ChartContent> {

  static Logger logger = LoggerFactory.getLogger(ChartContentSerializer.class);

  public ChartContentSerializer() {
    super(ChartContent.class);
  }

  @Override
  public void serialize(ChartContent chartContent, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
    jsonGenerator.writeStartObject();
    jsonGenerator.writeStringField("chartType", chartContent.getChartType().toString());
    if(chartContent.getType() != null) {
      jsonGenerator.writeStringField("type", chartContent.getType().toString());
    }

    if(chartContent.getTitle() != null) {
      jsonGenerator.writeStringField("title", chartContent.getTitle());
    }

    if(chartContent.getGroupName() != null) {
      jsonGenerator.writeStringField("groupName", chartContent.getGroupName());
    }

    if(chartContent.getBucketCount() != null) {
      jsonGenerator.writeNumberField("bucketCount", chartContent.getBucketCount());
    }

    if(chartContent.getAxes() != null) {
      jsonGenerator.writeFieldName("axes");
      JsonSerializer<Object> serializer = serializerProvider.findValueSerializer(Axes.class);
      serializer.serialize(chartContent.getAxes(), jsonGenerator, serializerProvider);

    }

    if(chartContent.getLegends() != null) {
      jsonGenerator.writeFieldName("legends");
      JsonSerializer<Object> serializer = serializerProvider.findValueSerializer(Legends.class);
      serializer.serialize(chartContent.getLegends(), jsonGenerator, serializerProvider);
    }

    jsonGenerator.writeFieldName("general");
    JsonSerializer<Object> serializer = serializerProvider.findValueSerializer(General.class);
    serializer.serialize(chartContent.getGeneral(), jsonGenerator, serializerProvider);

    jsonGenerator.writeStringField("cardStorageType", chartContent.getCardStorageType().toString());
    jsonGenerator.writeBooleanField("forceReplaceData", chartContent.getForceReplaceData());

    jsonGenerator.writeArrayFieldStart("dataComponent");
    List<Object> comps = chartContent.getDataComponent();
    for(int i = 0; i < comps.size(); i++) {
      Object comp = comps.get(i);
      Class<?> type = comp.getClass();

      JsonSerializer<Object> compSerializer = null;
      switch (type.getSimpleName()) {
        case "Rectangle":
          compSerializer = serializerProvider.findValueSerializer(Rectangle.class);
          break;
        case "Triangle":
          compSerializer = serializerProvider.findValueSerializer(Triangle.class);
          break;
        case "Arc":
          compSerializer = serializerProvider.findValueSerializer(Arc.class);
          break;
        case "Line":
          compSerializer = serializerProvider.findValueSerializer(Line.class);
          break;
        case "Area":
          compSerializer = serializerProvider.findValueSerializer(Area.class);
          break;
        case "Pie":
          compSerializer = serializerProvider.findValueSerializer(Pie.class);
          break;
        case "TreeMap":
          compSerializer = serializerProvider.findValueSerializer(TreeMap.class);
          break;
        case "Donut":
          compSerializer = serializerProvider.findValueSerializer(Pie.class);
          break;
        case "ConstantLine":
          compSerializer = serializerProvider.findValueSerializer(ConstantLine.class);
          break;
        case "Calender":
          compSerializer = serializerProvider.findValueSerializer(Calender.class);
          break;
        default:
          logger.error("Unknown type {}", type.getSimpleName());

      }
      if (compSerializer != null) {
        compSerializer.serialize(comp, jsonGenerator, serializerProvider);
      }
    }
    jsonGenerator.writeEndArray();
    jsonGenerator.writeEndObject();
  }
}
