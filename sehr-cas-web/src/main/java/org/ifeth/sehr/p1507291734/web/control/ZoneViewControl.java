/*
 * (C) 2015 MDI GmbH
 *
 * Based on an idea of Hans J Haase
 * Development Team: Hans J.
 */
package org.ifeth.sehr.p1507291734.web.control;

import java.io.IOException;
import org.ifeth.sehr.p1507291734.ejb.ZoneAdmin;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
//import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import org.apache.commons.lang.StringUtils;
import org.ifeth.sehr.p1507291734.web.listener.SAFQueueListener;
import org.ifeth.sehr.intrasec.entities.NetZones;
import org.ifeth.sehr.p1507291734.lib.LoggerUtility;
import org.ifeth.sehr.p1507291734.web.Constants;
import com.google.gson.Gson;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.ifeth.sehr.intrasec.entities.AdrMain;
//import org.ifeth.sehr.intrasec.entities.NetCenter;
import org.ifeth.sehr.p1507291734.ejb.AdrMainAdmin;
//import org.ifeth.sehr.p1507291734.web.beans.GeoLocation;
import org.primefaces.event.SelectEvent;
import javax.faces.view.ViewScoped;
import org.ifeth.sehr.core.exception.ObjectHandlerException;
import org.ifeth.sehr.core.lib.DeSerializer;
import org.ifeth.sehr.core.objects.GeoLocation;
import static org.ifeth.sehr.p1507291734.web.control.CenterViewControl.readJsonFromUrl;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

/**
 * Control of pages handling zone entries..
 *
 * @author HansJ
 */
