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
import org.ifeth.sehr.core.handler.LifeCARDObjectHandler;

/**
 * Routing and processing LifeCARD outgoing messages.
 * <p>
 * The work is in progress...<br/>
 * There are a lot of use cases not yet implemented!
 * </p>
 *
 * @author Hans J Haase &lt;hansjhaase@mdigmbh.de&gt;
 */
public class XNetLifeCARDOutBean {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");

  //TODO create parameter in route for run-time directory, 
  //see camel.apache.org/bean-binding,html
  public final static String baseDir = "/srv/sehrroot/data";

  @RecipientList
  public String[] route(Exchange exch) {
    //MapMessage msg = (MapMessage) exch.getIn();
    //Map<String, Object> body = exch.getIn().getBody(HashMap.class);
    Map<String, Object> headers = exch.getIn().getHeaders();
    try {
      String lcn = (String) headers.get("lcNumber");
      //should be like 'DE-0000000-0000000-00000000'
      if (StringUtils.isBlank(lcn) || LifeCARDObjectHandler.isSyntaxC778(lcn) == false) {
        Log.info(XNetLifeCARDOutBean.class.getName() + ":route():Invalid LC number!");
        return new String[]{"xzone:queue:sehr.DLQ.lifecard.out.queue"};
      }
      //Check county...
      boolean isCountryCodeValid = true;//LifeCARDObjectHandler.checkCountryCode(lcn);
      if (!isCountryCodeValid) {
        Log.info(XNetLifeCARDOutBean.class.getName() + ":route():Invalid country code!");
        return new String[]{"xzone:queue:sehr.DLQ.lifecard.out.queue"};
      }
      //TODO add more if required... e.g. notify someone in a list of consumers
      //e.g. if a medication has been changed and it is an elderly person inform a nurse
      //Finally send... LC message queues are on the root level by convention
      String parts[] = lcn.split("\\-", -1);
      return new String[]{"xroot:queue:sehr.lc." + parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3] + ".queue"};
      //,"file://"+baseDir+"/amqin/"+String.format("%07d",centerId)

    } catch (Exception ex) {
      Logger.getLogger(XNetLifeCARDOutBean.class.getName()).log(Level.SEVERE, null, ex);
    }
    //a local zone broker is always present, even on provider or upper levels.
    return new String[]{"xzone:queue:sehr.DLQ.lifecard.out.queue"};
  }
}
