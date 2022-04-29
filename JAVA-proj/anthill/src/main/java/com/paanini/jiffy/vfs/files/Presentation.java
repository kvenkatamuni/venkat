package com.paanini.jiffy.vfs.files;

import com.option3.docube.schema.CardStorageType;
import com.option3.docube.schema.DataSeries;
import com.option3.docube.schema.datasheet.Datasheet;
import com.option3.docube.schema.filter.Filter;
import com.option3.docube.schema.graph.TreeMap;
import com.option3.docube.schema.graph.*;
import com.option3.docube.schema.narration.NarrationCard;
import com.option3.docube.schema.narration.SingleValueGauge;
import com.option3.docube.schema.nodes.PresentationSchema;
import com.option3.docube.schema.nodes.Type;
import com.option3.docube.schema.statistics.StatisticsContent;
import com.option3.docube.schema.table.PivotContent;
import com.option3.docube.schema.table.TableContent;
import com.paanini.jiffy.dto.AccessEntry;
import com.paanini.jiffy.utils.SchemaUtils;
import com.paanini.jiffy.vfs.api.*;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by appmgr on 29/1/16.
 * presentation is stored like:
 *
 *                              ______________
 *                             | Presentation | --> props
 *                              --------------
 *                                  ||
 *                          JCR_CONTENT (Folder)
 *                       /                      \
 *       PRESENTATION_CONTENT(File)          REFERENCES(Folder)
 *              |                              /  | .. \
 *    JCR_CONTENT(NT_RESOURCE)               Multiple link node
 *    (Actual data is here)                 (Links to datasheets)
 *
 */
