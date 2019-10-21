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
 * Since 8/2015
 */
package org.ifeth.sehr.p1507291734.web.listener;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.activemq.camel.component.ActiveMQComponent;
import static org.apache.activemq.camel.component.ActiveMQComponent.activeMQComponent;
import org.apache.activemq.camel.component.ActiveMQConfiguration;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.commons.lang3.StringUtils;
import org.ifeth.sehr.p1507291734.lib.Constants;
import org.ifeth.sehr.p1507291734.web.beans.XNetCenterRoutingBean;
import org.ifeth.sehr.p1507291734.web.beans.XNetLifeCARDOutBean;
import org.ifeth.sehr.p1507291734.web.beans.XNetProcessCountryLevelBean;
import org.ifeth.sehr.p1507291734.web.beans.XNetProcessRootBean;
import org.ifeth.sehr.p1507291734.web.beans.XNetZoneRoutingBean;

/**
 * Send messages to XNET message bus and listen to the queue on the bus of the
 * zone this module is responsible for.
 *
 * <p>
 * This module uses ApacheCamel for processing and routing. So there is no
 * MessageListener implementation at this class!
 * </p>
 *
 * @author hansjhaase
 */
public class XNetMessaging {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");

  private Properties p;
  //private ActiveMQConnectionFactory xnetAMQFactory;
  //private ActiveMQConnection xnetCon;
  //private Session session;
  private String country = "de"; //'de' by default!
  private Integer zoneid = 0; //n/a by default!
  private int debug = 0;
  //queue of this zone at next level host to get messages by listening
  private String queueXNetIn;
  //queue of the next level SEHR host for outgoing messages
  private String queueXNetOut;
  //private MessageProducer producer;
  //private Destination xnetQueue;
  //private Destination localQueue;
  private String CorrId;

  private CamelContext camelContext;
  private String localDomain = null;
  //the next upper domain
  private String domain;

  /**
   * Parameterless constructor to initialize a listener to a provided SEHR
   * messaging bus.
   * <p>
   * The national (or global) SEHR messaging bus is bridged from the point of
   * view or a working group (zone). This listener connects to the bus using a
   * AMQ Connection and processes messages from the bus.
   * </p>
   *
   */
  public XNetMessaging() {

  }

  /**
   * Initialize listener to get messages via SEHR Messaging bus.
   *
   * The XNET listener allows zone-to-zone data transfers.
   *
   * @param p
   */
  public XNetMessaging(Properties p) {
    this.p = p;
    this.debug = Integer.parseInt(p.getProperty("debug", "0"));
    //use country, domain and zoneid to exchange messages 
    this.zoneid = Integer.parseInt(p.getProperty("zoneid", "0"));
    this.domain = p.getProperty("domain", "e-hn.org");
    if (StringUtils.isNotBlank(this.domain)) {
      this.localDomain = this.domain;
      String subdomain = p.getProperty("subdomain", "");
      if (StringUtils.isNotBlank(subdomain)) {
        this.localDomain = subdomain + "." + this.domain;
      } else {
        this.localDomain = "z" + String.format("%07d", this.zoneid) + "." + this.domain;
      }
    }
  }

