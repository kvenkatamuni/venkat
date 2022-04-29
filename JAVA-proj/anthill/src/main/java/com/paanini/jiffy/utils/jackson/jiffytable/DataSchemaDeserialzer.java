package com.paanini.jiffy.utils.jackson.jiffytable;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.option3.docube.schema.jiffytable.Column;
import com.option3.docube.schema.jiffytable.ColumnType;
import com.option3.docube.schema.jiffytable.DataSchema;
import com.option3.docube.schema.jiffytable.InnerTableSchema;
import com.option3.docube.schema.jiffytable.NestedStructureSchema;
import com.option3.docube.schema.jiffytable.Status;
import com.paanini.jiffy.constants.JiffyTable;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.utils.ObjectMapperFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Athul Krishna N S
 * @since 13/11/20
 */
public class DataSchemaDeserialzer extends StdDeserializer<DataSchema> {

  public DataSchemaDeserialzer() {
    super(DataSchemaDeserialzer.class);
  }

  @Override
  public DataSchema deserialize(JsonParser jsonParser,
      DeserializationContext ctxt) throws IOException {
    JsonNode node = jsonParser.getCodec().readTree(jsonParser);
    String id = readSchemaId(node);
    Status status = readSchemaStatus(node);
    List<Object> columns = readSchemaColumns(node, jsonParser, ctxt);
    return new DataSchema(id, columns, status);
  }

  private List<Object> readSchemaColumns(JsonNode node, JsonParser jsonParser,
      DeserializationContext ctxt) {
    List<Object> columns = new ArrayList<>();
    ArrayNode columnsNode = (ArrayNode) node.get("columns");
    columnsNode.forEach(column -> {
      if(column.get("type").asText().equals(ColumnType.INNER_TABLE.name())) {
        columns.add(getInnerTable(jsonParser, ctxt, column));
      } else if(column.get("type").asText().equals(ColumnType.NESTED_STRUCTURE
          .name())) {
        columns.add(getNestedStructure(jsonParser, ctxt, column));
      } else {
        columns.add(getBasicColumn(jsonParser, ctxt, column));

      }
    });
    return columns;
  }

  private Column getBasicColumn(JsonParser jsonParser,
      DeserializationContext ctxt, JsonNode column) {
    try {
      JsonParser colParser = this.getParser(column, jsonParser);
      return ctxt.readValue(colParser, Column.class);
    } catch(IOException e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  private NestedStructureSchema getNestedStructure(JsonParser jsonParser,
      DeserializationContext ctxt, JsonNode column) {
    try {
      JsonParser colParser = this.getParser(column, jsonParser);
      return ctxt.readValue(colParser, NestedStructureSchema.class);
    } catch(IOException e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  private InnerTableSchema getInnerTable(JsonParser jsonParser,
      DeserializationContext ctxt, JsonNode column) {
    try {
      JsonParser colParser = this.getParser(column, jsonParser);
      return ctxt.readValue(colParser, InnerTableSchema.class);
    } catch(IOException e) {
      throw new ProcessingException(e.getMessage());
    }
  }
  /**
   * Returns a string value property if present, else empty
   *
   * @return
   */
  private String readSchemaId(JsonNode node) {
    JsonNode idNode = node.get("id");
    return idNode != null && idNode.getNodeType() == JsonNodeType.STRING
        ? idNode.asText() : JiffyTable.EMPTY;

  }

  /**
   * Returns a string value property if present, else empty
   *
   * @return
   */
  private Status readSchemaStatus(JsonNode node) {
    JsonNode n = node.get("status");
    return n != null && n.getNodeType() == JsonNodeType.STRING ? Status
        .valueOf(n.asText()) : Status.DRAFT;
  }

  private JsonParser getParser(JsonNode node, JsonParser jsonParser)
      throws IOException {
    ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
    JsonFactory jsonFactory = new JsonFactory();
    String treeString = objectMapper
        .writeValueAsString(objectMapper.treeToValue(node, Object.class));
    JsonParser newParser = jsonFactory.createParser(treeString);
    newParser.setCodec(jsonParser.getCodec());
    newParser.nextToken();
    return newParser;
  }

}
