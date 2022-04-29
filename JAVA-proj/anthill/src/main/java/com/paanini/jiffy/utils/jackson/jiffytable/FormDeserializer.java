package com.paanini.jiffy.utils.jackson.jiffytable;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.option3.docube.schema.jiffytable.Button;
import com.option3.docube.schema.jiffytable.ColumnDisplay;
import com.option3.docube.schema.jiffytable.Condition;
import com.option3.docube.schema.jiffytable.Form;
import com.option3.docube.schema.jiffytable.FormColumn;
import com.option3.docube.schema.jiffytable.FormSection;
import com.option3.docube.schema.jiffytable.SectionDisplay;
import com.paanini.jiffy.exception.DocubeException;
import com.paanini.jiffy.utils.MessageCode;
import com.paanini.jiffy.utils.ObjectMapperFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Athul Krishna N S
 * @since 14/12/20
 */
public class FormDeserializer extends StdDeserializer<Form> {

  public FormDeserializer() {
    super(Form.class);
  }

  @Override
  public Form deserialize(JsonParser parser, DeserializationContext ctxt)
      throws IOException, JsonProcessingException {
    JsonNode node = parser.getCodec().readTree(parser);
    return readForm(node, parser, ctxt);
  }

  private Form readForm(JsonNode node, JsonParser parser, DeserializationContext ctxt) {
    String formName = readStringProperty(node, "name");
    List<Condition> conditions = getConditions(parser, node, ctxt);
    List<Button> buttons = getButtons(parser, node, ctxt);
    List<FormColumn> columnSettings = getColumnSettings(parser, node, ctxt);
    List<FormSection> formSections = getFormSections(parser, node, ctxt);
    return new Form(formName, conditions, buttons, columnSettings, formSections);
  }

  private List<FormSection> getFormSections(JsonParser parser, JsonNode node,
      DeserializationContext ctxt) {
    ArrayNode sectionNode = (ArrayNode) node.get("displaySection");
    List<FormSection> displaySections = new ArrayList<>();
    sectionNode.forEach(section -> {
      displaySections.add(getSection(parser, ctxt, section));
    });
    return displaySections;
  }

  private FormSection getSection(JsonParser parser, DeserializationContext ctxt, JsonNode section) {
    String tableReference = readStringProperty(section, "tableReference");
    ArrayNode displayNode = (ArrayNode) section.get("displaySettings");
    List<Object> displaySettings = new ArrayList<>();
    displayNode.forEach(node -> {
      if (node.has("type")) {
        displaySettings.add(getDisplaySetting(parser, ctxt, node));
      }
    });

    return new FormSection(tableReference, displaySettings);
  }

  private Object getDisplaySetting(JsonParser parser, DeserializationContext ctxt, JsonNode node) {
    String type = node.get("type").asText();
    if (type.equals("COLUMN") || type.equals("INNER_TABLE")) {
      return getColumnDisplay(parser, ctxt, node);
    } else {
      return getSectionDisplay(parser, ctxt, node);
    }
  }

  private SectionDisplay getSectionDisplay(JsonParser parser, DeserializationContext ctxt,
      JsonNode node) {
    try {
      JsonParser colParser = this.getParser(node, parser);
      SectionDisplay sectionDisplay = ctxt.readValue(colParser, SectionDisplay.class);
      if (sectionDisplay.getColumnLookup().isEmpty()) {
        throw new DocubeException(MessageCode.ANTHILL_ERR_FORM_SECTION_COLS);
      }
      return sectionDisplay;
    } catch(IOException e) {
      throw new DocubeException(MessageCode.ANTHILL_ERR_PARSE, e.getMessage());
    }
  }

  private ColumnDisplay getColumnDisplay(JsonParser parser, DeserializationContext ctxt,
      JsonNode node) {
    try {
      JsonParser colParser = this.getParser(node, parser);
      return ctxt.readValue(colParser, ColumnDisplay.class);
    } catch(IOException e) {
      throw new DocubeException(MessageCode.ANTHILL_ERR_PARSE, e.getMessage());
    }
  }

  private List<FormColumn> getColumnSettings(JsonParser parser, JsonNode node,
      DeserializationContext ctxt) {
    ArrayNode settingsNode = (ArrayNode) node.get("columnSettings");
    List<FormColumn> columnSettings = new ArrayList<>();
    settingsNode.forEach(button -> {
      columnSettings.add(getColumnSetting(parser, ctxt, button));
    });
    return columnSettings;
  }

  private FormColumn getColumnSetting(JsonParser parser, DeserializationContext ctxt,
      JsonNode setting) {
    try {
      JsonParser colParser = this.getParser(setting, parser);
      return ctxt.readValue(colParser, FormColumn.class);
    } catch(IOException e) {
      throw new DocubeException(MessageCode.ANTHILL_ERR_PARSE, e.getMessage());
    }
  }

  private List<Button> getButtons(JsonParser parser, JsonNode node, DeserializationContext ctxt) {
    ArrayNode buttonNode = (ArrayNode) node.get("buttonSettings");
    List<Button> buttons = new ArrayList<>();
    buttonNode.forEach(button -> {
      buttons.add(getButton(parser, ctxt, button));
    });
    return buttons;
  }

  private Button getButton(JsonParser parser, DeserializationContext ctxt, JsonNode button) {
    try {
      JsonParser colParser = this.getParser(button, parser);
      return ctxt.readValue(colParser, Button.class);
    } catch(IOException e) {
      throw new DocubeException(MessageCode.ANTHILL_ERR_PARSE, e.getMessage());
    }
  }

  private List<Condition> getConditions(JsonParser parser, JsonNode node,
      DeserializationContext context) {
    ArrayNode conditionNode = (ArrayNode) node.get("conditions");
    List<Condition> conditions = new ArrayList<>();
    conditionNode.forEach(condition -> {
      conditions.add(getCondition(parser,context, condition));
    });
    return conditions;
  }

  private Condition getCondition(JsonParser jsonParser,
      DeserializationContext ctxt, JsonNode condition) {
    try {
      JsonParser colParser = this.getParser(condition, jsonParser);
      return ctxt.readValue(colParser, Condition.class);
    } catch(IOException e) {
      throw new DocubeException(MessageCode.ANTHILL_ERR_PARSE, e.getMessage());
    }
  }

  /**
   * Returns a string value property if present, else empty
   *
   * @return
   */
  private String readStringProperty(JsonNode node, String property) {
    JsonNode idNode = node.get(property);
    if (idNode != null && idNode.getNodeType() == JsonNodeType.STRING) {
      return idNode.asText();
    }
    String errMessage = property.concat(" cannot be empty");
    throw new DocubeException(MessageCode.ANTHILL_ERR_FORM_NAME, errMessage);
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