public class Presentation extends PresentationSchema implements ExtraFileProps, BasicFileProps,
        Persistable, Exportable {

  public static final String REFERENCES = "references";
  private String path;
  private AccessEntry[] privileges;
  private String parentId;

  public Presentation(){
    setType(Type.PRESENTATION);
  }

  public Presentation(final SpecificRecordBase schema) {
    PresentationSchema cp = (PresentationSchema) schema;
    SchemaUtils.copy(cp, this);
  }

  public void updateReferences(Node file, List<Node> references) throws RepositoryException {
    Node presentationFolder = file.getNode(Property.JCR_CONTENT);
    if(presentationFolder.hasNode(REFERENCES)){
      presentationFolder.getNode(REFERENCES).remove();
    }

    Node referencesFolder = presentationFolder.addNode(REFERENCES, NodeType.NT_FOLDER);
    references.forEach(node -> {
      try {
        Node added = referencesFolder.addNode(node.getName(), NodeType.NT_LINKED_FILE);
        added.setProperty(Property.JCR_CONTENT, node);
      } catch (RepositoryException e) {
        throw new RuntimeException(e);
      }
    });

  }

  public void updateCardStorageType(CardStorageType storageType, Predicate<String> predicate) {
    updateCardStorageType(storageType, predicate, "id");
  }
  public void updateCardStorageType(CardStorageType storageType, Predicate<String> predicate,
                                    String column) {
    this.getContent().getContent().forEach(cardContent -> {
      if (cardContent instanceof ChartContent) {
        processChartContent(storageType, predicate, column, (ChartContent) cardContent);
      } else if (cardContent instanceof NarrationCard) {
        processNarrationCard(storageType, predicate, (NarrationCard) cardContent);
      } else if (cardContent instanceof StatisticsContent) {
        StatisticsContent st = (StatisticsContent) cardContent;
        //todo: update flag for stats
      } else if (cardContent instanceof TableContent) {
        processTableContent(storageType, predicate, (TableContent) cardContent);
      } else if (cardContent instanceof PivotContent) {
        processPivotContent(storageType, predicate, (PivotContent) cardContent);
      }

    });
  }

  private void processPivotContent(CardStorageType storageType, Predicate<String> predicate, PivotContent cardContent) {
    PivotContent pc = cardContent;
    if (pc.getDatasheet() != null && predicate.test(pc
            .getDatasheet().getId())) {
      pc.setCardStorageType(storageType);
      pc.setTableRows(new ArrayList<>());
    }
  }

  private void processTableContent(CardStorageType storageType, Predicate<String> predicate, TableContent cardContent) {
    TableContent tc = cardContent;
    if (tc.getDatasheet() != null && predicate.test(tc
            .getDatasheet().getId())) {
      tc.setCardStorageType(storageType);
      tc.setTableRows(new ArrayList<>());
    }
  }

  private void processNarrationCard(CardStorageType storageType, Predicate<String> predicate, NarrationCard cardContent) {
    NarrationCard n = cardContent;
    List<SingleValueGauge> singleValueGauge = n.getSingleValueGauge();
    boolean isDatasheetUsed = singleValueGauge != null &&
            singleValueGauge.stream()
                    .filter(s -> s.getDatasheet() != null)
                    .map(s ->  s.getDatasheet().getId())
                    .anyMatch(predicate);
    if (isDatasheetUsed) {
      n.setCardStorageType(storageType);
      singleValueGauge.stream().forEach(s -> s.setValue(null));
    }
  }

  private void processChartContent(CardStorageType storageType, Predicate<String> predicate, String column, ChartContent cardContent) {
    ChartContent c = cardContent;
    List<Object> dataComponent = c.getDataComponent();
    if (isDatasheetUsed(predicate, dataComponent, column)) {
      c.setCardStorageType(storageType);
      c.setDataComponent(this.emptyDataComponents(dataComponent, Optional.empty()));
    }
  }

  public void updateCardDataStatus(Predicate<String> predicate) {
    updateCardDataStatus(predicate, "id");
  }
  public void updateCardDataStatus(Predicate<String> predicate, String propName) {
    this.getContent().getContent().forEach(cardContent -> {
      if (cardContent instanceof ChartContent) {
        processChartContent(predicate, propName, (ChartContent) cardContent, Optional.empty());
      } else if (cardContent instanceof NarrationCard) {
        processNarrationCard(predicate, (NarrationCard) cardContent);
      } else if (cardContent instanceof StatisticsContent) {
        StatisticsContent st = (StatisticsContent) cardContent;
        //todo: update flag for stats
      } else if (cardContent instanceof TableContent) {
        processTableContent(predicate, (TableContent) cardContent);
      } else if (cardContent instanceof PivotContent) {
        processPivotContent(predicate, (PivotContent) cardContent);
      }

    });

  }

  private void processPivotContent(Predicate<String> predicate, PivotContent cardContent) {
    PivotContent pc = cardContent;
    if (predicate.test(pc.getDatasheet().getId())) {
      pc.setForceReplaceData(true);
      pc.setTableRows(new ArrayList<>());
    }
  }

  private void processTableContent(Predicate<String> predicate, TableContent cardContent) {
    TableContent tc = cardContent;
    if (predicate.test(tc.getDatasheet().getId())) {
      tc.setForceReplaceData(true);
      tc.setTableRows(new ArrayList<>());
    }
  }

  private void processNarrationCard(Predicate<String> predicate, NarrationCard cardContent) {
    NarrationCard n = cardContent;
    List<SingleValueGauge> singleValueGauge = n.getSingleValueGauge();
    boolean isDatasheetUsed = singleValueGauge != null &&
            singleValueGauge.stream()
                    .map(s -> s.getDatasheet().getId())
                    .anyMatch(predicate);
    if (isDatasheetUsed) {
      n.setForceReplaceData(true);
      singleValueGauge.stream().forEach(s -> s.setValue(null));
    }
  }

  private boolean isDatasheetUsed(Predicate<String> predicate, List<Object> dataComponent,
                                  String column) {
    return dataComponent.stream().anyMatch(comp -> {
      if (comp instanceof Rectangle) {
        Rectangle component = (Rectangle) comp;
        return isPredicateMatche(predicate, column, component.getDatasheet());
      } else if (comp instanceof Triangle) {
        Triangle component = (Triangle) comp;
        return isPredicateMatche(predicate, column, component.getDatasheet());
      } else if (comp instanceof Arc) {
        Arc component = (Arc) comp;
        return isPredicateMatche(predicate, column, component.getDatasheet());
      } else if (comp instanceof Line) {
        Line component = (Line) comp;
        return isPredicateMatche(predicate, column, component.getDatasheet());
      } else if (comp instanceof Area) {
        Area component = (Area) comp;
        return isPredicateMatche(predicate, column, component.getDatasheet());
      } else if (comp instanceof Pie) {
        Pie component = (Pie) comp;
        return isPredicateMatche(predicate, column, component.getDatasheet());
      } else if (comp instanceof TreeMap) {
        TreeMap component = (TreeMap) comp;
        return isPredicateMatche(predicate, column, component.getDatasheet());
      } else if (comp instanceof ConstantLine) {
        ConstantLine component = (ConstantLine) comp;
        return isPredicateMatche(predicate, column, component.getDatasheet());
      }
      return false;
    });
  }

  private boolean isPredicateMatche(Predicate<String> predicate, String column, Datasheet datasheet) {
    return datasheet != null && predicate.test
            (datasheet.get(column).toString());
  }

  private List<Object> emptyDataComponents(List<Object> dataComponent, Optional<Datasheet> dt) {
    return dataComponent.stream().map(comp -> {
      if (comp instanceof Rectangle) {
        return processRectangle(dt, (Rectangle) comp);
      } else if (comp instanceof Triangle) {
        return processTriangle(dt, (Triangle) comp);
      } else if (comp instanceof Arc) {
        return processArc(dt, (Arc) comp);
      } else if (comp instanceof Line) {
        return processLine(dt, (Line) comp);
      } else if (comp instanceof Area) {
        return processArea(dt, (Area) comp);
      } else if (comp instanceof Pie) {
        return processPie(dt, (Pie) comp);
      } else if (comp instanceof TreeMap) {
        return processTreeMap(dt, (TreeMap) comp);
      } else if (comp instanceof ConstantLine) {
        return processConstantLine(dt, (ConstantLine) comp);
      }
      return comp;
    }).collect(Collectors.toList());
  }

  private ConstantLine processConstantLine(Optional<Datasheet> dt, ConstantLine comp) {
    ConstantLine component = comp;
    List<DataSeries> seriesList= Arrays.asList(
            component.getConstantSeries()
    );
    seriesList.forEach(series -> {
      series.setValues(new ArrayList<>());
    });
    dt.ifPresent(d -> {
      if(component.getDatasheet().getName().equals(d.getName())) {
        component.setDatasheet(d);
      }
    });
    return component;
  }

  private TreeMap processTreeMap(Optional<Datasheet> dt, TreeMap comp) {
    TreeMap component = comp;
    List<DataSeries> seriesList= Arrays.asList(
            component.getXseries(), component.getYseries(), component.getFillSeries(), component.getRepeatSeries(),
            component.getSort(), component.getLimit()
    );
    seriesList.forEach(series -> {
      series.setValues(new ArrayList<>());
    });
    dt.ifPresent(d -> {
      if(component.getDatasheet().getName().equals(d.getName())) {
        component.setDatasheet(d);
      }
    });
    return component;
  }

  private Pie processPie(Optional<Datasheet> dt, Pie comp) {
    Pie component = comp;
    List<DataSeries> seriesList= Arrays.asList(
            component.getXseries(), component.getYseries(), component.getFillSeries(), component.getRepeatSeries(),
            component.getSort(), component.getLimit()
    );
    seriesList.forEach(series -> {
      series.setValues(new ArrayList<>());
    });
    dt.ifPresent(d -> {
      if(component.getDatasheet().getName().equals(d.getName())) {
        component.setDatasheet(d);
      }
    });
    return component;
  }

  private Area processArea(Optional<Datasheet> dt, Area comp) {
    Area component = comp;
    List<DataSeries> seriesList= Arrays.asList(
            component.getXseries(), component.getYseries(), component.getFillSeries(), component.getRepeatSeries(),
            component.getSort(), component.getLimit(), component.getLowerBound()
    );
    seriesList.forEach(series -> {
      series.setValues(new ArrayList<>());
    });
    dt.ifPresent(d -> {
      if(component.getDatasheet().getName().equals(d.getName())) {
        component.setDatasheet(d);
      }
    });
    return component;
  }

  private Line processLine(Optional<Datasheet> dt, Line comp) {
    Line component = comp;
    List<DataSeries> seriesList= Arrays.asList(
            component.getXseries(), component.getYseries(), component.getFillSeries(), component.getRepeatSeries(),
            component.getSort(),
            component.getLimit()
    );
    seriesList.forEach(series -> {
      series.setValues(new ArrayList<>());
    });
    dt.ifPresent(d -> {
      if(component.getDatasheet().getName().equals(d.getName())) {
        component.setDatasheet(d);
      }
    });
    return component;
  }

  private Arc processArc(Optional<Datasheet> dt, Arc comp) {
    Arc component = comp;
    List<DataSeries> seriesList= Arrays.asList(
            component.getXseries(), component.getYseries(), component.getFillSeries(), component.getRepeatSeries(),
            component.getSort(), component.getLimit(), component.getElementLength(), component.getStartAngle(),
            component.getEndAngle()
    );
    seriesList.forEach(series -> {
      series.setValues(new ArrayList<>());
    });
    dt.ifPresent(d -> {
      if(component.getDatasheet().getName().equals(d.getName())) {
        component.setDatasheet(d);
      }
    });
    return component;
  }

  private Triangle processTriangle(Optional<Datasheet> dt, Triangle comp) {
    Triangle component = comp;
    List<DataSeries> seriesList= Arrays.asList(
            component.getXseries(), component.getYseries(), component.getFillSeries(), component.getRepeatSeries(),
            component.getSort(), component.getLimit(), component.getElementLength(), component.getDirection()
    );
    seriesList.forEach(series -> {
      series.setValues(new ArrayList<>());
    });
    dt.ifPresent(d -> {
      if(component.getDatasheet().getName().equals(d.getName())) {
        component.setDatasheet(d);
      }
    });
    return component;
  }

  private Rectangle processRectangle(Optional<Datasheet> dt, Rectangle comp) {
    Rectangle component = comp;
    List<DataSeries> seriesList= Arrays.asList(
            component.getXseries(), component.getYseries(), component.getFillSeries(), component.getRepeatSeries(),
            component.getSort(), component.getLimit()
    );
    seriesList.forEach(series -> {
      series.setValues(new ArrayList<>());
    });
    dt.ifPresent(d -> {
      if(component.getDatasheet().getName().equals(d.getName())) {
        component.setDatasheet(d);
      }
    });
    return component;
  }


  private void updateCardDataSheet(Predicate<String> predicate, String propName,
                                   Datasheet newDatasheet) {
    this.getContent().getContent().forEach(cardContent -> {
      if (cardContent instanceof ChartContent) {
        processChartContent(predicate, propName, (ChartContent) cardContent, Optional.ofNullable(newDatasheet));
      } else if (cardContent instanceof NarrationCard) {
        processNarrationCard(predicate, newDatasheet, (NarrationCard) cardContent);
      } else if (cardContent instanceof StatisticsContent) {
        StatisticsContent st = (StatisticsContent) cardContent;
        //todo: update flag for stats
      } else if (cardContent instanceof TableContent) {
        processTableContent(predicate, newDatasheet, (TableContent) cardContent);
      } else if (cardContent instanceof PivotContent) {
        processPivotContent(predicate, newDatasheet, (PivotContent) cardContent);
      }

    });

  }

  private void processPivotContent(Predicate<String> predicate, Datasheet newDatasheet, PivotContent cardContent) {
    PivotContent pc = cardContent;
    if (pc.getDatasheet() != null && predicate.test(pc.getDatasheet().getName())) {
      pc.setForceReplaceData(true);
      pc.setTableRows(new ArrayList<>());
      pc.setDatasheet(newDatasheet);
    }
  }

  private void processTableContent(Predicate<String> predicate, Datasheet newDatasheet, TableContent cardContent) {
    TableContent tc = cardContent;
    if (tc.getDatasheet() != null && predicate.test(tc.getDatasheet().getName())) {
      tc.setForceReplaceData(true);
      tc.setTableRows(new ArrayList<>());
      tc.setDatasheet(newDatasheet);
    }
  }

  private void processNarrationCard(Predicate<String> predicate, Datasheet newDatasheet, NarrationCard cardContent) {
    NarrationCard n = cardContent;
    List<SingleValueGauge> singleValueGauge = n.getSingleValueGauge();
    boolean isDatasheetUsed = singleValueGauge != null &&
            singleValueGauge.stream()
                    .filter(s -> Objects.nonNull(s.getDatasheet()))
                    .map(s -> s.getDatasheet().getName())
                    .anyMatch(predicate);
    if (isDatasheetUsed) {
      n.setForceReplaceData(true);
      singleValueGauge.stream().forEach(s -> {
        s.setValue(null);
        if(s.getDatasheet().getName() == newDatasheet.getName()) {
          s.setDatasheet(newDatasheet);
        }
      });

    }
  }

  private void processChartContent(Predicate<String> predicate, String propName, ChartContent cardContent, Optional<Datasheet> newDatasheet2) {
    ChartContent c = cardContent;
    List<Object> dataComponent = c.getDataComponent();
    boolean usingSameDatasheet = isDatasheetUsed(predicate, dataComponent, propName);
    if (usingSameDatasheet) {
      c.setForceReplaceData(true);
      c.setDataComponent(this.emptyDataComponents(c.getDataComponent(), newDatasheet2));
    }
  }

  @Override
  public Schema getFileSchema() {
    return this.getSchema();
  }

  @Override
  public void setValue(int field, Object value) {
    this.put(field, value);
  }

  @Override
  public void accept(VfsVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public Object getValue(String fieldName) {
    return this.get(fieldName);
  }

  @Override
  public void setValue(String fieldName, Object value) {
    this.put(fieldName, value);
  }

  @Override
  public String getPath() {
    return this.path;
  }

  @Override
  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public String getParentId() {
    return parentId;
  }

  @Override
  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  @Override
  public AccessEntry[] getPrivileges() {
    return privileges;
  }

  @Override
  public void setPrivileges(AccessEntry[] privileges) {
    this.privileges = privileges;
  }

  @Override
  public Set<String> getDependencies() {
    return this.getContent().getDatasheets()
            .stream()
            .map(datasheet -> "@path:" + datasheet.getId().concat("|").concat(datasheet.getName()))
            .collect(Collectors.toSet());
  }

  @Override
  public Set<String> updateDependencies() {
    return Collections.<String>emptySet();
  }

  public void updateDependencies(List<Persistable> files){
    if((files != null)) {
      Map<String, Persistable> newFiles = getNewFiles(files);

      ArrayList<Datasheet> updatedDatasheets = new ArrayList<>();
      this.getContent().getDatasheets().forEach((file)->{
        if(newFiles.containsKey(file.getName())) {
          Persistable newDatasheet = newFiles.get(file.getName());
          file.setPath(((ExtraFileProps) newDatasheet).getPath());
          file.setId(((BasicFileProps) newDatasheet).getId());

          // update the dependent datasheet
          this.updateCardDataSheet(dtName -> dtName.equals(file.getName()),
                  "name", file);
        }


        updatedDatasheets.add(file);
      });
      if(updatedDatasheets.size() > 0) {
        this.getContent().setDatasheets(updatedDatasheets);
      }
      ArrayList<Object> contents = new ArrayList<>();
      List<Object> content = this.getContent().getContent();
      for(Object c : content){
        if(c instanceof NarrationCard){
          processNarrationCard(updatedDatasheets, contents, (NarrationCard) c);
        }else if(c instanceof Filter){
          processFilter(updatedDatasheets, contents, (Filter) c);
        }else {
          contents.add(c);
        }
      }
    }

  }

  private Map<String, Persistable> getNewFiles(List<Persistable> files) {
    Map<String, Persistable> newFiles = new HashMap<>();
    for(Persistable f : files){
      if(!newFiles.containsKey(((BasicFileProps)f).getName())){
        newFiles.put(((BasicFileProps)f).getName(), f);
      }
    }
    return newFiles;
  }

  private void processFilter(ArrayList<Datasheet> updatedDatasheets, ArrayList<Object> contents, Filter c) {
    Filter f = c;
    Datasheet datasheet = f.getDatasheet();
    if(Objects.nonNull(datasheet)){
      Optional<Datasheet> first = updatedDatasheets.stream()
              .filter(e -> e.getName().equals(datasheet.getName())).findFirst();
      if(first.isPresent()){
        f.setDatasheet(first.get());
      }
    }
    contents.add(f);
  }

  private void processNarrationCard(ArrayList<Datasheet> updatedDatasheets, ArrayList<Object> contents, NarrationCard c) {
    NarrationCard c1 = c;
    List<SingleValueGauge> singleValueGauge = c1.getSingleValueGauge();
    ArrayList<SingleValueGauge> newSingleGuage = new ArrayList<>();
    if(Objects.nonNull(singleValueGauge)){
      for(SingleValueGauge s : singleValueGauge){
        Datasheet datasheet = s.getDatasheet();
        if(Objects.nonNull(datasheet)){
          Optional<Datasheet> first = updatedDatasheets.stream()
                  .filter(e -> e.getName().equals(datasheet.getName())).findFirst();
          if(first.isPresent()){
            s.setDatasheet(first.get());
          }
        }
        newSingleGuage.add(s);
      }
    }
    c1.setSingleValueGauge(newSingleGuage);
    contents.add(c1);
  }

}