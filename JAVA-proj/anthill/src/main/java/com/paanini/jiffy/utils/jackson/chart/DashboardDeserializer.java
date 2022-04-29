package com.paanini.jiffy.utils.jackson.chart;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.option3.docube.schema.CardTypes;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by appmgr on 25/8/16.
 */
public class DashboardDeserializer extends StdDeserializer<Dashboard> {

  public DashboardDeserializer() {
    this(Dashboard.class);
  }

  protected DashboardDeserializer(Class<?> vc) {
    super(vc);
  }

  protected DashboardDeserializer(JavaType valueType) {
    super(valueType);
  }

  protected DashboardDeserializer(StdDeserializer<?> src) {
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
  public Dashboard deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
    JsonNode node = jsonParser.getCodec().readTree(jsonParser);

    JsonParser layoutParser = this.getParser(node.get("layout"), jsonParser);
    Layout layout = deserializationContext.readValue(layoutParser, Layout.class);

    Iterator<JsonNode> datasheets = node.get("datasheets").elements();
    List<Datasheet> dtList = new ArrayList<>();

    while(datasheets.hasNext()) {
      JsonParser datasheetParser = this.getParser(datasheets.next(), jsonParser);
      Datasheet datasheet = deserializationContext.readValue(datasheetParser, Datasheet.class);

      dtList.add(datasheet);
    }


    Iterator<JsonNode> customPalettesJson = node.get("customPalettes").elements();
    List<CustomPalette> customPalettes = new ArrayList<>();

    while(customPalettesJson.hasNext()) {
      JsonParser custompaletteParser = this.getParser(customPalettesJson.next(), jsonParser);
      CustomPalette palette = deserializationContext.readValue(custompaletteParser, CustomPalette.class);

      customPalettes.add(palette);
    }



    Iterator<JsonNode> contents = node.get("content").elements();
    List<Object> contentList = new ArrayList<>();

    while(contents.hasNext()) {
      JsonNode content = contents.next();
      if(content == null || content.asText().equals("null")){
        contentList.add(null);
      } else {
        processCardType(jsonParser, deserializationContext, contentList, content);
      }



    }
    return new Dashboard(layout, dtList, contentList, customPalettes);
  }

  private void processCardType(JsonParser jsonParser, DeserializationContext deserializationContext, List<Object> contentList, JsonNode content) throws IOException {
    CardTypes cardType = content.has("type") ? CardTypes.valueOf(content.get("type").asText()) : null;
    JsonParser contentParser = this.getParser(content, jsonParser);
    if(cardType != null) {
      switch (cardType) {
        case GRAPH:
          contentList.add(deserializationContext.readValue(contentParser, ChartContent.class));
          break;
        case EMPTY:
          contentList.add(deserializationContext.readValue(contentParser, EmptyCard.class));
          break;
        case FILTER:
          contentList.add(deserializationContext.readValue(contentParser, Filter.class));
          break;
        case NARRATION:
          contentList.add(deserializationContext.readValue(contentParser, NarrationCard.class));
          break;
        case KPI_RIBBON:
          contentList.add(deserializationContext.readValue(contentParser, NarrationCard.class));
          break;
        case DESCRIPTION:
          contentList.add(deserializationContext.readValue(contentParser, NarrationCard.class));
          break;
        case STATISTICS:
          contentList.add(deserializationContext.readValue(contentParser, StatisticsContent.class));
          break;
        case TABLE:
        case JIFFY_TABLE:
          contentList.add(deserializationContext.readValue(contentParser, TableContent.class));
          break;
        case PIVOT:
          contentList.add(deserializationContext.readValue(contentParser, PivotContent.class));
          break;
        case PIVOT_GRAPH:
          contentList.add(deserializationContext.readValue(contentParser, PivotGraphContent.class));
          break;
      }
    } else {
      //card type is not present when statistic card is using chart
      contentList.add(deserializationContext.readValue(contentParser, ChartContent.class));
    }
  }
}

