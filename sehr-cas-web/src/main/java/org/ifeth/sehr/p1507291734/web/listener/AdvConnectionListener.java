/*
 * (C) 2015 MDI GmbH
 * 
 */
package org.ifeth.sehr.p1507291734.web.listener;

import com.google.common.eventbus.EventBus;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.*;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.advisory.AdvisorySupport;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ConnectionId;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.activemq.command.DataStructure;
import org.apache.activemq.command.RemoveInfo;
import org.ifeth.sehr.p1507291734.web.beans.MQClients;
import org.ifeth.sehr.p1507291734.web.beans.NotificationEvent;

/**
 * Listen for AMQ connection messages.
 *
 * @author hansjhaase
 */
public class AdvConnectionListener implements MessageListener {
  
  private static final Logger logger = Logger.getLogger("org.ifeth.sehr.p1507291734.web");
  private static AdvConnectionListener advConListener;
  //private static final String FS = System.getProperty("file.separator");
  //private static final String ACKFILE = System.getProperty("java.io.tmpdir") + FS + "AckMessage_";
  private Properties p;
  private ActiveMQConnection jmsconnection = null;
  private Session session;
  
  private int debug = 0;
  //@EJB(beanName = "JMedDatasource")
  //this listener class is not a bean, we must use JNDI
  //java:global/Database-ejb/Datasource
  //private DatasourceBean datasource;
  private final Map<ConnectionId, MQClients> mqClients = new ConcurrentHashMap<>();
  private EventBus evBus; //app baed event processing

  private AdvConnectionListener(EventBus evBus) {
    this.evBus = evBus;
  }

  /**
   * Activate connection monitoring.
   * <p>
   * Use
   * {@link #configure(java.util.Properties, org.apache.activemq.ActiveMQConnection)}
   * and run {@link #activateAdvisory()} after configuration</p>
   *
   * @param evBus
   * @return
   */
  public static synchronized AdvConnectionListener getInstance(EventBus evBus) {
    if (advConListener == null) {
      advConListener = new AdvConnectionListener(evBus);
    }
    return advConListener;
  }
  
  private void initialize() {
    this.debug = Integer.parseInt(p.getProperty("debug", "0"));
    logger.finer(AdvConnectionListener.class.getName() + ":initialize():debug=" + debug);
  }
  
  public boolean configure(Properties p, ActiveMQConnection amqCon) {
    this.p = p;
    if (amqCon == null || amqCon.isClosed()) {
      logger.finer(AdvConnectionListener.class.getName() + ":configure():No valid AMQ connection.");
      evBus.post(new NotificationEvent("Missing AMQ connection to start advisory connection listener"));
      return false;
    }
    if (isAdvisorySession()) {
      stop(); //stop before start/restart
    }
    this.jmsconnection = amqCon;
    activateAdvisory(); //and start advisory process
    return isAdvisorySession();
  }
  
  public boolean isAdvisorySession() {
    return (this.session != null);
  }
  
  public boolean activateAdvisory() {
    initialize();
    try {
      this.session = jmsconnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    } catch (JMSException ex) {
      logger.log(Level.SEVERE, AdvConnectionListener.class.getName() + ":activateAdvisory():JMS Session Error:" + ex.getMessage());
      return false;
    }
    //Destination advisoryDestination = session.createTopic("ActiveMQ.Advisory..>");
    Destination advisoryDestination = AdvisorySupport.getConnectionAdvisoryTopic();
    //Destination advisoryDestination = AdvisorySupport.getFullAdvisoryTopic(destination);
    try {
      MessageConsumer advisory = this.session.createConsumer(advisoryDestination);
      advisory.setMessageListener(this);
      if (!jmsconnection.isStarted()) {
        jmsconnection.start();
      }
    } catch (JMSException ex) {
      logger.log(Level.SEVERE, AdvConnectionListener.class.getName() + ":activateAdvisory():JMS error " + ex.getMessage());
      return false;
    }
    return true;
  }

