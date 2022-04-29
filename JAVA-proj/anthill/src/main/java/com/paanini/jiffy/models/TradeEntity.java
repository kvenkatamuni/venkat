package com.paanini.jiffy.models;

import java.util.HashMap;

/**
 * @author Athul Krishna N S
 * @since 02/11/20
 */
public class TradeEntity extends TradeFile{
  public TradeEntity(String name, boolean selected, HashMap<String, TradeFile> list, String type) {
    super(name, selected);
    this.list = list;
    this.type = type;
  }

  HashMap<String, TradeFile> list;
  String type;

  public HashMap<String, TradeFile> getList() {
        return list;
    }

  public void setList(HashMap<String, TradeFile> list) {
        this.list = list;
    }

  public String getType() {
        return type;
    }

  public void setType(String type) {
        this.type = type;
    }
}
