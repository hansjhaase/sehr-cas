/*
 * 
 */
package org.ifeth.sehr.p1507291734.web.beans;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
public class NotificationEvent extends BusEvent {

  long dtStamp;

  public NotificationEvent(String msg) {
    super(msg);
    this.dtStamp = System.currentTimeMillis();
  }

  @Override
  public String toString() {
    return (String) super.getEvent();
  }
}
