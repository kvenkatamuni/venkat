package com.paanini.jiffy.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.option3.docube.schema.Dashboard;
import com.option3.docube.schema.DataSeries;
import com.option3.docube.schema.approles.Role;
import com.option3.docube.schema.datasheet.filter.ConditionOperand;
import com.option3.docube.schema.datasheet.meta.Column;
import com.option3.docube.schema.graph.ChartContent;
import com.option3.docube.schema.jiffytable.DataSchema;
import com.option3.docube.schema.jiffytable.Form;
import com.option3.docube.schema.jiffytable.InnerTableSchema;
import com.option3.docube.schema.jiffytable.NestedStructureSchema;
import com.paanini.jiffy.jcrquery.QueryModel;
import com.paanini.jiffy.models.ImportAppOptions;
import com.paanini.jiffy.models.TradeApp;
import com.paanini.jiffy.utils.jackson.approles.RoleDeserializer;
import com.paanini.jiffy.utils.jackson.chart.ChartContentDeserializer;
import com.paanini.jiffy.utils.jackson.chart.ChartContentSerializer;
import com.paanini.jiffy.utils.jackson.chart.DashboardDeserializer;
import com.paanini.jiffy.utils.jackson.chart.DashboardSerializer;
import com.paanini.jiffy.utils.jackson.chart.DataSeriesDeserializer;
import com.paanini.jiffy.utils.jackson.chart.DataSeriesSerializer;
import com.paanini.jiffy.utils.jackson.datasheet.ColumnDeserializer;
import com.paanini.jiffy.utils.jackson.datasheet.ColumnSerializer;
import com.paanini.jiffy.utils.jackson.files.PersistableDeserializer;
import com.paanini.jiffy.utils.jackson.filter.ConditionOperandDeserializer;
import com.paanini.jiffy.utils.jackson.filter.ConditionOperandSerializer;
import com.paanini.jiffy.utils.jackson.impex.ImportOptionsDeserializer;
import com.paanini.jiffy.utils.jackson.impex.TradeAppDeserializer;
import com.paanini.jiffy.utils.jackson.jcrquery.QueryModelDeserializer;
import com.paanini.jiffy.utils.jackson.jiffytable.*;
import com.paanini.jiffy.vfs.api.Persistable;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Created by appmgr on 8/9/16.
 */
@Service
public class ObjectMapperFactory {

  static Logger logger = LoggerFactory.getLogger(ObjectMapperFactory.class);

  public ObjectMapperFactory(){
    logger.debug("constructor Object mapper factory");
  }

  private static class IgnoreInheritedIntrospector extends JacksonAnnotationIntrospector {
    @Override
    public boolean hasIgnoreMarker(final AnnotatedMember m) {
      return m.getName().equals("getSchema")
              || m.getName().contains("getClassSchema")
              || m.getName().contains("getSpecificData")
              || m.getName().contains("getFileSchema")
              || m.getName().contains("getLinkId")
              || super.hasIgnoreMarker(m);
    }
  }

  public static ObjectMapper createObjectMapper() {
    logger.debug("Instantiating Object mapper");
    ObjectMapper mapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.addDeserializer(DataSeries.class, new DataSeriesDeserializer());
    module.addDeserializer(ChartContent.class, new ChartContentDeserializer());
    module.addDeserializer(Dashboard.class, new DashboardDeserializer());

    module.addSerializer(DataSeries.class, new DataSeriesSerializer());
    module.addSerializer(ChartContent.class, new ChartContentSerializer());
    module.addSerializer(Dashboard.class, new DashboardSerializer());

    module.addSerializer(ConditionOperand.class, new ConditionOperandSerializer());
    module.addDeserializer(ConditionOperand.class, new ConditionOperandDeserializer());


    module.addSerializer(Column.class, new ColumnSerializer());
    module.addDeserializer(Column.class, new ColumnDeserializer());

    module.addDeserializer(DataSchema.class, new DataSchemaDeserialzer());
    module.addDeserializer(NestedStructureSchema.class, new NestedStructureDeserializer());
    module.addDeserializer(InnerTableSchema.class, new InnerTableDeserializer());
    module.addDeserializer(com.option3.docube.schema.jiffytable.Column.class,
        new com.paanini.jiffy.utils.jackson.jiffytable.ColumnDeserializer());


    //module.addDeserializer(JiffyTableRow.class, new JiffyTableRowDeserializer());
    module.addDeserializer(QueryModel.class, new QueryModelDeserializer());

    module.addDeserializer(Persistable.class,new PersistableDeserializer());

    module.addDeserializer(Role.class, new RoleDeserializer());
    module.addDeserializer(TradeApp.class, new TradeAppDeserializer());
    module.addDeserializer(ImportAppOptions.class, new ImportOptionsDeserializer());
    module.addDeserializer(Form.class, new FormDeserializer());
    mapper.setAnnotationIntrospector(new ObjectMapperFactory.IgnoreInheritedIntrospector());
    mapper.registerModule(module);
    return mapper;
  }

  public static String writeValueAsString(Object data) throws IOException {
    ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
    return objectMapper.writeValueAsString(data);
  }

  public static Map readValue(File file, Class<Map> nodeClass) throws IOException {
    ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
    return objectMapper.readValue(file, nodeClass);
  }

  public static Map readValue(String str, Class<Map> nodeClass) throws IOException {
    ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
    return objectMapper.readValue(str, nodeClass);
  }

  public static <T> T readValueAsObject(String str, Class<T> nodeClass) throws IOException {
    ObjectMapper objectMapper = getInstance();
    return objectMapper.readValue(str, nodeClass);
  }

  private static class ResourceHolder {
    public static final ObjectMapper resource = createObjectMapper();
  }

  public static ObjectMapper getInstance() {
    return ResourceHolder.resource;
  }

}
