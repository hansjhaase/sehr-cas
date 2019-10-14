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
 * Refactored from 'sehr-saf-tool' on 11.08.2015
 */
package org.ifeth.sehr.p1507291734.web.listener;

import com.google.common.eventbus.EventBus;
import java.io.StringWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.*;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.apache.activemq.ActiveMQConnection;
import org.apache.commons.lang3.StringUtils;
import org.ifeth.sehr.intrasec.entities.NetCenter;
import org.ifeth.sehr.p1507291734.web.beans.NotificationEvent;

/**
 * Listener on the 'service' queue of a zone this module is responsible for.
 * <p>
 * <b>N.B.:</b> This class has been moved from 'sehr-saf' application.
 * </p>
 *
 * @author hansjhaase
 */
public class SAFQueueListener implements MessageListener {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");
  //private static final String FS = System.getProperty("file.separator");
  //private static final String ACKFILE = System.getProperty("java.io.tmpdir") + FS + "AckMessages_";

  //Access to the JPA, AMQ etc. context  
  private ServletContext ctx;

  private Properties p;
  //private ActiveMQConnectionFactory connectionFactory;
  private ActiveMQConnection amqcon = null;
  private Session session;
  private Destination destination;
  private MessageConsumer consumer;
  private EntityManagerFactory emf;
  //private EntityManager em;
  private EventBus eventBus;
  private int zoneid = 0; //for testing only!
  private int debug = 0;

  private String queue = null;

  /**
   * Parameterless constructor.
   *
   * <p>
   * To initialize and start the listener use
   * {@link #configure(ServletContext ctx, java.lang.String)}
   * </p>.
   *
   */
  public SAFQueueListener() {
  }

//  /**
//   * Create Instance with initialization (by property file) and a given AMQ
//   * connection.
//   * <p>
//   * Use this to initialize the default listener on a SEHR service queue of the
//   * zone this module is responsible for as defined by the properties.
//   * </p>
//   *
//   * @param p
//   * @param amqCon
//   */
//  public SAFQueueListener(Properties p, ActiveMQConnection amqCon) {
//    this.amqcon = amqCon;
//    this.p = p;
//    //if no zone is defined we're using a zero zone for testing(0000000) 
//    this.zoneid = Integer.parseInt(p.getProperty("zoneid", "0000000"));
//    //+++ since 8/2015 'saf' is not longer used - it is a 'service'!
//    this.queue = "sehr." + String.format("%07d", this.zoneid) + ".service.queue";
//    initialize();
//  }
  /**
   * Initialize AMQ session and activates the listener for the service queue.
   */
  private void initialize() {
    this.debug = Integer.parseInt(this.p.getProperty("debug", "0"));

    Log.fine(SAFQueueListener.class.getName() + ":initialize():debug=" + debug);
    try {
      this.session = this.amqcon.createSession(false, Session.AUTO_ACKNOWLEDGE);
      this.destination = this.session.createQueue(this.queue);
      this.consumer = this.session.createConsumer(this.destination);
      this.consumer.setMessageListener(this);
      if (!this.amqcon.isStarted()) {
        this.amqcon.start();
      }
      Log.info(SAFQueueListener.class.getName() + ":initialize():Administrative 'service' listener activated: " + this.queue + ", ClientID=" + this.amqcon.getClientID());
    } catch (JMSException | NullPointerException ex) {
      Log.log(Level.SEVERE, SAFQueueListener.class.getName() + ":initialize():JMS error " + ex.getMessage());
    }
  }

