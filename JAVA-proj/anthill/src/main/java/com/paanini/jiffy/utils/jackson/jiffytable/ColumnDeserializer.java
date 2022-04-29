package com.paanini.jiffy.utils.jackson.jiffytable;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.option3.docube.schema.jiffytable.AcceptedValues;
import com.option3.docube.schema.jiffytable.Column;
import com.option3.docube.schema.jiffytable.ColumnType;
import com.option3.docube.schema.jiffytable.ConstraintType;
import com.option3.docube.schema.jiffytable.ForeignKey;
import com.option3.docube.schema.jiffytable.ReferenceColumn;
import com.option3.docube.schema.jiffytable.Restriction;
import com.option3.docube.schema.jiffytable.RestrictionValues;
import com.option3.docube.schema.jiffytable.Source;
import com.paanini.jiffy.constants.Common;
import com.paanini.jiffy.constants.JiffyTable;
import com.paanini.jiffy.exception.ProcessingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * @author Priyanka Bhoir
 * @since 13/11/20
 */
public class ColumnDeserializer extends StdDeserializer<Column> {

  public ColumnDeserializer() {
    super(Column.class);
  }

  @Override
  public Column deserialize(JsonParser jsonParser, DeserializationContext
      deserializationContext) throws IOException {
    JsonNode node = jsonParser.getCodec().readTree(jsonParser);
    return new Column(readStringProperty(node, "id"),
        readBooleanPrperty(node, "unique", false),
        readStringProperty(node, "name"),
        ColumnType.valueOf(readStringProperty(node, "type")),
        readBooleanPrperty(node, "editable", true),
        readBooleanPrperty(node, "hidden", false),
        readArrayProperty(node, Common.CONSTRAINTS),
        Source.valueOf(readStringProperty(node, "source")),
        readStringProperty(node, "modelVersion"),
            readStringProperty(node, "aliasName"));
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
          Optional<ForeignKey> fk = getForeignKey(node);
          fk.ifPresent(constraints::add);
        } else if (constraintType.equals(ConstraintType
            .REFERENCE_COLUMN.name())) {
          Optional<ReferenceColumn> rc = getReferenceColumn(node);
          rc.ifPresent(constraints::add);
        }

      }
    });
    return constraints;
  }

  private Optional<ReferenceColumn> getReferenceColumn(JsonNode node) {
    final JsonNode referenceColNode = node.get(Common.CONSTRAINTS);
    if(referenceColNode instanceof ArrayNode) {
      ArrayNode arrNode = (ArrayNode) referenceColNode;
      List<Optional<ReferenceColumn>> jsonNodes = new ArrayList<>();
      for(JsonNode jsonNode : arrNode) {
        jsonNodes.add(Optional.of(new ReferenceColumn(
            readStringProperty(jsonNode, "tableName"),
            readStringProperty(jsonNode, "columnPath"),
            ConstraintType.REFERENCE_COLUMN)));
      }
      if(!jsonNodes.isEmpty()){
        return jsonNodes.get(0);
      }
    }
    return Optional.empty();
  }

  private Optional<ForeignKey> getForeignKey(JsonNode node) {
    final JsonNode foreginKeyNode = node.get(Common.CONSTRAINTS);
    if(foreginKeyNode instanceof ArrayNode) {
      ArrayNode arrNode = (ArrayNode) foreginKeyNode;
      List<Optional<ForeignKey>> jsonNodes = new ArrayList<>();
      for(JsonNode jsonNode : arrNode) {
        jsonNodes.add(Optional.of(new ForeignKey(
            readStringProperty(jsonNode, "tableName"),
            readStringProperty(jsonNode, "columnId"),
            ConstraintType.FOREIGN_KEY)));
      }
      if(!jsonNodes.isEmpty()){
        return jsonNodes.get(0);
      }
    }
    return Optional.empty();
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
    if (Objects.isNull(idNode)) {
      return JiffyTable.EMPTY;
    }
    return idNode != null && idNode.getNodeType() == JsonNodeType.STRING
        ? idNode.asText() : JiffyTable.EMPTY;

  }

}
