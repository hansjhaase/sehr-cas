/*
 * (C)2015 MDI GmbH for the SEHR Community
 */
package org.ifeth.sehr.p1507291734.web.beans;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.camel.Exchange;
import org.apache.camel.RecipientList;
import org.apache.commons.lang3.StringUtils;

/**
 * Routing and processing from SEHR country bus.
 * <p>
 * The work is in progress...<br/>
 * There are a lot of use cases not yet implemented!
 * </p>
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
public class XNetProcessCountryLevelBean {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web.beans");
  private String casCountryCode = "de"; //country this CAS is responsible for
  private String casDomain = "e-hn.org"; //domain this CAS is responsible for
  //TODO create parameter in route for run-time directory, 
  //see camel.apache.org/bean-binding.html
  public final static String baseDir = "/srv/sehrroot/data";

  @RecipientList
  public String[] route(Exchange exch) {
    String locDomain = casCountryCode + "." + casDomain;
    //MapMessage msg = (MapMessage) exch.getIn();
    //Map<String, Object> body = exch.getIn().getBody(HashMap.class);
    Map<String, Object> headers = exch.getIn().getHeaders();

    try {
      //we assume that the domain has been build correctly
      //e.g. rcvDomain=z9999999.anysubnet.de.e-hn.org
      //e.g. rcvDomain=z9999901.de.e-hn.org
      //e.g. rcvDomain=z9999902.de.private.net 
      String domain = (String) headers.get("rcvDomain");
      //verify domain, by convention the there must be more than 3 parts...
      if (StringUtils.isNotBlank(domain)) {
        String domParts[] = domain.split("\\.");
        int lParts = domParts.length;
        if (lParts < 4) {
          Log.info("Invalid syntax: rcvDomain '" + domain + "' is to short.");
          return new String[]{"xctry:queue:sehr.xnet.DLQ." + locDomain + ".queue"};
        }
        String rootPart = domParts[lParts - 2] + "." + domParts[lParts - 1];
        if (!casDomain.equals(rootPart)) {
          Log.info("Foreign domain detected: rcvDomain '" + domain + "' is not part of '" + rootPart + "'");
          return new String[]{"xctry:queue:sehr.xnet.DLQ." + locDomain + ".queue"};
        }
        String ctryPart = domParts[lParts - 3];
        if (!casCountryCode.equals(ctryPart)) {
          Log.info("Other country detected: rcvDomain '" + domain + "' is part of '" + ctryPart + "'");
          return new String[]{"xroot:queue:sehr.xnet." + ctryPart + "." + casDomain + ".queue"};
        }
        //TODO check country syntax ISO 2-letters
        String providerPart = domParts[lParts - 4];
          //Forward message to the domain queue on the country bus
        //The domain broker will pick it up from here... (listen on this queue)
        return new String[]{"xctry:queue:sehr.xnet." + providerPart + "." + locDomain + ".queue"};
          //,"file://"+baseDir+"/amqin/"+String.format("%07d",centerId)

      }
      //MDI/MyJMed ... try to continue routing based on zoneid only
      //This works only on 1-level targets (not for zones within subnets)
      Integer zoneId = (Integer) headers.get("rcvZoneId");
      if (zoneId != null && zoneId > 0) {
        String tmpDomain = "z" + String.format("%07d", zoneId) + "." + locDomain;
        headers.put("rcvDomain", tmpDomain);
        headers.put("XCheckrcvDomain", "added due to missing property");
        exch.getIn().setHeaders(headers);
        Log.info("MyJMed/JMed helper route: Missing rcvDomain build: " + tmpDomain);
        return new String[]{"xctry:queue:sehr.xnet." + tmpDomain + ".queue"};
      }
    } catch (Exception ex) {
      Logger.getLogger(XNetProcessCountryLevelBean.class.getName()).log(Level.SEVERE, null, ex);
    }
    //return new String[]{"file://"+baseDir+"/amqin/unknown"};
    return new String[]{"xctry:queue:sehr.DLQ." + locDomain + ".queue"};
  }
}
