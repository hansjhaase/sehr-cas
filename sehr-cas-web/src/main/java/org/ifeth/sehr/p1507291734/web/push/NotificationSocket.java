/*
 * (C)2015 MDI GmbH for the SEHR Community
 */
package org.ifeth.sehr.p1507291734.web.push;

import java.util.logging.Logger;
import javax.websocket.RemoteEndpoint;
import org.primefaces.push.EventBus;
import org.primefaces.push.annotation.OnClose;
import org.primefaces.push.annotation.OnMessage;
import org.primefaces.push.annotation.OnOpen;
import org.primefaces.push.annotation.PushEndpoint;
import org.primefaces.push.impl.JSONEncoder;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@PushEndpoint("/notification")
public class NotificationSocket {

  private static final Logger Log = Logger.getLogger("org.ifeth.p1507291734.web");

  @OnOpen
  public void onOpen(RemoteEndpoint r, EventBus eventBus) {
    Log.finer(NotificationSocket.class.getName() + ":OnOpen()" + r.toString());
   }
  @OnClose
  public void onClose(RemoteEndpoint r, EventBus eventBus) {
    Log.finer(NotificationSocket.class.getName() + ":OnClose()" + r.toString());
  }

  @OnMessage(encoders = {JSONEncoder.class})
  public String onMessage(String s) {
    return s;
  }
}
