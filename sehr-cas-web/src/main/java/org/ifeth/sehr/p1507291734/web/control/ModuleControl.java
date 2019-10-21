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
 * since 8/2015
 */
package org.ifeth.sehr.p1507291734.web.control;

import com.google.common.eventbus.EventBus;
import java.io.IOException;
import org.ifeth.sehr.core.spec.Constants;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import javax.inject.Named;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
//import javax.faces.bean.ApplicationScoped;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang3.StringUtils;
//import org.ifeth.sehr.core.lib.StringFormat;
//import org.ifeth.sehr.core.objects.SEHRConfigurationObject;
import org.ifeth.sehr.intrasec.entities.NetCenter;
import org.ifeth.sehr.intrasec.entities.NetServices;
import org.ifeth.sehr.intrasec.entities.NetZones;
import org.ifeth.sehr.p1507291734.lib.LoggerUtility;
import org.ifeth.sehr.p1507291734.web.beans.PropertyBean;
import org.ifeth.sehr.p1507291734.web.listener.SAFQueueListener;
import org.ifeth.sehr.p1507291734.web.listener.XNetMessaging;
import org.ifeth.sehr.p1507291734.web.utils.NotificationEventProcessor;
import org.primefaces.push.EventBusFactory;

/**
 * Common control for WEB amd mobile views with application wide functions.
 *
 * @author hansjhaase
 */
