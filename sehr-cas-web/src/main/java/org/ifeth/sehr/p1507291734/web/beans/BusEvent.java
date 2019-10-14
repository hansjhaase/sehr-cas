/*
 *
 */
package org.ifeth.sehr.p1507291734.web.beans;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
public abstract class BusEvent {

  private Object eventObject;

  public BusEvent(Object o) {
    this.eventObject = 0;
  }

  public Object getEvent() {
    return this.eventObject;
  }
}
