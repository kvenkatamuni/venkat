package com.paanini.jiffy.utils.jackson.chart;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.option3.docube.schema.CustomPalette;
import com.option3.docube.schema.Dashboard;
import com.option3.docube.schema.EmptyCard;
import com.option3.docube.schema.datasheet.Datasheet;
import com.option3.docube.schema.filter.Filter;
import com.option3.docube.schema.graph.ChartContent;
import com.option3.docube.schema.layout.Layout;
import com.option3.docube.schema.narration.NarrationCard;
import com.option3.docube.schema.statistics.StatisticsContent;
import com.option3.docube.schema.table.PivotContent;
import com.option3.docube.schema.table.PivotGraphContent;
import com.option3.docube.schema.table.TableContent;
import com.paanini.jiffy.encryption.api.CyberArk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created by appmgr on 9/9/16.
 */
public class DashboardSerializer extends StdSerializer<Dashboard> {
  private static final Logger LOGGER =
          LoggerFactory.getLogger(DashboardSerializer.class);

  public DashboardSerializer() {
    super(Dashboard.class);
  }

  @Override
  public void serialize(Dashboard dashboard, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
    jsonGenerator.writeStartObject();

    jsonGenerator.writeFieldName("layout");
    JsonSerializer<Object> valueSerializer = serializerProvider.findValueSerializer(Layout.class);
    valueSerializer.serialize(dashboard.getLayout(), jsonGenerator, serializerProvider);


    jsonGenerator.writeArrayFieldStart("datasheets");
    for(int i = 0; i < dashboard.getDatasheets().size(); i++) {
      JsonSerializer<Object> datasheetSerializer = serializerProvider.findValueSerializer(Datasheet.class);
      datasheetSerializer.serialize(dashboard.getDatasheets().get(i), jsonGenerator, serializerProvider);
    }
    jsonGenerator.writeEndArray();

    jsonGenerator.writeArrayFieldStart("customPalettes");
    List<CustomPalette> customPalettes = dashboard.getCustomPalettes();
    for(int i = 0; i < customPalettes.size(); i++) {
      JsonSerializer<Object> customPaletteSerilalizer = serializerProvider.findValueSerializer(CustomPalette.class);
      customPaletteSerilalizer.serialize(customPalettes.get(i), jsonGenerator, serializerProvider);
    }
    jsonGenerator.writeEndArray();

    jsonGenerator.writeArrayFieldStart("content");
    List<Object> content = dashboard.getContent();
    for(int i = 0; i < content.size(); i++) {
      Object card = content.get(i);
      if(card == null) {
        jsonGenerator.writeObject(null);
      } else {
        Class<?> type = card.getClass();
        JsonSerializer<Object> contentSerializer = null;
        switch (type.getSimpleName()) {
          case "ChartContent":
            contentSerializer = serializerProvider.findValueSerializer(ChartContent.class);
            break;
          case "EmptyCard":
            contentSerializer = serializerProvider.findValueSerializer(EmptyCard.class);
            break;
          case "Filter":
            contentSerializer = serializerProvider.findValueSerializer(Filter.class);
            break;
          case "NarrationCard":
            contentSerializer = serializerProvider.findValueSerializer(NarrationCard.class);
            break;
          case "StatisticsContent":
            contentSerializer = serializerProvider.findValueSerializer(StatisticsContent.class);
            break;
          case "TableContent":
            contentSerializer = serializerProvider.findValueSerializer(TableContent.class);
            break;
          case "PivotContent":
            contentSerializer = serializerProvider.findValueSerializer(PivotContent.class);
            break;
          case "PivotGraphContent":
            contentSerializer = serializerProvider.findValueSerializer(PivotGraphContent.class);
            break;
          default:
            LOGGER.error("Unknown type {}", type.getSimpleName());
        }

        if (contentSerializer != null) {
          contentSerializer.serialize(card, jsonGenerator, serializerProvider);
        }
      }

    }
    jsonGenerator.writeEndArray();

    jsonGenerator.writeEndObject();
  }
}
