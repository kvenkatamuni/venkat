package com.paanini.jiffy.utils;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.security.AccessControlException;
import javax.jcr.version.VersionException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JcrUtils {

  static class Holder{
    static Map<String,String> map = new HashMap<>();
    static{
      map.put(AccessControlException.class.getSimpleName(),"Access control");
      map.put(ConstraintViolationException.class.getSimpleName(),"Constraint violated");
      map.put(InvalidItemStateException.class.getSimpleName(),"Invalid item state");
      map.put(InvalidLifecycleTransitionException.class.getSimpleName(),"Invalid life cycle");
      map.put(InvalidNodeTypeDefinitionException.class.getSimpleName(),"Invalid node type definition");
      map.put(InvalidQueryException.class.getSimpleName(),"Invalid query");
      map.put(InvalidSerializedDataException.class.getSimpleName(),"Invalid serialized data");
      map.put(ItemExistsException.class.getSimpleName(),"Item already exists");
      map.put(ItemNotFoundException.class.getSimpleName(),"Item not found");
      map.put(LockException.class.getSimpleName(),"Cannot lock");
      map.put(LoginException.class.getSimpleName(),"Cannot login");
      map.put(MergeException.class.getSimpleName(),"Cannot merge");
      map.put(NamespaceException.class.getSimpleName(),"Invalid namespace");
      map.put(NodeTypeExistsException.class.getSimpleName(),"Node already Exists");
      map.put(NoSuchNodeTypeException.class.getSimpleName(),"No such node types");
      map.put(NoSuchWorkspaceException.class.getSimpleName(),"No such workspace");
      map.put(PathNotFoundException.class.getSimpleName(),"Path not found");
      map.put(ReferentialIntegrityException.class.getSimpleName(),
              "File already in use. Referential integrity violation");
      map.put(UnsupportedRepositoryOperationException.class.getSimpleName(),"Unsupported repository operation");
      map.put(ValueFormatException.class.getSimpleName(),"Invalid value format");
      map.put(VersionException.class.getSimpleName(),"Version error");
      map.put(IOException.class.getSimpleName(),"Avro Serialization error");
      map.put(AccessDeniedException.class.getSimpleName(), "The " +
              "operation is not permitted for current user. ");
    }
    static Map<String, String> getValues(){
      return map;
    }
  }

  public static String mapException(Class<?> klass){
    return Holder.getValues()
            .containsKey(klass.getSimpleName()) ? Holder.getValues().get(klass.getSimpleName()) : "";
  }
}
