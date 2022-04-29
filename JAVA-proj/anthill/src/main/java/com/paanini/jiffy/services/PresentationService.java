package com.paanini.jiffy.services;

import com.option3.docube.schema.Dashboard;
import com.option3.docube.schema.layout.Layout;
import com.option3.docube.schema.layout.Section;
import com.paanini.jiffy.utils.RoleManager;
import com.paanini.jiffy.utils.VfsManager;
import com.paanini.jiffy.utils.validator.InputValidator;
import com.paanini.jiffy.vfs.files.Presentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PresentationService {

  @Autowired
   VfsManager vfsManager;

  @Autowired
  RoleManager roleManager;

  static Logger logger = LoggerFactory.getLogger(PresentationService.class);

  public String saveDashboardService(Presentation presentation, String parentId) {
    logger.debug("dashboard object is {}", presentation);

    InputValidator.validateFileName(presentation.getName());
    Presentation ppt = vfsManager.savePresentation(presentation, parentId);
    addDatasetDependency(ppt,parentId);
    return ppt.getId();
  }

  private void addDatasetDependency(Presentation ppt, String appId){
    Set<String> datasetIds = ppt.getContent().getDatasheets().stream()
            .map(datasheet -> datasheet.getId()).collect(Collectors.toSet());
    Set<String> categoryTableIds = vfsManager.getCategoryTableIds(datasetIds,appId);
    datasetIds.addAll(categoryTableIds);
    roleManager.addTableDependency(ppt.getId(), datasetIds,appId);
  }

  public void updateDashboardService(Presentation presentation) {
    InputValidator.validateFileName(presentation.getName());
    logger.debug("dashboard object is {}", presentation);
    vfsManager.updateGeneric(presentation);
    updateDatasetDependency(presentation, presentation.getParentId());
  }

  public void updateDatasetDependency(Presentation ppt, String appId){
    Set<String> datasetIds = ppt.getContent().getDatasheets().stream()
            .map(datasheet -> datasheet.getId()).collect(Collectors.toSet());
    /**
     * category table is fetched with name if the logic is changed
     * need to change the below logic of getting categoryTableIds
     */
    Set<String> categoryTableIds = vfsManager.getCategoryTableIds(datasetIds,appId);
    datasetIds.addAll(categoryTableIds);
    roleManager.updateTableDependency(ppt.getId(), datasetIds,appId);
  }

  public List<String> getCardIds(String presentationId){
    logger.info("[PS] Fetching card id for presentation {}", presentationId);
    List<String> cardIds = new ArrayList<>();
    Presentation presentation = vfsManager.getFile(presentationId);
    List<Section> section = ((Layout)((Dashboard) presentation.getContent()).getLayout()).getSections()
            .stream().collect(Collectors.toList());
    section.forEach((k)->{
      if(k instanceof Section){
        k.getPapers().stream().forEach((p)->{
          p.getColumns().stream().forEach((column)->{
            column.getCards().stream().forEach((card)->{
              if(!Objects.isNull(card.getCardUUID())){
                cardIds.add(card.getCardUUID());
              }
            });
          });
        });
      }
    });
    return cardIds;
  }

}
