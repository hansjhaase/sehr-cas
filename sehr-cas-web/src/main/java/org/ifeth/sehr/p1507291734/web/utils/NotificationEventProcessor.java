/*
 * 
 */
package org.ifeth.sehr.p1507291734.web.utils;

import com.google.common.eventbus.Subscribe;
import java.util.logging.Logger;
import org.ifeth.sehr.p1507291734.web.beans.NotificationEvent;
import org.primefaces.push.EventBus;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
public class NotificationEventProcessor {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");
  private org.primefaces.push.EventBus pfEventBus;

  @Subscribe
  public void handleNotificationEvent(NotificationEvent event) {
    Log.info(NotificationEventProcessor.class.getName() + "(EventBus):NotificationEvent received :" + event.toString());
    if (this.pfEventBus != null) {
      this.pfEventBus.publish("/notification", event.toString());
    }
    //TODO more actions

  }

  public void setWEBSocketProcessor(EventBus pfEventBus) {
    //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    this.pfEventBus = pfEventBus;
  }

}
