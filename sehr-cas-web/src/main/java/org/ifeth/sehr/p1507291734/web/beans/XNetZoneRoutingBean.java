/*
 * (C)2015 MDI GmbH for the SEHR Community
 */
package org.ifeth.sehr.p1507291734.web.beans;

import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.camel.Exchange;
import org.apache.camel.RecipientList;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
public class XNetZoneRoutingBean {

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
      //msg.getIntProperty("rcvZoneId");
      //if(zoneId==null){
      //  return new String[]{"local:queue:sehr.DLQ.queue"};
      //}

      //receiver domain, e.g. 
      //[subnet].de.e-hn.org', 'de.e-hn.org', 'e-hn.org' 
      //or a 'private.net'
      String domain = (String) headers.get("rcvDomain");
      //msg.getStringProperty("rcvDomain");
      if (StringUtils.isBlank(domain)) {
        domain = "e-hn.org";
        String country = (String) headers.get("rcvCountryId"); //ISO2
        //msg.getStringProperty("rcvCountryId"););
        if (StringUtils.isBlank(country)) {
          country = "de"; //'de' by default ;)
          //headers.put("rcvCountryId", countryId);
        } else {
          String[] locales = Locale.getISOCountries();
          for (String countryCode : locales) {
            Locale obj = new Locale("", countryCode);
            //System.out.println("Country Code = " + obj.getCountry()
            //        + ", Country Name = " + obj.getDisplayCountry());
            if (obj.getCountry().equalsIgnoreCase(country)) {
              //found...
              break;
            }
          }
          country = "de"; //'de' by default if code not found ;)          
        }
        domain = country + "." + domain;
        //Add missing domain to header...
        headers.put("rcvDomain", domain);
        headers.put("XADDDEDrcvDomain", "property corrected by specification");
      }
      headers.put("XRECEIVED", System.currentTimeMillis());
      exch.getOut().setHeaders(headers);
      return new String[]{"xdom:queue:sehr.xnet.z" + String.format("%07d", zoneId) + "." + domain + ".queue"};
      //,"file://"+baseDir+"/amqin/"+String.format("%07d",centerId)

    } catch (Exception ex) {
      Logger.getLogger(XNetZoneRoutingBean.class.getName()).log(Level.SEVERE, null, ex);
    }
    //return new String[]{"file://"+baseDir+"/amqin/unknown"};
    return new String[]{"xdom:queue:sehr.DLQ.queue"};
  }
}
