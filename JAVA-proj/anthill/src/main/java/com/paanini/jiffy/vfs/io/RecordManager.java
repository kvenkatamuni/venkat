package com.paanini.jiffy.vfs.io;

import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.authorizationManager.AuthorizationUtils;
import com.paanini.jiffy.constants.Content;
import com.paanini.jiffy.constants.FileProps;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.jcrquery.readers.JCRQuery;
import com.paanini.jiffy.utils.NodeLocator;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import ai.jiffy.secure.client.auditlog.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Objects;
import java.util.Optional;

import static com.paanini.jiffy.utils.CacheUtils.generateKey;

/**
 * Created by Priyanka Bhoir on 5/8/19
 *
 * The RecordManager controls the CRUD operations on content repository node.
 */
public class RecordManager {
  private final Services services;
  private final Session session;
  private final NodeLocator nodeLocator;
  AuditLogger auditLogger;
  static Logger logger = LoggerFactory.getLogger(RecordManager.class);
  public RecordManager(Services services, Session session,
                       NodeLocator nodeLocator) {
    this.services = services;
    auditLogger = services.getAuditLogger();
    this.nodeLocator = nodeLocator;
    this.session = session;
  }

  /**
   * Reads a content node as one of the {@code Persistable} classes
   * @param node Jackrabbit node to be read
   * @return Actual type representing a {@code Persistable} object
   */
  public <T extends Persistable>T read(Node node, FolderViewOption option)
          throws RepositoryException {
    CacheManager cacheManager = services.getCacheManager();
    Cache cache = cacheManager.getCache(Content.CACHE_NAME);
    String cacheKey = getKey(node);
    Persistable persistable = null;
    if (Objects.nonNull(cacheKey)){
      persistable = cache.get(cacheKey, Persistable.class);
    }
    JCRQuery jcrQuery = option.getJCRQuery();
    if(Objects.nonNull(persistable)){
      boolean permission = jcrQuery.hasPermission(node, session, option.getView());
      if(permission){
        logger.info("Reading from cache for file {}" , node.getName());
        return (T) persistable;
      }else {
        logger.error("User doesn't have permission for :" + node.getName());
        throw new ProcessingException("User doesn't have " +
                "permission to perform this action");
      }
    }
    T result = readWithouCache(node, option);
    if(!skipCaching(result)){
      String key = getKey(node);
      if(Objects.nonNull(key)){
        cache.putIfAbsent(key,result);
      }
    }

    return result;
  }
  
  private <T extends Persistable> T readWithouCache(Node node, FolderViewOption option){
    node = NodeUtils.getLinkSource(node);
//        services.setKeyRackManager(getNodeKeyRack(node));
    ContentNode contentNode = new ContentNode(node, session,
            option.getJCRQuery());

    JackrabbitReader reader = new JackrabbitReader(services, option);
    contentNode.accept(reader);

    return (T) reader.getResult();
  }

  /**
   * writes a persistable file onto the jackrabbit.
   * @param file
   * @param node
   * @param <T>
   * @return
   */
  public <T extends Persistable>T create(T file, Node node)
          throws RepositoryException {
//        services.setKeyRackManager(getNodeKeyRack(node));
    String name = file.getValue("name").toString();
    name = name.trim();
    file.setValue("name",name);
    JackrabbitWriter writer = JackrabbitWriter.forCreate(services, node);
    file.accept(writer);

    if(Utils.skipCommioncreateLog(node,file,"add")){
     return file;
    }
    String Component = Utils.getComponent(file);
    String msg = Utils.getEndMessage(file, node,"Add");
    String msg1 = Utils.getStartMEssage("Addition", Component);
    auditLogger.log(Component,
            "Add",
            new StringBuilder(msg1).
                    append(((BasicFileProps) file).getName())
                    .append(msg)
                    .toString(),
            "Success",
            Optional.empty());
    return file;
  }

  /**
   * updates an existing persistable file in jackrabbit.
   * @param file
   * @param node
   * @param <T>
   * @return
   */
  public <T extends Persistable>T update(T file, Node node)
          throws RepositoryException {
//        services.setKeyRackManager(getNodeKeyRack(node));
    JackrabbitWriter writer = JackrabbitWriter.forUpdate(services, node.getParent());
    file.accept(writer);
    invalidateCache((BasicFileProps) file);
    if(Utils.skipCommioncreateLog(node.getParent(),file,"update")){
      return file;
    }
    String Component = Utils.getComponent(file);
    String msg = Utils.getEndMessage(file,node.getParent(),"update");
    String msg1 = Utils.getStartMEssage("Updation",Component);

    auditLogger.log(Component,
            "Update",
            new StringBuilder(msg1).
                    append(((BasicFileProps)file).getName())
                    .append(msg)
                    .toString(),
            "Success",
            Optional.empty());
    return file;
  }

  private void invalidateCache(BasicFileProps file) {
    CacheManager cacheManager = services.getCacheManager();
    Cache cache = cacheManager.getCache(Content.CACHE_NAME);
    String key = generateKey(file.getId(), file.getType().toString());
    if(Objects.nonNull(key)){
      cache.evictIfPresent(key);
    }
  }

  /*private KeyRackManager getNodeKeyRack(Node node) throws RepositoryException {
    String path = nodeLocator
            .getUserBasePath(getOwner(node));
    return new KeyRackManager(session, path);
  }*/

  private String getOwner(Node parent) throws RepositoryException {
    return parent.hasProperty(FileProps.OWNER)
            ? parent.getProperty(FileProps.OWNER).getString()
            : this.session.getUserID();
  }

  private Boolean skipCaching(Persistable file){
    BasicFileProps fileProps = (BasicFileProps) file;
    Type type = fileProps.getType();
    if(AuthorizationUtils.cacheSkippedFiles().contains(type)){
      return true;
    }
    return false;
  }

  private String getKey(Node node) throws RepositoryException {
      String identifier = node.getIdentifier();
      String type = node.getProperty("type").getString();
      return generateKey(identifier,type);
  }

}

