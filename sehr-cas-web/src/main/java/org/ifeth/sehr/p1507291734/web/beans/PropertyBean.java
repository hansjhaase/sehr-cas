/*
 * (C) IFETH
 */
package org.ifeth.sehr.p1507291734.web.beans;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.ifeth.sehr.core.objects.SEHRConfigurationObject;
import org.ifeth.sehr.core.spec.SEHRConstants;
import org.ifeth.sehr.p1507291734.lib.Constants;

/**
 * Common control to manage the module configuration.
 *
 * @author hansjhaase
 */
@Singleton
//@Startup
public class PropertyBean implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger Log = Logger.getLogger("org.ifeth.p1507291734.web");
  private static final String MODULECFG = "/WEB-INF/" + Constants.MODULE_NAME + ".properties";

  private Properties pPlain = null;
  private Properties pInterpreted = null; //all %{} should be ready now
  private String zoneid;
  private String subdomain;
  private SEHRConfigurationObject sco;

  public enum States {

    BEFORESTARTED, STARTED, PAUSED, SHUTTINGDOWN
  };

  private States state;

  @PostConstruct
  public void initialize() {
    state = States.BEFORESTARTED;
    // Perform intialization

    state = States.STARTED;
    Log.info(PropertyBean.class.getName()+":@PostConstruct:Configuration loaded.");
  }

  @PreDestroy
  public void terminate() {
    state = States.SHUTTINGDOWN;
    // Perform termination
     Log.info(PropertyBean.class.getName()+":@PreDestroy:Termination in progress.");
  }

  public void processConfiguration() {
    try {
      InitialContext ic = new InitialContext();
      pPlain = (Properties) ic.lookup(Constants.ICPropName);
      sco = (SEHRConfigurationObject) ic.lookup(SEHRConstants.icSCO);
    } catch (NamingException e) {
      Log.severe(PropertyBean.class.getName() + ":processConfiguration():" + e.getMessage());
    }

    if (pPlain != null) {
      pInterpreted = pPlain;
      zoneid = (String) pPlain.getProperty("zoneid");
      subdomain = pPlain.getProperty("subdomain", "");
      if (!StringUtils.isBlank(subdomain) && subdomain.contains("%{zoneid}")) {
        Map<String, String> values = new HashMap<>();
        values.put("zoneid", zoneid);
        StrSubstitutor sub = new StrSubstitutor(values, "%{", "}");
        subdomain = sub.replace(pPlain.getProperty("zoneid", ""));
        pInterpreted.setProperty("subdomain", subdomain);
      }
    } else {
      Log.warning(PropertyBean.class.getName() + ":processConfiguration():No configuration object in JNDI context !");
    }
  }

  public States getState() {
    return state;
  }

  public void setState(States state) {
    this.state = state;
  }

  public Properties getPlainSettings() {
    if (pPlain == null) {
      processConfiguration();
    }
    return pPlain;
  }

  public Properties getConfiguration() {
    if (pInterpreted == null) {
      processConfiguration();
    }
    return pInterpreted;
  }

  public SEHRConfigurationObject getSEHRConfigurationObject() {
    if (pInterpreted == null) {
      processConfiguration();
    }
    return this.sco;
  }

  public SEHRConfigurationObject buildClientSEHRConfigurationObject() {
    if (pInterpreted == null) {
      processConfiguration();
    }
    //build SEHR configuration for requesting by (unsecure) clients
    SEHRConfigurationObject cfgObj = this.sco;
    if (this.sco == null) {
      cfgObj = new SEHRConfigurationObject();
      cfgObj.setZoneid(pInterpreted.getProperty("zoneid", "0000000"));

      //old: activemqurl, since XNET: sehrxnetzoneurl
      cfgObj.setAMQBrokerURL(pInterpreted.getProperty("sehrxnetzoneurl"));
      //+++ passwords are not allowed to store on this context (unsecure)
      //cfgObj.setAMQUser(pInterpreted.getProperty("sehrxnetzoneuser"));
      //cfgObj.setAMQUserPw(pInterpreted.getProperty("sehrxnetzonepw"));
      cfgObj.setDomain(pInterpreted.getProperty("domain"));
      cfgObj.setSubdomain(pInterpreted.getProperty("subdomain"));
      Integer corbaPort = Integer.decode(pInterpreted.getProperty("portCORBA", "3700"));
      cfgObj.setIIOPPort(corbaPort);
      //TODO: use mail settings from GF contect in future
      cfgObj.setIMAP(pInterpreted.getProperty("mailIMAP"));
      //TODO: use mail settings from GF contect in future
      cfgObj.setSMTP(pInterpreted.getProperty("mailSMTP"));
      //+++ passwords are not allowed in this context (unsecure)
      cfgObj.setPOP3(pInterpreted.getProperty("mailPOP3"));
      //+++ passwords are not allowed in this context (unsecure)

      //SEHRIP: To access zone host by VPN etc.
      cfgObj.setSEHRIP(pInterpreted.getProperty("SEHRIP"));

      cfgObj.setRootOID(pInterpreted.getProperty("RootOID"));
    }
    //--- clear internal parameters to prevent hacking
    cfgObj.setModuleOwner(null);
    cfgObj.setModuleRoot(null);
    cfgObj.setHomePath(null);
    //--- do not allow native DB access by clients (by convention)
    cfgObj.setDbHost(null);
    cfgObj.setDbPort(null);
    cfgObj.setDbSec(null);
    cfgObj.setDbSecUser(null);
    cfgObj.setDbSecUserPw(null);

    cfgObj.setLDAPUserDN(null); //prevents unauthorized access on LDAP
    cfgObj.setLDAPUserPw(null);

    cfgObj.setAMQUser(null); //prevents unauthorized access on AMQ
    cfgObj.setAMQUserPw(null);
    cfgObj.setSecKey(null); //prevents unauthorized access on SEHR services
    //the client that requests the connectivity data set must get a 
    //written (secure transferred) statement containing auth and passwords 
    //or PKI key to use the basic services
    Log.fine(PropertyBean.class.getName() + ":buildClientSEHRConfigurationObject():cfg=" + cfgObj.toString());
    return cfgObj;
  }
}
