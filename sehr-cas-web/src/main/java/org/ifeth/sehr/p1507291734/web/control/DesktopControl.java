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
 * Refactored from 'sehr-saf' on 3.08.2015
 */
package org.ifeth.sehr.p1507291734.web.control;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
//import javax.faces.bean.ManagedBean;
//import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.apache.camel.Route;
import org.apache.camel.ServiceStatus;
import org.apache.camel.model.RouteDefinition;
import org.apache.commons.lang3.StringUtils;
import org.ifeth.sehr.intrasec.entities.NetCenter;
import org.ifeth.sehr.p1507291734.ejb.CenterAdmin;
import org.ifeth.sehr.p1507291734.web.MessagingManager;
import org.ifeth.sehr.p1507291734.web.listener.XNetMessaging;
import org.ifeth.sehr.p1507291734.web.model.XNetMessagingHandler;

import org.primefaces.event.CloseEvent;
import org.primefaces.event.DashboardReorderEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleEvent;
import org.primefaces.model.DashboardColumn;
import org.primefaces.model.DashboardModel;
import org.primefaces.model.DefaultDashboardColumn;
import org.primefaces.model.DefaultDashboardModel;
import org.primefaces.push.EventBus;
import org.primefaces.push.EventBusFactory;
import org.primefaces.util.Base64;

@Named(value = "desktopCtrl")
@RequestScoped
public class DesktopControl implements Serializable, Converter {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");

  @Inject
  private ModuleControl moduleCtrl;
  @EJB
  private CenterAdmin centerAdmin;

  private DashboardModel model;
  private String routeId;
  private String controlCommand;
  private Route route;
  private Integer centerId = 601311; //default for testings
  private List<NetCenter> l;
  private EventBus pfEventBus; //the HTML client socket EventBus

  @PostConstruct
  public void init() {
    pfEventBus = EventBusFactory.getDefault().eventBus();
    model = new DefaultDashboardModel();
    DashboardColumn column1 = new DefaultDashboardColumn();
    DashboardColumn column2 = new DefaultDashboardColumn();
    //DashboardColumn column3 = new DefaultDashboardColumn();

    column1.addWidget("zones");
    column1.addWidget("centers");
    column1.addWidget("users");

    column2.addWidget("status");
    column2.addWidget("apps");
    column2.addWidget("notes");

    model.addColumn(column1);
    model.addColumn(column2);
    l = centerAdmin.listCentersByZoneId(moduleCtrl.getLocalZoneID());

  }

  public void handleReorder(DashboardReorderEvent event) {
    FacesMessage message = new FacesMessage();
    message.setSeverity(FacesMessage.SEVERITY_INFO);
    message.setSummary("Reordered: " + event.getWidgetId());
    message.setDetail("Item index: " + event.getItemIndex() + ", Column index: " + event.getColumnIndex() + ", Sender index: " + event.getSenderColumnIndex());
    addMessage(message);

    pfEventBus.publish("/notification", event.getWidgetId() + " reordered but not saved. ");
  }

  public void handleClose(CloseEvent event) {
    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Panel Closed", "Closed panel id:'" + event.getComponent().getId() + "'");
    addMessage(message);
  }

