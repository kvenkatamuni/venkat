package com.paanini.jiffy.utils;

import com.paanini.jiffy.exception.ProcessingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.paanini.jiffy.vfs.io.FileSystemWriter;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class DependencyGraph {

  private static Logger LOGGER = LoggerFactory.getLogger(FileSystemWriter.class);
  private final DefaultDirectedGraph graph;

  private DependencyGraph(DefaultDirectedGraph graph) {
    this.graph = graph;
  }

  public static DependencyGraph create(Map<String, Set<String>> dependencies) {
    return new DependencyGraph(createGraph(dependencies, new HashMap<>()));
  }

  public static DependencyGraph createWithAliases(Map<String, Set<String>> dependencies,
      Map<String, List<String>> aliases) {
    aliases = aliases == null ? new HashMap<>() : aliases;
    return new DependencyGraph(createGraph(dependencies, aliases));
  }

  private static DefaultDirectedGraph createGraph(Map<String, Set<String>> dependencies,
      final Map<String, List<String>> aliases) {
    DefaultDirectedGraph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
    for (Map.Entry<String, Set<String>> variable : dependencies.entrySet()) {
      graph.addVertex(variable.getKey());
    }

    CycleDetector<String, DefaultEdge> cycleDetector = new CycleDetector<>(graph);
    if (cycleDetector.detectCycles()) {
      throw new ProcessingException("variables cannot be cyclically dependent on each other"
          + Objects.toString(cycleDetector.findCycles()));
    }
    return graph;
  }

  public List<String> getSortedNodes() {
    TopologicalOrderIterator<String, DefaultEdge> orderIterator = new TopologicalOrderIterator<>(graph);

    List<String> sortedColumns = new ArrayList<>();
    while (orderIterator.hasNext()) {
      String column = orderIterator.next();
      sortedColumns.add(column);
    }
    return sortedColumns;
  }
}
