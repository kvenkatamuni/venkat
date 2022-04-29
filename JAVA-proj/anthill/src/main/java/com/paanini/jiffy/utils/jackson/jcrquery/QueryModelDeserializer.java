package com.paanini.jiffy.utils.jackson.jcrquery;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.paanini.jiffy.constants.Common;
import com.paanini.jiffy.jcrquery.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class QueryModelDeserializer extends StdDeserializer<QueryModel> {

  public QueryModelDeserializer() {
    super(QueryModel.class);
  }

  @Override
  public QueryModel deserialize(JsonParser jsonParser,
                                DeserializationContext
                                        deserializationContext) throws IOException {
    JsonNode node = jsonParser.getCodec().readTree(jsonParser);
    ObjectMapper mapper = new ObjectMapper();
    return new QueryModel(readProjectionProperty(node, mapper),
            readFilterProperty(node, mapper),
            readOrderProperty(node),
            getPaginationDetail(node, "limit"),
            getPaginationDetail(node, "offset"));
  }

  private Integer getPaginationDetail(JsonNode node, String field){
    if (Objects.isNull(node.get(field))) {
      return null;
    }
    return node.get(field).asInt();
  }

  private List<Filter> readFilterProperty(JsonNode node, ObjectMapper mapper) {
    if (Objects.isNull(node.get("filters"))) {
      return new ArrayList<>();
    }
    List<Filter> filters = new ArrayList<>();
    boolean check = true;
    List<JsonNode> data = mapper.convertValue(node.get("filters"),
            List.class);
    for (Object content : data) {
      Object column = ((Map) content).get(Common.COLUMN);
      Object value = ((Map) content).get("value");
      Object operator = ((Map) content).get("operator");
      if (Objects.isNull(column) || Objects.isNull(value) ||
              Objects.isNull(operator)) {
        check = false;
        break;
      }



      filters.add(new Filter(column.toString(),
              Operator.valueOf(operator.toString()), value));
    }

    if (!check) {
      return new ArrayList<>();
    }
    return filters;
  }

  private List<String> readProjectionProperty(JsonNode node,
                                              ObjectMapper mapper) {
    if (Objects.isNull(node.get("projections"))) {
      return new ArrayList<>();
    }
    return mapper.convertValue(node.get("projections"),List.class);
  }

  private Order readOrderProperty(JsonNode node) {
    if (Objects.isNull(node.get(Common.ORDER))) {
      return new Order("name", SortOrder.ASC);
    }

    if (Objects.isNull(node.get(Common.ORDER).get(Common.COLUMN)) ||
            Objects.isNull(node.get(Common.ORDER).get("sortOrder"))) {
      return new Order("name", SortOrder.ASC);
    }
    return new Order(node.get(Common.ORDER).get(Common.COLUMN).asText(),
            SortOrder.valueOf(node.get(Common.ORDER).get("sortOrder").asText()));
  }

}