  /**
   * Configure a service queue listener of a zone that is managed by this SEHR
   * host.
   *
   * <p>
   * A <b>service queue</b> is a p2p queue of a zone (a group of care
   * facilities) to receive messages from connected centers (the facilities) and
   * devices (e.g. mobiles or card terminals) for administrative purposes; Take
   * a look at the illustration
   * <a href='https://www.lucidchart.com/invitations/accept/bea10c36-c742-4bb3-8271-925db32f0a73' target='_blank'>SEHR
   * Zone Service Messaging</a>.
   * </p>
   * <p>
   * For details of the SEHR messaging topology and serving multiple zones, e.g.
   * on a country (national) level or a provider serving multiple groups (like a
   * hospital group) see SEHR specification
   * <a href='https://www.lucidchart.com/invitations/accept/d5e8996f-47df-452e-ae66-08c9393d464c' target='_blank'>SEHR
   * Zone Center Topology</a>
   * </p>
   * <p>
   * If parameter 'queue' is null the locally defined zone id from the
   * configuration file (properties) will be used.
   * </p>
   *
   * @param ctx
   * @param queue The service queue; syntax: 'sehr.[zoneid].service.queue'
   * @return
   */
  public boolean configure(ServletContext ctx, String queue) {
    this.ctx = ctx;
    this.p = (Properties) ctx.getAttribute("Properties");
    this.amqcon = (ActiveMQConnection) ctx.getAttribute("ActiveMQConnection");
    this.emf = (EntityManagerFactory) ctx.getAttribute("EntityManagerFactory");
    if (!this.emf.isOpen()) {
      Log.log(Level.WARNING, SAFQueueListener.class.getName() + ":configure():Context EMF is closed but required.");
      return false;
    }
    this.eventBus = (EventBus) ctx.getAttribute("EventBus");
    if (p == null || amqcon == null || amqcon.isClosed()) {
      Log.log(Level.WARNING, SAFQueueListener.class.getName() + ":configure():Context is invalid.");
      this.eventBus.post(new NotificationEvent("No connection to messaging host"));
      return false;
    }
    if (StringUtils.isBlank(queue)) {
      this.zoneid = Integer.parseInt(p.getProperty("zoneid", "0000000"));
      //+++ since 8/2015 'saf' is not longer used - it is a 'service'!
      this.queue = "sehr." + String.format("%07d", this.zoneid) + ".service.queue";
    } else {
      this.queue = queue;
    }
    if (isSession()) {
      stop();
    }
    initialize();
    return isSession();
  }

  public boolean isSession() {
    return (this.amqcon != null && !this.amqcon.isClosed() && this.session != null);
  }

