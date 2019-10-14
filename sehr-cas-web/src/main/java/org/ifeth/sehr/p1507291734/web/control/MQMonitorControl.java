/*
 * (C) 2015 MDI GmbH
 *
 * Based on an idea of Hans J Haase
 * Development Team: Hans J.
 */
package org.ifeth.sehr.p1507291734.web.control;

import java.io.Serializable;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
//import javax.faces.bean.SessionScoped;
import javax.inject.Named;
//import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang.StringUtils;
import org.ifeth.sehr.core.exception.GenericSEHRException;
import org.ifeth.sehr.p1507291734.web.MessagingManager;
import org.ifeth.sehr.p1507291734.web.beans.MQClients;
import org.ifeth.sehr.p1507291734.web.listener.AdvConnectionListener;

/**
 * Global application control for mobiles.
 *
 * @author HansJ (hansjhaase@mdigmbh.de)
 */
@Named(value = "mqControl")
@RequestScoped
public class MQMonitorControl implements Serializable {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");
  private static final long serialVersionUID = 1L;
  @Inject
  private MobileControl mobileCtrl;
  private MQClients selectedMQClient;

  //============================================= constructors, initialization
  /**
   * Manages the mobile (HTML) app
   */
  public MQMonitorControl() {
  }

  //============================================= getter/setter
  //============================================= methods of actions etc.
//  public String gotoPage(String page, String anchor) {
//    FacesContext fctx = FacesContext.getCurrentInstance();
//    ExternalContext ectx = fctx.getExternalContext();
//    HttpServletRequest req = (HttpServletRequest) ectx.getRequest();
//    Log.log(Level.FINEST, "{0}:gotoPage():ContextPath={1}", new Object[]{MQMonitorControl.class.getName(), req.getContextPath()});
//    return page;//+".xhtml";
//  }
  public String checkStatus() {
    Log.info(MQMonitorControl.class.getName() + ":checkStatus():Username=" + mobileCtrl.getUsername());
    if (mobileCtrl.getUsername() == null || mobileCtrl.getUsername().isEmpty()) {
      return "/mobile/login";
    }
    if (!mobileCtrl.isAMQConnected()) {
      Log.warning(MQMonitorControl.class.getName() + ":checkStatus():No AMQ Connection!");
      return "/mobile/index";
    }
    ServletContext ctx = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
    AdvConnectionListener adv = (AdvConnectionListener) ctx.getAttribute(AdvConnectionListener.class.getName());
    if (adv == null) {
      MessagingManager msgManager = MessagingManager.getInstance(ctx);
      if (msgManager.isConnected()) {
        try {
          msgManager.startAdvMonitor("ConnectionAdvisory");
        } catch (GenericSEHRException ex) {
          Logger.getLogger(MQMonitorControl.class.getName()).log(Level.INFO, null, ex.getMessage());
          return "/mobile/index";
        }
      } else {
        Log.info(MQMonitorControl.class.getName() + ":checkStatus():No MessagingManager!");
        return "/mobile/index";
      }
    }
    //TODO implement switch to last page that has been visited in this use case
    return "/mobile/mqMonitor?ui-page=vwMonitor";
  }

  public String getMQBroker() {
    ServletContext ctx = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
    ActiveMQConnectionFactory amqConFac = (ActiveMQConnectionFactory) ctx.getAttribute("ActiveMQConnectionFactory");
    return (amqConFac != null ? amqConFac.getBrokerURL() : "n/a");
  }

  public String getMQConSts() {
    ServletContext ctx = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
    ActiveMQConnection amqCon = (ActiveMQConnection) ctx.getAttribute("ActiveMQConnection");
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm z");
    return (amqCon != null ? sdf.format(amqCon.getTimeCreated()) : "n/a");
  }

  public List<MQClients> listMQClients(String regexpr) {
    ServletContext ctx = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();

    AdvConnectionListener adv = (AdvConnectionListener) ctx.getAttribute(AdvConnectionListener.class.getName());
    List l = new ArrayList<>();
    if (adv == null) {
      Log.warning(MQMonitorControl.class.getName() + ":listMQClients():No Listener to get monitored data.");
      return l;
    }
    for (MQClients c : adv.getMQClients().values()) {
      String s = c.getClientId();
      if (s != null) {
        if (regexpr != null && s.matches(regexpr)) {
          l.add(c);
        } else {
          l.add(c);
        }
      }
    }
    return l;
  }

  /**
   * @return the selectedMQClient
   */
  public MQClients getSelectedMQClient() {
    return selectedMQClient;
  }

  /**
   * @param selectedMQClient the selectedMQClient to set
   */
  public void setSelectedMQClient(MQClients selectedMQClient) {
    this.selectedMQClient = selectedMQClient;
  }

  public String loadMQClient() {
    ExternalContext ectx = FacesContext.getCurrentInstance().getExternalContext();
    ServletContext ctx = (ServletContext) ectx.getContext();
    Map requestMap = ectx.getRequestParameterMap();

    String conid = (String) requestMap.get("conid");
    Log.finer(MQMonitorControl.class.getName() + ":loadMQClient():conid=" + conid);
    AdvConnectionListener adv = (AdvConnectionListener) ctx.getAttribute(AdvConnectionListener.class.getName());
    //List l = new ArrayList<>();
    if (adv == null) {
      Log.warning("loadMQClient():No Listener to get client data.");
      return "/mobile/mqMonitor?ui-page=vwMonitor";
    }
    for (MQClients c : adv.getMQClients().values()) {
      if (c.getConIdAsString().equals(conid)) {
        selectedMQClient = c;
      }
    }
    return "/mobile/mqMonitorClientInfo";
  }

  public String cvtLong2DT(Long l) {
    Log.info(MQMonitorControl.class.getName() + ":cvtLong2DT():" + l);
    if (l == null) {
      return "n/a";
    }
    Date date = new Date(l);
    Format format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    return format.format(date);
  }

  public String getSubStr(String s, int l) {
    if (StringUtils.isBlank(s)) {
      return s;
    }
    if (s.length() <= l) {
      return s;
    }
    s = s.substring(0, l);
    return s + "...";
  }
}