@Named(value = "zoneViewCtrl")
@ViewScoped
public class ZoneViewControl implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger Log = Logger.getLogger("org.ifeth.p1507291734.web");
  private static final String MONITORCFG = "/WEB-INF/ZoneAdv.map";
  private static final Gson GJson = new Gson();

  @EJB
  private ZoneAdmin ejbZoneAdmin;
  @EJB
  private AdrMainAdmin ejbAdrMainAdmin;

  @Inject
  private SessionControl sessionCtrl;
  @Inject
  private ModuleControl moduleCtrl;
  private int zoneid;
  private NetZones netZones;
  private List<NetZones> lstNetZones;
  private AdrMain adrResp; //responsible person/organization of the zone
  private int respOrgAdrId = -1;
  private GeoLocation geoData;
  //============================================= constructors, initialization

  public ZoneViewControl() {
  }

  @PostConstruct
  public void init() {
    netZones = new NetZones();
    lstNetZones = new ArrayList<>();
    int debug = moduleCtrl.getDebugMode();
    LoggerUtility.assignLevelByDebug(debug, Log);
  }

  //============================================= getter/setter
  public NetZones getZoneObject() {
    return netZones;
  }

  public void setZoneObject(NetZones object) {
    Log.fine(ZoneViewControl.class.getName() + ":setZoneObject():" + object);
    this.netZones = object;
    if (this.netZones.getRespOrgAdrId() > 0) {
      Log.finer(ZoneViewControl.class.getName() + ":setZoneObject():RespOrgAdrId=" + this.netZones.getRespOrgAdrId());
      this.adrResp = ejbAdrMainAdmin.readByAdrId(this.netZones.getRespOrgAdrId());
    } else {
      this.adrResp = new AdrMain();
    }
  }

  /**
   * @return the adrResp
   */
  public AdrMain getAdrResp() {
    Log.info(ZoneViewControl.class.getName() + ":getAdrResp():" + adrResp);
    return adrResp;
  }

  /**
   * @param adrResp the adrResp to set
   */
  public void setAdrResp(AdrMain adrResp) {
    Log.info(ZoneViewControl.class.getName() + ":setAdrResp():" + adrResp);
    this.adrResp = adrResp;
    if (adrResp != null) {
      this.respOrgAdrId = adrResp.getAdrid();
      this.netZones.setRespOrgAdrId(adrResp.getAdrid());
    }
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
    Log.fine(ZoneViewControl.class.getName() + ":doSaveZoneObject():" + netZones);

    if (ejbZoneAdmin.save(netZones)) {
      if (!lstNetZones.contains(netZones)) {
        lstNetZones.add(netZones);
      }
      FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Success", netZones.toString() + " saved."));
    } else {
      FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Error", netZones.toString() + " NOT saved."));
    }
    return "intern";
  }

  public String doSaveRespAdr() {
    Log.fine(ZoneViewControl.class.getName() + ":doSaveRespAdr():" + this.adrResp);
    this.respOrgAdrId = ejbAdrMainAdmin.save(this.adrResp);
    if (this.respOrgAdrId >= 0) {
      this.netZones.setRespOrgAdrId(this.respOrgAdrId);
      FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Success", adrResp.toString() + " saved."));
      if (ejbZoneAdmin.save(this.netZones)) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Success", this.netZones.toString() + " with new address updated."));
      }
    } else {
      FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Error", adrResp.toString() + " NOT saved."));
    }
    return "intern";
  }

  public void setRespOrgAdrId(int adrid) {
    this.respOrgAdrId = adrid;
    if (this.adrResp == null || this.adrResp.getAdrid() != adrid || adrid <= 0) {
      if (adrid > 0) {
        this.adrResp = ejbAdrMainAdmin.readByAdrId(adrid);
      } else {
        //create blank (new) record
        this.adrResp = new AdrMain();
      }
    }
  }

  public int getRespOrgAdrId() {
    return this.respOrgAdrId;
  }

  public List<AdrMain> cplAdrResp(String query) {
    Log.fine(ZoneViewControl.class.getName() + ":cplAdrResp():" + query);

    Map<String, String> params = new HashMap();
    if (StringUtils.isNotBlank(query)) {
      params.put("match", query.trim()); //match like ...
    }
    List<AdrMain> l = ejbAdrMainAdmin.listAdressesByParams(params);
    return l;
  }

  public void onAdrSelect(SelectEvent event) {
    Log.info(ZoneViewControl.class.getName() + ":onAdrSelect()" + event.getObject());
    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Item Selected", "" + event.getObject()));
  }

  public void doCheckRegistration(NetZones z) {
    Log.finer(ZoneViewControl.class.getName() + ":doCheckRegistration()");
    if (z == null) {
      return;
    }

    String grwlMessage = "Checking " + z.toString();
    int val = 0;
    //int newval = val;
    if (sessionCtrl.statusNetZones().containsKey(z)) {
      val = sessionCtrl.statusNetZones().get(z);
    }
    //URL=bit0
    if (isSEHRWebSSL(z)) {
      val |= Constants.maskIsURLSEHRWebSSL;
      grwlMessage += "<br/>URL via TLS/SSH";
    } else {
      val &= Constants.maskIsURLSEHRWebSSL;
      grwlMessage += "<br/>URL is unsecure!";
    }
    Log.fine(ZoneViewControl.class.getName() + ":doCheckRegistration():zone=" + z.toString() + ", val=" + Integer.toBinaryString(val));
    sessionCtrl.statusNetZones().put(z, val);
    FacesContext fctx = FacesContext.getCurrentInstance();
    fctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Status...", grwlMessage));
  }

  public boolean isLocalZone(Integer zid) {
    return zid != null && zid.equals(moduleCtrl.getLocalZoneID());
  }

  public boolean isSEHRWeb(NetZones z) {
    Log.info(ZoneViewControl.class.getName() + ":isSEHRWeb():" + z.toString());

    if (sessionCtrl.statusNetZones().containsKey(z)) {
      int val = sessionCtrl.statusNetZones().get(z);
      Log.finer(ZoneViewControl.class.getName() + ":isSEHRWeb():val=" + Integer.toBinaryString(val));
      return (val & Constants.maskIsURLSEHRWeb) == Constants.maskIsURLSEHRWeb;
    }
    return false;
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
    Log.info(ZoneViewControl.class.getName() + ":showZone():" + netZones.toString());
    return "intern";
  }

  public List<NetZones> getRegisteredZones() {
    Log.info(ZoneViewControl.class.getName() + ":getRegisteredZones()");
    return ejbZoneAdmin.listActiveZones();
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

  public String actionClose() {
    sessionCtrl.setPage("/inc/desktop.xhtml");
    return "intern";
  }

  public void onClose() {
    sessionCtrl.setPage("/inc/desktop.xhtml");
  }

  public boolean isHttp(String url) {
    return StringUtils.startsWithIgnoreCase(url, "http://");
  }

  public boolean isPublic(NetZones z) {
    return z.ispublic();
  }

  public String getStatusMonitor(NetZones z) {
    if (z == null) {
      return "The status can't be determined (null)";
    }
    FacesContext fctx = FacesContext.getCurrentInstance();
    ExternalContext ectx = fctx.getExternalContext();
    ServletContext ctx = (ServletContext) ectx.getContext();
    Map<String, String> zoneAdvMap = (HashMap) ctx.getAttribute("ZoneAdv");
    if (zoneAdvMap == null || !zoneAdvMap.containsKey(z.getZoneidstr())) {
      return "The services of this zone (" + z.getZoneid() + ") are not monitored.";
    }
    List<NetZones> lMonitored = sessionCtrl.activeMonitoredZones(true);
    for (NetZones m : lMonitored) {
      if (m.getZoneid().equals(z.getZoneid())) {
        return "Service queue activated on this host.";
      }
    }
    return "No monitoring on by this host. Applications that do require services interface may not working!";
  }

  public String getStatusImgMonitor(NetZones z) {
    if (z == null) {
      return "blank16x16.png";
    }
    FacesContext fctx = FacesContext.getCurrentInstance();
    ExternalContext ectx = fctx.getExternalContext();
    ServletContext ctx = (ServletContext) ectx.getContext();
    //1. check if zone is flagged to be served by this sehr-cas app
    Map<String, String> zoneAdvMap = (HashMap) ctx.getAttribute("ZoneAdv");
    if (zoneAdvMap == null || !zoneAdvMap.containsKey(z.getZoneidstr())) {
      return "blank16x16.png";
    }
    //2. check if zone is currently served (listening for requests)
    List<NetZones> lMonitored = sessionCtrl.activeMonitoredZones(true);
    for (NetZones m : lMonitored) {
      if (m.getZoneid().equals(z.getZoneid())) {
        return "serv_on16x16.png";
      }
    }
    return "serv_off16x16.png";
  }

  public boolean hasPublicKey(NetZones z) {
    return z.hasPublicKey();
  }

  public String viewAddress(Integer adrid) {
    Log.info(ZoneViewControl.class.getName() + ":viewAddress():adr=" + adrid);
    AdrMain adrMain = ejbAdrMainAdmin.readByAdrId(adrid);
    if (adrMain != null) {
      StringBuilder sb = new StringBuilder();
      //GERMAN style
      sb.append(adrMain.getTitle()).append("<br/>");
      sb.append(adrMain.getAdr1()).append("<br/>");
      sb.append(adrMain.getAdr2()).append("<br/>");
      sb.append(adrMain.getAdr3()).append("<br/>");
      sb.append(adrMain.getStreet()).append(" ").append(adrMain.getHN()).append("<br/>");
      sb.append(adrMain.getZip()).append(" ").append(adrMain.getCity()).append("<br/>");
      return sb.toString();
    }
    return "- no address -";
  }

  /**
   * Check if there is a secure connection on port 8181 to SEHR-CAS.
   *
   * @param z
   * @return
   */
  public boolean isSEHRWebSSL(NetZones z) {
    if (StringUtils.isBlank(z.getPriip())) {
      return false;
    }
    String httpUrl = "http://" + z.getPriip() + ":8080/sehr-cas-web/";
    Log.info(ZoneViewControl.class.getName() + ":isSEHRWebSSL():testing HTTP URL:" + httpUrl);
    StringBuilder sb = new StringBuilder();
    sb.append(httpUrl).append(": ");
    boolean stsUrl = moduleCtrl.isHTTPAccess(httpUrl);
    sb.append(stsUrl ? "ok" : "failed").append("<br/>\n");

    sb.append("Testing SSL connection of '" + z.getPriip() + ":8181'").append(": ");
    boolean stsHost = isHostAvailable(z.getPriip(), 8181);
    sb.append(stsHost ? "ok" : "failed").append("<br/>\n");
    FacesContext fctx = FacesContext.getCurrentInstance();
    fctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Status...", "" + sb.toString()));

    return true;//stsUrl & stsHost;
  }

  /**
   * Get lon/lat from address or try to get it from geolocation DB.
   *
   * @param z
   * @return
   */
  public String getGeoLocation(NetZones z) {
    if (z == null || z.getRespOrgAdrId() == null) {
      return "";
    }
    AdrMain adrMain = ejbAdrMainAdmin.readByAdrId(z.getRespOrgAdrId());
    if (adrMain == null || adrMain.getGeolat() == null || adrMain.getGeolng() == null) {
      return "";
    }
    GeoLocation gl = null;

    if (adrMain.getGeoLocation() != null) {
      try {
        gl = (GeoLocation) DeSerializer.deserialize(adrMain.getGeoLocation());
        gl.setData(null); //remove due to GJson to JSON parsing error
      } catch (ObjectHandlerException ex) {
        Logger.getLogger(CenterViewControl.class.getName()).log(Level.WARNING, ex.getMessage());
      }
    }
    if (gl == null && adrMain.getGeolat() != null && adrMain.getGeolng() != null) {
      //old style just lon and lat - generate GeoLoc dataset
      gl = new GeoLocation(null, "way", adrMain.getGeolng(), adrMain.getGeolat());
    }

    String s = GJson.toJson(gl);
    return s;
  }

  public void searchGeoLocation(Integer adrid) {
    Log.fine(ZoneViewControl.class.getName() + ":searchGeoLocation(" + adrid + ")");
    if (this.adrResp == null || this.adrResp.getAdrid() == null || !this.adrResp.getAdrid().equals(adrid)) {
      this.adrResp = ejbAdrMainAdmin.readByAdrId(adrid);
    }
    if (this.adrResp != null && this.adrResp.getCity() != null) {
      Log.finer(ZoneViewControl.class.getName() + ":searchGeoLocation():" + this.adrResp);

      try {
        //get data from Address otherwise use the center of Germany ;)
        String osmurl = "http://nominatim.openstreetmap.org/search/";
        String search = "?format=json&addressdetails=0&limit=1";
        //String charset = "UTF-8";
        //url = new URL(osmurl + URLEncoder.encode(adrMain.getStreet()+" "+adrMain.getCity(), charset).replaceAll("\\+","%20") + search);
        //Log.info("url=" + url.toString());
        //Nominatim accepts " " but not URL encoding
        String q = (StringUtils.isNotBlank(adrResp.getHN()) ? adrResp.getHN() + " " : "");
        q += (StringUtils.isNotBlank(adrResp.getStreet()) ? adrResp.getStreet() + " " : "") + adrResp.getCity();
        String s = URLEncoder.encode(q, "UTF-8").replaceAll("\\+", "%20");
        String surl = osmurl + s + search;
        Log.finer(ZoneViewControl.class.getName() + ":searchGeoLocation():" + surl);
        JSONObject json = readJsonFromUrl(surl);
        if (json == null) {
          this.geoData = null;
          this.adrResp.setGeoLocation(null);
          this.adrResp.setGeolat(null);
          this.adrResp.setGeolng(null);
          return;
        }
        Log.finer(ZoneViewControl.class.getName() + ":searchGeoLocation():\n" + json.toString());
        this.adrResp.setGeolat(Double.parseDouble("" + json.get("lat")));
        this.adrResp.setGeolng(Double.parseDouble("" + json.get("lon")));
        //prepare GeoLocation object
        this.geoData = new GeoLocation(null, this.adrResp.getGeolng(), this.adrResp.getGeolat());
        //store raw data (the seach result)
        this.geoData.setData(json.toString());
        //update AdrMain
        this.adrResp.setGeoLocation(DeSerializer.serialize(this.geoData));
        //OLD this.adrMain.setGeoLocation(DeSerializer.serialize(json.toString()));
        ejbAdrMainAdmin.save(this.adrResp);

        //RequestContext rctx = RequestContext.getCurrentInstance();
        //rctx.update("frmViewCenter:tvDlgEditCenter:tabGeoLoc");
      } catch (JSONException | NumberFormatException | UnsupportedEncodingException | ObjectHandlerException ex) {
        Logger.getLogger(CenterViewControl.class.getName()).log(Level.WARNING, ex.getMessage());
      }
    }
  }

  //============================================= private mthods
  //TODO move to SEHR Core

  private boolean isHostAvailable(String hostName, int port) {
    try (Socket socket = new Socket()) {
      InetSocketAddress socketAddress = new InetSocketAddress(hostName, port);
      socket.connect(socketAddress, 3000);
      //TODO check for sehr-cas or 24100 ;)
      socket.close();
      return true;
    } catch (SocketTimeoutException ex) {
      Logger.getLogger(SessionControl.class.getName()).log(Level.SEVERE, null, ex.getMessage());
    } catch (IOException ex) {
      Logger.getLogger(SessionControl.class.getName()).log(Level.SEVERE, null, ex);
    }
    return false;
  }

}
