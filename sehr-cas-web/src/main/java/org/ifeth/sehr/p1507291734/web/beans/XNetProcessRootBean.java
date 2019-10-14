/*
 * (C)2015 MDI GmbH for the SEHR Community
 */
package org.ifeth.sehr.p1507291734.web.beans;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.camel.Exchange;
import org.apache.camel.RecipientList;
import org.apache.commons.lang.StringUtils;

/**
 * Routing and processing SEHR root queue.
 * <p>
 * The work is in progress...<br/>
 * There are a lot of use cases not yet implemented!
 * </p>
 * @author Hans J Haase &lt;hansjhaase@mdigmbh.de&gt;
 */
public class XNetProcessRootBean {

  //TODO create parameter in route for run-time directory, 
  //see camel.apache.org/bean-binding,html
  public final static String baseDir = "/srv/sehrroot/data";

  @RecipientList
  public String[] route(Exchange exch) {
    //MapMessage msg = (MapMessage) exch.getIn();
    //Map<String, Object> body = exch.getIn().getBody(HashMap.class);
    Map<String, Object> headers = exch.getIn().getHeaders();
    try {
      String ehnTopLevel="e-hn.org";
      String domain = (String) headers.get("rcvDomain");
      //should be like 'z9999999.de.e-hn.org'
      if (StringUtils.isBlank(domain)) {
        headers.put("rcvDomain", ehnTopLevel);
        headers.put("XCheckrcvDomain", "added due to missing property");
      }else{
        String[] parts = domain.split("\\.");
        int lParts=parts.length;
        if(lParts>0){
          ehnTopLevel = (lParts>=2?parts[lParts-2]+".":"")+parts[lParts-1];        
        }
      }
      String countryId = (String) headers.get("rcvCountryId");
      if (StringUtils.isBlank(countryId)) {
        countryId = "de"; //'de' by default ;)
        headers.put("rcvCountryId", countryId);
        headers.put("XCheckrcvCountryId", "added due to missing property");
      }
      exch.getIn().setHeaders(headers);
      //forward message to the queue of the country on root bus
      //The country broker will pick it up from here... (listen on this queue)
      //At root level we do not now the final destination broker...
      return new String[]{"xroot:queue:sehr.xnet."+ countryId + "." + ehnTopLevel + ".queue"};
      //,"file://"+baseDir+"/amqin/"+String.format("%07d",centerId)

    } catch (Exception ex) {
      Logger.getLogger(XNetProcessRootBean.class.getName()).log(Level.SEVERE, null, ex);
    }
    //return new String[]{"file://"+baseDir+"/amqin/unknown"};
    return new String[]{"xroot:queue:sehr.DLQ.e-hn.org.queue"};
  }
}
