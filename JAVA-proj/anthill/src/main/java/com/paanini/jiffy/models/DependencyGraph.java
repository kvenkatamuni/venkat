package com.paanini.jiffy.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Priyanka Bhoir
 * @since 03/11/20
 */
public class DependencyGraph {
  private List<Node> nodes;
  private List<Edge> edges;

  public DependencyGraph(List<Node> nodes, List<Edge> edges) {
    this.edges = edges;
    this.nodes = nodes;
  }

  public DependencyGraph() {

  }

  public List<Node> getNodes() {
    return nodes;
  }

  public void setNodes(List<Node> nodes) {
    this.nodes = nodes;
  }

  public List<Edge> getEdges() {
    return edges;
  }

  public void setEdges(List<Edge> edges) {
    this.edges = edges;
  }

  public static class Builder {
    private HashMap<String, TradeFile> files;
    private List<Node> nodes = new ArrayList<>();
    private List<Edge> edges = new ArrayList<>();

    private HashMap<String, Set<String>> doc = new HashMap<>();
    private HashMap<String, String> childrenMap = new HashMap<>();

    public static Builder withExistingGraph(DependencyGraph graph) {
      Builder builder = new Builder();
//            builder.nodes = graph.getNodes();
      builder.edges = graph.getEdges();

      return builder;
    }

    public Builder setFiles(HashMap<String, TradeFile> files) {
      this.files = files;
      return this;
    }

    public Builder setDependency(HashMap<String, Set<String>> docubeDeependecies) {
      this.doc = docubeDeependecies;
      return this;
    }

    public DependencyGraph build() {
      files.entrySet()
          .forEach(entry -> {
            visitChildren(entry.getKey(), entry.getValue());
          });

      doc.forEach((key, dependencies) -> {
        String startNode = childrenMap.get(key) == null ? key : childrenMap.get(key);
        List<Edge> nodeEdges = dependencies.stream()
            .map(dependency -> new Edge(startNode,
                childrenMap.get(dependency) == null ? dependency : childrenMap.get(dependency),
                false))
            .collect(Collectors.toList());
        edges.addAll(nodeEdges);
      });

      return new DependencyGraph(nodes, edges);
    }

    private void visitChildren(String currentLevel, TradeFile entity) {
      if(entity instanceof TradeEntity) {
        ((TradeEntity) entity).getList().entrySet().forEach(child -> {
          visitChildren(currentLevel + "/" + child.getKey(), child.getValue());
        });
      } else if (entity != null) {
        nodes.add(new Node(currentLevel, false));
        childrenMap.put(entity.name, currentLevel);
      }
    }
  }
}