  /**
   * Process SEHR messages from the p2p service queue of a zone.
   * <p>
   * see also
   * <a href='https://www.evernote.com/Home.action#n=8797cdde-8f98-42f5-bfb7-522f3796f55b&ses=4&sh=2&sds=5&' target='_blank'>SEHR
   * EHR Message Bus Conventions - SEHR JMS Types</a>
   * </p>
   *
   * @param msg
   */
  @Override
  public void onMessage(Message msg) {

    String jmsType;
    try {
      Log.info(SAFQueueListener.class.getName() + ":onMessage():Processing message " + (StringUtils.isEmpty(msg.getJMSType()) ? " of JMSType '" + msg.getJMSType() + "'" : msg.toString()));

      jmsType = msg.getJMSType().toUpperCase();
      if (StringUtils.isBlank(jmsType)) {
        Log.fine(SAFQueueListener.class.getName() + ":onMessage():No 'JMSType' - must be one of ECHO, PING, LOCAL, NOEIS, AUTH*, or REGISTER*");
        return;
      }
      if (!(msg instanceof MapMessage)) {
        Log.warning(SAFQueueListener.class.getName() + ":onMessage():Invalid SEHR service message (must be a MapMessage)");
        return;
      }
      MapMessage sehrMsg = (MapMessage) msg;
      NetCenter netCenter = null;
      if (sehrMsg.propertyExists("origCenterId")) {
        //from center...
        int cid = Integer.parseInt(sehrMsg.getStringProperty("origCenterId"));
        int zid = 0;
        if (sehrMsg.propertyExists("origZoneId")) {
          //located at / assigned to zone
          zid = Integer.parseInt(sehrMsg.getStringProperty("origZoneId"));
        } else {
          //otherwise use registered zone or global zone
          zid = Integer.parseInt(p.getProperty("zoneid", "9999999"));
        }
        netCenter = verifyNetCenter(cid, zid);
        if (netCenter == null) {
          Log.warning(SAFQueueListener.class.getName() + ":onMessage():Originating center not in DB!");
          //TODO inform the admin...
        }
      }
      if (jmsType.contains("AUTH")) {
        //Process SEHR#AUTH[APP|LC|ZONE|CENTER] messages
        boolean isLicenced = false;
        short verified = -1; //-1n=n/a, 0=found but failed, 1=ok
        if (jmsType.endsWith("CENTER")) {
          //int cid = Integer.parseInt(sehrMsg.getStringProperty("origCenterId"));
          //checked above!  
          if (netCenter != null) {
            verified = 1;
          }
          //TODO check 'end date' and NetServices if center is allowed to use app
          isLicenced = true;
        }
        Destination replyTo = getReplyTo(sehrMsg);
        if (Log.isLoggable(Level.FINE) || replyTo == null) {
          StringBuilder sb = new StringBuilder();
          sb.append(jmsType.toUpperCase() + " message received:\n");
          //TODO implement message marshalling/un- in SEHR core library 
          if (sehrMsg.propertyExists("centerid")) {
            sb.append("Center......:" + sehrMsg.getStringProperty("centerid") + "\n");
            sb.append("('centerid' is deprecated. Use 'origCenterId'!)\n");
          } else if (sehrMsg.propertyExists("origCenterId")) {
            sb.append("Center......:" + sehrMsg.getStringProperty("origCenterId") + "\n");
          }
          //PIK of a registered module/service
          String appToken = "n/a"; //invalid app !?
          if (sehrMsg.propertyExists("AppToken")) {
            appToken = sehrMsg.getStringProperty("AppToken");
          }
          sb.append("AppToken....:" + appToken + "\n");
          if (jmsType.equals("AUTHAPP")) {
            //TODO get MODULE_NAME by PIK, e.g.  JMedShare4Care, JMedApoTXClient
          }
          //a generated key by zone responsible admin, 
          //unique per end point (receiver or producer)
          sb.append("SEHRAuthKey:." + sehrMsg.getStringProperty("SEHRAuthKey") + "\n");
          //TODO check SEHRAuthKey against a valid, not outdated DB entry NetServices
          //e.g. "NetService sharing data by a center using JMedS4C app"
          //if empty a REGISTER message must be sent... return isLicenced false so far...
          sb.append("Reply to....:" + (replyTo == null ? "ERROR: NO REPY INFO" : replyTo.toString()) + "\n");
          sb.append("CorrID......:" + sehrMsg.getJMSCorrelationID() + "\n");
          sb.append(sehrMsg.getStringProperty("subject") + "\n");
          Log.info(SAFQueueListener.class.getName() + ":onMessage():\n" + sb.toString());
        }

        if (replyTo == null) {
          Log.info(SAFQueueListener.class.getName() + ":onMessage():Error sending response - no reply info.");
          return;
        }
        //System.out.println("BrokerUrl: " + connectionFactory.getBrokerURL());
        if (this.amqcon.isClosed()) {
          Log.info(SAFQueueListener.class.getName() + ":onMessage():Error sending response - connection is closed.");
          return;
        }
        //this.session = this.amqcon.createSession(false, Session.AUTO_ACKNOWLEDGE);
        //MessageProducer replyProducer = this.session.createProducer(null);
        //MapMessage response = this.session.createMapMessage();
        //response.setJMSCorrelationID(sehrMsg.getJMSCorrelationID());
        Map<String, Object> data = new HashMap<>();
        //if SEHR is running... inform client about some stuff... 
        //TODO check status of licence/payment by SEHRAuthKey
        //see above, so far it is ok to reply 'true'

        data.put("registered", (verified == 1));
        data.put("isLicenced", isLicenced);
        if (verified >= 0 && jmsType.endsWith("CENTER")) {
          String xmlString = "";
          try {
            JAXBContext context = JAXBContext.newInstance(NetCenter.class);
            Marshaller m = context.createMarshaller();
            //for pretty-print XML in JAXB
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
//      m.setProperty("com.sun.xml.bind.NamespacePrefixMapper", new NamespacePrefixMapper() {
//        @Override
//        public String getPreferredPrefix(String arg0, String arg1, boolean arg2) {
//          return "NetCenter";
//        }
//      });
            // Write to System.out for debugging
            // m.marshal(emp, System.out);
            StringWriter sw = new StringWriter();
            m.marshal(netCenter, sw);
            xmlString = sw.toString();
            data.put("XML#NetCenter", xmlString);
          } catch (JAXBException e) {
            data.put("NetCenter#Name", netCenter.getName());
            data.put("NetCenter#StartDt", netCenter.getStartdt());
            Log.warning("Error marshalling " + netCenter.toString() + ":" + e.getMessage());
          }
        }

        long tDiff = new Date().getTime() - sehrMsg.getJMSTimestamp();
        //to the header...
        //response.setLongProperty("ping", tDiff);
        //to the body... a plain 'text' tag...
        //response.setString("text", "Time request-response:" + tDiff + " ms");
        //Destination destReply = this.session.createQueue(replyTo);
        //it is ok to send reply as NON_PERSISTENT, Prio 4 and 10 Min time to live 
        //replyProducer.send(replyTo, response, DeliveryMode.NON_PERSISTENT, 4, 600000);
        data.put("ping", tDiff);
        data.put("text", "Time request-response:" + tDiff + " ms");
        if (!sendResponse(sehrMsg, "AUTH / verification result", "POJO#Map", data)) {
          Log.warning(SAFQueueListener.class.getName() + ":onMessage():Error sending response.");
        }
      }
      if (jmsType.contains("VRFY") || jmsType.contains("VERIFY")) {
        //Process SEHR#VRFY[APP|LC|ZONE|CENTER] messages
        boolean isLicenced = false;
        short verified = -1; //-1n=n/a, 0=found but failed, 1=ok
        if (jmsType.endsWith("CENTER")) {
          //int cid = Integer.parseInt(sehrMsg.getStringProperty("origCenterId"));
          //checked above!  
          if (netCenter != null) {
            verified = 1;
          }
          //TODO check 'end date' and NetServices if center is allowed to use app
          isLicenced = true;
        }
        Destination replyTo = getReplyTo(sehrMsg);
        if (Log.isLoggable(Level.FINE) || replyTo == null) {
          StringBuilder sb = new StringBuilder();
          sb.append(jmsType.toUpperCase() + " message received:\n");
          //TODO implement message marshalling/un- in SEHR core library 
          if (sehrMsg.propertyExists("centerid")) {
            sb.append("Center......:" + sehrMsg.getStringProperty("centerid") + "\n");
            sb.append("('centerid' is deprecated. Use 'origCenterId'!)\n");
          } else if (sehrMsg.propertyExists("origCenterId")) {
            sb.append("Center......:" + sehrMsg.getStringProperty("origCenterId") + "\n");
          }
          //PIK of a registered module/service
          String appToken = "n/a"; //invalid app !?
          if (sehrMsg.propertyExists("AppToken")) {
            appToken = sehrMsg.getStringProperty("AppToken");
          }
          sb.append("AppToken....:" + appToken + "\n");
          if (jmsType.endsWith("APP")) {
            //TODO get MODULE_NAME by PIK, e.g.  JMedShare4Care, JMedApoTXClient
          }
          //a generated key by zone responsible admin, 
          //unique per end point (receiver or producer)
          sb.append("SEHRAuthKey:." + sehrMsg.getStringProperty("SEHRAuthKey") + "\n");
          //TODO check SEHRAuthKey against a valid, not outdated DB entry NetServices
          //e.g. "NetService sharing data by a center using JMedS4C app"
          //if empty a REGISTER message must be sent... return isLicenced false so far...
          sb.append("Reply to....:" + (replyTo == null ? "ERROR: NO REPLY INFO" : replyTo.toString()) + "\n");
          sb.append("CorrID......:" + sehrMsg.getJMSCorrelationID() + "\n");
          sb.append("Subject.....:" + sehrMsg.getStringProperty("subject") + "\n");
          Log.info(SAFQueueListener.class.getName() + ":onMessage():\n" + sb.toString());
        }

        if (replyTo == null) {
          Log.info(SAFQueueListener.class.getName() + ":onMessage():Error sending response - no reply info.");
          return;
        }
        //System.out.println("BrokerUrl: " + connectionFactory.getBrokerURL());
        if (this.amqcon.isClosed()) {
          Log.info(SAFQueueListener.class.getName() + ":onMessage():Error sending response - connection is closed.");
          return;
        }
        //this.session = this.amqcon.createSession(false, Session.AUTO_ACKNOWLEDGE);
        //MessageProducer replyProducer = this.session.createProducer(null);
        //MapMessage response = this.session.createMapMessage();
        //response.setJMSCorrelationID(sehrMsg.getJMSCorrelationID());
        Map<String, Object> data = new HashMap<>();
        //if SEHR is running... inform client about some stuff... 
        //TODO check status of licence/payment by SEHRAuthKey
        //see above, so far it is ok to reply 'true'

        data.put("registered", (verified == 1));
        data.put("isLicenced", isLicenced);
        if (verified >= 0 && jmsType.endsWith("CENTER")) {
          String xmlString = "";
          try {
            JAXBContext context = JAXBContext.newInstance(NetCenter.class);
            Marshaller m = context.createMarshaller();
            //for pretty-print XML in JAXB
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
//      m.setProperty("com.sun.xml.bind.NamespacePrefixMapper", new NamespacePrefixMapper() {
//        @Override
//        public String getPreferredPrefix(String arg0, String arg1, boolean arg2) {
//          return "NetCenter";
//        }
//      });
            // Write to System.out for debugging
            // m.marshal(emp, System.out);
            StringWriter sw = new StringWriter();
            m.marshal(netCenter, sw);
            xmlString = sw.toString();
            data.put("XML#NetCenter", xmlString);
          } catch (JAXBException e) {
            data.put("NetCenter#Name", netCenter.getName());
            data.put("NetCenter#StartDt", netCenter.getStartdt());
            Log.warning("Error marshalling " + netCenter.toString() + ":" + e.getMessage());
          }
        }

        long tDiff = new Date().getTime() - sehrMsg.getJMSTimestamp();
        //to the header...
        //response.setLongProperty("ping", tDiff);
        //to the body... a plain 'text' tag...
        //response.setString("text", "Time request-response:" + tDiff + " ms");
        //Destination destReply = this.session.createQueue(replyTo);
        //it is ok to send reply as NON_PERSISTENT, Prio 4 and 10 Min time to live 
        //replyProducer.send(replyTo, response, DeliveryMode.NON_PERSISTENT, 4, 600000);
        data.put("ping", tDiff);
        data.put("text", "Time request-response:" + tDiff + " ms");
        if (!sendResponse(sehrMsg, "VRFY / verification result", "POJO#Map", data)) {
          Log.warning(SAFQueueListener.class.getName() + ":onMessage():Error sending response.");
        }
      } else if (jmsType.contains("ECHO") || jmsType.contains("PING")) {
        //reply ms to sender queue, nothing else to process
        if (msg instanceof MapMessage) {
          Destination replyTo = getReplyTo(sehrMsg);
          String corrId = sehrMsg.getJMSCorrelationID();
          //PIK of JMedS4C, JMedApoTXClient, MyJMed etc
          String appToken = "n/a"; //invalid app !?
          if (sehrMsg.propertyExists("AppToken")) {
            appToken = sehrMsg.getStringProperty("AppToken");
          }
          //a generated key, unique per endpoint (receiver or producer)
          String authKey = "n/a"; //invalid app !?
          if (sehrMsg.propertyExists("SEHRAuthKey")) {
            authKey = sehrMsg.getStringProperty("SEHRAuthKey");
          }

          if (Log.isLoggable(Level.FINE)) {
            StringBuilder sb = new StringBuilder();
            //TODO implement message marshalling/un- in SEHR core library 
            sb.append("Keep Alive Message received:\n");
            if (sehrMsg.propertyExists("centerid")) {
              //old syntax
              sb.append("Center......:" + sehrMsg.getStringProperty("centerid") + "\n");
              sb.append("('centerid' is deprecated. Use 'origCenterId'!)\n");
            } else if (sehrMsg.propertyExists("origCenterId")) {
              sb.append("Center......:" + sehrMsg.getStringProperty("origCenterId") + "\n");
            }
            sb.append("AppToken....:" + appToken + "\n");
            sb.append("SEHRAuthKey :" + authKey + "\n");
            sb.append("Reply to....:" + replyTo.toString() + "\n");
            sb.append("CorrID......:" + corrId + "\n");
            if (sehrMsg.propertyExists("subject")) {
              sb.append(sehrMsg.getStringProperty("subject") + "\n");
            }
            Log.info(SAFQueueListener.class.getName() + ":onMessage():\n" + sb.toString());
          }
          if (replyTo != null) {
            //System.out.println("BrokerUrl: " + connectionFactory.getBrokerURL());
            String subject = "Response for CorrID " + corrId;

            Map<String, Object> data = new HashMap<>();
            long tDiff = new Date().getTime() - sehrMsg.getJMSTimestamp();
            data.put("ping", tDiff);
            data.put("text", "Time request-response:" + tDiff + " ms");
            sendResponse(sehrMsg, subject, "POJO#Map", data);
          }

        } else {
          Log.warning("Wrong message format - MapMessage expected!");
        }
      } else if (jmsType.contains("REGISTER")) {
        //--- quick and dirty ... not yet fully implemented
        //Destination replyTo = getReplyTo(sehrMsg);
        try {
          if (debug >= 8) {
            Enumeration<?> mapNames = sehrMsg.getMapNames();
            Log.finest("MapMessage:key-values:");
            while (mapNames.hasMoreElements()) {
              String key = (String) mapNames.nextElement();
              String value = sehrMsg.getString(key);
              Log.fine(key + ":" + value);
            }
          }
        } catch (JMSException jmse) {
          Log.fine(SAFQueueListener.class.getName() + ":onMessage():JMS error:" + jmse.getMessage());
        }
      }

    } catch (JMSException jmse) {
      Log.fine(SAFQueueListener.class.getName() + ":onMessage():JMS error:" + jmse.getMessage());
    }
  }

