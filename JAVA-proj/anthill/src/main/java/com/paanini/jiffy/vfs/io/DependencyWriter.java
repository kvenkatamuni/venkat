package com.paanini.jiffy.vfs.io;

import com.paanini.jiffy.utils.RoleManager;
import com.paanini.jiffy.utils.VfsManager;
import com.paanini.jiffy.vfs.api.Exportable;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.api.VfsVisitor;
import com.paanini.jiffy.vfs.files.Folder;
import com.paanini.jiffy.vfs.files.JiffyTable;
import com.paanini.jiffy.vfs.files.Presentation;
import com.paanini.jiffy.vfs.files.SecureVaultEntry;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Priyanka Bhoir
 * @since 03/11/20
 */
public class DependencyWriter implements VfsVisitor {
  private Stack<Folder> folders = new Stack<>();
  private Persistable result;
  private VfsManager vfsManager;
  private final RoleManager roleManager;
  Map<String, List<Persistable>> dependencyMap;
  static Logger LOGGER = LoggerFactory.getLogger(DependencyWriter.class);

  public DependencyWriter(Map<String,List<Persistable>> dependencyMap,
                          VfsManager vfsManager,
                          RoleManager roleManager) {
    this.dependencyMap=dependencyMap;
    this.vfsManager = vfsManager;
    this.roleManager = roleManager;
  }
  @Override
  public void enterFolder(Folder folder) {
    folders.push(folder);
  }

  @Override
  public void visit(Persistable file) {
    if((!dependencyMap.isEmpty()) && (file instanceof Exportable)){
      try {
        ((Exportable) file).updateDependencies(dependencyMap.get(file.getValue("name")));
      } catch (Exception e) {
        LOGGER.warn("Failed while updating dependency ", e);
      }
    }

    if(file instanceof SecureVaultEntry){
      vfsManager.assignAdminPrivilleges(((SecureVaultEntry) file).getCreatedBy()
          ,((SecureVaultEntry) file).getPath(),((SecureVaultEntry) file).getGlobal());
    }

    if(file instanceof Presentation){
      Presentation presentation = (Presentation) file;
      Set<String> datasetIds = presentation.getContent().getDatasheets().stream()
              .map(datasheet -> datasheet.getId()).collect(Collectors.toSet());
      roleManager.addTableDependency(presentation.getId(), datasetIds,presentation.getParentId());
    }

    if(file instanceof JiffyTable){
      roleManager.upsertJiffyTableDependency((JiffyTable) file);
    }

  }

  @Override
  public void exitFolder(Folder folder) {
    Folder pop = folders.pop();
    if (folders.empty()) {
      result = pop;
    }
  }

  public Persistable getUpdatedDocument(){
    return result;
  }
}
