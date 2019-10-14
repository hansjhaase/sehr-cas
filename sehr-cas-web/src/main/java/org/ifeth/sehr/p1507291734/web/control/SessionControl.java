/*
 * (C) MDI GmbH
 */
package org.ifeth.sehr.p1507291734.web.control;

import java.io.IOException;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;
import org.ifeth.sehr.core.objects.UserSessionObject;
import org.ifeth.sehr.intrasec.entities.LcMain;
import org.ifeth.sehr.intrasec.entities.NetCenter;
import org.ifeth.sehr.intrasec.entities.NetZones;
import org.ifeth.sehr.intrasec.entities.UsrMain;
import org.ifeth.sehr.p1507291734.ejb.AccessControl;
import org.ifeth.sehr.p1507291734.ejb.ZoneAdmin;
import org.ifeth.sehr.p1507291734.web.Constants;
import org.ifeth.sehr.p1507291734.web.listener.SAFQueueListener;
import org.ifeth.sehr.p1507291734.web.utils.MonitoringUtils;
import org.primefaces.push.EventBus;
import org.primefaces.push.EventBusFactory;

/**
 * WEB/Mobile HTML5 browser session conrol.
 *
 * @author hansjhaase
 */
@Named(value = "sessionControl")
@SessionScoped
public class SessionControl implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");

  @EJB(beanName = "AccessControl")
  private AccessControl accessControl;
  @EJB(beanName = "ZoneAdmin")
  private ZoneAdmin ejbZoneAdmin;
  @Inject
  private ModuleControl moduleCtrl;

  private String username = "";
  private String password = "";
  private String greeting = "Hello Admin!";
  private String page = "/inc/welcome_en.xhtml";
  private String zoneTitle = "Gruppe n/a";
  private boolean sessMobile = false;
  private NetZones netZones;
  private NetCenter netCenter;
  private UsrMain usrMain; //the current user object we are using
  private LcMain lcMain; //the current LifeCARD holder record we are using
  private List<NetZones> lMonitoredZones = new ArrayList<>();

  private final Map<NetZones, Integer> statusNetZones = new HashMap<>();
  private EventBus pfEventBus; //the HTML client socket EventBus

  /**
   * Creates a new instance of SessionControl.
   */
  public SessionControl() {
    pfEventBus = EventBusFactory.getDefault().eventBus();
  }

  public NetZones getZoneObject() {
    return netZones;
  }

  public void setZoneObject(NetZones object) {
    Log.finer(SessionControl.class.getName() + ":setZoneObject():" + object);
    this.netZones = object;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
    this.greeting = "Hallo " + username;
  }

  public String getGreeting() {
    return this.greeting;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String login() {
    FacesContext fctx = FacesContext.getCurrentInstance();
    if (StringUtils.isBlank(this.username) || StringUtils.isBlank(this.password)) {
      fctx.addMessage("fldUsername", new FacesMessage(FacesMessage.SEVERITY_WARN, "Blank username and/or password not allowed", ""));
      return "index";
    }
    ExternalContext ectx = fctx.getExternalContext();
    ServletContext sctx = (ServletContext) ectx.getContext();
    Properties p = (Properties) sctx.getAttribute("Properties");

    HttpSession sess = (HttpSession) ectx.getSession(false);
    HttpServletRequest req = (HttpServletRequest) ectx.getRequest();
    if (Log.isLoggable(Level.FINEST)) {
      Map<String, String> headers = MonitoringUtils.getRequestHeadersInMap(req, true);
    }
    if (this.username.equals(p.getProperty("moduleOwner"))
            && this.password.equals(p.getProperty("modulePw"))) {
      Log.info(SessionControl.class.getName() + ":access():Login by module owner configuration from " + req.getRemoteAddr());
      UserSessionObject usrSessObj = new UserSessionObject();
      usrSessObj.setUsrid(0); //logged in as module admin
      usrSessObj.setSessionid(sess.getId());
      usrSessObj.setUsrInfo("A user logged in using module settings!");
      usrSessObj.setLoggedin(new Date());
      sess.setAttribute("UserSessionObject", usrSessObj);
      pfEventBus.publish("/notification", usrSessObj.getUsrInfo());
      this.page = "/inc/desktop.xhtml";
      return "intern";
    }
    //int zid=moduleCtrl.getLocalZoneID(); //moduleCtrl is null, why?
    int zid = Integer.parseInt(p.getProperty("zoneID", "0"));
    UserSessionObject usrSessObj = accessControl.login2SEHRLocation(this.username, this.password, zid, sess.getId());
    if (usrSessObj != null) {
      sess.setAttribute("UserSessionObject", usrSessObj);
      this.page = "/inc/desktop.xhtml";
      pfEventBus.publish("/notification", usrSessObj.getUsrInfo() + " logged in.");
      return "intern";
    }

    fctx.addMessage("msgLogin", new FacesMessage(FacesMessage.SEVERITY_WARN, "Loginname a/o password are wrong", ""));
    return "index";
  }

  public UserSessionObject getUserSessionObject() {
    FacesContext fctx = FacesContext.getCurrentInstance();
    ExternalContext ectx = fctx.getExternalContext();
    HttpSession sess = (HttpSession) ectx.getSession(false);
    return (UserSessionObject) sess.getAttribute("UserSessionObject");
  }

  public String logout() {
    Log.info(SessionControl.class.getName() + ":logout()");
    FacesContext fctx = FacesContext.getCurrentInstance();
    ExternalContext ectx = fctx.getExternalContext();

    HttpSession sess = (HttpSession) ectx.getSession(false);
    UserSessionObject usrSessObj = (UserSessionObject) sess.getAttribute("UserSessionObject");
    boolean success;
    if (usrSessObj != null) {
      success = accessControl.logout(usrSessObj.getUsrid());
    } else {
      success = true; //on a ModuleOwner login there is no sess object
    }

    this.page = "/inc/welcome_en.xhtml";
//    if (userSession == null) {
//      FacesContext context = FacesContext.getCurrentInstance();
//      context.addMessage("frmMySEHR", new FacesMessage(FacesMessage.SEVERITY_INFO, "Error but closing session.", ""));
//      return "index";
//    }
//    boolean success = userSession.Logout(userSession.getUsrId());
    if (!success) {
      //"frmMySEHR"
      fctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Logout error but closing session.", ""));
    }
    return "index";
  }

  public String getLastlogin() {
    String lastlogin = "n/a";
//    if (userSession != null) {
//      Date d = userSession.getUserSessionObject().getLastloggedin();
//      lastlogin = (d != null ? sdfDE.format(d) : "");
//    }
    return lastlogin;
  }

  /**
   * @return the page
   */
  public String getPage() {
    return page;
  }

  /**
   * @param page the page to set
   */
  public void setPage(String page) {
    this.page = page;
  }

  /**
   * @param page the page to set
   * @return
   */
  public String viewPage(String page) {
    this.page = page;
    return "intern";
  }
// public void onTabChange(TabChangeEvent event) {
//    FacesMessage msg = new FacesMessage("Wechsel", "" + event.getTab().getTitle());
//    FacesContext.getCurrentInstance().addMessage(null, msg);
//    if(event.getTab().getId().equalsIgnoreCase("tabJMedOnline")){
//      this.page = "/inc/intro_de";
//    }else if (event.getTab().getId().equalsIgnoreCase("tabService")){
//      this.page = "/inc/service_de";
//    }else{
//      this.page = "/inc/intro_de";
//    }
//  }
//
//  public void onTabClose(TabCloseEvent event) {
//    //FacesMessage msg = new FacesMessage("Tab Closed", "Closed tab: " + event.getTab().getTitle());
//    //FacesContext.getCurrentInstance().addMessage(null, msg);
//  }

  /**
   * @return the centerName
   */
  public String getZoneTitle() {
    return zoneTitle;
  }

  /**
   * @param zoneTitle the title to show
   */
  public void setZoneTitle(String zoneTitle) {
    this.zoneTitle = zoneTitle;
  }

  /**
   * @return the isMobile
   */
  public boolean isSessMobile() {
    return sessMobile;
  }

  /**
   * @param isMobile the isMobile to set
   */
  public void setSessMobile(boolean isMobile) {
    this.sessMobile = isMobile;
  }

  /**
   * @return the netCenter
   */
  public NetCenter getNetCenter() {
    return netCenter;
  }

  /**
   * @param netCenter the netCenter to set
   */
  public void setNetCenter(NetCenter netCenter) {
    this.netCenter = netCenter;
  }

  /**
   * @return the usrMain
   */
  public UsrMain getUsrMain() {
    return usrMain;
  }

  /**
   * @param usrMain the usrMain to set
   */
  public void setUsrMain(UsrMain usrMain) {
    this.usrMain = usrMain;
  }

  /**
   * @return the lcMain
   */
  public LcMain getLcMain() {
    return lcMain;
  }

  /**
   * @param lcMain the lcMain to set
   */
  public void setLcMain(LcMain lcMain) {
    this.lcMain = lcMain;
  }

  public boolean isInternetAvailable() {
    return isHostAvailable("google.com", 80) || isHostAvailable("paypal.com", 80);
    // || isHostAvailable("de.e-hn.org");
  }

  private boolean isHostAvailable(String hostName, int port) {
    try (Socket socket = new Socket()) {
      InetSocketAddress socketAddress = new InetSocketAddress(hostName, port);
      socket.connect(socketAddress, 3000);
      return true;
    } catch (UnknownHostException | SocketTimeoutException ex) {
      Logger.getLogger(SessionControl.class.getName()).log(Level.SEVERE, null, ex.getMessage());
    } catch (IOException ex) {
      Logger.getLogger(SessionControl.class.getName()).log(Level.SEVERE, null, ex.getMessage());
    }
    return false;
  }

  /**
   * @return the statusNetZones
   */
  public Map<NetZones, Integer> statusNetZones() {
    if (statusNetZones.isEmpty()) {
      List<NetZones> list = ejbZoneAdmin.listActiveZones();
      for (NetZones z : list) {
        //sehr-cas is currently a pure GF app on port 8080
        int val = 0;
        if (isHostAvailable(z.getPriip(), 8080)) {
          val = val | Constants.maskIsURLSEHRWeb;
        }
        //TODO check and add LDAP status using mask
        Log.finer(SessionControl.class.getName() + ":statusNetZones():val=" + Integer.toBinaryString(val));
        statusNetZones.put(z, val);
      }
    }
    return statusNetZones;
  }

  /**
   * Get list of monitored/controlled zones by this host.
   *
   * @param refresh update the contents / list
   * @return
   */
  public List<NetZones> activeMonitoredZones(boolean refresh) {

    FacesContext fctx = FacesContext.getCurrentInstance();
    ExternalContext ectx = fctx.getExternalContext();
    if (lMonitoredZones.isEmpty() || refresh == true) {
      lMonitoredZones = new ArrayList<>();
      ServletContext ctx = (ServletContext) ectx.getContext();
      Map<String, SAFQueueListener> mMonitor = (HashMap) ctx.getAttribute("SAFQueueListener");
      if (!mMonitor.isEmpty()) {
        for (String queue : mMonitor.keySet()) {
          SAFQueueListener queuelistener = mMonitor.get(queue);
          if (queuelistener.isSession()) {
            String z = queue.substring(5, 12); //get zoneid part
            //TODO check if registered in DB
            NetZones zone = new NetZones();
            zone.setHostid(-1);
            zone.setZoneid(Integer.parseInt(z));
            zone.setTitle(queue);
            lMonitoredZones.add(zone);
          } else {
            Log.info(SessionControl.class.getName() + ":activeMonitoredZones():Registered to be monitored but no session for " + queue);
          }
        }
      } else {
        Log.info(SessionControl.class.getName() + ":activeMonitoredZones():No monitored zones (no 'SAFQueueListener').");

      }
    }
    return lMonitoredZones;
  }
}