  /**
   * Close the messaging session to process service messages.
   *
   * <p>
   * The connection to the message broker will not be closed due to other
   * processors and listeners.
   * </p>
   *
   * @return
   */
  public boolean stop() {
    try {
      if (this.session != null) {
        this.session.close();
        Log.info(SAFQueueListener.class.getName() + ":stop():Session closed - No service related messages for zone '" + getZoneIDFromQName(this.queue) + "' will be processed");
        this.session = null;
      }
    } catch (JMSException ex) {
      Log.log(Level.SEVERE, SAFQueueListener.class.getName() + ":stop():JMS error " + ex);
      return false;
    }
    return true;
  }

  /**
   * Get ZoneID by the name of the queue.
   *
   * Extracts the zoneid from queue name (sehr.[zoneid].service.queue.
   */
  private String getZoneIDFromQName(String queuename) {
    String parts[] = queuename.split("\\.");
    return parts[1];
  }

  /**
   * Extract 'replyTo' from message.
   *
   * Camel rewrites JMSReplyTo for routing; to get the SEHR end point over the
   * complex routes there is a property named 'sehrReplyTo'. If there is no such
   * property inside the header the method returns the 'JMSReplyTo' value.
   *
   * @param msg
   * @return
   */
  private Destination getReplyTo(MapMessage msg) {
    Destination replyTo = null;
    try {
      if (msg.propertyExists("sehrReplyTo")) {
        replyTo = this.session.createQueue(msg.getStringProperty("sehrReplyTo"));
      } else {
        replyTo = msg.getJMSReplyTo();//or cut off queue:// from ...toString();
      }
    } catch (JMSException ex) {
      Logger.getLogger(SAFQueueListener.class.getName()).log(Level.WARNING, null, ex.getMessage());
    }
    return replyTo;//replyTo != null ? replyTo.trim() : null;
  }