@Named(value = "moduleCtrl")
@DependsOn("PropertyBean")
@ApplicationScoped
public class ModuleControl implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger Log = Logger.getLogger("org.ifeth.p1507291734.web");
  private Properties p;
  //runtime SEHR configuration of this zone host
  //can be set from sehr-osi but also by sehr-cas
  //private SEHRConfigurationObject sco;
  //--- class related attributes
  //private SEHRServiceRemote sehrService;
  //private String sehrhostip = "127.0.0.1"; //host of a local SEHR host
  private List<NetCenter> lstConCenter = new ArrayList<>();
  private List<NetZones> lstConZones = new ArrayList<>();
  private List<NetServices> lstConServices = new ArrayList<>();
  private String countryLoc = "zz"; //Default=unknown - see ISO conventions
  //values of the property file
  //private String pzoneID;
  //private String psubdomain;
  private EventBus eventBus; //app scoped event processing
  private org.primefaces.push.EventBus pfEventBus; //HTML5 EventBus

  @EJB
  private PropertyBean pBean;

  @Inject
  private ServletContext sctx;

  /**
   * Creates a new instance of ModuleControl
   */
  public ModuleControl() {
  }

  @PostConstruct
  private void init() {
    Log.fine(ModuleControl.class.getName() + "@PostConstruct:init()");
    p = pBean.getConfiguration();

    if (p != null) {
      //default is error+warning
      int debug = Integer.parseInt(p.getProperty("debug", "2"));
      LoggerUtility.assignLevelByDebug(debug, Log);
      Log.finer(ModuleControl.class.getName() + "@PostConstruct:init():Logging Level=" + Log.getLevel());
    } else {
//      try {
//        InitialContext ic = new InitialContext();
//        p = (Properties) ic.lookup(Constants.ICPropName);
//        pzoneID = p.getProperty("zoneid");
//        psubdomain = p.getProperty("subdomain", "");
//        if (!StringUtils.isBlank(psubdomain) && psubdomain.contains("%{zoneid}")) {
//          Map<String, String> values = new HashMap<>();
//          values.put("zoneid", pzoneID);
//          StrSubstitutor sub = new StrSubstitutor(values, "%{", "}");
//          psubdomain = sub.replace(p.getProperty("subdomain", ""));
//        }
//      } catch (NamingException e) {
//        Log.severe(ModuleControl.class.getName() + ":init():" + e.getMessage());
//      }
    }
    if (sctx.getAttribute("EventBus") == null) {
      //Configure EventBus for app wide publish/subscribe
      this.eventBus = new EventBus();
      //this.pfEventBus = EventBusFactory.getDefault().eventBus();
      NotificationEventProcessor evtProcessor = new NotificationEventProcessor();
      pfEventBus = EventBusFactory.getDefault().eventBus();
      evtProcessor.setWEBSocketProcessor(this.pfEventBus);
      eventBus.register(evtProcessor);
      sctx.setAttribute("EventBus", this.eventBus);
      Log.info(ModuleControl.class.getName() + "EventBus initialized ...");
    }
  }

  /**
   * Get debugginglevel by configuration file 'WEB-INF/...properties'.
   *
   * @return A value from 0...9
   */
  public int getDebugMode() {
    return Integer.parseInt(p.getProperty("debug", "0"));
  }

  public String validateCountryCode(String cc) {
    boolean found = false;

    if (!StringUtils.isBlank(cc)) {
      String[] loc = Locale.getISOCountries();
      for (String loc1 : loc) {
        //System.out.println("ISO Contry " + a + ":" + loc1);
        if (cc.toUpperCase().equalsIgnoreCase(loc1)) {
          found = true;
          break; //found
        }
      }
    }
    return (found ? cc : "zz"); //do not return unknown codes
  }

  /**
   * Get the country code this zone is configured for.
   *
   * @return
   */
  public String getCountryCode() {
    return p.getProperty("country", getSystemCountryCode());
  }

  public String getSystemCountryCode() {
    return Locale.getDefault().getLanguage();
  }

  public String getVersion() {
    return Constants.MAJOR_VERSION;
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

  /**
   * The EHN domain this zone is connected to for exchanging XNET messages
   * globally or on a provider level.
   * <p>
   * This is the domein level where the queue for this zone is located to get
   * messages from other zones (groups oor communities of care).
   * </p>
   *
   * @return
   */
  public String getDomain() {
    return (String) getProperty("domain");
  }

  /**
   * The local domain this zone is configured for.
   *
   * @return
   */
  public String getOperatingDomain() {
    String domain = getDomain();
    if (domain != null) {
      String s = (String) p.getProperty("subdomain", "");
      domain = (s != null ? s + "." + domain : domain);
    }
    return (domain != null ? domain : "n/a");
  }

  /**
   * Returns the local SEHRHostIP to access SEHR-OSI service on port 24100 if
   * running.
   *
   * <p>
   * With the new SEHR-CAS module this is not required. Since SEHR-CAS we're
   * using messaging to transmit data. SEHR-OSI is not yet refactored to use
   * ApacheMQ and the 'SEHR MessagingBus'.
   * </p>
   *
   * @return
   */
  public String getSEHRHostIP() {
    return (String) getSEHRConfigurationValue("serverip");
  }

//  public void addMessage(String summary) {
//    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, null);
//    FacesContext.getCurrentInstance().addMessage(null, message);
//  }
  public Properties getProperties() {
    if (p == null) {
      init(); //try to reinit...
    }
    return p;
  }

  /**
   * Get value from module property file.
   *
   * @param key
   * @return
   */
  public Object getProperty(String key) {
    if (p == null) {
      init(); //try to reinit...
    }
    Log.config(ModuleControl.class.getName() + ":getProperty():" + (p != null ? (key + ":" + p.getProperty(key, null)) : "No configuration!"));
    return (p != null ? p.getProperty(key, null) : null);
  }

  /**
   * Add connected center to an internal list.
   * <p>
   * Used as cache of known connected centers so far)
   * </p>
   *
   * @param netCenter
   */
  public void addConnectedCenter(NetCenter netCenter) {
    if (this.lstConCenter == null) {
      this.lstConCenter = new ArrayList<>();
    }
    this.lstConCenter.add(netCenter);
  }

  /**
   * List of connected centers using the SEHR messaging bus of this zone.
   *
   * @return
   */
  public List<NetCenter> listConnectedCenter() {
    return this.lstConCenter;
  }

  /**
   * Remove center from the list if it disconnects from message bus.
   *
   * @param cid
   */
  public void removeConnectedCenter(int cid) {
    Iterator it = lstConCenter.iterator();
    while (it.hasNext()) {
      NetCenter nc = (NetCenter) it.next();
      if (nc.getNetCenterPK().getCenterid() == cid) {
        it.remove();
        return;
      }
    }
  }

  public boolean isLoginMobileAllowed() {
    //TODO 2nd check against ACL table
    String s = (String) getProperty("allowAdminMOBILE");
    return (s != null && s.equals("1"));
  }

  public boolean isLoginWebAllowed() {
    //TODO 2nd check against ACL table
    String allowAdminWEB = (String) getProperty("allowAdminWEB");
    Log.finer(ModuleControl.class.getName() + "isLoginWebAllowed():allowAdminWEB=" + allowAdminWEB);
    return (allowAdminWEB != null && allowAdminWEB.equals("1"));
  }

  public String getLocalAMQBrokerURL() {
    //FacesContext fctx = FacesContext.getCurrentInstance();
    //ExternalContext ectx = fctx.getExternalContext();
    //ServletContext sctx = (ServletContext) ectx.getContext();
    ActiveMQConnectionFactory amqcf = (ActiveMQConnectionFactory) sctx.getAttribute("ActiveMQConnectionFactory");
    String brokerURL = "n/a";
    if (amqcf != null) {
      brokerURL = amqcf.getBrokerURL();
    }
    return brokerURL;
  }

  public String getXNetStatus() {
    String info = "<span style='color:red;font-weight:bold;'>- no XNET connection -</span>";
    //FacesContext fctx = FacesContext.getCurrentInstance();
    //ExternalContext ectx = fctx.getExternalContext();
    //ServletContext sctx = (ServletContext) ectx.getContext();
    XNetMessaging xnetMessaging = (XNetMessaging) sctx.getAttribute("XNetMessaging");
    if (xnetMessaging != null) {
      info = xnetMessaging.getInfo(true); //true=as HTML
    }
    return info;
  }

  public String getStatus() {
    String info;
    //FacesContext fctx = FacesContext.getCurrentInstance();
    //ExternalContext ectx = fctx.getExternalContext();
    //ServletContext sctx = (ServletContext) ectx.getContext();
    XNetMessaging xnetMessaging = (XNetMessaging) sctx.getAttribute("XNetMessaging");
    if (xnetMessaging != null) {
      if (xnetMessaging.getEISContext() != null) {
        info = "XNet (EHN) routing " + xnetMessaging.getEISContext().getStatus().name().toLowerCase();
      } else {
        info = "<span style='color:red;'>No XNet routing (EHN) context.</span>";
      }
    } else {
      String ehndomain = getDomain(); //health networking domain
      //By convention a SEHR based network operated by IFETH ends with 'e-hn.org'
      //A national SEHR based network is prefixed with 2 letters:
      //'[countrycode].e-hn.org', e.g. 'de.e-hn.org'
      //A private/local provider can use 'my.network.org' or 'my.de.e-hn.org' 
      //if he operates a subnet.
      if (StringUtils.isBlank(ehndomain)) {
        info = "<span style='color:red;'>No Health Networking Domain</span>";
      } else if (StringUtils.endsWithIgnoreCase(ehndomain, "e-hn.org")) {
        String country = (String) getProperty("country");
        if (!StringUtils.isBlank(country)) {
          //System.out.println(getSystemCountryCode() + "=?" + new Locale(country).getLanguage());
          if (getSystemCountryCode().equalsIgnoreCase(new Locale(country).getLanguage())) {
            info = "<span style='color:red;'>No Connection/No Route</span> to the health net '" + ehndomain + "'";
          } else {
            info = "<span style='color:red;'>Mismatching country code</span> of the health net '" + ehndomain + "'";
          }
        } else {
          info = "'<span style='color:red;'>No Connection/No Route</span> to the health net '" + ehndomain + "'";
        }
      } else {
        info = "'<span style='color:red;'>No Connection/No Route</span> to '" + ehndomain + "'";
      }
    }
    return info;
  }

  public boolean isAMQConnected() {
    //FacesContext fctx = FacesContext.getCurrentInstance();
    //ExternalContext ectx = fctx.getExternalContext();
    //ServletContext sctx = (ServletContext) ectx.getContext();
    ActiveMQConnection amqCon = (ActiveMQConnection) sctx.getAttribute("ActiveMQConnection");
    if (amqCon != null) {
      return amqCon.isStarted();
    }
    return false;
  }

  public boolean isLocalServiceQueueActive() {
    //FacesContext fctx = FacesContext.getCurrentInstance();
    //ExternalContext ectx = fctx.getExternalContext();
    //ServletContext sctx = (ServletContext) ectx.getContext();
    ActiveMQConnection amqCon = (ActiveMQConnection) sctx.getAttribute("ActiveMQConnection");
    if (amqCon != null && amqCon.isStarted()) {
      int zoneid = Integer.parseInt(p.getProperty("zoneid", "0000000"));
      //+++ since 8/2015 'saf' is not longer used - it is a 'service'!
      String queue = "sehr." + String.format("%07d", zoneid) + ".service.queue";
      Map<String, SAFQueueListener> safListeners = (HashMap) sctx.getAttribute("SAFQueueListener");
      if (safListeners != null && safListeners.containsKey(queue)) {
        return true;
      }
    }
    return false;
  }

  public boolean isXNetConnected() {
    //FacesContext fctx = FacesContext.getCurrentInstance();
    //ExternalContext ectx = fctx.getExternalContext();
    //ServletContext sctx = (ServletContext) ectx.getContext();
    XNetMessaging xnetMessaging = (XNetMessaging) sctx.getAttribute("XNetMessaging");
    Log.log(Level.INFO, "{0}:isXNetConnected():{1}", new Object[]{ModuleControl.class.getName(), xnetMessaging != null ? xnetMessaging.toString() : "No XNetMessaging instance"});
    if (xnetMessaging != null) {
      return xnetMessaging.isStarted();
    }
    return false;
  }

  public String startXNet() {
    //FacesContext fctx = FacesContext.getCurrentInstance();
    //ExternalContext ectx = fctx.getExternalContext();
    //ServletContext sctx = (ServletContext) ectx.getContext();
    String content = "";
    XNetMessaging xnetMessenger = (XNetMessaging) sctx.getAttribute("XNetMessaging");
    //grabbed from XNetService servlet ;)
    if (xnetMessenger != null && xnetMessenger.isStarted()) {
      content = "SEHR XNet prcessor already running<br/>";
      content += xnetMessenger.getInfo(true);
    } else {
      if (!p.containsKey("sehrxnetdomainurl")) {
        content = "SEHR XNet domain host not defined (sehrxnetdomainurl)<br/>\n";
      } else {
        //queue to send out messages (EHNDomain), e.g. 'de.e-hn.org'
        String domain = p.getProperty("domain", "");
        String queueEHNDomain = "sehr.xnet." + domain + ".queue";

        //local queue of the zone to get messages from XNET (health net)
        String subdomain = p.getProperty("subdomain", "");

        String localDomain = subdomain + "." + domain;
        if (StringUtils.isBlank(subdomain)) {
          content += "Configuration Error: 'subdomain' must be a country code like 'de' or a valid zone domain name like 'z[ZID]'\n<br/>";
        } else if (StringUtils.isBlank(domain)) {
          content += "Configuration Error: 'domain' must be a valid domain name like 'e-hn.org'<br/>";
          //} else if (localDomain.equalsIgnoreCase(p.getProperty("domain", ""))) {
          //  content += "Parent domain is same as receiving domain. This makes no sense!<br/>";
        } else {
          if (!localDomain.toLowerCase().startsWith("z")) {
            content += "<span style='background-color:yellow;'>Local domain syntax differs from SEHR specification.</span><br/>";
          }
          String queueLocalDomain = "sehr.xnet." + localDomain + ".queue";
          //We're using Apache Camel for message routing and processing!
          xnetMessenger = new XNetMessaging(p);
          xnetMessenger.setCountry(p.getProperty("country", "de"));
          if (xnetMessenger.connect(queueEHNDomain, queueLocalDomain)) {
            sctx.setAttribute("XNetMessaging", xnetMessenger);
            content = xnetMessenger.getInfo(true);
            //content += xnetMessenger.getRoutesInfo(true);
          } else {
            content = "SEHR XNet connection error<br/>";
          }
        }
      }
    }
    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Result", content);
    addMessage(message);
    return null;
  }

  /**
   * Local service queue check.
   *
   * @return
   */
  public String getServiceQueueStatus() {
    FacesContext fctx = FacesContext.getCurrentInstance();
    ExternalContext ectx = fctx.getExternalContext();
    ServletContext sctx = (ServletContext) ectx.getContext();
    int zoneid = Integer.parseInt(p.getProperty("zoneid", "0000000"));
    String status = "<p style='color:red;'>Inactive service messaging for this zone (" + String.format("%07d", zoneid) + ")!</p>";
    //+++ since 8/2015 'saf' is not longer used - it is a 'service'!
    String queue = "sehr." + String.format("%07d", zoneid) + ".service.queue";
    Map<String, SAFQueueListener> safListeners = (HashMap) sctx.getAttribute("SAFQueueListener");
    if (safListeners != null && safListeners.containsKey(queue)) {
      status = "Listening on '" + queue + "'.";
    }
    return status;
  }

  /**
   * Check for a top level (root) or 'e-hn.org' configuration.
   *
   * <p>
   * By convention a top level is not a community of care. It is a health net
   * (operated by a provider using SEHR) to allow zones (communities of care) to
   * exchange EHR data by messaging.
   * </p>
   *
   * @return
   */
  public String getTopLevelCheck() {
    String domain = getDomain();
    String parts[] = domain.split("\\.");
    if (parts.length <= 2 || "e-hn.org".equalsIgnoreCase(domain)) {
      return "This host has been configured as '" + domain + "', a top Level configuration!";
    }
    return "";
  }

  /**
   * Check for HTTP access.
   * <p>
   * <b>Note:</b>This procedure currently checks the URL using
   * 'HttpURLConnection', not 'https'!</p>
   *
   * @param url
   * @return
   */
  public boolean isHTTPAccess(String url) {
    Log.fine(ModuleControl.class.getName() + ":isHTTPAccess():url=" + url);
    try {
      final URL u = new URL(url);
      HttpURLConnection huc = (HttpURLConnection) u.openConnection();
      huc.setConnectTimeout(1000); //try for 1000ms only
      huc.setRequestMethod("HEAD");
      int responseCode = huc.getResponseCode();
      Log.finer(ModuleControl.class.getName() + ":isHTTPAccess():responseCode=" + responseCode);
      return responseCode == 200;
    } catch (MalformedURLException ex) {
      Logger.getLogger(ModuleControl.class.getName()).log(Level.WARNING, ex.getMessage());
    } catch (ProtocolException ex) {
      Logger.getLogger(ModuleControl.class.getName()).log(Level.WARNING, ex.getMessage());
    } catch (IOException ex) {
      Logger.getLogger(ModuleControl.class.getName()).log(Level.WARNING, ex.getMessage());
    }
    return false;
  }

  public boolean isInternetAvailable() {
    return isHostAvailable("google.com");
    // || isHostAvailable("paypal.com")
    // || isHostAvailable("de.e-hn.org")
  }

  public boolean isHostAvailable(String hostName) {
//+++ socket is returning true even server is not reachable
//    try (Socket socket = new Socket()) {
//      InetAddress inetAddr = InetAddress.getByName(hostName);
//      InetSocketAddress socketAddress = new InetSocketAddress(inetAddr, 80);
//      socket.connect(socketAddress, 1000);
//      //check but do nothing else
//      socket.close();
//      return true;
//    } catch (UnknownHostException | SocketTimeoutException ex) {
//      Logger.getLogger(SessionControl.class.getName()).log(Level.SEVERE, null, ex.getMessage());
//    } catch (IOException ex) {
//      Logger.getLogger(SessionControl.class.getName()).log(Level.SEVERE, null, ex.getMessage());
//    }
    Runtime runtime = Runtime.getRuntime();
    Process proc;
    try {
      proc = runtime.exec("ping -c 1 " + hostName);
      int mPingResult = proc.waitFor();
      if (mPingResult == 0) {
        return true;
      }
    } catch (IOException | InterruptedException ex) {
      Logger.getLogger(SessionControl.class.getName()).log(Level.SEVERE, null, ex);
    }
    return false;
  }

  public String getTest() {
    return "test";
  }

  private void addMessage(FacesMessage message) {
    FacesContext.getCurrentInstance().addMessage(null, message);
  }
}
