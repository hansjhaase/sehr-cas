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
import org.apache.activemq.command.ConnectionId;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
public class MQClients implements Serializable {

  private ConnectionId conid; //Key!
  private String clientid;
  private String clientip;
  private Long conOpened;
  private Long conClosed;

  public MQClients() {
  }

  public MQClients(ConnectionId conid) {
    this.conid = conid;
  }

  /**
   * @return the clientid
   */
  public String getClientId() {
    return clientid;
  }

  /**
   * @param clientid the clientid to set
   */
  public void setClientId(String clientid) {
    this.clientid = clientid;
  }

  /**
   * @return the clientip
   */
  public String getClientIP() {
    return clientip;
  }

  /**
   * @param clientip the clientip to set
   */
  public void setClientIP(String clientip) {
    this.clientip = clientip;
  }

  /**
   * @return the conOpended
   */
  public Long getConOpened() {
    return conOpened;
  }

  /**
   * @param l the time the connection has been opened
   */
  public void setConOpened(Long l) {
    this.conOpened = l;
  }

  /**
   * @return the conClosed
   */
  public Long getConClosed() {
    return conClosed;
  }

  /**
   * @param conClosed the conClosed to set
   */
  public void setConClosed(Long conClosed) {
    this.conClosed = conClosed;
  }
  @Override
  public String toString(){
    return conid.getValue()+" - "+clientid;
  }

  /**
   * @return the conid
   */
  public ConnectionId getConid() {
    return conid;
  }
  /**
   * @return the conid
   */
  public String getConIdAsString() {
    return conid.getValue();
  }

  /**
   * @param conid the conid to set
   */
  public void setConid(ConnectionId conid) {
    this.conid = conid;
  }
}