  private boolean sendResponse(MapMessage inMsg, String subject, String dataset, Object data) {

    try {
      if (this.amqcon.isClosed()) {
        Log.info(SAFQueueListener.class.getName() + ":sendResponse():Error sending response - connection is closed.");
        return false;
      }
      Destination destReply = getReplyTo(inMsg);
      //this.session = this.amqcon.createSession(false, Session.AUTO_ACKNOWLEDGE);
      MessageProducer replyProducer = this.session.createProducer(null);
      MapMessage response = this.session.createMapMessage();
      response.setJMSCorrelationID(inMsg.getJMSCorrelationID());
      //on responses use same JMS type!
      response.setJMSType(inMsg.getJMSType());
      //if SEHR is running... inform client about some stuff...
      //TODO check status of licence/payment by SEHRAuthKey
      if (data instanceof HashMap) {
        Map mData = (HashMap) data;
        if (mData.containsKey("isLicenced")) {
          response.setBooleanProperty("isLicenced", (boolean) mData.containsValue("isLicenced"));
        }
      }

      long tDiff = new Date().getTime() - inMsg.getJMSTimestamp();
      //set header properties
      response.setLongProperty("ping", tDiff); //put to the header
      //"POJO#Map" etc
      response.setStringProperty("dataset", dataset);
      //the subject
      response.setStringProperty("subject", subject);
      //and in the body...
      response.setObject("data", data);
      //Destination destReply = this.session.createQueue(replyTo);
      replyProducer.send(destReply, response);
      Log.info(SAFQueueListener.class.getName() + ":sendResponse():Response has been sent to " + destReply.toString());
    } catch (JMSException ex) {
      Logger.getLogger(SAFQueueListener.class.getName()).log(Level.SEVERE, null, ex.getMessage());
      return false;
    }
    return true;
  }

  private NetCenter verifyNetCenter(int cid, int zid) {
    NetCenter netCenter = null;
    EntityManager em1 = null;
    try {
      //this.emf = (EntityManagerFactory) (new InitialContext()).lookup("java:comp/env/jpa/p1507291734/HibJTAIntraSEC");
      em1 = this.emf.createEntityManager();
      Query qry = em1.createNamedQuery("NetCenter.findByCIDZID", NetCenter.class);
      qry.setParameter("centerid", cid);
      qry.setParameter("zoneid", zid);
      List<NetCenter> list = qry.getResultList();
      if (!list.isEmpty()) {
        Log.info(SAFQueueListener.class.getName() + ":verifyNetCenter():" + list.size() + " found.");
        netCenter = list.get(0);
        //TODO more checks like is center valid, registered etc...
      }
    } catch (Exception ex) {
      Log.warning(SAFQueueListener.class.getName() + ":verifyNetCenter():" + ex.getMessage());
      throw new RuntimeException(ex);
    } finally {
      if (em1 != null && em1.isOpen()) {
        em1.close();
      }
    }
    return netCenter;
  }
}
