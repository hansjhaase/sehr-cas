/*
 * (C) 2015 IFETH
 */
package org.ifeth.sehr.p1507291734.web.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.lang3.StringUtils;
import org.ifeth.sehr.p1507291734.lib.Constants;
import org.ifeth.sehr.p1507291734.web.listener.XNetMessaging;

/**
 * Helping class to handle messages on the XNet bus.
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
public class XNetMessagingHandler {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");

  private final CamelContext xnetContext;
  private final XNetMessaging xnetMessenger;
  private Properties p;

  public XNetMessagingHandler(XNetMessaging xnetMessaging) {
    this.xnetMessenger = xnetMessaging;
    this.xnetContext = xnetMessaging.getEISContext();
  }

  public int getLocalZoneID() {
    int zid = Integer.parseInt(p.getProperty("zoneid", "0"));
    return zid;
  }

  public String getLocalZoneIDAsString() {
    int zid = getLocalZoneID();
    return (zid > 0 ? String.format("%07d", zid) : null);
  }

  public Object getSEHRConfigurationValue(String key) {
    return getProperty(key);
  }

  public String getDomain() {
    return (String) getSEHRConfigurationValue("domain");
  }

  public String getLocalDomain() {
    String domain = getDomain();
    if (domain != null) {
      String s = (String) getSEHRConfigurationValue("subdomain");
      domain = (s != null ? s + "." + domain : domain);
    }
    return (domain != null ? domain : "n/a");
  }

  public String sendMonitorCommand(String routeId, String action) {
    String s;
    try {
      ProducerTemplate template = xnetContext.createProducerTemplate();
      template.sendBody("controlbus:route?routeId=" + routeId + "&action=" + action, null);
      s = "Control command " + action + " sent to route " + routeId;
    } catch (Exception e) {
      Log.warning(XNetMessagingHandler.class.getName() + ":sendMonitorCommand():" + e.getMessage());
      s = "Error: " + e.getMessage();
    }
    return s;
  }

  public String sendTestMessage(String toDomain, Integer toZID, Integer toCID, String subject, Object body) {

    String localDomain = getLocalDomain();
    Map header = new HashMap();
    header.put("JMSType", "SEHR#XNET");
    header.put("sehrReplyTo", "queue://" + xnetMessenger.getZoneQueue());
    //header.put("JMSReplyTo", ActiveMQConverter.toDestination("queue://"+this.queueLocal));
    header.put("JMSCorrelationID", "XNET-Test-" + System.currentTimeMillis());
    //header.put("JMSDeliveryMode", null);
    //header.put("JMSDestination", null);
    //header.put("JMSExpiration", 30000);
    //header.put("JMSMessageID", null);
    header.put("JMSPriority", 4);
    //header.put("JMSRedelivered", false);
    header.put("JMSTimestamp", System.currentTimeMillis());
    if (!StringUtils.isBlank(localDomain)) {
      header.put("origDomain", localDomain);
    }
    if (!StringUtils.isBlank(toDomain)) {
      header.put("rcvDomain", toDomain);
    }

    String zoneId = p.getProperty("zoneid", "");
    if (!StringUtils.isBlank(zoneId)) {
      //if this SEHR-CAS component is running on a zone host...
      header.put("origZoneId", zoneId); //just a test from a zone
    }
    header.put("rcvZoneId", toZID); //Integer!
    header.put("rcvCenterId", toCID); //Integer!
    //Important! 'subject' is  part of header for routing 
    //if starting with 'SEHR', 'Ref:', 'Issue#' etc.
    header.put("subject", (subject != null ? subject : "Test"));
    //Map body = new HashMap(); //body as plain text is Map is ok...
    //body.put("testTransform", "The jumping rabbit...");
    //body.put("text", "Hi folks!");
    if (body == null) {
      body = "Hi folks!"; //body as plain text is ok...
    }
    //send to next level host; the host that responsible for the domain 
    //this zone has been attached to
    String toXNetEndpoint = xnetMessenger.getXNetDomainBus(); 
    if(StringUtils.isBlank(toXNetEndpoint)){
      Log.warning(XNetMessagingHandler.class.getName() + ":sendTestMessage():No 'XNetDomainBus");
      return "Error sending message. No 'XNetDomainBus";
    }
    try {
      ProducerTemplate template = xnetContext.createProducerTemplate();
      template.sendBodyAndHeaders("xdom:queue:" + toXNetEndpoint, body, header);
      //template.sendBody("xnet:queue:" + xnetMessenger.getZoneQueue(), map);
    } catch (CamelExecutionException cee) {
      Log.warning(XNetMessagingHandler.class.getName() + ":sendTestMessage():" + cee.getMessage());
      return "Error sending message to " + toXNetEndpoint;
    } catch (RuntimeException re) {
      Log.severe(XNetMessagingHandler.class.getName() + ":sendTestMessage():" + re.getMessage());
      return "Error sending message to " + toXNetEndpoint;
    }
    return "Message sent to " + toXNetEndpoint;
  }

  /**
   * Get value from module property file.
   */
  private Object getProperty(String key) {
    if (p == null) {
      init(); //try to reinit...
    }
    return (p != null ? p.getProperty(key, null) : null);
  }

  private void init() {
    try {
      InitialContext ic = new InitialContext();
      p = (Properties) ic.lookup(Constants.ICPropName);
    } catch (NamingException e) {
      Log.severe(XNetMessagingHandler.class.getName() + ":init():" + e.getMessage());
    }
  }
}
