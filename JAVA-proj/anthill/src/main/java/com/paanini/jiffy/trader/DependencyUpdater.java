package com.paanini.jiffy.trader;

import com.paanini.jiffy.constants.TraderConstants;
import com.paanini.jiffy.exception.DocubeException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.models.Edge;
import com.paanini.jiffy.models.Node;
import com.paanini.jiffy.models.TradeApp;
import com.paanini.jiffy.models.TradeEntity;
import com.paanini.jiffy.utils.FileUtils;
import com.paanini.jiffy.utils.MessageCode;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.Folder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import static com.paanini.jiffy.constants.TraderConstants.EXPORT_SUMMARY;

public class DependencyUpdater {
    Persistable file;
    String folderPath;
    Map<String, Set<String>> dependencies;
    TradeApp tradeApp;

    static Logger logger = LoggerFactory.getLogger(DependencyUpdater.class);

    public DependencyUpdater(Persistable file, String folderPath, Map<String, Set<String>> dependencies
            , TradeApp tradeApp){
        this.file = file;
        this.folderPath = folderPath;
        this.dependencies = dependencies;
        this.tradeApp = tradeApp;
    }

    public TradeApp updateDependencies(Persistable file, String folderPath, Map<String, Set<String>> dependencies
            , TradeApp tradeApp) {
        logger.info("[DU] updating dependencies");
        for (Map.Entry<String, Set<String>> variable : dependencies.entrySet()) {
            logger.debug("[DU] checking dependency for {} ", variable.getKey());
            for (String dep : variable.getValue()) {
                tradeApp = processDep(file, folderPath, dependencies, tradeApp, variable, dep);
            }
        }
        logger.info("[DU] Successfully updated dependencies");
        return tradeApp;
    }

    private TradeApp processDep(Persistable file, String folderPath, Map<String, Set<String>> dependencies, TradeApp tradeApp, Map.Entry<String, Set<String>> variable, String dep) {
        logger.debug("[DU] checking dependency for {} with files: {} ", variable.getKey(), variable.getValue());
        if (!dependencies.containsKey(dep)) {
            logger.debug("[DU] dependent file not found {} ", dep);
            Persistable exportFile = ((Folder) file).getChildren().stream()
                    .filter(expFile -> expFile.getValue("name").toString().equalsIgnoreCase(variable.getKey()))
                    .findAny()
                    .orElse(null);
            if(exportFile == null){
                logger.error("[DU] Failed to update the dependency, export file not found");
                throw new ProcessingException("Failed to update the dependency");
            }
            String exportFileName = exportFile.getValue("name").toString();
            String fileType = exportFile.getValue("type").toString();

            tradeApp = updateTradeFile(tradeApp, exportFileName, dep, fileType);
            File exportSummaryFile = getExportSummaryFile(folderPath);
            Path deleteDirectory = Paths.get(folderPath).resolve((String) file.getValue("name"))
                    .resolve("children").resolve(exportFileName);
            if(deleteDirectory.toFile().exists()){
                try {
                    logger.debug("[DU] deleting directory {}, due to missing dependency", deleteDirectory);
                    FileUtils.deleteDirectory(deleteDirectory);
                } catch (IOException e) {
                    logger.error("[DU] Failed to delete the dependent file {}", deleteDirectory);
                    throw new ProcessingException("Failed to delete the dependent file");
                }
            }
            generateExportReport(exportSummaryFile, exportFileName, dep, fileType );
            logger.debug("Removing file {} ", variable.getKey());
        }
        return tradeApp;
    }

    private File getExportSummaryFile(String folderPath) {
        File exportSummaryFile = Paths.get(folderPath).resolve(EXPORT_SUMMARY).toFile();
        if(!exportSummaryFile.exists()){
            try {
                logger.debug("[DU] creating summary file");
                boolean isFileCreated = exportSummaryFile.createNewFile();
                if(!isFileCreated){
                    logger.error("[DU] Failed to create summary file");
                    throw new ProcessingException("Failed to create summary file");
                }
            } catch (IOException e) {
                logger.error("[DU] Failed to create summary file");
                throw new ProcessingException("Failed to create summary file");
            }
        }
        return exportSummaryFile;
    }

    private TradeApp updateTradeFile(TradeApp tradeApp, String exportFile, String dependentFile, String type) {
        logger.debug("[DU] updating trade file");
        updateTradeAppDependencies(tradeApp, exportFile, type.toLowerCase());
        updateNodes(tradeApp, exportFile, type.toLowerCase());
        updateEdges(tradeApp, exportFile, dependentFile, type.toLowerCase());
        return tradeApp;
    }

    private TradeApp updateTradeAppDependencies(TradeApp tradeApp, String exportFile, String fileType) {
        logger.debug("[DU] updating trade app dependencies");
        ((TradeEntity) tradeApp.getFiles().get(fileType)).getList().remove(exportFile);
        return tradeApp;
    }

    private TradeApp updateNodes(TradeApp tradeApp, String exportFile, String fileType) {
        logger.debug("[DU] updating the nodes");
        Node node = tradeApp.getGraph().getNodes().stream().filter(n -> n.getName()
                .equalsIgnoreCase(fileType+"/"+exportFile)).findAny()
                .orElse(null);
        if(node != null) {
            tradeApp.getGraph().getNodes().remove(node);
        }
        return tradeApp;
    }

    private TradeApp updateEdges(TradeApp tradeApp, String exportFile, String dependentFile, String fileType) {
        logger.debug("[DU] updating the edges");
        Edge edge = tradeApp.getGraph().getEdges().stream().filter(e -> e.getStartNode().
                equalsIgnoreCase(fileType+"/"+exportFile) && e.getEndNode().equalsIgnoreCase(dependentFile))
                .findAny()
                .orElse(null);
        if(edge != null){
            tradeApp.getGraph().getEdges().remove(edge);
        }
        return tradeApp;
    }

    private void generateExportReport(File exportSummaryFile, String file, String dependentFile, String fileType) {
        logger.info("[DU] generating export report");
        try(java.io.FileWriter fileWriter = new java.io.FileWriter(exportSummaryFile, true)){
            String errorMessage = String.format(TraderConstants.EXPORT_ERROR_MESSAGE, fileType.toLowerCase(), file,
                    dependentFile);
            fileWriter.write(errorMessage +"\n");

        } catch (IOException e){
            throw new DocubeException(MessageCode.DCBE_ERR_EXP, e.getMessage());
        }
    }
}
