/*
 * (C) MDI GmbH
 */
package org.ifeth.sehr.p1507291734.web.listener;

import java.util.logging.Logger;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.servlet.ServletContext;

/**
 *
 * @author hansjhaase
 */
public class JMSExceptionListener implements ExceptionListener {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");
  private final ServletContext ctx;
  
  public JMSExceptionListener(ServletContext ctx) {
    this.ctx=ctx;
  }

  @Override
  public void onException(JMSException jmse) {
    Log.warning("JMSExceptionListener:onException():" + jmse.getMessage());
    ctx.setAttribute("isJMSConnected", false);
  }

}
