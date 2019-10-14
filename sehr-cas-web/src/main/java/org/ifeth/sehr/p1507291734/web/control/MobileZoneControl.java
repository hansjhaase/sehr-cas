/*
 * (C) 2015 MDI GmbH
 *
 * Based on an idea of Hans J Haase
 * Development Team: Hans J.
 */
package org.ifeth.sehr.p1507291734.web.control;

import org.ifeth.sehr.p1507291734.ejb.ZoneAdmin;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import org.apache.commons.lang.StringUtils;
import org.ifeth.sehr.intrasec.entities.AdrMain;
import org.ifeth.sehr.p1507291734.web.MessagingManager;
import org.ifeth.sehr.p1507291734.web.listener.SAFQueueListener;
import org.ifeth.sehr.intrasec.entities.NetZones;
import org.ifeth.sehr.p1507291734.web.beans.GeoLocation;
import com.google.gson.Gson;
import org.ifeth.sehr.p1507291734.ejb.AdrMainAdmin;

/**
 * Control of mobile pages to view and manage zones.
 *
 * @author HansJ
 */
@Named(value = "mobZoneCtrl")
@SessionScoped
public class MobileZoneControl implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");
  private static final String MONITORCFG = "/WEB-INF/ZoneAdv.map";
  private static final Gson GJson = new Gson();

  @EJB
  private ZoneAdmin zoneManager;
  @EJB
  private AdrMainAdmin ejbAdrMainAdmin;
  @Inject
  private MobileControl mobileCtrl;
  @Inject
  private ModuleControl moduleCtrl;

  private int zoneid;
  private NetZones netZones;
  private List<NetZones> lstNetZones;

  //============================================= constructors, initialization
  public MobileZoneControl() {
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

  public void setZoneObject(NetZones entity) {
    this.netZones = entity;
    this.zoneid = entity.getZoneid();
  }

  public List<NetZones> getListMonitoredZones() {

    if (lstNetZones == null || lstNetZones.isEmpty()) {
      this.lstNetZones = listMonitoredZones();
    }
    return lstNetZones;
  }

  //============================================= methods of actions etc.
  public String doSaveZoneObject() {
    if (lstNetZones == null || lstNetZones.isEmpty()) {
      this.lstNetZones = listMonitoredZones();
    }
    if (!lstNetZones.contains(netZones)) {
      //add to map 
      ServletContext ctx = (ServletContext) FacesContext
              .getCurrentInstance().getExternalContext().getContext();

      Map<String, String> zoneAdvMap = (HashMap) ctx.getAttribute("ZoneAdv");
      if (zoneAdvMap == null) {
        zoneAdvMap = new HashMap<>();
      }
      if (!zoneAdvMap.containsKey(netZones.getZoneidstr())) {
        zoneAdvMap.put(netZones.getZoneidstr(), netZones.getTitle());
        Properties pMap = new Properties();
        try {
          InputStream is = ctx.getResourceAsStream(MONITORCFG);
          pMap.load(is);
          Log.finest(pMap.toString());
          if (!pMap.containsKey(netZones.getZoneidstr())) {
            pMap.setProperty(netZones.getZoneidstr(), netZones.getTitle());
            OutputStream out = new FileOutputStream(ctx.getRealPath(MONITORCFG));
            pMap.store(out, "Modified...");
          }
        } catch (FileNotFoundException ex) {
          Logger.getLogger(MobileZoneControl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
          Logger.getLogger(MobileZoneControl.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
      //add to DB if zoneid not exist

      lstNetZones.add(netZones);
    }

    return "pm:vwZones?transition=flip";
  }

  public void doDeleteEvent() {
    if (lstNetZones.contains(netZones)) {
      //remove from monitoring map
      lstNetZones.remove(netZones);
      //TODO remove from config map
    }
  }

  public String doPrepareNewZoneObject() {
    netZones = new NetZones();

    return "pm:edit?transition=flip";
  }

  public String showZone() {
    Log.info(MobileZoneControl.class.getName() + ":showZone():" + netZones.toString());
    return "zoneDtlInfo?transition=flip";
  }

  public List<NetZones> getRegisteredZones() {
    Log.info(MobileZoneControl.class.getName() + ":listRegisteredZones()");
    return zoneManager.listActiveZones();
  }

  /**
   * Get the number of assigned / manged centers of the selected.
   *
   * @return
   */
  public Integer cntCenterOfZone() {
    Log.info(MobileZoneControl.class.getName() + ":cntCenterOfZone()");
    return zoneManager.countCentersOfZone(zoneid, true);
  }

  public boolean isLocalZoneServed() {
    Log.finer(MobileZoneControl.class.getName() + ":isLocalZoneServed()");
    this.lstNetZones = listMonitoredZones();
    if (lstNetZones == null || lstNetZones.isEmpty()) {
      return false;
    }
    for (NetZones z : lstNetZones) {
      Log.finer(MobileZoneControl.class.getName() + ":" + moduleCtrl.getLocalZoneID() + "=?" + z.getZoneid());
      if (z.getZoneid() == moduleCtrl.getLocalZoneID()) {
        return true;
      }
    }
    return false;
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

  public String doActivateSAFQueue() {
    if (this.zoneid <= 0) {
      Log.fine(MobileZoneControl.class.getName() + "doActivateSAFQueue():Invalid zone ID: " + this.zoneid);
      return "pm:vwZonesAddMonitor";
    }
    Log.fine(MobileZoneControl.class.getName() + "doActivateSAFQueue():zid=" + this.zoneid);
    //check if zone is in DB (registered)
    NetZones netZones = zoneManager.readNetZonesByID(this.zoneid);
    if (netZones == null) {
      Log.fine(MobileZoneControl.class.getName() + "doActivateSAFQueue():No entry (registration) for zone ID: " + this.zoneid);
      return "pm:vwZonesAddMonitor";
    }
    //TODO check if this host is allowed to service the zone
    ServletContext ctx = (ServletContext) FacesContext
            .getCurrentInstance().getExternalContext().getContext();
    MessagingManager msgManager = MessagingManager.getInstance(ctx);
    if (msgManager.isConnected()) {
      if (msgManager.addServiceListener(String.format("%07d", this.zoneid))) {
        //rebuild list of controlled zones
        this.lstNetZones = listMonitoredZones();
        this.zoneid = 0; //set blank = n/a
      }
    }
    return "pm:vwZones?transition=flip";
  }

  public String getGeoLocation(NetZones z) {
    Log.info(MobileZoneControl.class.getName() + ":getGeoLocation()");
    if (z == null || z.getRespOrgAdrId() == null) {
      return "";
    }
    //TODO get location from address (if any)
    GeoLocation gl = new GeoLocation(); ///default is MDI GmbH location ;)
    AdrMain adrMain = ejbAdrMainAdmin.readByAdrId(z.getRespOrgAdrId());
    if (adrMain == null || adrMain.getGeolat() == null || adrMain.getGeolng() == null) {
      return "";
    }
    gl.setLat(adrMain.getGeolat());
    gl.setLon(adrMain.getGeolng());
    String s = GJson.toJson(gl);
    return s;
  }

  public boolean isServiceQueue(int zoneid) {
    FacesContext fctx = FacesContext.getCurrentInstance();
    ExternalContext ectx = fctx.getExternalContext();
    ServletContext ctx = (ServletContext) ectx.getContext();
    Map<String, SAFQueueListener> mMonitor = (HashMap) ctx.getAttribute("SAFQueueListener");
    if (!mMonitor.isEmpty()) {
      for (String queue : mMonitor.keySet()) {
        String z = queue.substring(5, 12); //get zoneid part
        int zid = Integer.parseInt(z);
        System.out.println("Checking for zone #" + zoneid);
        if (zid == zoneid) {
          SAFQueueListener listener = mMonitor.get(z);
          if (listener != null) {
            return listener.isSession();
          }
        }
      }
    }
    return false;
  }

  public String doStopSAFQueue() {
    if (this.zoneid <= 0) {
      Log.fine(MobileZoneControl.class.getName() + "doStopSAFQueue():Invalid zoneid:" + this.zoneid);
      return null;//"pm:#vwZonesAddMonitor";
    }
    FacesContext fctx = FacesContext.getCurrentInstance();
    ExternalContext ectx = fctx.getExternalContext();
    ServletContext ctx = (ServletContext) ectx.getContext();
    Map<String, SAFQueueListener> mMonitor = (HashMap) ctx.getAttribute("SAFQueueListener");
    if (!mMonitor.isEmpty()) {
      for (String queue : mMonitor.keySet()) {
        String z = queue.substring(5, 12); //get zoneid part
        int zid = Integer.parseInt(z);
        Log.fine(MobileZoneControl.class.getName() + "doStopSAFQueue():Processing zone #" + zid);
        if (zid == this.zoneid) {
          Log.info(MobileZoneControl.class.getName() + "doStopSAFQueue():Closing queue for zone #" + zid);
          SAFQueueListener listener = mMonitor.get(z);
          if (listener != null) {
            listener.stop();
          }
          mMonitor.remove(z);
        }
      }
    }
    return null;
  }

  public String checkLoginStatus() {
    Log.finest(MobileZoneControl.class.getName() + ":checkLoginStatus():Username=" + mobileCtrl.getUsername());
    if (StringUtils.isBlank(mobileCtrl.getUsername())) {
      return "/mobile/login";
    }
    //TODO implement switch to last page that has been visited...
    return "/mobile/index?data-page=vwIntro";
  }

  public String statusBgColor(int zid) {
    String style = "";
    if (zid == moduleCtrl.getLocalZoneID()) {
      style = "color: #00DDDD;";
    }
    return style;
  }

  private List<NetZones> listMonitoredZones() {
    List l = new ArrayList<>();
    FacesContext fctx = FacesContext.getCurrentInstance();
    ExternalContext ectx = fctx.getExternalContext();
    ServletContext ctx = (ServletContext) ectx.getContext();
    Map<String, SAFQueueListener> mMonitor = (HashMap) ctx.getAttribute("SAFQueueListener");
    if (!mMonitor.isEmpty()) {
      for (String queue : mMonitor.keySet()) {
        String z = queue.substring(5, 12); //get zoneid part
        //TODO check if registered in DB
        NetZones zone = new NetZones();
        zone.setHostid(-1);
        zone.setZoneid(Integer.parseInt(z));
        zone.setTitle(queue);
        l.add(zone);
      }
    }
    return l;
  }
}