  public void handleToggle(ToggleEvent event) {
    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, event.getComponent().getId() + " toggled", "Status:" + event.getVisibility().name());
    addMessage(message);
  }

  private void addMessage(FacesMessage message) {
    FacesContext.getCurrentInstance().addMessage(null, message);
  }

  public DashboardModel getModel() {
    return model;
  }

  public void alActivateLocalServiceQueue(ActionEvent evt) {
    FacesContext fctx = FacesContext.getCurrentInstance();
    ExternalContext ectx = fctx.getExternalContext();
    ServletContext sctx = (ServletContext) ectx.getContext();
    MessagingManager jmsMan = MessagingManager.getInstance(sctx);
    if(!jmsMan.isConfigured()){
      jmsMan.configure(moduleCtrl.getProperties());
    }
    //show message to start AMQ server!
    jmsMan.addServiceListener(moduleCtrl.getLocalZoneIDAsString());    
  }

  public List<String> completeCID(String query) {
    List<String> results = new ArrayList<>();
    //TODO get center id  from database
    List<NetCenter> l = centerAdmin.listCentersByZoneId(moduleCtrl.getLocalZoneID());
    for (NetCenter nc : l) {
      String cid = String.format("%07d", nc.getNetCenterPK().getCenterid());
      if (cid.contains(query)) {
        results.add(cid);
      }
    }
    return results;
  }

  public List<Route> completeRoutes(String query) {
    List<Route> result = new ArrayList<>();
    FacesContext fctx = FacesContext.getCurrentInstance();
    ExternalContext ectx = fctx.getExternalContext();
    ServletContext sctx = (ServletContext) ectx.getContext();
    XNetMessaging xnetMessenger = (XNetMessaging) sctx.getAttribute("XNetMessaging");
    if (xnetMessenger != null) {
      //XNetMessagingHandler msgHandler = new XNetMessagingHandler(xnetMessenger);
      //List<String> routes = xnetMessenger.getRouteNameStatus();
      List<Route> routes = xnetMessenger.getRoutes();
      for (Route r : routes) {
        if (r.getId().contains(query)) {
          result.add(r);
        }
      }
    }
    return result;
  }

  public String controlCommand() {
    FacesMessage message;
    FacesContext fctx = FacesContext.getCurrentInstance();
    ExternalContext ectx = fctx.getExternalContext();
    ServletContext sctx = (ServletContext) ectx.getContext();
    XNetMessaging xnetMessenger = (XNetMessaging) sctx.getAttribute("XNetMessaging");
    if (xnetMessenger != null && this.route != null) {
      XNetMessagingHandler msgHandler = new XNetMessagingHandler(xnetMessenger);
      //String toDomain = xnetMessenger.getXNetDomain();
      String result = msgHandler.sendMonitorCommand(this.route.getId(), this.controlCommand);
      message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Control Command", result);
    } else {
      message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Control Command", "NOT sent (no route or no XNET context)");
    }
    addMessage(message);
    return null;
  }

  public void handleSelect(SelectEvent e) {
    Log.info(DesktopControl.class.getName() + ":handleSelect():e=" + e.toString());
    Object o = e.getObject();
    if (o != null && o instanceof Route) {
      this.route = (Route) o;
      FacesContext.getCurrentInstance().addMessage(null,
              new FacesMessage(FacesMessage.SEVERITY_INFO,
                      "Route selected :: Route ID :: " + this.route.getId(), ""));
    }
  }

  public void doSelect(Route r) {
    Log.fine(DesktopControl.class.getName() + ":doSelect():r=" + r);
    this.route = r;
    if (this.route != null) {
      FacesContext.getCurrentInstance().addMessage(null,
              new FacesMessage(FacesMessage.SEVERITY_INFO,
                      "Route selected :: Route ID :: " + this.route.getId(), ""));
    }
  }

  public String testXNetInbox() {
    FacesMessage message;
    FacesContext fctx = FacesContext.getCurrentInstance();
    ExternalContext ectx = fctx.getExternalContext();
    ServletContext sctx = (ServletContext) ectx.getContext();

    HttpServletRequest request = (HttpServletRequest) ectx.getRequest();
    String fldTestXnetMsg2CenterId = request.getParameter("frmDesktop:fldTestXnetMsg2CenterId_input");
    if (fldTestXnetMsg2CenterId == null) {
      message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Test message", "Center ID required!");
      addMessage(message);
      return null;
    }
    int localZID = moduleCtrl.getLocalZoneID();
    this.centerId = Integer.parseInt(fldTestXnetMsg2CenterId);
    boolean bFound = false;
    for (NetCenter nc : l) {
      if (this.centerId.equals(nc.getNetCenterPK().getCenterid())) {
        bFound = true;
        break;
      }
    }
    if (!bFound) {
      message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Test message", "Center ID not found in zone #" + localZID + " !");
      addMessage(message);
      return null;
    }
    XNetMessaging xnetMessenger = (XNetMessaging) sctx.getAttribute("XNetMessaging");
    if (xnetMessenger != null) {
      XNetMessagingHandler msgHandler = new XNetMessagingHandler(xnetMessenger);
      String toDomain = xnetMessenger.getXNetLocalDomain();
      String result = msgHandler.sendTestMessage(toDomain, localZID, this.centerId, "Testing Message Route", "This message has been sent to '" + toDomain + "' using XNET outgoing bus '" + xnetMessenger.getXNetDomainBus() + "'.\nThe receiver (destination) by header is center #" + this.centerId + " at zone #" + localZID);
      //N.B. The XNET routing service must be started on the host processing the SEHR XNET country bus....
      message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Test message", result);
    } else {
      message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Test message", "NOT sent (no XNET Context)");
    }
    addMessage(message);
    return null;
  }

  /**
   * @return the routeId
   */
  public String getRouteId() {
    return routeId;
  }

  /**
   * @param routeId the routeId to set
   */
  public void setRouteId(String routeId) {
    this.routeId = routeId;
  }

  /**
   * @return the controlCommand
   */
  public String getControlCommand() {
    return controlCommand;
  }

  /**
   * @param controlCommand the controlCommand to set
   */
  public void setControlCommand(String controlCommand) {
    this.controlCommand = controlCommand;
  }

  @Override
  public Object getAsObject(FacesContext facesContext, UIComponent uiComponent, String value) {
    Log.info(DesktopControl.class.getName() + ":getAsObject():value=" + value);
    if (StringUtils.isBlank(value)) {
      return null;
    }
    try {
      byte[] bRouteId = Base64.decode(value);
      ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bRouteId));
      Object o = ois.readObject();
      ois.close();
      //--- get Route object
      FacesContext fctx = FacesContext.getCurrentInstance();
      ExternalContext ectx = fctx.getExternalContext();
      ServletContext sctx = (ServletContext) ectx.getContext();
      XNetMessaging xnetMessenger = (XNetMessaging) sctx.getAttribute("XNetMessaging");
      Route r = xnetMessenger.getRoute((String) o);
      return r;
    } catch (IOException | ClassNotFoundException e) {
      Log.log(Level.SEVERE, "Unable to decode " + value, e);
      return null;
    }
  }

  @Override
  public String getAsString(FacesContext facesContext, UIComponent uiComponent, Object value) {
    Log.info(DesktopControl.class.getName() + ":getAsString():value=" + (value != null ? value.toString() : "is null"));
    if (value == null) {
      return "";
    }
    if (value instanceof Route) {
      String id = ((Route) value).getId();
      try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(id);
        oos.close();
        return Base64.encodeToString(baos.toByteArray(), false);
      } catch (IOException e) {
        Log.log(Level.SEVERE, "Unable to encode " + value.toString(), e);
        return "";
      }
    } else {
      Log.log(Level.SEVERE, "Unable to convert " + value.toString());
      return "";
    }
  }

  /**
   * @return the route
   */
  public Route getRoute() {
    return route;
  }

  /**
   * @param route the route to set
   */
  public void setRoute(Route route) {
    this.route = route;
  }

  public String getRouteDetails(Route r) {
    String s = "n/a";
    FacesContext fctx = FacesContext.getCurrentInstance();
    ExternalContext ectx = fctx.getExternalContext();
    ServletContext sctx = (ServletContext) ectx.getContext();
    XNetMessaging xnetMessenger = (XNetMessaging) sctx.getAttribute("XNetMessaging");
    if (xnetMessenger != null && r != null) {
      s = xnetMessenger.getRouteInfo(r, true);
    }
    return s;
  }

  public String getRouteInfo(Route r) {
    FacesContext fctx = FacesContext.getCurrentInstance();
    ExternalContext ectx = fctx.getExternalContext();
    ServletContext sctx = (ServletContext) ectx.getContext();
    String s = "n/a";
    XNetMessaging xnetMessenger = (XNetMessaging) sctx.getAttribute("XNetMessaging");
    if (xnetMessenger != null && r != null) {
      RouteDefinition rDef = r.getRouteContext().getRoute();
      s = rDef.getId();//.getShortName(); is only 'route'
      ServiceStatus rSts = rDef.getStatus(xnetMessenger.getEISContext());
      s += " (" + rSts.toString() + ")";
    }
    return s;
  }

  /**
   * @return the centerId
   */
  public Integer getCenterId() {
    return centerId;
  }

  /**
   * @param centerId the centerId to set
   */
  public void setCenterId(Integer centerId) {
    this.centerId = centerId;
  }
}