  /**
   * Start a messaging session to send or receive messages.
   *
   * @return
   */
  public boolean start() {
    if (this.queueXNetOut == null || this.queueXNetIn == null) {
      Log.log(Level.WARNING, "{0}:start():{1}", new Object[]{XNetMessaging.class.getName(), "in and/or out queues are not configured!"});
      return false;
    }
//    Log.log(Level.FINEST, "{0}:start():debug={1}", new Object[]{XNetMessaging.class.getName(), this.debug});
//    if (this.xnetCon == null || this.xnetCon.isClosed()) {
//      Log.warning(XNetMessaging.class.getName() + ":start():No XNET connection.");
//      return;
//    }
    //start routing of messages from the XNET messaging bus      
    try {
      this.camelContext.start();
    } catch (Exception ex) {
      Log.log(Level.SEVERE, "{0}:start():{1}", new Object[]{XNetMessaging.class.getName(), ex.getMessage()});
      return false;
    }
    //register XNet using Apache Camel ProducerTemplate
    this.CorrId = "XNet-Start-" + System.currentTimeMillis();

    try {
      ProducerTemplate prod = this.camelContext.createProducerTemplate();
      //Camel always overrides the JMSReplyTo and sets it to a temp-queue 
      //regardless user settings. So we use a processor AND '?replyTo' syntax;)
      prod.request("xdom:queue:" + this.queueXNetOut + "?replyTo=" + "queue://" + this.queueXNetIn, new Processor() {

        @Override
        public void process(Exchange exchange) throws Exception {
          Map<String, Object> header = new HashMap<>();
          header.put("JMSType", "SEHR#XNET");
          //header.put("JMSReplyTo", "queue://"+queueXNetIn);
          header.put("sehrReplyTo", "queue://" + queueXNetIn);
          //header.put("JMSReplyTo", ActiveMQConverter.toDestination("queue://"+this.queueXNetLocal));
          header.put("JMSCorrelationID", CorrId);
          //header.put("JMSDeliveryMode", null);
          //header.put("JMSDestination", null);
          //header.put("JMSExpiration", 30000);
          //header.put("JMSMessageID", null);
          header.put("JMSPriority", 4);
          //header.put("JMSRedelivered", false);
          header.put("JMSTimestamp", System.currentTimeMillis());
          //SEHR specific headers
          //by EIS convention this is a typical 'event' message 
          //I'm online now....
          header.put("MsgType", "notification");
          //AppToken (=PIK)
          header.put("AppToken", Constants.MODULE_PIK);

          if (StringUtils.isNotBlank(localDomain)) {
            header.put("origDomain", localDomain); //String
          }
          if (zoneid > 0) {
            header.put("origZoneId", zoneid); //Integer
          }
          //register this SEHR networking infrastructure on next level (parent) 
          header.put("rcvDomain", domain); //String
          //header.put("rcvZoneId", 0); //Integer
          //header.put("rcvCenterId", 0); //Intege!
          //Important! 'subject' is  part of header for routing 
          //if starting with 'SEHR', 'Ref:', 'Issue#' etc.
          header.put("subject", localDomain + " started XNET exchange...");
          exchange.getIn().setHeaders(header);
          Map<String, Object> body = new HashMap<>();
          body.put("text", "Hello, we're ready for data exchange. Outgoing messages will be send to " + queueXNetOut + ". Incoming messages are expected at " + queueXNetIn);
          exchange.getIn().setBody(body);
        }
      });

//    //register on XNet and start listener using a AMQ connection
//    try {
//      this.session = this.xnetCon.createSession(false, Session.AUTO_ACKNOWLEDGE);
//      //queue of the SEHR XNET bus
//      this.xnetQueue = this.session.createQueue(this.queueXNetOut);
//      //zone (domain) this handler is resposible for
//      this.localQueue = this.session.createQueue(this.queueXNetLocal);
//
//      //send initial message to infom other on the bus 
//      this.producer = session.createProducer(this.xnetQueue);
//      this.producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
//
//      //MessageConsumer responseConsumer = session.createConsumer(this.localQueue);
//      //responseConsumer.setMessageListener(this);
//      //Now create the 'I'm ready to participate' message
//      //TODO use SEHRMessageCreator asap (sehr-core library V6.3)
//      MapMessage mMessage = session.createMapMessage();
//      //see https://www.evernote.com/shard/s306/sh/ebf811c4-fbc4-40ec-a487-d52abcff25c0/9b03744ad8f32c64caacde967d0561d0
//      mMessage.setJMSType("SEHR#XNET");
//      //by EIS convention this is a typical 'event' message 
//      //I'm online now....
//      mMessage.setStringProperty("msgtype", "notification");
//      //AppToken (=PIK)
//      mMessage.setStringProperty("AppToken", Constants.MODULE_PIK);
//      //yes, a zone has to be approved too! 
//      //therefore it is a control message, not just an 'event'
//      mMessage.setString("body", "Hello from Zone ID " + this.zoneid);
//      mMessage.setJMSReplyTo(this.localQueue);
//      this.CorrId = "XNet-" + System.currentTimeMillis();
//      mMessage.setJMSCorrelationID(CorrId);
//      if (!this.xnetCon.isStarted()) {
//        this.xnetCon.start();
//      }
//      this.producer.send(mMessage);
      Log.log(Level.INFO, "{0}:start():Sent connection request to {1}", new Object[]{XNetMessaging.class.getName(), this.queueXNetOut});// + ", JMS ClientID=" + xnetCon.getClientID());
    } catch (CamelExecutionException ex) {
      Log.log(Level.SEVERE, XNetMessaging.class.getName() + ":start():JMS error " + ex.getMessage());
    }
    return true;
  }