  /**
   * Process connection advisory messages...
   *
   * @param msg
   */
  @Override
  public void onMessage(Message msg) {
    
    logger.finest(AdvConnectionListener.class.getName() + ":onMessage():Processing advisory message '" + msg.toString() + "'");
    if (msg instanceof ActiveMQMessage) {
      ActiveMQMessage aMsg = (ActiveMQMessage) msg;
      DataStructure ds = aMsg.getDataStructure();
      if (ds.getDataStructureType() == ConnectionInfo.DATA_STRUCTURE_TYPE) {
        ConnectionInfo ci = (ConnectionInfo) ds;
        String clientid = ci.getClientId();
        logger.fine(AdvConnectionListener.class.getName() + ":onMessage():Processing ConnectionInfo '" + ci.getConnectionId() + "'");
        MQClients mqClient;
        if (!mqClients.containsKey(ci.getConnectionId())) {
          mqClient = new MQClients(ci.getConnectionId());
          mqClient.setClientIP(ci.getClientIp());
          mqClient.setClientId(clientid);
          mqClient.setConOpened(System.currentTimeMillis());
          mqClient.setConClosed(null);
          mqClients.put(ci.getConnectionId(), mqClient);
        } else {
          mqClient = mqClients.get(ci.getConnectionId());
          mqClient.setClientIP(ci.getClientIp());
          mqClient.setClientId(clientid);
          mqClient.setConOpened(System.currentTimeMillis());
          mqClient.setConClosed(null);
        }
//        try {
//          Destination dest = this.session.createQueue("sehr.9999998.0601311.queue"); // the destination of the client
//          ConsumerEventSource source = new ConsumerEventSource(jmsconnection, dest);
//          source.setConsumerListener(new ConsumerListener() {
//            @Override
//            public void onConsumerEvent(ConsumerEvent event) {
//              if (event.isStarted()) {
//                System.out.println("a new consumer has started - " + event.getConsumerId());
//              } else {
//                System.out.println("a consumer has dropped - " + event.getConsumerId());
//              }
//            }
//          });
//        } catch (JMSException ex) {
//          logger.info(AdvConnectionListener.class.getName() + ":onMessage():Error creating destination event listener:" + ex.getMessage());
//        }
        logger.info(AdvConnectionListener.class.getName() + ":onMessage():Advisory message:Connection started:" + ci);
      } else if (ds.getDataStructureType() == RemoveInfo.DATA_STRUCTURE_TYPE) {
        RemoveInfo ri = (RemoveInfo) ds;
        ConnectionId conid = (ConnectionId) ri.getObjectId();
        MQClients mqClient;
        if (!mqClients.containsKey(conid)) {
          mqClient = new MQClients(conid);
          mqClient.setClientIP(null);
          mqClient.setClientId(null);
          mqClient.setConOpened(null); //unknown
          mqClient.setConClosed(System.currentTimeMillis());
          mqClients.put(conid, mqClient);
        } else {
          mqClient = mqClients.get(conid);
          mqClient.setConClosed(System.currentTimeMillis());
        }
        logger.info(AdvConnectionListener.class.getName() + ":onMessage():Advisory message:Connection removed:" + ri);
      } else {
        logger.info(AdvConnectionListener.class.getName() + ":onMessage():Advisory message:Other connectioninfo:" + ds);
      }
    }
    
  }
  
  public void stop() {
    if (this.session != null) {
      try {
        this.session.close();
        logger.info(AdvConnectionListener.class.getName() + ":stop():Session closed - No advisory connection messages will be processed.");
      } catch (JMSException ex) {
        logger.log(Level.SEVERE, AdvConnectionListener.class.getName() + ":stop():" + ex.getMessage());
      }
      this.session = null;
    }
  }

  /**
   * @return Get current list of AMQ clients
   */
  public Map<ConnectionId, MQClients> getMQClients() {
    return this.mqClients;
  }
}
