package com.paanini.jiffy.utils.jackson.files;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.ColorPalette;
import com.paanini.jiffy.vfs.files.Config;
import com.paanini.jiffy.vfs.files.DataSheet;
import com.paanini.jiffy.vfs.files.FileSet;
import com.paanini.jiffy.vfs.files.Folder;
import com.paanini.jiffy.vfs.files.JiffyTable;
import com.paanini.jiffy.vfs.files.License;
import com.paanini.jiffy.vfs.files.Notebook;
import com.paanini.jiffy.vfs.files.Presentation;
import com.paanini.jiffy.vfs.files.SecureVaultEntry;
import com.paanini.jiffy.vfs.files.SparkModelFile;
import java.io.IOException;

public class PersistableDeserializer extends StdDeserializer<Persistable> {

  public PersistableDeserializer() {
    super(Persistable.class);
  }

  @Override
  public Persistable deserialize(JsonParser jsonParser,
      DeserializationContext deserializationContext) throws IOException {
    JsonNode node = jsonParser.getCodec().readTree(jsonParser);
    return createConcreteFile(node, jsonParser, deserializationContext);
  }

  private <T extends Persistable> T createConcreteFile(JsonNode node, JsonParser jsonParser,
      DeserializationContext deserializationContext) throws IOException {
    JsonParser parser = getParser(node, jsonParser);

    switch (Type.valueOf(node.get("type").asText())){
      case FOLDER: return (T) deserializationContext.readValue(parser, Folder.class);
      case PRESENTATION : return (T) deserializationContext.readValue(parser, Presentation.class);
      case DATASHEET :return (T) deserializationContext.readValue(parser, DataSheet.class);
      //case SQL_APPENDABLE_DATASHEET : return (T) deserializationContext.readValue(parser, DataSheet.class);
      //case KUDU_DATASHEET : return (T) deserializationContext.readValue(parser, Folder.class);
      //case CUSTOM_FILE : return (T) deserializationContext.readValue(parser, .class);
      case SQL_DATASHEET :return (T) deserializationContext.readValue(parser, DataSheet.class);
      case CONFIGURATION : return (T) deserializationContext.readValue(parser, Config.class);
      case FILE : return (T) deserializationContext.readValue(parser, Folder.class);
      case FILESET : return (T) deserializationContext.readValue(parser, FileSet.class);
      case COLOR_PALETTE : return (T) deserializationContext.readValue(parser, ColorPalette.class);
      //case DATASHEET_RESTRICTION : return (T) deserializationContext.readValue(parser, Folder.class);
      case SPARK_MODEL_FILE : return (T) deserializationContext.readValue(parser, SparkModelFile.class);
      //case ALL : return (T) deserializationContext.readValue(parser, Folder.class);
      //case OS_FILE : return (T) deserializationContext.readValue(parser, Folder.class);
      //case KEY_RACK : return (T) deserializationContext.readValue(parser, Folder.class);
      case NOTEBOOK : return (T) deserializationContext.readValue(parser, Notebook.class);
      case JIFFY_TABLE : return (T) deserializationContext.readValue(parser, JiffyTable.class);
      case SECURE_VAULT_ENTRY : return (T) deserializationContext.readValue(parser, SecureVaultEntry.class);
      case APP_ROLES : return (T) deserializationContext.readValue(parser, Folder.class);
      //case JIFFY_TASKS : return (T) deserializationContext.readValue(parser, Folder.class);
      //case BOT_MANAGEMENT : return (T) deserializationContext.readValue(parser, Folder.class);
      case LICENSE : return (T) deserializationContext.readValue(parser, License.class);
      default:
        throw new ProcessingException("Concrete Implementation not found");
    }
    
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
}