  /**
   * Connect to the brokers (of the local zone/domain and SEHR XNET domain).
   *
   * @param queueXNetP - parent/top level XNET domain queue to send messages out
   * @param queueXNetL - queue of zone on XNET parent/top level to get messages
   * @return
   */
  public boolean connect(String queueXNetP, String queueXNetL) {
    String xnetRootURL = p.getProperty("sehrxnetrooturl", "");
    String xnetCountryURL = p.getProperty("sehrxnetcountryurl", "");
    String xnetDomainURL = p.getProperty("sehrxnetdomainurl", "");
    String xnetZoneURL = p.getProperty("sehrxnetzoneurl", "");
    if (StringUtils.isBlank(xnetDomainURL)) {
      //we can't send and retrieve messages to next level XNET (provider)
      Log.warning(XNetMessaging.class.getName() + ":connect():No XNET broker for EHN domain.");
      return false;
    }
    if (StringUtils.isBlank(xnetZoneURL)) {
      //we can't send and retrieve messages inside local XNET zone/community
      Log.warning(XNetMessaging.class.getName() + ":connect():No XNET broker for local zone defined.");
      return false;
    }
    this.queueXNetIn = queueXNetL;
    this.queueXNetOut = queueXNetP;
    //this.xnetParentAMQFactory = new ActiveMQConnectionFactory(xnetParentURL);
    //this.xnetLocalAMQFactory = new ActiveMQConnectionFactory(pxnetLocalURL);
    //"vm://localhost?broker.persistent=false"));
    this.camelContext = new DefaultCamelContext();
    //connect parent/top level XNET broker ...
    if (!StringUtils.isBlank(xnetRootURL)) {
      ActiveMQComponent mqCompRoot = new ActiveMQComponent();
      mqCompRoot.setBrokerURL(xnetRootURL);
      mqCompRoot.setUserName(p.getProperty("sehrxnetrootuser", "sehruser"));
      mqCompRoot.setPassword(p.getProperty("sehrxnetrootpw", "user4sehr"));
      this.camelContext.addComponent("xroot", mqCompRoot);
    }
    //connect national XNET broker ...
    if (!StringUtils.isBlank(xnetCountryURL)) {
      ActiveMQComponent mqCompCtry = new ActiveMQComponent();
      mqCompCtry.setBrokerURL(xnetCountryURL);
      mqCompCtry.setUserName(p.getProperty("sehrxnetcountryuser", "sehruser"));
      mqCompCtry.setPassword(p.getProperty("sehrxnetcountrypw", "user4sehr"));
      this.camelContext.addComponent("xctry", mqCompCtry);
    }
    //connect upper level XNET broker ...
    ActiveMQComponent mqCompDom = new ActiveMQComponent();
    mqCompDom.setBrokerURL(xnetDomainURL);
    mqCompDom.setUserName(p.getProperty("sehrxnetdomainuser", "sehruser"));
    mqCompDom.setPassword(p.getProperty("sehrxnetdomainpw", "user4sehr"));
    this.camelContext.addComponent("xdom", mqCompDom);
    //connect local XNET broker ...
    ActiveMQComponent mqCompZone = new ActiveMQComponent();
    mqCompZone.setBrokerURL(xnetZoneURL);
    mqCompZone.setUserName(p.getProperty("sehrxnetzoneuser", "sehruser"));
    mqCompZone.setPassword(p.getProperty("sehrxnetzonepw", "user4sehr"));
    this.camelContext.addComponent("xzone", mqCompZone);
    //+"vm://localhost?broker.persistent=false"));
    try {
      XNetRoutes routes = new XNetRoutes(this.camelContext, this.p);
      routes.setCountry(this.country); //country this zone is assigned to...
      this.camelContext.addRoutes(routes);
      this.camelContext.setTracing(true);
      this.camelContext.setAutoStartup(true);
      InitialContext ic;
      try {
        ic = new InitialContext();
        ic.rebind("XNetContext", this.camelContext);
      } catch (NamingException ne) {
        Logger.getLogger(XNetMessaging.class.getName()).log(Level.SEVERE, null, ne);
      }
//    } catch (JMSException ex) {
//      Logger.getLogger(XNetMessaging.class.getName()).log(Level.SEVERE, null, ex);
//      return false;
    } catch (Exception ex) {
      Logger.getLogger(XNetMessaging.class.getName()).log(Level.SEVERE, null, ex);
      return false;
    }
    start(); //start may take a while...
    return true;
  }

//  public boolean isSession() {
//    return (this.session != null && !this.xnetCon.isClosed());
//  }
  /**
   * For testing the route.
   *
   * @param message
   * @return
   */
  public boolean test(String message) {
    if (StringUtils.isBlank(message)) {
      Log.log(Level.WARNING, XNetMessaging.class.getName() + ":test():no message.");
      return false;
    }
    if (this.camelContext == null || this.camelContext.isSuspended()) {
      Log.log(Level.WARNING, XNetMessaging.class.getName() + ":test():" + (this.camelContext == null ? "XNET context not present (null)" : "XNET context suspended " + this.camelContext.isSuspended()));
      return false;
    }
    try {
      ProducerTemplate template = this.camelContext.createProducerTemplate();
      //testings
      if (StringUtils.contains(message, "stress")) {
        for (int i = 0; i < 10; i++) {
          String body = i + " of 10 times stress test";
          template.sendBody("xdom:queue:" + this.queueXNetIn, body);
        }
      } else if (StringUtils.contains(message, "transform")) {
        //.when().simple("${body} is 'java.util.HashMap'").to("direct:processBody")
        Map body = new HashMap();
        body.put("testTransform", true);
        body.put("sent", System.currentTimeMillis());
        template.sendBody("xdom:queue:" + this.queueXNetIn, body);
      }
    } catch (CamelExecutionException ex) {
      Log.log(Level.SEVERE, XNetMessaging.class.getName() + ":test():Test failed:" + ex.getMessage());
      return false;
    }
    Log.log(Level.FINER, XNetMessaging.class.getName() + ":test():Testing route from " + this.queueXNetIn);
    return true;
  }

//  /**
//   * Process any kind of defined messages from the SEHR messaging bus queue for
//   * this domain.
//   *
//   * @param msg
//   */
//  @Override
//  public void onMessage(Message msg) {
//
//    String jmsType;
//    try {
//      Log.info(XNetMessaging.class.getName() + ":onMessage():Processing message " + (StringUtils.isEmpty(msg.getJMSType()) ? " of JMSType '" + msg.getJMSType() + "'" : msg.toString()));
//
//      jmsType = msg.getJMSType();
//      if (jmsType == null) {
//        Log.fine(XNetMessaging.class.getName() + ":onMessage():No 'JMSType' - must be one of a SEHR types like PING, ECHO, REGISTER-, AUTH-, XNET...");
//      } else if (jmsType.contains("XNET")) {
//        //SEHR#XNET - common messages to be routed or processed
//        if (msg instanceof MapMessage) {
//          //SEHRMessages are MapMessage by convention
//          MapMessage m = (MapMessage) msg;
//          if (debug >= 8) {
//            debugMapMessage(m);
//          }
//          XNetMessagingHandler msgProcessor = new XNetMessagingHandler(session, m);
//          msgProcessor.run(); //start thread...
//        } else {
//          Log.fine(XNetMessaging.class.getName() + ":onMessage():This kind of message is not yet implemented.");
//        }
//        //... finally reply if requested
//        if (msg.getJMSReplyTo() != null) {
//          //System.out.println("BrokerUrl: " + connectionFactory.getBrokerURL());
//          if (this.xnetCon.isClosed()) {
//            Log.info(XNetMessaging.class.getName() + ":onMessage():Error sending response - connection is closed.");
//            return;
//          }
//          //this.session = this.jmsconnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//          MessageProducer replyProducer = this.session.createProducer(null);
//          TextMessage response = this.session.createTextMessage();
//          response.setJMSCorrelationID(msg.getJMSCorrelationID());
//
//          //if SEHR is running... inform client about some stuff... 
//          //TODO do sth like verifying licence/payment by SEHRAuthKey
//          response.setBooleanProperty("isLicenced", true);
//
//          long tDiff = new Date().getTime() - msg.getJMSTimestamp();
//          response.setLongProperty("ping", tDiff);
//          response.setText("Time request-response:" + tDiff + " ms");
//          replyProducer.send(msg.getJMSReplyTo(), response);
//        }
//      } else {
//        Log.fine(XNetMessaging.class.getName() + ":onMessage():Invalid 'JMSType' - must be SEHR type XNET...");
//      }
//    } catch (JMSException jmse) {
//      Log.fine(XNetMessaging.class.getName() + ":onMessage():JMS error:" + jmse.getMessage());
//    }
//  }
  /**
   * Stop the session to send messages and the EID context listener to receive
   * and process messages.
   *
   * Use close() to shut down the XNET service ...
   */
  public void stop() {
    if (this.camelContext == null) {
      return;
    }
    //set to 1 minute shutdown
    this.camelContext.getShutdownStrategy().setTimeout(60);
    for (Route r : this.camelContext.getRoutes()) {
      Log.fine(XNetMessaging.class.getName() + ":stop():Stopping XNet route " + r.getId());
      try {
        this.camelContext.stopRoute(r.getId());
      } catch (Exception ex) {
        Log.warning(XNetMessaging.class.getName() + ":stop():Error closing route " + r.getId() + ":" + ex.getMessage());
      }
    }
    try {
      this.camelContext.stop();
    } catch (Exception ex) {
      Log.warning(XNetMessaging.class.getName() + ":stop():Error closing CamelContext. " + ex.getMessage());
    }
    Log.info(XNetMessaging.class.getName() + ":stop():XNet context closed.");
  }

