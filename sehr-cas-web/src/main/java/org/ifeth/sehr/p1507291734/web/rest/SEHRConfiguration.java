/*
 * (C) IFETH
 */
package org.ifeth.sehr.p1507291734.web.rest;

import java.beans.XMLEncoder;
import java.io.ByteArrayOutputStream;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.enterprise.context.RequestScoped;
import org.ifeth.sehr.core.objects.SEHRConfigurationObject;
import org.ifeth.sehr.p1507291734.web.beans.PropertyBean;
import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;

/**
 * REST Web Service to get some non critical settings.
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@Path("config")
@RequestScoped
public class SEHRConfiguration {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");
  private static final Gson GJson = new Gson();

  @EJB
  private PropertyBean pBean;

  /**
   * Creates a new instance of SEHRConfiguration
   */
  public SEHRConfiguration() {
  }

  /**
   * Retrieves representation of SEHRConfigurationObject.
   * <p>
   * On external requests this configuration instance does not contains
   * passwords and secure settings.
   * </p>
   *
   * @return org.ifeth.sehr.core.objects.SEHRConfigurationObject
   */
  @GET
  @Path("xmlEncoded")
  @Produces("application/xml")
  public String getXml() {
    String outXML;
    SEHRConfigurationObject cfg = getSEHRCfgObj();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (XMLEncoder xmlEncoder = new XMLEncoder(baos)) {
      xmlEncoder.writeObject(cfg);
      outXML = baos.toString();
    }

    //or JAXB...
    //StringWriter sw = new StringWriter();
    //jaxbMarshaller.marshal(cfg, sw);
    //outXML = sw.toString();
    return outXML;
  }

  @GET
  @Path("jsonEncoded")
  @Produces("application/json")
  public String getJson() {
    SEHRConfigurationObject cfg = getSEHRCfgObj();
    String out = GJson.toJson(cfg);
    return out;
  }

  private SEHRConfigurationObject getSEHRCfgObj() {
    SEHRConfigurationObject cfg;
    try {
      cfg = pBean.buildClientSEHRConfigurationObject();
      //not allowed for external clients by convention
//      cfg.setModuleOwner(null);
//      cfg.setModuleRoot(null);
//      cfg.setHomePath(null);
//      //prevents native access on DB (not allowed by convention)
//      cfg.setDbHost(null); 
//      cfg.setDbPort(null); 
//      cfg.setDbSec(null); 
//      cfg.setDbSecUser(null); 
//      cfg.setDbSecUserPw(null);
//      
//      cfg.setLDAPUserDN(null); //prevents unauthorized access on LDAP
//      cfg.setLDAPUserPw(null);
//      
//      cfg.setAMQUser(null); //prevents unauthorized access on AMQ
//      cfg.setAMQUserPw(null);
//      cfg.setSecKey(null); //prevents unauthorized access on SEHR services
//      //the client that requests the connectivity data set must get a 
//      //written (secure transferred) statement containing auth and passwords 
//      //or PKI key to use the basic services
//      Log.fine(SEHRConfiguration.class.getName() + ":getSEHRCfgObj():cfg=" + cfg.toString());

      //replace 127.0.0.1 or localhoat in AMQBrokerUrl by Server IP
      String amqURL = cfg.getAMQBrokerURL();
      if (StringUtils.isNotBlank(amqURL)) {
        //TODO get host IP if there is no fqdn
        String amqHost = (StringUtils.isNotBlank(cfg.getSubdomain()) ? cfg.getSubdomain() + "." : "") + cfg.getDomain();
        if (amqURL.contains("127.0.0.1")) {
          StringUtils.replace(amqURL, "127.0.0.1", amqHost);
        } else if (amqURL.contains("localhost")) {
          StringUtils.replace(amqURL, "localhost", amqHost);
        }
        cfg.setAMQBrokerURL(amqURL);
      }
    } catch (RuntimeException re) {
      Log.severe(SEHRConfiguration.class.getName() + ":getSEHRCfgObj():" + re.toString());
      return null;
    }
    return cfg;
  }
}
