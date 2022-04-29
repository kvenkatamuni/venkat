package com.paanini.jiffy.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.option3.docube.schema.approles.Role;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.AppRoles;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradeApp {

  String id;
  String name;
  String path;
  HashMap<String, TradeFile> files;
  DependencyGraph graph;

  public TradeApp() {
  }

  public TradeApp(String id, String name, String path, HashMap<String, TradeFile> files,
                  DependencyGraph graph) {
    this.id = id;
    this.name = name;
    this.path = path;
    this.files = files;
    this.graph = graph;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public HashMap<String, TradeFile> getFiles() {
    return files;
  }

  public void with(HashMap<String, TradeFile> files) {
    this.files = files;
  }

  public DependencyGraph getGraph() {
    return graph;
  }

  public void setGraph(DependencyGraph graph) {
    this.graph = graph;
  }

  public static class Builder {

    private String id;
    private String name;
    private String path;
    private HashMap<String, TradeFile> files = new HashMap<>();

    private HashMap<String, Set<String>> dependencies = new HashMap<>();
    private Optional<DependencyGraph> graph = Optional.empty();

    public static Builder fromExisting(TradeApp tradeApp) {
      Builder builder = new Builder();
      builder.id = tradeApp.id;
      builder.name = tradeApp.name;
      builder.path = tradeApp.path;
      builder.files = tradeApp.files;
      builder.graph = Optional.ofNullable(tradeApp.graph);
      return builder;
    }

    public Builder setId(String id) {
      this.id = id;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setPath(String path) {
      this.path = path;
      return this;
    }

    public Builder addFileInfo(Persistable persistable) {
      BasicFileProps fileProps = (BasicFileProps) persistable;
      Type type = (fileProps).getType();
      TabType tradeType = TabType.from(type);

      if (tradeType != null) {
        if (type == Type.APP_ROLES) {
          /**
           * commented this logic because custom roles have been revamped
           * changed from APP ROLE file type
           */
        } else {
          addFile(((BasicFileProps) persistable).getName(), type);
        }
      }

      return this;
    }

    public Builder addRoles(String name) {
      if (this.files.containsKey(TabType.userRoles.name())) {
        if (this.files.get(TabType.userRoles.name()) instanceof TradeEntity) {
          ((TradeEntity) this.files.get(TabType.userRoles.name())).getList().put(name,
              new TradeFile(name, false));
        }
      } else {
        HashMap<String, TradeFile> tab = new HashMap<>();
        tab.put(name, new TradeFile(name, false));

        this.files.put(TabType.userRoles.name(), new TradeEntity(TabType.userRoles.name()
            , false, tab, TabType.userRoles.name()));
      }

      return this;
    }

    public Builder addFile(String name, Type type) {
      TabType tradeType = TabType.from(type);

      //set the dafault value of selected file as false
      if (tradeType != null) {
        if (this.files.containsKey(tradeType.name())) {
          if (this.files.get(tradeType.name()) instanceof TradeEntity) {
            ((TradeEntity) this.files.get(tradeType.name())).getList().put(name,
                new TradeFile(name, false));
          }
        } else {
          //set the dafault value of selected file as false
          HashMap<String, TradeFile> tab = new HashMap<>();
          tab.put(name, new TradeFile(name, false));
          this.files.put(tradeType.name(), new TradeEntity(tradeType.getDisplayName(),
              false, tab, tradeType.name()));
        }
      }

      return this;
    }

    public Builder setJiffyFiles(HashMap<String, TradeFile> jiffyFiles) {
      files.putAll(jiffyFiles);
      return this;
    }

    public Builder setDependecies(HashMap<String, Set<String>> dependecies) {
      this.dependencies = dependecies;
      return this;
    }

    public Builder setJiffyDependency(JiffyDependency dependency) {
      dependencies.putAll(flattenJiffyDependencies(dependency));
      return this;
    }

    public TradeApp buildApp() {
      Arrays.stream(TabType.values()).forEach(tabType -> {
        if (!files.containsKey(tabType.name())) {
          files.put(tabType.name(), new TradeEntity(tabType.getDisplayName(), false,
              new HashMap<>(), tabType.name()));
        }
      });
      DependencyGraph.Builder graphBuilder = graph.isPresent() ?
          DependencyGraph.Builder.withExistingGraph(graph.get()) :
          new DependencyGraph.Builder();
      DependencyGraph dGraph = graphBuilder.setFiles(files).setDependency(dependencies).build();

      return new TradeApp(id, name, path, this.files, dGraph);
    }

    private Map<String, Set<String>> flattenJiffyDependencies(JiffyDependency dependency) {
      HashMap<String, Set<String>> flat = new HashMap<>();

      flat.putAll(flattenTab(TabType.cluster.name(), dependency.getCluster()));
      flat.putAll(flattenTab(TabType.configurations.name(), dependency.getConfigurations()));
      flat.putAll(flattenTab(TabType.excelMacros.name(), dependency.getExcelMacros()));
      flat.putAll(flattenTab(TabType.functions.name(), dependency.getFunctions()));
      flat.putAll(flattenTab(TabType.tasks.name(), dependency.getTasks()));
      flat.putAll(flattenTab(TabType.ui.name(), dependency.getUi()));
      flat.putAll(flattenTab(TabType.uicomponents.name(), dependency.getUicomponents()));
      flat.putAll(flattenTab(TabType.templates.name(), dependency.getTemplates()));
      flat.putAll(flattenTab(TabType.flows.name(), dependency.getFlows()));

      return flat;
    }

    private Map<String, Set<String>> flattenTab(String prefix, Map<String, Object> tab) {
      HashMap<String, Set<String>> dep = new HashMap<>();

        if (tab == null) {
            return dep;
        }

      tab.forEach((key, value1) -> {
        if (value1 instanceof Map) {
          dep.putAll(flattenTab(prefix + "/" + key, (Map<String, Object>) value1));
        } else if (value1 instanceof List) {
          List value = (List) value1;
          dep.put(prefix + "/" + key, new HashSet<String>(value));
        }
      });

      return dep;
    }
  }
}
