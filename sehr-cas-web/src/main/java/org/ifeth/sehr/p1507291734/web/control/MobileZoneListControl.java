/*
 * (C) 2015 MDI GmbH
 *
 * Based on an idea of Hans J Haase
 * Development Team: Hans J.
 */
package org.ifeth.sehr.p1507291734.web.control;

import org.ifeth.sehr.p1507291734.ejb.ZoneAdmin;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import org.apache.commons.lang.StringUtils;
import org.ifeth.sehr.p1507291734.web.listener.SAFQueueListener;
import org.ifeth.sehr.intrasec.entities.NetZones;

/**
 * Control of mobile view (page) to work with lists of zones.
 *
 * @author HansJ
 */
@Named(value = "mobZoneListCtrl")
@RequestScoped
public class MobileZoneListControl implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger Log = Logger.getLogger("org.ifeth.p1507291734.web");
  @EJB
  private ZoneAdmin zoneManager;
  @Inject
  private MobileControl mobileCtrl;
          
  private int zoneid;
  private NetZones netZones;
  private List<NetZones> lstNetZones;

  //============================================= constructors, initialization
  public MobileZoneListControl() {
  }

  @PostConstruct
  public void init() {
    netZones = new NetZones();
    lstNetZones = new ArrayList<>();
  }

  //============================================= getter/setter
  public NetZones getZoneObject() {
    return netZones;
  }

  public void setZoneObject(NetZones object) {
    this.netZones = object;
  }

  public List<NetZones> getListMonitoredZones() {

    if (lstNetZones == null || lstNetZones.isEmpty()) {
      //lstVEvents = new ArrayList<>(); //done by init()...
      //lstNetZones.add(createDummyZone());
      FacesContext fctx = FacesContext.getCurrentInstance();
      ExternalContext ectx = fctx.getExternalContext();
      ServletContext ctx = (ServletContext) ectx.getContext();
      Map<String, SAFQueueListener> mAppList = (HashMap) ctx.getAttribute("SAFQueueListener");
      if (!mAppList.isEmpty()) {
        for (String queue : mAppList.keySet()) {
          NetZones zone = new NetZones();
          zone.setHostid(-1);
          zone.setTitle(queue);
          lstNetZones.add(zone);
        }
      }
    }

    return lstNetZones;
  }

  //============================================= methods of actions etc.
  public String doSaveZoneObject() {
    if (!lstNetZones.contains(netZones)) {
      //TODO add to DB
      lstNetZones.add(netZones);
    }

    return "pm:vwZones?transition=flip";
  }

  public void doDeleteEvent() {
    if (lstNetZones.contains(netZones)) {
      //TODO remove from DB
      lstNetZones.remove(netZones);
    }
  }

  public String doPrepareNewZoneObject() {
    netZones = new NetZones();

    return "pm:edit?transition=flip";
  }

  public String showZone() {
    Log.info(MobileZoneListControl.class.getName()+":showZone():" + netZones.toString());
    return "zoneShowDetails?transition=flip";
  }

  public List<NetZones> getRegisteredZones() {
    Log.info(MobileZoneListControl.class.getName()+":listRegisteredZones()");
    return zoneManager.listActiveZones();
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
  
  public String checkLoginStatus() {
    Log.finest(MobileControl.class.getName()+":checkLoginStatus():Username=" + mobileCtrl.getUsername());
    if (StringUtils.isBlank(mobileCtrl.getUsername())) {
      return "/mobile/login";
    }
    return "pm:vwRegZoneList";
  }
}
