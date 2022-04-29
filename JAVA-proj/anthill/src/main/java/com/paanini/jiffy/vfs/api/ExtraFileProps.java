package com.paanini.jiffy.vfs.api;


import com.paanini.jiffy.dto.AccessEntry;

import java.util.List;

/**
 * @author Priyanka Bhoir
 * @since 12/8/19
 */
public interface ExtraFileProps {
  /**
   *
   * @return
   */
  String getPath();

  /**
   *
   */
  void setPath(String path);


  /**
   *
   * @return
   */
  String getParentId();

  /**
   *
   */
  void setParentId(String id);

  /**
   * Gets the permissions for file in docube
   * @return
   */
  AccessEntry[] getPrivileges();

  /**
   * Sets the permission information on file object,
   * The Permissions are read from jackrabbit nodes
   * @param privileges
   */
  void setPrivileges(AccessEntry[] privileges);

}

