/*
 * (C) 2015 MDI GmbH
 *
 * Based on an idea of Hans J Haase
 * Development Team: Hans J.
 */
package org.ifeth.sehr.p1507291734.web.control;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
//import javax.faces.bean.SessionScoped;
import javax.inject.Named;
//import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.activemq.ActiveMQConnection;
import org.apache.commons.lang3.StringUtils;
import org.ifeth.sehr.core.objects.UserSessionObject;
import org.ifeth.sehr.intrasec.entities.UsrMain;
import org.ifeth.sehr.p1507291734.ejb.AccessControl;
import org.ifeth.sehr.p1507291734.web.MessagingManager;
import org.ifeth.sehr.p1507291734.web.beans.ChatMsg;
import org.ifeth.sehr.p1507291734.web.listener.SEHRMessagingListener;
import org.ifeth.sehr.p1507291734.web.utils.MultiEventProcessor;
import org.primefaces.push.EventBusFactory;

/**
 * Global application control for mobiles.
 *
 * @author HansJ (hansjhaase@mdigmbh.de)
 */
@Named(value = "mobileControl")
@SessionScoped
public class MobileControl implements Serializable {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");
  private static final long serialVersionUID = 1L;

  @EJB(beanName = "AccessControl")
  private AccessControl accessControl;

  @Inject
  private ModuleControl moduleCtrl;

  @Inject
  private SessionControl sessionCtrl;

  private String username;
  private String password;
  private UsrMain loggedInUser;
  private String sehrxnetzoneurl;
  private String viewPage = "vwIntro"; //default on start
  private String xhtmlPage = "mobile/login.xhtml"; //default on start
  private EventBus eventBus;
  private MultiEventProcessor evtProcessor;
  //private volatile int cntNewMsg=0; //new messages arrived during session
  private int cntNewMsg = 0; //new messages arrived during session
  private org.primefaces.push.EventBus pfEventBus;

  //============================================= constructors, initialization
  /**
   * Manages the mobile (HTML) app.
   */
  public MobileControl() {

  }

  //============================================= getter/setter
  /**
   * @return the username
   */
  public String getUsername() {
    Log.finest(MobileControl.class.getName() + ":getUsername():Username=" + username);
    return username;
  }

  /**
   * @param username the username to set
   */
  public void setUsername(String username) {
    Log.finest(MobileControl.class.getName() + ":setUsername():username=" + username);
    this.username = username;
  }

  /**
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * @param password the password to set
   */
  public void setPassword(String password) {
    Log.finest(MobileControl.class.getName() + ":setPassword():password=" + password);
    this.password = password;
  }