  /**
   * Stop and close connection - Shutdown the XNET bus service.
   *
   * XNET does not have multiple connections (or listeners) to the next parent
   * domain broker. So we close the connection to the XNET broker also.
   */
  public void close() {
    stop();
//    try {
//      if (this.xnetCon != null && !this.xnetCon.isClosed()) {
//        this.xnetCon.close();
//      }
//    } catch (JMSException ex) {
//      Log.log(Level.SEVERE, XNetMessaging.class.getName() + ":close():JMS error " + ex);
//    }
  }

  /**
   * Get the local FQ domain name of the zone.
   *
   * @return localDomain - e.g. z0000000.de.e-hn.org
   */
  public String getXNetLocalDomain() {
    return this.localDomain;
  }

  /**
   * Get the domain of the XNet bus for outgoing messages.
   *
   * @return e.g. de.e-hn.org
   */
  public String getXNetDomainBus() {
    //+++do not beautify here... it is used by ProducerTemplate
    //String s = this.queueXNetOut != null ? this.queueXNetOut : "n/a";
    //return s;
    return this.queueXNetOut;
  }

  public String getInfo(boolean isHTML) {
    String crlf = "\n";
    if (isHTML) {
      crlf = "<br/>\n";
    }
    StringBuilder sb = new StringBuilder();
    if (this.camelContext != null && this.camelContext.getStatus().isStarted()) {
      if (StringUtils.isNotBlank(p.getProperty("sehrxnetrooturl", ""))) {
        ActiveMQComponent amqCompXRoot = (ActiveMQComponent) this.camelContext.getComponent("xroot");
        ActiveMQConfiguration amqConfXRoot = (ActiveMQConfiguration) amqCompXRoot.getConfiguration();
        boolean bOutOnly = true;
        for (Route r : this.camelContext.getRoutes()) {
          //String id = r.getId();
          String from = r.getRouteContext().getFrom().getUri();
          if (from.contains("xroot")) {
            bOutOnly = false;
          }
        }
        sb.append("- XNET Root Bus     : ")
                .append(amqConfXRoot.getBrokerURL())
                .append(bOutOnly ? " [OUT]" : " [IN/OUT]")
                .append(crlf);
      }
      if (StringUtils.isNotBlank(p.getProperty("sehrxnetcountryurl", ""))) {
        ActiveMQComponent amqCompXCL = (ActiveMQComponent) this.camelContext.getComponent("xctry");
        ActiveMQConfiguration amqConfXCL = (ActiveMQConfiguration) amqCompXCL.getConfiguration();
        boolean bOutOnly = true;
        for (Route r : this.camelContext.getRoutes()) {
          //String id = r.getId();
          String from = r.getRouteContext().getFrom().getUri();
          if (from.contains("xctry")) {
            bOutOnly = false;
          }
        }
        sb.append("- XNET Country '" + p.getProperty("country", "de") + "' Bus : ")
                .append(amqConfXCL.getBrokerURL())
                .append(bOutOnly ? " [OUT]" : " [IN/OUT]")
                .append(crlf);
      }
      if (this.camelContext.hasComponent("xdom") != null) {
        ActiveMQComponent amqComp = (ActiveMQComponent) this.camelContext.getComponent("xdom");
        ActiveMQConfiguration amqConf = (ActiveMQConfiguration) amqComp.getConfiguration();
        sb.append("- XNET Domain Bus   : ").append(amqConf.getBrokerURL()).append(crlf);
      }
      if (this.camelContext.hasComponent("xzone") != null) {
        ActiveMQComponent amqCompXZone = (ActiveMQComponent) this.camelContext.getComponent("xzone");
        ActiveMQConfiguration amqConfXZone = (ActiveMQConfiguration) amqCompXZone.getConfiguration();
        sb.append("- XNET Zone Bus     : ").append(amqConfXZone.getBrokerURL()).append(crlf);
      }
      //sb.append("- Local domain      : ").append(this.localDomain).append(crlf);
      sb.append("- XNET Channel      : ").append(this.queueXNetIn).append(crlf);
      sb.append("- XNET Connection   : since ").append(this.camelContext.getUptime()).append(crlf);
    } else {
      sb.append("- XNET Connection   : not connected/no session").append(crlf);
    }
    return sb.toString();
  }

