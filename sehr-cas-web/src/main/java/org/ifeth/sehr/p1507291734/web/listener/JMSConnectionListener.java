/*
 * (C) 2012 MDI GmbH
 */
package org.ifeth.sehr.p1507291734.web.listener;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.event.ConnectionEvent;
import javax.mail.event.ConnectionListener;
import javax.servlet.ServletContext;

/**
 *
 * @author hansjhaase
 */
public class JMSConnectionListener implements ConnectionListener {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");
  private final ServletContext ctx;

  public JMSConnectionListener(ServletContext ctx) {
    this.ctx = ctx;
  }

  @Override
  public void opened(ConnectionEvent e) {
    Log.log(Level.WARNING, "JMSConnectionListener:opened():" + e.toString());
    ctx.setAttribute("isJMSConnected", true);
  }

  @Override
  public void disconnected(ConnectionEvent e) {
    Log.log(Level.WARNING, "JMSConnectionListener:disconnected():" + e.toString());
    ctx.setAttribute("isJMSConnected", false);
  }

  @Override
  public void closed(ConnectionEvent e) {
    Log.log(Level.WARNING, "JMSConnectionListener:closed():" + e.toString());
    ctx.setAttribute("isJMSConnected", false);
  }
}