  //============================================= methods of actions etc.
  public void doChangeUserPassword() {
    Log.log(Level.FINEST, "{0}:doChangeUserPassword():username={1}", new Object[]{MobileControl.class.getName(), this.password});
    //TODO
    FacesContext fctx = FacesContext.getCurrentInstance();
    fctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "NYI", "Not yet implemented."));

  }

  public String initMobile() {
    Log.log(Level.INFO, "{0}:initMobile()", new Object[]{MobileControl.class.getName()});
    //do redirect to change URL, not a forward only
    xhtmlPage = "mobile/login?faces-redirect=true"; //base on init
    sessionCtrl.setPage(xhtmlPage);
    return xhtmlPage;
  }

  public String gotoPage(String page, String view) {
    FacesContext fctx = FacesContext.getCurrentInstance();
    ExternalContext ectx = fctx.getExternalContext();
    HttpServletRequest req = (HttpServletRequest) ectx.getRequest();
    Log.log(Level.FINEST, "{0}:gotoPage():ContextPath={1}", new Object[]{MobileControl.class.getName(), req.getContextPath()}); //returns '/sehr-cas-web'
    sessionCtrl.setPage(page);
    if (!StringUtils.isBlank(view)) {
      //JMobile ui-page is PF view
      page += "&ui-page=" + view;
    }
    return page;
  }

  public String doLogin() {
    FacesContext fctx = FacesContext.getCurrentInstance();
    if (StringUtils.isBlank(this.username) || StringUtils.isBlank(this.password)) {
      Log.info(MobileControl.class.getName() + ":doLogin():username (" + this.username + ")or password is null" + this.password);
      //FIX ME this throws an error on jquery mobile
      //fctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Fehler", "Username/Password missing"));
      return "/mobile/login";
    }
    sessionCtrl.setSessMobile(true);
    //reset page handling
    xhtmlPage = "mobile/login?faces-redirect=true"; //base on login stage
    sessionCtrl.setPage(xhtmlPage);
    boolean isAccess = false;
    ExternalContext ectx = fctx.getExternalContext();
    ServletContext ctx = (ServletContext) ectx.getContext();
    Map<String, String> httpRequestInfo = getHeadersInfo((HttpServletRequest) ectx.getRequest());
    Properties p = (Properties) ctx.getAttribute("Properties");
    HttpSession sess = (HttpSession) fctx.getExternalContext().getSession(false);
    Log.info(MobileControl.class.getName() + ":doLogin():" + this.username + "(" + this.password + ")...");
    //login as module owner
    if (this.username.equals(p.getProperty("moduleOwner"))
            && this.password.equals(p.getProperty("modulePw"))) {
      this.loggedInUser = new UsrMain();
      this.loggedInUser.setUsrname(this.username);
      this.loggedInUser.setUsrid(0);//Module Owner...
      this.loggedInUser.setStatus(0); //valid; required by UsrSessObj
      UserSessionObject usrSessObj = new UserSessionObject();
      usrSessObj.setUsrid(0); //logged in as module admin
      usrSessObj.setSessionid(sess.getId());
      usrSessObj.setLoggedin(new Date());
      usrSessObj.setUsrMain(this.loggedInUser);
      sess.setAttribute("UserSessionObject", usrSessObj);
      isAccess = true;
    } else {
      int zid = moduleCtrl.getLocalZoneID();
      UserSessionObject usrSessObj = accessControl.login2SEHRLocation(this.username, this.password, zid, sess.getId());
      if (usrSessObj != null) {
        sess.setAttribute("UserSessionObject", usrSessObj);
        this.loggedInUser = usrSessObj.getUsrMain();
        //check ACL for rights on SEHR mobile app
        isAccess = true;
      }
    }
    if (isAccess) {
      //internal application bus, not for PF/HTML5
      //this.eventBus = new EventBus();
      this.eventBus = (EventBus) ctx.getAttribute("EventBus");
      //push messaging bus (PF/HTML5)
      this.pfEventBus = EventBusFactory.getDefault().eventBus();
      this.evtProcessor = new MultiEventProcessor(moduleCtrl.getLocalZoneID());
      this.evtProcessor.setWEBSocketProcessor(this.pfEventBus);
      this.eventBus.register(this.evtProcessor);

      //ActiveMQ present and connected?
      //MessagingManager msgManager = MessagingManager.getInstance(ctx);
      //ActiveMQConnection amqCon = (ActiveMQConnection) msgManager.getConnection();
      ActiveMQConnection amqCon = (ActiveMQConnection) ctx.getAttribute("ActiveMQConnection");
      if (amqCon != null && !amqCon.isClosed()) {
        SEHRMessagingListener chatHdl = new SEHRMessagingListener(this.eventBus, sess.getId(), amqCon);
        //ChatHandler uses an already established connection!
        javax.jms.Session chatSession = chatHdl.createSession(moduleCtrl.getLocalZoneID());
        if (chatSession != null) {
          sess.setAttribute("ChatHandler", chatHdl);
          //messenger.joinRoom("public", rs.getString(4) + "@" + p.getProperty("centerID"));
          //String nick = prsMain.getVorname() != null ? prsMain.getVorname() : prsMain.getInfo();
          String nick = this.username;
          chatHdl.joinRoom("public", nick + "@" + moduleCtrl.getLocalZoneIDAsString());
          Log.fine(MobileControl.class.getName() + ":doLogin():Chat session started. Messenger Object=" + chatHdl.toString());
        }
      }
      Log.info(MobileControl.class.getName() + ":doLogin():Login in by " + this.username + " from " + httpRequestInfo.get("from"));
      viewPage = "vwIntro";
      sessionCtrl.setPage("/mobile/index"); //current from now
      //return sessionCtrl.getPage() + "?ui-page=" + viewPage + "&faces-redirect=true";
      //viewPage is processed by script '$(document).ready...' 
      return sessionCtrl.getPage() + "?faces-redirect=true";
    }
    //fctx.addMessage("msgMobileLogin", new FacesMessage(FacesMessage.SEVERITY_WARN, "Fehler", "Loginname u/o Kennwort falsch"));
    //fctx.addMessage("msgLogin", new FacesMessage(FacesMessage.SEVERITY_WARN, "Fehler", "Loginname u/o Kennwort falsch"));
    //fctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Fehler", "Loginname u/o Kennwort falsch"));
    return sessionCtrl.getPage(); //login...
  }

  public String checkLoginStatus() {
    Log.fine(MobileControl.class.getName() + ":checkLoginStatus()");
    Log.finest(MobileControl.class.getName() + ":checkLoginStatus():username=" + username);
    UserSessionObject uso = sessionCtrl.getUserSessionObject();
    //implement uso.getStatus()!=3 ;valid/payed - see UsrMain
    if (uso == null || sessionCtrl.isSessMobile() == false) {
      return "/mobile/login?faces-redirect=true";
    }
    //continue with last page and view that has been used...
    return sessionCtrl.getPage() + "?ui-page=" + viewPage;
  }

  /**
   * Logout ActionListener for menuitem to work properly.
   * <p>
   * For navigation purposes to another xhtml we do currently need this kind of
   * implementation.
   * </p>
   * <pre>
   *      <p:menuitem value="Logout" actionListener="#{mobileControl.logout}"
   *                       oncomplete="$.mobile.changePage('./login.xhtml');"/>
   * </pre>
   *
   * @param event
   */
  public void logout(ActionEvent event) {
    Log.finest(MobileControl.class.getName() + ":logout():event" + event.getSource());
    Log.fine(MobileControl.class.getName() + ":logout():Logout event for user " + username);
    doLogout(); //just for invalidation of the session attributes...
  }

  public String doLogout() {
    Log.info(MobileControl.class.getName() + ":doLogout():User " + username);
    xhtmlPage = "mobile/login?faces-redirect=true"; //use login stage
    sessionCtrl.setPage(xhtmlPage);
    FacesContext fctx = FacesContext.getCurrentInstance();
    HttpSession sess = (HttpSession) fctx.getExternalContext().getSession(false);
    SEHRMessagingListener chatHdl = (SEHRMessagingListener) sess.getAttribute("ChatHandler");
    if (chatHdl != null) {
      chatHdl.closeSession();
      sess.removeAttribute("ChatHandler");
    }
    if (this.eventBus != null && this.evtProcessor != null) {
      //unregister mobile and session based event processor
      this.eventBus.unregister(this.evtProcessor);
    }
    this.loggedInUser = null;

    this.username = null;
    this.password = null;
    sess.removeAttribute("UserSessionObject");
    //sess.invalidate();
    Log.info(MobileControl.class.getName() + ":logout():navigation to " + sessionCtrl.getPage());
    return sessionCtrl.getPage();
  }

  public boolean isAMQConnected() {
    ServletContext ctx = (ServletContext) FacesContext
            .getCurrentInstance().getExternalContext().getContext();

    ActiveMQConnection amqCon = (ActiveMQConnection) ctx.getAttribute("ActiveMQConnection");
    boolean status = (amqCon != null && !amqCon.isClosed());// ? true : false;
    return status;
  }

  public String doReconnect() {
    if (!isAMQConnected()) {
      Log.info(MobileControl.class.getName() + ":doReconnect()");

      ServletContext sctx = (ServletContext) FacesContext
              .getCurrentInstance().getExternalContext().getContext();
      MessagingManager msgMan = MessagingManager.getInstance(sctx);

      if (!msgMan.configure(
              (Properties) sctx.getAttribute("Properties"))) {
        Log.info(MobileControl.class.getName() + ":doReconnect():Error reconfiguring AMQ broker!");
        return "pm:vwSettings";
      } else {
        HashMap<String, String> pMap = (HashMap) sctx.getAttribute("ZoneAdv");
        for (final Map.Entry<String, String> entry : pMap.entrySet()) {
          Log.info(MobileControl.class.getName() + ":doReconnect():service listener for zone ID " + entry.getKey());
          msgMan.addServiceListener(entry.getKey());
        }
      }
    }
    return "pm:vwIntro";
  }

  /**
   * Get the URL of the messaging broker for zone / community messages.
   *
   * @return the sehrxnetzoneurl
   */
  public String getAMQUrl() {
    sehrxnetzoneurl = (String) moduleCtrl.getProperty("sehrxnetzoneurl");
    return sehrxnetzoneurl != null ? sehrxnetzoneurl : "n/a";
  }

  /**
   * Set the the of the local AMQ service/broker.
   *
   * @param url
   */
  public void setAMQUrl(String url) {
    this.sehrxnetzoneurl = url;
  }

  //get request headers
  private Map<String, String> getHeadersInfo(HttpServletRequest request) {

    Map<String, String> map = new HashMap<>();

    Enumeration headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String key = (String) headerNames.nextElement();
      String value = request.getHeader(key);
      Log.finest(key + ":" + value);
      map.put(key, value);
    }

    return map;
  }

  /**
   * @return the viewPage
   */
  public String getViewPage() {
    Log.info(MobileControl.class.getName() + ":getViewPage():" + viewPage);
    return viewPage;
  }

  /**
   * @param viewPage the viewPage to set
   */
  public void setViewPage(String viewPage) {
    Log.info(MobileControl.class.getName() + ":setViewPage():viewPage=" + viewPage);

    this.viewPage = viewPage;
  }

  public boolean isLoginMobileAllowed() {
    //TODO 2nd check against ACL table
    String s = (String) moduleCtrl.getProperty("allowAdminMOBILE");
    return (s != null && s.equals("1"));
  }

  /**
   * @return the current xhtmlPage
   */
  public String getXhtmlPage() {
    //return xhtmlPage;
    return sessionCtrl.getPage();
  }

  /**
   * @param xhtmlPage the xhtmlPage to set
   */
  public void setXhtmlPage(String xhtmlPage) {
    this.xhtmlPage = xhtmlPage;
    if (StringUtils.contains(xhtmlPage, "mobile/login")) {
      sessionCtrl.setPage("mobile/login?faces-redirect=true");
    } else {
      sessionCtrl.setPage(xhtmlPage);
    }
  }

  public List<String> getListRooms() {
    return (this.evtProcessor == null ? null : this.evtProcessor.listChatRooms());
  }

  public List<UsrMain> getListUsers() {
    return (this.evtProcessor == null ? null : this.evtProcessor.listChatUsers());
  }

  /**
   * @param room
   * @return the listChatMsg
   */
  public List<ChatMsg> getListChatMsg(String room) {
    if (this.evtProcessor == null || room == null) {
      return null;
    }

    if (room.equalsIgnoreCase("public")) {
      return this.evtProcessor.listChatMsgPublic();
    }
    return this.evtProcessor.listChatMsgOfRoom(room);
  }

  public String getCountMessages() {
    String s = "You have ";
    int i = 0;
    if (this.evtProcessor != null) {
      if (this.evtProcessor.listChatMsgPublic() == null || this.evtProcessor.listChatMsgPublic().isEmpty()) {
        s += "no public ";
      } else {
        i += this.evtProcessor.listChatMsgPublic().size();
        s += this.evtProcessor.listChatMsgPublic().size() + " public ";
      }
      if (this.loggedInUser != null) {
        String userRoom = String.format("%08d", this.loggedInUser.getUsrid());
        if (this.evtProcessor.listChatMsgOfRoom(userRoom) == null
                || this.evtProcessor.listChatMsgOfRoom(userRoom).isEmpty()) {
          s += "and no private ";
        } else {
          i += this.evtProcessor.listChatMsgOfRoom(this.username).size();
          s += this.evtProcessor.listChatMsgOfRoom(this.username).size() + " private ";
        }
      }
      this.cntNewMsg = i;
    }
    return s;
  }

  public int getCount() {
    return cntNewMsg;
  }

  public void setCount(int count) {
    this.cntNewMsg = count;
  }

  public void increment() {
    ChatMsg test = new ChatMsg(null, null);
    this.evtProcessor.listChatMsgPublic().add(test);
    this.cntNewMsg = this.evtProcessor.listChatMsgPublic().size();
    //org.primefaces.push.EventBus pfEventBus = EventBusFactory.getDefault().eventBus();
    this.pfEventBus.publish("/counter", String.valueOf(this.cntNewMsg));
  }
}
