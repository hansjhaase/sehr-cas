/*
 * (C) 2015 MDI GmbH
 *
 * Based on an idea of Hans J Haase
 * Development Team: Hans J.
 */
package org.ifeth.sehr.p1507291734.web.control;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
//import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.ifeth.sehr.intrasec.entities.DefModule;
import org.ifeth.sehr.intrasec.entities.NetCenter;
import org.ifeth.sehr.intrasec.entities.NetServices;
import org.ifeth.sehr.intrasec.entities.NetZones;
import org.ifeth.sehr.p1507291734.ejb.ModuleAdmin;
import org.ifeth.sehr.p1507291734.ejb.NetServiceAdmin;

/**
 * Manageging services.
 *
 * @author HansJ
 */
@Named(value = "serviceViewCtrl")
@ViewScoped
public class ServiceViewControl implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");

  @EJB
  private NetServiceAdmin serviceAdmin;

  @EJB
  private ModuleAdmin moduleAdmin;

  @Inject
  private SessionControl sessionCtrl;

  private int zoneid; //to get modules of a zone
  private int moduleid; //recid
  private DefModule curModule;
  private NetServices selNetServices;
  private List<NetServices> lstNetServices;
  private NetCenter curNetCenter;
  private NetZones curNetZones;

  //============================================= constructors, initialization
  public ServiceViewControl() {
  }

  @PostConstruct
  public void init() {
    selNetServices = new NetServices();
    lstNetServices = new ArrayList<>();
  }

  //============================================= getter/setter
  public DefModule getModuleObject() {
    return curModule;
  }

  public void setModuleObject(DefModule object) {
    this.curModule = object;
  }

  //============================================= methods of actions etc.
  public void saveService(ActionEvent ae) {
    Log.finer(ServiceViewControl.class.getCanonicalName() + ":saveService():" + (curModule == null ? "null" : curModule.toString()));
    if (selNetServices == null) {
      return;
    }
    //moduleAdmin.saveModule(this.selNetServices);
    addMessage(selNetServices.toString() + " saved.");
  }

  public String actionSaveService() {
    Log.finer(ServiceViewControl.class.getCanonicalName() + ":actionSaveService():" + (this.selNetServices == null ? "null" : this.selNetServices.toString()));
    if (this.selNetServices == null) {
      return "intern";
    }
    serviceAdmin.saveNetServices(this.selNetServices);
    addMessage(this.selNetServices.toString() + " saved.");
    if (!this.lstNetServices.contains(this.selNetServices)) {
      this.lstNetServices.add(this.selNetServices);
    }

    return "intern";
  }

  public void actionDeactivateService() {
    if (selNetServices == null) {
      return;
    }
    if (lstNetServices.contains(selNetServices)) {
      //TODO deactivate in DB
      lstNetServices.remove(getSelNetServices());
    }
  }

  public void prepareNewService() {
    this.selNetServices = new NetServices();
    addMessage(this.selNetServices.toString() + " prepared.");
  }

  public String actionPrepareNewService() {
    this.selNetServices = new NetServices();
    return "intern";
  }

  public String showServiceInfo() {
    Log.finer(ServiceViewControl.class.getName() + ":showServiceInfo():" + (this.selNetServices == null ? "null" : this.selNetServices.toString()));
    addMessage(this.selNetServices.toString());
    return "intern";
  }

  public List<NetServices> getRegisteredServices() {
    Log.finer(ServiceViewControl.class.getName() + ":getRegisteredServices()");
    if (this.curModule == null) {
      this.lstNetServices = serviceAdmin.listNetServices();
    } else {
      this.lstNetServices = serviceAdmin.listNetServicesByModule(this.curModule.getModid());
    }
    return this.lstNetServices;
  }

  public List<NetServices> getListServices() {
    Log.finer(ServiceViewControl.class.getName() + ":getListServices()");
    if (this.curModule != null) {
      //if a selected module is present list all services assigned to this module
      this.lstNetServices = serviceAdmin.listNetServicesByModule(this.curModule.getModid());
      return this.lstNetServices;
    }
    //check if we have selected a center ...
    this.curNetCenter = sessionCtrl.getNetCenter();
    if (this.curNetCenter != null) {
      //list all services the center has been attached to...
      this.lstNetServices = serviceAdmin.listNetServicesByZIDCID(this.curNetCenter.getNetCenterPK().getZoneid(), this.curNetCenter.getNetCenterPK().getCenterid());
    } else {
      //check if we have selected a zone
      this.curNetZones = sessionCtrl.getZoneObject();
      if (this.curNetZones != null) {
        //list all registered services of this zone...
        this.lstNetServices = serviceAdmin.listNetServicesByZID(this.curNetZones.getZoneid());
      } else {
        //list all bindings
        this.lstNetServices = serviceAdmin.listNetServices();
      }
    }
    return this.lstNetServices;
  }

  /**
   * @return the zoneid
   */
  public int getZoneId() {
    return zoneid;
  }

  /**
   * @param zoneid the zoneid to set
   */
  public void setZoneId(int zoneid) {
    this.zoneid = zoneid;
  }

  /**
   * @return the moduleid
   */
  public int getModuleid() {
    return moduleid;
  }

  /**
   * @param modid the moduleid to set
   */
  public void setModuleid(int modid) {
    this.moduleid = modid;
  }

  public String actionClose() {
    sessionCtrl.setPage("/inc/desktop.xhtml");
    return "intern";
  }

  public void onClose() {
    sessionCtrl.setPage("/inc/desktop.xhtml");
  }

  public boolean isHttp(String url) {
    boolean matchHttpHttps = url.matches("^(http|https)://.*");
    return matchHttpHttps;
  }

  /**
   * @return the selModule
   */
  public DefModule getSelModule() {
    return curModule;
  }

  /**
   * @param selModule the selModule to set
   */
  public void setSelModule(DefModule selModule) {
    this.curModule = selModule;
  }

  public void addMessage(String summary) {
    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, null);
    FacesContext.getCurrentInstance().addMessage(null, message);
  }

  /**
   * @return the selNetServices
   */
  public NetServices getSelNetServices() {
    return selNetServices;
  }

  /**
   * @param selNetServices the selNetServices to set
   */
  public void setSelNetServices(NetServices selNetServices) {
    this.selNetServices = selNetServices;
  }

  public String getModuleIdent(Integer modid) {
    if (modid == null) {
      return null;
    }
    DefModule m = null;
    try {
      m = moduleAdmin.readModuleById(modid);
    } catch (RuntimeException ex) {
      Log.severe(ServiceViewControl.class.getName() + ":getModuleIdent():" + ex.getMessage());
    }
    return m != null ? m.getName() : "n/a (" + modid + ")";
  }

  /**
   * @return the curNetCenter
   */
  public NetCenter getCurNetCenter() {
    return curNetCenter;
  }

  /**
   * @param curNetCenter the curNetCenter to set
   */
  public void setCurNetCenter(NetCenter curNetCenter) {
    this.curNetCenter = curNetCenter;
  }

  /**
   * @return the curNetZones
   */
  public NetZones getCurNetZones() {
    return curNetZones;
  }

  /**
   * @param curNetZones the curNetZones to set
   */
  public void setCurNetZones(NetZones curNetZones) {
    this.curNetZones = curNetZones;
  }

  public void checkModulePIK(String pik) {
    addMessage("checking module by AppToken " + pik + " not yet implemented.");
    //TODO check global IFETH registration (LDAP) for given AppToken...
  }
}
