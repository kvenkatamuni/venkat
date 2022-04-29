package com.paanini.jiffy.encryption.api;

import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.exception.VaultException;

import java.util.Optional;

/**
 * Created by Nidhin Francis on 01/05/2020.
 */

public interface Vault {

  /** returns encrypted value based on the named key provided - hashicorp
   *  stores value in vault - cyberArk
   *
   * @param key
   * @param value
   * @return encrypted String - hashicorp , empty String - cyberark
   * @throws ProcessingException
   * returns decrypted value based on the named key provided - hashicorp
   *  returns the value based on the key - CyberArk
   * @param key
   * @param data encrypted data in case of Hashicorp , empty in cyber ark
   * @return decrypted value - hashicorp, value based on  key -  cyberArk
   * @throws ProcessingException
   *//*
    String get(String key,String data) throws VaultException; //document the parameter details*/

  /**delete the named key - Hashicorp,
   * @param key
   * @throws ProcessingException
   */
  void delete(String key)  throws VaultException;

  /**returns the encrypted value based on the key - HashiCorp
   * updates value reperesented by the key - CyberArk
   * @param key
   * @param value
   * @return encrypted value - Hashicorp , empty String - CyberArk
   * @throws ProcessingException
   */
  String update(String key, String value) throws VaultException;

  /**
   * Initialize the server
   * @throws ProcessingException
   */
  void init() throws ProcessingException, VaultException;

  default boolean check(VaultInput input)
  {
    return true;
  }

  String insert(VaultInput input) throws VaultException;

  String get(VaultInput input) throws VaultException;
}