  public List<Route> getRoutes() {
    return (this.camelContext != null ? this.camelContext.getRoutes() : null);
  }

  public List<String> getRouteNameStatus() {
    List<String> list = new ArrayList<>();
    if (this.camelContext != null) {
      for (Route r : this.camelContext.getRoutes()) {
        //String id = r.getId();
        //String from = r.getRouteContext().getFrom().getUri();
        //String route = r.getRouteContext().getRoute().toString();
        RouteDefinition rDef = r.getRouteContext().getRoute();
        String route = rDef.getShortName();
        ServiceStatus rSts = rDef.getStatus(this.camelContext);
        route += " (" + rSts.toString() + ")";
        //sb.append("Route " + id + ": " + from);
        list.add(route);
      }
    }
    return list;
  }

  public String getRouteInfo(Route r, boolean isHTML) {
    String crlf = "\n";
    if (isHTML) {
      crlf = "<br/>";
    }
    StringBuilder sb = new StringBuilder();
    if (this.camelContext != null) {
      //String id = r.getId();
      //String from = r.getRouteContext().getFrom().getUri();
      String route = r.getRouteContext().getRoute().toString();
      //sb.append("Route " + id + ": " + from);
      sb.append(route).append(crlf);
    } else {
      sb.append("No XNET Route to display").append(crlf);
    }
    return sb.toString();
  }

