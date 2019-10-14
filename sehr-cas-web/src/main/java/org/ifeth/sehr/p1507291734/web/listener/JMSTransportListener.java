/*
 * (C) MDI GmbH
 */
package org.ifeth.sehr.p1507291734.web.listener;

import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import org.apache.activemq.transport.TransportListener;

/**
 *
 * @author hansjhaase
 */
public class JMSTransportListener implements TransportListener {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");
  private final ServletContext ctx;

  public JMSTransportListener(ServletContext ctx) {
    this.ctx = ctx;
  }

  @Override
  public void onCommand(Object command) {
    Log.finer("JMSTransportListener:onCommand():'" + command + "'");
    //logger.finest("JMSTransportListener:onCommand():ctx path=" + ctx.getContextPath());
  }

  @Override
  public void onException(IOException exception) {
    Log.finer("JMSTransportListener:onException():'" + exception + "'");
    ctx.setAttribute("isJMSConnected", false);
  }

  @Override
  public void transportInterupted() {
    Log.info("JMSTransportListener:Transport interuption detected.");
    ctx.setAttribute("isJMSConnected", false);
  }

  @Override
  public void transportResumed() {
    Log.info("JMSTransportListener:Transport resumption detected.");
    ctx.setAttribute("isJMSConnected", true);
  }
}
