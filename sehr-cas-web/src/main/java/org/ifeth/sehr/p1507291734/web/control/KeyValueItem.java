/*
 * (C) IFETHr.
 */
package org.ifeth.sehr.p1507291734.web.control;

import java.io.Serializable;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
public class KeyValueItem implements Serializable{
  private short key;
  private String value;
  
  public KeyValueItem(short key, String value){
    this.key=key;
    this.value=value;
  }

  /**
   * @return the key
   */
  public short getKey() {
    return key;
  }

  /**
   * @param key the key to set
   */
  public void setKey(short key) {
    this.key = key;
  }

  /**
   * @return the value
   */
  public String getValue() {
    return value;
  }

  /**
   * @param value the value to set
   */
  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 29 * hash + this.key;
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final KeyValueItem other = (KeyValueItem) obj;
    if (this.key != other.key) {
      return false;
    }
    return true;
  }
  
  @Override
  public String toString(){
    return this.value+" ("+this.key+")";
  }
}