  public String getRoutesInfo(boolean isHTML) {
    String crlf = "\n";
    if (isHTML) {
      crlf = "<br/>";
    }
    StringBuilder sb = new StringBuilder();
    if (this.camelContext != null) {
      for (Route r : this.camelContext.getRoutes()) {
        //String id = r.getId();
        //String from = r.getRouteContext().getFrom().getUri();
        String route = r.getRouteContext().getRoute().toString();
        //sb.append("Route " + id + ": " + from);
        sb.append(route).append(crlf);
      }
    } else {
      sb.append("No XNET Route/Processing (not started?)").append(crlf);
    }
    return sb.toString();
  }

  @Override
  public String toString() {

    String s = "XNET n/a";
    if (this.camelContext != null) {
      s = this.camelContext.getUptime();
    }
//    if (isSession()) {
//      s = queueXNetLocal + "@" + this.xnetCon.getBrokerInfo().getBrokerURL();
//      s += "  - Connection Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm.ss").format(this.xnetCon.getTimeCreated());
//    }
    return s;
  }

  private void debugMapMessage(MapMessage m) {
    StringBuilder sb = new StringBuilder();
    Enumeration<?> mapNames;
    try {
      mapNames = m.getMapNames();
      sb.append("MapMessage:key-values:");
      while (mapNames.hasMoreElements()) {
        String key = (String) mapNames.nextElement();
        String value = m.getString(key);
        sb.append(key + ":" + value);
      }
      System.out.println(sb.toString());
    } catch (JMSException ex) {
      Logger.getLogger(XNetMessaging.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  public CamelContext getEISContext() {
    return this.camelContext;
  }

  public String getZoneQueue() {
    return this.queueXNetIn;
  }

  /**
   * @param country the country to set
   */
  public void setCountry(String country) {
    this.country = country;
  }

  public boolean isStarted() {
    if (this.camelContext != null) {
      return this.camelContext.getStatus().isStarted();
    }
    return false;
  }

  public Route getRoute(String routeId) {
    if (this.camelContext != null) {
      for (Route r : this.camelContext.getRoutes()) {
        if (r.getId().equals(routeId)) {
          return r;
        }
      }
    }
    return null;
  }

  //============================================= embedded classes
  private class XNetRoutes extends RouteBuilder {

    private final Logger Log = Logger.getLogger("org.ifeth.p1507291734.web");

    private CamelContext cc;
    private Properties p;

    private int zoneid = 0; //0=only domain based routing

    private String root = "e-hn.org"; //the default XNET root domain
    private String country = "de"; //default
    private String domain = ""; //the domain the zone is subnetted to
    private String zonedomain = ""; //the full name of the zone domain

    //default is IFETH domain 'e-hn.org', see above!
    private String queueXNetRoot = "sehr.xnet." + root + ".queue";
    //default is Germany, the location of the SEHR initiative ;)
    private String queueXNetCountry = "sehr.xnet." + country + "." + root + ".queue";
    private String queueXNetDom; //build on properties below
    private String queueXNetZone; //build on properties below

    public XNetRoutes(CamelContext cc, Properties p) {
      this.cc = cc;
      this.p = p;
      this.zoneid = Integer.parseInt(p.getProperty("zoneid", "0"));
      //XNET is domain based so we have to trust the domain, subdomain 
      //property settings...
      this.domain = p.getProperty("domain");
      this.queueXNetDom = "sehr.xnet." + this.domain + ".queue";
      this.zonedomain = p.getProperty("subdomain") + "." + p.getProperty("domain");
      //local (zone) domain this CAS is responsible for: 
      //sehr.xnet.[sub/zone].[domain].queue
      this.queueXNetZone = "sehr.xnet." + this.zonedomain + ".queue";
    }

//        @Override
//        public void configure() {
//          //works - but useless :)
//          //from("test-jms:queue:test.queue").to("file:///tmp/test");
//        }
    private final Processor transformMessage = new Processor() {
      @Override
      public void process(Exchange exchange)
              throws Exception {
        //see method test(), modifying body is ok
        Map<String, Object> modifyBody = exchange.getIn().getBody(HashMap.class);
        modifyBody.put("XRECEIVED", System.currentTimeMillis());
        exchange.getIn().setBody(modifyBody);
        System.out.println("-- (transformMessage) modified body='" + modifyBody.toString() + "' processed --");
      }
    };
    /**
     * Process request-reply (notification) message for the domain.
     *
     */
    private final Processor procDomainMessage = new Processor() {
      @Override
      public void process(Exchange exchange)
              throws Exception {
        /* do not modify body!
         Map<String, Object> body = exchange.getIn().getBody(HashMap.class);
         body.put("XRECEIVED", System.currentTimeMillis());
         exchange.getIn().setBody(body);
         */
        Map<String, Object> header = exchange.getIn().getHeaders();
        header.put("XRECEIVED", System.currentTimeMillis());
        exchange.getOut().setHeaders(header);
        //ProducerTemplate template = cc.createProducerTemplate();
        //template.sendBody("local:queue:sehr." + String.format("%07d", zoneid) + "." + String.format("%07d", cid) + ".queue", body);
        System.out.println("-- (procDomainMessage) modified header='" + header.toString() + "' processed --");
      }
    };

    @Override
    public void configure() throws Exception {
      //errorHandler(deadLetterChannel("local:queue:sehr.DQL.queue"));
      // +++ camel test: works, but useless ;)
      //from("xnet:queue:test.queue").to("file:///tmp/test1");

      //Get messages from XNET bus for routing, processing:
      //1st by Header: Country, domain, zone, center 
      //2nd by content...
      if (this.cc.getComponent("xroot") != null) {
        //true (1) if this SEHR-CAS is processing the global broker!   
        short processRoot = Short.parseShort(p.getProperty("processRoot", "0"));
        if (processRoot == 1) {
          //TODO implement XML routing file
          //TODO build routes... to country brokers etc...
          from("xroot:queue:" + this.queueXNetRoot)
                  .log("(xroot) ${in.headers}")
                  .bean(XNetProcessRootBean.class);

        }
      }
      if (this.cc.getComponent("xctry") != null) {
        //true (1) if this SEHR-CAS is responsible for processing national
        //messages and is connected to a national broker!
        short processCountryBus = Short.parseShort(p.getProperty("processCountry", "0"));
        if (processCountryBus == 1) {
          String selfDomain = country + "." + root;
          from("xctry:queue:" + this.queueXNetCountry)
                  .log("(xctry) ${in.headers}")
                  .choice()
                  .when(isPropertyValue("rcvDomain", selfDomain)).process(procDomainMessage).to("xctry:queue:sehr.inbox." + selfDomain + ".queue") /*.to("direct:afterTrans")*/
                  .otherwise().bean(XNetProcessCountryLevelBean.class)
                  .end();
        }
      }

      from("xdom:queue:" + this.queueXNetZone)
              .log("(xdom) ${in.headers}")
              .choice()
              .when(isProperty("rcvCountryId")).to("direct:headerRcvCountry")
              .when(isProperty("rcvDomain")).to("direct:headerRcvDomain")
              .when(isProperty("rcvZoneId")).to("direct:headerRcvZID")
              .when(isProperty("rcvCenterId")).to("direct:headerRcvCID")
              .when().simple("${body} is 'java.util.HashMap'").to("direct:processBody")
              .otherwise().setHeader("route").constant("unknown").to("xdom:queue:sehr.DLQ.queue")
              .end();
      //Body is a HashMap - Ok, now route/process according to the content
      from("direct:processBody")
              .choice()
              .when(isBodyProperty("testTransform")).to("direct:optionTransform")
              .otherwise().to("direct:invalid")
              .end();
      //Country processing... countries are below xnetroot by specification
      from("direct:headerRcvCountry")
              .choice()
              .when(isPropertyValue("rcvCountryId", this.country)).to("direct:headerRcvDomain")
              .otherwise().log("routing msg for country ${in.header.rcvCountryId} to XNET root...").to("xdom:queue:sehr.xnet." + p.getProperty("sehrxnetroot") + ".queue")
              .end();
      //Domain processing... domain is an important routing context
      from("direct:headerRcvDomain")
              .choice()
              .when(isPropertyValue("rcvDomain", this.domain)).to("direct:headerRcvZID")
              /* TODO check if zone is at domain */
              .when(isPropertyValue("rcvZoneId", this.zoneid)).to("direct:headerRcvCID")
              .otherwise().log("routing msg for ${in.header.rcvDomain} to XNET root...").to("xdom:queue:sehr.xnet." + p.getProperty("sehrxnetroot") + ".queue")
              .end();
      //Zone processing if the message arrived at the domain...
      from("direct:headerRcvZID")
              .choice()
              .when(isPropertyValue("rcvZoneId", this.zoneid)).to("direct:headerRcvCID")
              .otherwise().bean(XNetZoneRoutingBean.class)
              .end();
      //Final processing: deliver to a center queue, process or err/out...
      //send message to instance of bean and cache bean for reusage
      from("direct:headerRcvCID").bean(XNetCenterRoutingBean.class);
      //send message to a new bean instance
      //from("direct:headerRcvCID").bean(new XNetCenterRoutingBean());
      from("direct:invalid").to("stream:err");
      from("direct:optionTransform").process(transformMessage).to("direct:afterTrans");
      from("direct:afterTrans").to("stream:out");

      from("direct:LifeCARDOut")
              .log("(LC) ${in.headers}")
              .bean(XNetLifeCARDOutBean.class);
      /*  +++ does not work as expected    
       from("xnet:queue:" + queueXNetLocal)
       .log("${in.headers}")
       .choice()
       .when(simple("${in.header.MsgType} == &#39;notification&#39;")).to("file:///tmp/test2")
       .otherwise().to("local:queue:sehr." + String.format("%07d", zoneid) + ".inbox.queue")
       .end();
       */
    }

    /* EIS use case:  is a given key-value pair in the message header? */
    private Predicate isProperty(final String exptectedProp) {
      return new Predicate() {
        @Override
        public boolean matches(Exchange exchange) {
          Map<String, Object> headers = exchange.getIn().getHeaders();
          try {
            if (headers.containsKey(exptectedProp)) {
              return true;
            }
          } catch (Exception ex) {
            Logger.getLogger(XNetMessaging.class.getName()).log(Level.SEVERE, null, ex);
          }
          return false;
        }
      };
    }
    /* EIS use case:  is a given key-value pair in the MapMessage body? */

    private Predicate isBodyProperty(final String exptectedKey) {
      return new Predicate() {
        @Override
        public boolean matches(Exchange exchange) {
          return exchange.getIn().getBody(HashMap.class).containsKey(exptectedKey);
        }
      };
    }

    /* EIS use case:  is UserType(key)='patient'(value) in MapMessage header? */
    private Predicate isPropertyValue(final String key, final Object expectedValue) {
      return new Predicate() {
        @Override
        public boolean matches(Exchange exchange) {
          boolean result = false;
          //routing is header based, not by content (body)
          //Object value = exchange.getIn().getBody(HashMap.class).get(key);
          //MapMessage msg = (MapMessage)exchange.getIn();
          Map<String, Object> headers = exchange.getIn().getHeaders();
          try {
            if (key.equalsIgnoreCase("rcvZoneId") || key.equalsIgnoreCase("rcvCenterId")) {
              int value = (int) headers.get(key);
              result = (value == (int) expectedValue);
            }
            if (key.equalsIgnoreCase("rcvCountryId") || key.equalsIgnoreCase("rcvDomain")) {
              String value = (String) headers.get(key);
              result = expectedValue.equals(value);
            }
          } catch (Exception ex) {
            Logger.getLogger(XNetMessaging.class.getName()).log(Level.SEVERE, null, ex.getMessage());
          }
          Log.info("Processing key=" + key + " eq expVal=" + expectedValue + ": result is " + result);
          return result;
        }
      };
    }

    /**
     * @param country the country to set
     */
    public void setCountry(String country) {
      this.country = country;
    }

    /**
     * @param queue of the zone on a parent XNET bus to get messages from
     */
    public void setXNetZone(String queue) {
      this.queueXNetZone = queue;
    }
  } //end of class XNetRoutes
}
