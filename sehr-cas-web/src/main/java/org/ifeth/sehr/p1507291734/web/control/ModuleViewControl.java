/*
 * (C) 2015 MDI GmbH
 *
 * Based on an idea of Hans J Haase
 * Development Team: Hans J.
 */
package org.ifeth.sehr.p1507291734.web.control;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
//import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.camel.RuntimeCamelException;
import org.apache.commons.lang.StringUtils;
import org.ifeth.sehr.intrasec.entities.AdrMain;
import org.ifeth.sehr.intrasec.entities.DefModule;
import org.ifeth.sehr.intrasec.entities.NetServices;
import org.ifeth.sehr.p1507291734.ejb.AdrMainAdmin;
import org.ifeth.sehr.p1507291734.ejb.ModuleAdmin;
import org.primefaces.event.SelectEvent;

/**
 * Control of pages handling module (app) entries..
 *
 * @author HansJ
 */
@Named(value = "moduleViewCtrl")
@ViewScoped
public class ModuleViewControl implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");
  //TODO move to sehr-core constants
  private static final short ADRTYPE_PRODUCER = 2;

  @EJB
  private ModuleAdmin moduleAdmin;

  @EJB
  private AdrMainAdmin ejbAdrMainAdmin;

  @Inject
  private SessionControl sessionCtrl;

  private int zoneid; //to get modules of a zone
  private int moduleid; //recid
  private DefModule selModule;
  private NetServices fltNetServices;
  //private DefModule defModule;
  private List<DefModule> lstDefModule;
  private Map<String, String> lstTypes;
  private List<AdrMain> lstAdrMain;
  private AdrMain adrMain; //referenced AdrMain entry of a module

  //============================================= constructors, initialization
  public ModuleViewControl() {
  }

  @PostConstruct
  public void init() {
    selModule = new DefModule();
    lstDefModule = new ArrayList<>();
    lstTypes = new HashMap<>();
    lstTypes.put("0", "n/a"); //binary 0000
    lstTypes.put("1", "no GUI"); //binary 0001
    lstTypes.put("2", "Standard GUI (Desktop)"); //binary 0010
    lstTypes.put("4", "Android"); //binary 0100
    lstTypes.put("8", "iOS"); //binary 1000
    lstTypes.put("16", "WEB 2.0 (HTML5)"); //binary 10000

    lstAdrMain = new ArrayList<>();
  }

  /**
   * @return the fltNetServices
   */
  public NetServices getFltNetServices() {
    return fltNetServices;
  }

  /**
   * Activate a filter to show only the module used by 'NetServices' object.
   *
   * @param fltNetServices the fltNetServices to set
   */
  public void setFltNetServices(NetServices fltNetServices) {
    this.fltNetServices = fltNetServices;
    try {
      this.selModule = moduleAdmin.readModuleById(fltNetServices.getModid());
    } catch (RuntimeCamelException rte) {
      Log.severe(ModuleViewControl.class.getName() + ":setFltNetServices():" + rte.getMessage());
    }
  }

  //============================================= getter/setter
  public DefModule getModuleObject() {
    return selModule;
  }

  public void setModuleObject(DefModule object) {
    this.selModule = object;
  }

  public List<DefModule> getListModules() {

    if (lstDefModule == null || lstDefModule.isEmpty()) {

    }
    return lstDefModule;
  }

  //============================================= methods of actions etc.
  public void saveModule(ActionEvent ae) {
    Log.finer(ModuleViewControl.class.getCanonicalName() + ":saveModule():" + (this.selModule == null ? "null" : this.selModule.toString()));
    if (this.selModule == null) {
      return;
    }
    moduleAdmin.saveModule(this.selModule);
    addMessage(this.selModule.toString() + " saved.");
  }

  public String actionSaveModule() {
    Log.finer(ModuleViewControl.class.getCanonicalName() + ":actionSaveModule():" + (this.selModule == null ? "null" : this.selModule.toString()));
    if (this.selModule == null) {
      return "intern";
    }
    moduleAdmin.saveModule(this.selModule);
    addMessage(this.selModule.toString() + " saved.");
    if (!this.lstDefModule.contains(this.selModule)) {
      this.lstDefModule.add(this.selModule);
    }
    this.adrMain = null;//reset
    return "intern";
  }

  public void actionDeleteModule() {
    if (lstDefModule.contains(selModule)) {
      //TODO deactivate in DB
      lstDefModule.remove(selModule);
    }
  }

  public void prepareNewModule() {
    selModule = new DefModule();
    addMessage(this.selModule.toString() + " prepared.");
  }

  public String actionPrepareNewModule() {
    selModule = new DefModule();
    return "intern";
  }

  public String showModuleInfo() {
    Log.finer(ModuleViewControl.class.getName() + ":showModuleInfo():" + selModule.toString());
    addMessage(this.selModule.toString());
    return "intern";
  }

  public List<DefModule> getRegisteredModules() {
    Log.finer(ModuleViewControl.class.getName() + ":getRegisteredModules()");
    if (this.fltNetServices != null) {
      this.lstDefModule = new ArrayList<>();
      if (this.selModule != null) {
        this.lstDefModule.add(this.selModule);
      }
    } else {
      this.lstDefModule = moduleAdmin.listModules();
    }
    return this.lstDefModule;
  }

  public Map<String, String> getListTypes() {
    Log.finer(ModuleViewControl.class.getName() + ":getListTypes()");
    return this.lstTypes;
  }

  public List<AdrMain> getListAdrMain() {
    Log.finer(ModuleViewControl.class.getName() + ":getListAdrMain()");
    this.lstAdrMain = ejbAdrMainAdmin.listAdrMain(ADRTYPE_PRODUCER);
    return this.lstAdrMain;
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

  /**
   * @return the adrMain
   */
  public AdrMain getAdrMain() {
    return adrMain;
  }

  /**
   * @param adrMain the adrMain to set
   */
  public void setAdrMain(AdrMain adrMain) {
    this.adrMain = adrMain;
  }

  public String actionClose() {
    sessionCtrl.setPage("/inc/desktop.xhtml");
    return "intern";
  }

  public void onClose() {
    sessionCtrl.setPage("/inc/desktop.xhtml");
  }

  public boolean isHttp(String url) {
    boolean matchHttpHttps = url.matches("^(https?)://.*");
    return matchHttpHttps;
  }

  /**
   * @return the selModule
   */
  public DefModule getSelModule() {
    return selModule;
  }

  /**
   * @param selModule the selModule to set
   */
  public void setSelModule(DefModule selModule) {
    this.selModule = selModule;
    if (selModule.getAdrid() != null && selModule.getAdrid() >= 0) {
      //load referenced address also
      this.adrMain = ejbAdrMainAdmin.readByAdrId(selModule.getAdrid());
    } else {
      this.adrMain = null; //currently there is no address assigned
    }
  }

  public void addMessage(String summary) {
    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, null);
    FacesContext.getCurrentInstance().addMessage(null, message);
  }

  public void onTypeChange() {
    String out = "null";
    if (this.selModule != null) {
      out = this.selModule.toString() + "/type=" + Integer.toBinaryString(0xFFFF & this.selModule.getType());
    }
    Log.finer(ModuleViewControl.class.getName() + ":onTypeChange():" + out);
    addMessage("onTypeChange():DefModule=" + out);
  }

  public void onAdrChange(final AjaxBehaviorEvent event) {
    Object src = event.getSource();
    Log.finer(ModuleViewControl.class.getName() + ":onAdrChange():" + src.toString());

    String out = "DefModule is null";
    if (this.selModule != null) {
//      if (this.selModule.getAdrid() == null) {
//        this.selModule.setAdrid(0); //null not allowed by DB table
//      }
//      out = this.selModule.toString() + "/adrid=" + this.selModule.getAdrid();

      if (this.adrMain != null) {
        out += "/" + this.adrMain.toString();
        this.selModule.setAdrid(this.adrMain.getAdrid());
      } else {
        this.selModule.setAdrid(0);
      }
      Log.finer(ModuleViewControl.class.getName() + ":onAdrChange():adrid=" + this.selModule.getAdrid());
    }
    addMessage("onAdrChange():" + out);
  }
}
