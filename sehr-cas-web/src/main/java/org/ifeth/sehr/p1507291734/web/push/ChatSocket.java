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
@PushEndpoint("/chatMsgCount")
public class ChatSocket {

  private static final Logger Log = Logger.getLogger("org.ifeth.p1507291734.web");

  @OnOpen
  public void onOpen(RemoteEndpoint r, EventBus eventBus) {
    Log.finer(ChatSocket.class.getName() + ":OnOpen()" + r.toString());
    //eventBus.publish(room + "/*", new Message(String.format("%s has entered the room '%s'",  username, room), true));
  }
  @OnClose
  public void onClose(RemoteEndpoint r, EventBus eventBus) {
    Log.finer(ChatSocket.class.getName() + ":OnClose()" + r.toString());
    //eventBus.publish(room + "/*", new Message(String.format("%s has left the room '%s'",  username, room), true));
  }

  @OnMessage(encoders = {JSONEncoder.class})
  public String onMessage(String count) {
    return count;
  }
}
