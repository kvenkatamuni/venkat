package com.paanini.jiffy.utils.jackson.jiffytable;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.option3.docube.schema.jiffytable.AcceptedValues;
import com.option3.docube.schema.jiffytable.Column;
import com.option3.docube.schema.jiffytable.ColumnType;
import com.option3.docube.schema.jiffytable.ConstraintType;
import com.option3.docube.schema.jiffytable.ForeignKey;
import com.option3.docube.schema.jiffytable.InnerTableSchema;
import com.option3.docube.schema.jiffytable.NestedStructureSchema;
import com.option3.docube.schema.jiffytable.Restriction;
import com.option3.docube.schema.jiffytable.RestrictionValues;
import com.option3.docube.schema.jiffytable.Source;
import com.paanini.jiffy.constants.JiffyTable;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.utils.ObjectMapperFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Priyanka Bhoir
 * @since 13/11/20
 */
public class NestedStructureDeserializer extends StdDeserializer<NestedStructureSchema> {

  public NestedStructureDeserializer() {
    super(NestedStructureSchema.class);
  }


  @Override
  public NestedStructureSchema deserialize(JsonParser p,
      DeserializationContext ctxt)
      throws IOException {
    JsonNode node = p.getCodec().readTree(p);
    List<Object> columns = readSchemaColumns(node, p, ctxt);
    return new NestedStructureSchema(readStringProperty(node, "id"),
        readBooleanPrperty(node, "unique", false),
        readStringProperty(node, "name"),
        ColumnType.valueOf(readStringProperty(node, "type")),
        readBooleanPrperty(node, "editable", true),
        readBooleanPrperty(node, "hidden", false),
        readArrayProperty(node, "constraints"),
        Source.valueOf(readStringProperty(node, "source")),
        columns);
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


  /**
   * reads the constraints in the given json
   *
   * @param node
   * @param property
   * @return
   */
  private List<Object> readArrayProperty(JsonNode node, String property) {
    List<Object> constraints = new ArrayList<>();
    node.get(property).forEach(jsonNode -> {
      String constraintType = readStringProperty(jsonNode,
          "constraintType");
      if (!constraintType.isEmpty()) {
        if (constraintType.equals(ConstraintType.RESTRICTION.name())) {
          constraints.add(getRestriction(jsonNode));
        } else if (constraintType.equals(ConstraintType
            .ACCEPTED_VALUES.name())) {
          ColumnType columnType = ColumnType.valueOf(readStringProperty
              (node, "type"));
          constraints.add(formatAcceptedValues(jsonNode, columnType));
        } else if (constraintType.equals(ConstraintType
            .FOREIGN_KEY.name())) {
          ColumnType columnType = ColumnType
              .valueOf(readStringProperty(node, "type"));
          constraints.add(
              new ForeignKey(readStringProperty(node, "tableId"),
                  readStringProperty(node, "string"),
                  ConstraintType.FOREIGN_KEY));
        }
      }
    });
    return constraints;
  }

  /**
   * Reads an "acceptedvalues" constraint from the given json
   *
   * @param node
   * @return
   */

  private AcceptedValues formatAcceptedValues(JsonNode node, ColumnType type){
    List<Object> acceptedValues = new ArrayList<>();
    boolean convertToNumeric = type.equals(ColumnType.AUTO_NUMBER) || type
        .equals(ColumnType.NUMERIC);
    node.get("values").forEach(value -> {
      if(convertToNumeric) {
        acceptedValues.add(getNumericValue(value));
      } else {
        acceptedValues.add(value.asText());
      }
    });
    return new AcceptedValues(acceptedValues,
        ConstraintType.ACCEPTED_VALUES);
  }

  private Object getNumericValue(JsonNode node) {
    String value = node.asText();
    Pattern longPattern = Pattern.compile(JiffyTable.LONG_PATTERN);
    Pattern decimalPattern = Pattern.compile(JiffyTable.DECIMAL_PATTERN);
    if(longPattern.matcher(value).matches()) {
      return Long.parseLong(value);
    } else if(decimalPattern.matcher(value).matches()) {
      return Double.parseDouble(value);
    }

    throw new ProcessingException("Invalid numeric value " + value);
  }

  /**
   * Reads a restriction constraint from the given json
   *
   * @param node
   * @return
   */

  private Restriction getRestriction(JsonNode node) {
    return new Restriction(RestrictionValues.valueOf(readStringProperty
        (node, "restrictionName")), ConstraintType.RESTRICTION);
  }

  /**
   * returns a boolean field
   *
   * @return
   */
  private boolean readBooleanPrperty(JsonNode node, String property,
      boolean defaultValue) {
    JsonNode booleanNode = node.get(property);
    return (booleanNode != null && booleanNode.getNodeType() ==
        JsonNodeType.BOOLEAN) ? booleanNode.asBoolean() : defaultValue;
  }

  /**
   * Returns a string value property if present, else empty
   *
   * @return
   */
  private String readStringProperty(JsonNode node, String property) {
    JsonNode idNode = node.get(property);
    return idNode != null && idNode.getNodeType() == JsonNodeType.STRING
        ? idNode.asText() : JiffyTable.EMPTY;

  }


}

