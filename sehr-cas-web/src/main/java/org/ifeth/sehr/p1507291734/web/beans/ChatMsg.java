/*
 * (C) 2015 MDI GmbH for the SEHR Community
 * Licensed under the European Union Public Licence - EUPL v.1.1 ("License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://ec.europa.eu/idabc/servlets/Doc?id=31979
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Refactored from 'sehr-saf-tool' on 3.08.2015
 */
package org.ifeth.sehr.p1507291734.web.beans;

import java.io.Serializable;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
public class ChatMsg implements Serializable {

  private String ID; //UUID of the message, useful for references also
  //reference to an ID for answering on messages
  private String parentID;
  //flag originator UUID to identify replys and own messages
  private String origUUID;
  private String from; //nickname
  private Integer userIDFrom;
  private Short type = 0; //default PUBLIC
  private String privateToNick;
  private Integer privateToUserID;
  private Integer centerFrom; //if originator of the msg is a center
  private Integer centerTo;
  private Integer zoneFrom;
  private Integer zoneTo;
  private String text;

  /**
   * Creates new object of a chat message.
   *
   * @param origUUID session UUID of the handlet that produces the message
   * @param uuid unique UUID for each message
   */
  public ChatMsg(String origUUID, String uuid) {
    this.ID = uuid;
    this.origUUID = origUUID;
  }

  /**
   * @return the ID
   */
  public String getID() {
    return ID;
  }

  /**
   * @param ID the ID to set
   */
  public void setID(String ID) {
    this.ID = ID;
  }

  /**
   * @return the parentID
   */
  public String getParentID() {
    return parentID;
  }

  /**
   * @param parentID the parentID to set
   */
  public void setParentID(String parentID) {
    this.parentID = parentID;
  }

  /**
   * @return the from
   */
  public String getFrom() {
    return from;
  }

  /**
   * @param from the from to set
   */
  public void setFrom(String from) {
    this.from = from;
  }

  /**
   * @return the userIDFrom
   */
  public Integer getUserIDFrom() {
    return userIDFrom;
  }

  /**
   * @param userIDFrom the userIDFrom to set
   */
  public void setUserIDFrom(Integer userIDFrom) {
    this.userIDFrom = userIDFrom;
  }

  /**
   * @return the type
   */
  public Short getType() {
    return type;
  }

  /**
   * @param type the type to set
   */
  public void setType(Short type) {
    this.type = type;
  }

  /**
   * @return the privateTo
   */
  public String getPrivateToNick() {
    return privateToNick;
  }

  /**
   * @param privateTo the privateTo to set
   */
  public void setPrivateToNick(String privateTo) {
    this.privateToNick = privateTo;
  }

  /**
   * @return the privateUserIDTo
   */
  public Integer getPrivateToUserID() {
    return privateToUserID;
  }

  /**
   * @param usrid
   * @param privateUserIDTo the privateUserIDTo to set
   */
  public void setPrivateToUserID(Integer usrid) {
    this.privateToUserID = usrid;
  }

  /**
   * @return the centerTo
   */
  public Integer getCenterTo() {
    return centerTo;
  }

  /**
   * @param centerTo the centerTo to set
   */
  public void setCenterTo(Integer centerTo) {
    this.centerTo = centerTo;
  }

  /**
   * @return the zoneFrom
   */
  public Integer getZoneFrom() {
    return zoneFrom;
  }

  /**
   * @param zoneFrom the zoneFrom to set
   */
  public void setZoneFrom(Integer zoneFrom) {
    this.zoneFrom = zoneFrom;
  }

  /**
   * @return the zoneTo
   */
  public Integer getZoneTo() {
    return zoneTo;
  }

  /**
   * @param zoneTo the zoneTo to set
   */
  public void setZoneTo(Integer zoneTo) {
    this.zoneTo = zoneTo;
  }

  /**
   * @return the text
   */
  public String getText() {
    return text;
  }

  /**
   * @param text the text to set
   */
  public void setText(String text) {
    this.text = text;
  }

  /**
   * @return the centerFrom
   */
  public Integer getCenterFrom() {
    return centerFrom;
  }

  /**
   * @param centerFrom the centerFrom to set
   */
  public void setCenterFrom(Integer centerFrom) {
    this.centerFrom = centerFrom;
  }

  /**
   * @return the origUUID
   */
  public String getOrigUUID() {
    return origUUID;
  }

  /**
   * @param origUUID the origUUID to set
   */
  public void setOrigUUID(String origUUID) {
    this.origUUID = origUUID;
  }

  //--------------------------------------------- formatted output

  public String getFromText(String s) {
    StringBuilder sb = new StringBuilder();
    sb.append(this.from);
    sb.append(s); //"\n" or ":" etc.
    sb.append(this.text);
    return sb.toString();
  }

  public String getFromPrivateToText(String s1, String s2) {
    StringBuilder sb = new StringBuilder();
    sb.append(this.from);
    sb.append(s1); //"@" etc.
    sb.append(this.privateToNick);
    sb.append(s2); //"\n" or ":" etc.
    sb.append(this.text);
    return sb.toString();
  }

  @Override
  public String toString() {
    return text + " [" + this.ID + "]";
  }
}
