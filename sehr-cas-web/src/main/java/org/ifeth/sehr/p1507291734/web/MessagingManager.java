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
 * Refactored from 'sehr-saf' on 3.08.2015
 */
package org.ifeth.sehr.p1507291734.web;

import com.google.common.eventbus.EventBus;
import org.ifeth.sehr.p1507291734.lib.Constants;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.servlet.ServletContext;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang.StringUtils;
import org.ifeth.sehr.core.exception.GenericSEHRException;
import org.ifeth.sehr.p1507291734.web.listener.AdvConnectionListener;
import org.ifeth.sehr.p1507291734.web.listener.JMSExceptionListener;
import org.ifeth.sehr.p1507291734.web.listener.JMSTransportListener;
import org.ifeth.sehr.p1507291734.web.listener.SAFQueueListener;

/**
 * A singelton class to configure and manage the AMQ connection.
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
public class MessagingManager {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");

  private static MessagingManager instance = null;
  private final ServletContext ctx;
  private EventBus eventBus = null;

  private Properties p;
  private int zoneid = 9999999; //testing only!
  private ActiveMQConnectionFactory connectionFactory;
  private Connection jmsconnection;
  private ActiveMQConnection amqconnection;

  protected MessagingManager(ServletContext ctx) {
    // Exists only to defeat instantiation.
    this.ctx = ctx;
    if (ctx != null) {
      this.eventBus = (EventBus) ctx.getAttribute("EventBus");
    }
  }

  public static MessagingManager getInstance(ServletContext ctx) {
    if (instance == null) {
      instance = new MessagingManager(ctx);
    }
    return instance;
  }

  public Connection getConnection() {
    return jmsconnection;
  }

  public ActiveMQConnectionFactory getAMQConnectionFactory() {
    return connectionFactory;
  }

  public ActiveMQConnection getAMQConnection() {
    if (getConnection() == null) {
      return null;
    }
    return (ActiveMQConnection) jmsconnection;
  }

  public boolean isConnected() {
    if (getConnection() == null) {
      return false;
    }
    return (!amqconnection.isClosed());
  }

  public boolean isStarted() {
    if (!isConnected()) {
      return false;
    }
    return amqconnection.isStarted();
  }

  /**
   * Check for AMQ components (none if AMQ was down on start/deployment).
   *
   * @return
   */
  public boolean isConfigured() {
    ActiveMQConnectionFactory amqConFac = (ActiveMQConnectionFactory) ctx.getAttribute("ActiveMQConnectionFactory");
    ActiveMQConnection amqCon = (ActiveMQConnection) ctx.getAttribute("ActiveMQConnection");
    if (amqCon != null && amqConFac != null) {
      return true;
    }
    return false;
  }

  public boolean configure(Properties p) {
    this.p = p;
    this.zoneid = Integer.parseInt(p.getProperty("zoneid", "0000000"));

    return initJMS();
  }

  /**
   * (re-)Init JMS connection but don't start it.
   *
   * Start connection in a module where a listener can process the messages!
   *
   * @param ctx
   * @return
   */
  private boolean initJMS() {
    if (this.ctx != null) {
      amqconnection = (ActiveMQConnection) ctx.getAttribute("ActiveMQConnection");
      if (amqconnection != null && !amqconnection.isClosed()) {
        try {
          amqconnection.close();
        } catch (Exception ex) {
          Log.log(Level.SEVERE, MessagingManager.class.getName() + ":initJMS():Error closing existing connection:{0}", ex.toString());
        }
        ctx.setAttribute("ActiveMQConnection", null);
        ctx.setAttribute("ActiveMQConnectionFactory", null);
      }
    }
    //--- using the properties file from WEB-INF...
    //String url = p.getProperty("activemqurl");
    //changed 12/2015
    String url = p.getProperty("sehrxnetzoneurl");
    if (url == null || url.isEmpty()) {
      Log.severe(MessagingManager.class.getName() + ":initJMS():No local broker property.");
      return false;
    }

    //--- init new connection
    connectionFactory = new ActiveMQConnectionFactory(url);
    try {
      //jmsConnection is heavyweight... keep it!
      jmsconnection = connectionFactory.createConnection(p.getProperty("sehrxnetzoneuser"), p.getProperty("sehrxnetzonepw"));
      //amqConnection = ActiveMQConnectionFactory.createActiveMQConnection((String)p.getProperty("activemqUser"), (String)p.getProperty("activemqPw"));
      amqconnection = (ActiveMQConnection) jmsconnection;
    } catch (Exception ex) {
      Log.log(Level.SEVERE, MessagingManager.class.getName() + ":initJMS():Init JMS Error:{0}", ex.toString());
      return false;
    }
    //--- set ClientID
    try {
      jmsconnection.setClientID("ZoneID:" + String.format("%07d", zoneid) + "#ModulePIK:" + Constants.MODULE_PIK + "#0"); // + new Date().getTime());
    } catch (javax.jms.InvalidClientIDException ace) {
      Log.log(Level.WARNING, MessagingManager.class.getName() + ":initJMS():Invalid ID:{0}", ace.toString());
      try {
        //use an alternative unique ID
        jmsconnection.setClientID("ZoneID:" + String.format("%07d", zoneid) + "#ModulePIK:" + Constants.MODULE_PIK + "#" + new Date().getTime());
      } catch (JMSException ex) {
        Log.log(Level.SEVERE, MessagingManager.class.getName() + ":initJMS():Ongoing Invalid ClientID:{0}", ex.getMessage());
        try {
          //close on errors
          jmsconnection.close();
        } catch (JMSException ex1) {
          Log.log(Level.SEVERE, MessagingManager.class.getName() + ":initJMS():Error closing connection after ongoing ClientID error:{0}", ex1.getMessage());
        }
        return false;
      }
    } catch (JMSException ex) {
      Log.log(Level.SEVERE, MessagingManager.class.getName() + ":initJMS():JMS error:{0}", ex.getMessage());
      return false;
    }

    //On stateless EJB's the listeners will be thrown away after accessing the 
    //SL EJB. So we can't use that kind of listening there but here we can! ;)
    try {
      //don't start unless there is a listener to process messages...
      //amqconnection.start();
      amqconnection.setExceptionListener(new JMSExceptionListener(ctx));
      amqconnection.addTransportListener(new JMSTransportListener(ctx));
    } catch (JMSException ex) {
      Log.log(Level.SEVERE, MessagingManager.class.getName() + ":initJMS():JMS Error:{0}", ex.toString());
      return false;
    }
    if (ctx != null) {
    //--- by team decision: the recommended way to track instances 
      //track the local broker for zone based messaging
      ctx.setAttribute("ActiveMQConnection", amqconnection);
      ctx.setAttribute("ActiveMQConnectionFactory", connectionFactory);
    }
    //--- connection should be open but may not started at this point
    return (!amqconnection.isClosed());
  }

  public void close() {
    if (amqconnection != null && !amqconnection.isClosed()) {
      try {
        amqconnection.close();
        if(ctx!=null){
          ctx.setAttribute("ActiveMQConnection", null);
        }
        //ctx.setAttribute("ActiveMQConnectionFactory", null);
        Log.log(Level.INFO, MessagingManager.class.getName() + ":close():Messaging connection closed.");
      } catch (JMSException ex) {
        Log.log(Level.SEVERE, MessagingManager.class.getName() + ":close():JMS error {0}", ex.getMessage());
      }
    }
  }

  public void startAdvMonitor(String monitor) throws GenericSEHRException {
    switch (monitor) {
      case "ConnectionAdvisory":
      default:
        if (ctx.getAttribute("EventBus") != null) {
          throw new GenericSEHRException("Application error: EventBus not found!");
        }
        if (ctx.getAttribute(AdvConnectionListener.class.getName()) != null) {
          throw new GenericSEHRException("Listener (Monitor) already configured.");
        }
        EventBus evBus = (EventBus) ctx.getAttribute("EventBus");
        AdvConnectionListener advJMSListener = AdvConnectionListener.getInstance(evBus);
        if (advJMSListener.configure(p, amqconnection)) {
          ctx.setAttribute(AdvConnectionListener.class.getName(), advJMSListener);
          Log.log(Level.INFO, MessagingManager.class.getName() + ":configureAdvMonitor():AMQ connection: started=" + amqconnection.isStarted() + " / URL=" + amqconnection.getBrokerInfo().getBrokerURL());
        }
    }
  }

  public boolean addServiceListener(String zoneid) {
    if (StringUtils.isBlank(zoneid)) {
      //use registered zone
      int zid = Integer.parseInt(p.getProperty("zoneid"));
      zoneid = String.format("%07d", zid);
    } else if (zoneid.length() != 7) {
      //by convention 7 digets only
      Log.log(Level.INFO, MessagingManager.class.getName() + ":addServiceListener():Zone ID failure (not 7 digets): " + zoneid);
      return false;
    }
    //+++8/2015: 'saf' changed to 'service'
    //String queue = "sehr." + zoneid + ".saf.queue";  
    String queue = "sehr." + zoneid + ".service.queue";

    ActiveMQConnection amqConnection = (ActiveMQConnection) ctx.getAttribute("ActiveMQConnection");
    if (amqConnection == null || amqConnection.isClosed()) {
      Log.log(Level.INFO, MessagingManager.class.getName() + ":addServiceListener():No AMQ Connection for serving zone ID " + zoneid);
      return false;
    }
    Map<String, SAFQueueListener> safListeners = (HashMap) ctx.getAttribute("SAFQueueListener");
    if (safListeners == null) {
      safListeners = new HashMap<>();
      ctx.setAttribute("SAFQueueListener", safListeners);
    }
    SAFQueueListener serviceListener;
    //stop and remove existing listener on a queue...    
    if (safListeners.containsKey(queue)) {
      serviceListener = (SAFQueueListener) safListeners.get(queue);
      serviceListener.stop();
      safListeners.remove(queue);
    } else {
      serviceListener = new SAFQueueListener();
    }
    //create or reinitialize new listener
    if (serviceListener.configure(ctx, queue)) {
      safListeners.put(queue, serviceListener);
      Log.log(Level.INFO, MessagingManager.class.getName() + ":addSAFListener():Listening on queue '" + queue + "' started. / URL=" + amqConnection.getBrokerInfo().getBrokerURL());
    } else {
      Log.log(Level.INFO, MessagingManager.class.getName() + ":addSAFListener():Listening on queue '" + queue + "' failed! / URL=" + amqConnection.getBrokerInfo().getBrokerURL());
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    String s = "No AMQ ConnectionFactory.";
    if (connectionFactory != null) {
      s = connectionFactory.getBrokerURL();
      if (connectionFactory.getClientID() != null) {
        s += "#" + connectionFactory.getClientID();
      }
      s += "@" + Integer.toHexString(hashCode());
    }
    return s;
  }
}
