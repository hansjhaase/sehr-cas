/*
 * (C)2015 MDI GmbH for the SEHR Community
 */
package org.ifeth.sehr.p1507291734.web.beans;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.camel.Exchange;
import org.apache.camel.RecipientList;

/**
 * Routing (forwarding) messages to centers for a given zone received by XNet
 * bus zone queue.
 *
 * This class is part of EIS using Apache Camel routing/processing.
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
public class XNetCenterRoutingBean {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web.beans");

  //TODO create parameter in route for run-time directory, 
  //see camel.apache.org/bean-binding,html
  public final static String baseDir = "/srv/sehrroot/data";

  @RecipientList
  public String[] route(Exchange exch) {
    //MapMessage msg = (MapMessage) exch.getIn();
    //Map<String, Object> body = exch.getIn().getBody(HashMap.class);
    Map<String, Object> headers = exch.getIn().getHeaders();
    try {
      int zoneId = (int) headers.get("rcvZoneId");
      //int zoneId = msg.getIntProperty("rcvZoneId");
      //int zoneId = (Integer) body.get("rcvZoneId");
      int centerId = (int) headers.get("rcvCenterId");
      //msg.getIntProperty("rcvCenterId");//(Integer) body.get("rcvCenterId");
      String qCenter = String.format("%07d", zoneId) + "." + String.format("%07d", centerId);
      Log.info(XNetCenterRoutingBean.class.getName() + ":route():JMSReplyTo=" + headers.get("JMSReplyTo") + ", msg delivered to " + qCenter);
      return new String[]{"xzone:queue:sehr." + qCenter + ".queue"};
      //,"file://"+baseDir+"/amqin/"+String.format("%07d",centerId)    
    } catch (Exception ex1) {
      Logger.getLogger(XNetCenterRoutingBean.class.getName()).log(Level.SEVERE, null, ex1.getMessage());
    }
    //return new String[]{"file://"+baseDir+"/amqin/unknown"};
    return new String[]{"xzonec:queue:sehr.DLQ.queue"};
  }
}
