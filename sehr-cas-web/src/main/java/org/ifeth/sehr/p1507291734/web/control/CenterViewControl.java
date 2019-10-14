/*
 * (C) 2015 MDI GmbH for the SEHR community
 *
 * Based on an idea of Hans J Haase
 * Development Team: Hans J.
 */
package org.ifeth.sehr.p1507291734.web.control;

import com.google.gson.Gson;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped; //use this on JSF 2.2 and CDI!
import javax.inject.Inject;
import javax.inject.Named;
import org.ifeth.sehr.core.exception.ObjectHandlerException;
import org.ifeth.sehr.core.lib.DeSerializer;
import org.ifeth.sehr.core.objects.GeoLocation;
import org.ifeth.sehr.intrasec.entities.AdrMain;
import org.ifeth.sehr.intrasec.entities.NetCenter;
import org.ifeth.sehr.intrasec.entities.NetZones;
import org.ifeth.sehr.p1507291734.ejb.AdrMainAdmin;
import org.ifeth.sehr.p1507291734.ejb.CenterAdmin;
import org.ifeth.sehr.p1507291734.ejb.ZoneAdmin;
import org.primefaces.event.CloseEvent;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.event.data.FilterEvent;
import org.primefaces.json.JSONException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import javax.faces.event.ActionEvent;
import org.apache.commons.lang.StringUtils;
import org.ifeth.sehr.intrasec.entities.NetCenterPK;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.context.RequestContext;
import org.primefaces.json.JSONObject;

/**
 * Control of pages handling center entries.
 *
 * @author HansJ
 */
@Named(value = "centerViewCtrl")
@ViewScoped
public class CenterViewControl implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger Log = Logger.getLogger("org.ifeth.p1507291734.web");
  private static final Gson GJson = new Gson();

  @EJB
  private CenterAdmin ejbCenterAdmin;
  @EJB
  private ZoneAdmin ejbZoneAdmin;
  @EJB
  private AdrMainAdmin ejbAdrMainAdmin;

  @Inject
  private SessionControl sessionCtrl;
  @Inject
  private ModuleControl moduleCtrl;

  private int centerid = -1;
  private int zoneid = -1;
  private NetZones netZones = null; //to get centers of a zone object
  private List<NetZones> lstNetZones;
  private NetCenter netCenter;
  private AdrMain adrMain;
  private List<NetCenter> lstNetCenter;
  private List<NetCenter> lstNetCenterFiltered;
  private GeoLocation geoData;
  private String action = "none"; //none, edit, add etc 
  private boolean modified = false;
  private String tabShown = "";
//============================================= constructors, initialization

  public CenterViewControl() {
  }

  @PostConstruct
  public void init() {
    Log.fine(CenterViewControl.class.getName() + ":init()");

    //if there is a zone in session to work with...
    netZones = sessionCtrl.getZoneObject();
    if (netZones != null) {
      this.zoneid = netZones.getZoneid();
      Log.finer(CenterViewControl.class.getName() + ":init():" + netZones);
      lstNetCenter = ejbCenterAdmin.listCentersByZoneId(netZones.getZoneid());
    } else {
      lstNetCenter = ejbCenterAdmin.listCenters();
    }
    this.lstNetZones = ejbZoneAdmin.listActiveZones();
    this.netCenter = new NetCenter(-1, this.zoneid); //default: no center object selected
    this.adrMain = new AdrMain();
    this.geoData = null;
  }

  //============================================= getter/setter
  public void setAction(String action) {
    this.action = action;
  }

  public String getAction() {
    return this.action;
  }

  /**
   * @return the modified
   */
  public boolean isModified() {
    return modified;
  }

  /**
   * @param modified the modified to set
   */
  public void setModified(boolean modified) {
    this.modified = modified;
  }

  public NetZones getZoneObject() {
    return netZones;
  }

  public void setZoneObject(NetZones object) {
    this.netZones = object;
  }

  public NetCenter getNetCenter() {
    if (netCenter == null || netCenter.getNetCenterPK() == null) {
      netCenter = new NetCenter(-1, this.zoneid);
    }
    return netCenter;
  }

  public void setNetCenter(NetCenter nc) {
    Log.fine(CenterViewControl.class.getName() + ":setNetCenter():" + nc);
    this.netCenter = nc;
    if (this.netCenter != null) {
      this.adrMain = ejbAdrMainAdmin.readByAdrId(nc.getAdrid());
      if (this.getAdrMain() == null) {
        this.adrMain = new AdrMain();
        this.geoData = null; //null is correct
      } else {
        if (adrMain.getGeoLocation() != null) {
          try {
            this.geoData = (GeoLocation) DeSerializer.deserialize(adrMain.getGeoLocation());
          } catch (ObjectHandlerException ex) {
            Logger.getLogger(CenterViewControl.class.getName()).log(Level.WARNING, ex.getMessage());
          }
        } else if (adrMain.getGeolat() != null && adrMain.getGeolng() != null) {
          this.geoData = new GeoLocation(null, adrMain.getGeolng(), adrMain.getGeolat());
        } else {
          this.geoData = null;
          if (adrMain.getCity() != null) {
            //get data from Address otherwise use the center of Germany ;)
            String osmurl = "http://nominatim.openstreetmap.org/search/";
            String search = "?format=json&addressdetails=0&limit=1";
            String charset = "UTF-8";

            //URL url;
            try {
              //url = new URL(osmurl + URLEncoder.encode(adrMain.getStreet()+" "+adrMain.getCity(), charset) + search);
              //Log.info("url=" + url.toString());
              String surl = osmurl + URLEncoder.encode((adrMain.getStreet() != null ? adrMain.getStreet() + " " : "") + adrMain.getCity(), charset) + search;
              JSONObject json = readJsonFromUrl(surl);
              Log.finer(json.toString());
              this.adrMain.setGeolat(Double.parseDouble("" + json.get("lat")));
              this.adrMain.setGeolng(Double.parseDouble("" + json.get("lon")));
              this.geoData = new GeoLocation(null, this.adrMain.getGeolng(), this.adrMain.getGeolat());
              //this.geoData.setData(json.toString()); 
              RequestContext rctx = RequestContext.getCurrentInstance();
              rctx.update("frmViewCenter:tvDlgEditCenter:tabGeoLoc");
            } catch (IOException | JSONException | NumberFormatException ex) {
              Logger.getLogger(CenterViewControl.class.getName()).log(Level.WARNING, ex.getMessage());
            }
          }
        }
      }
      this.action = "edit";
    } else {
      this.netCenter = new NetCenter(-1, this.zoneid);
    }
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

  /**
   * @return the geoData
   */
  public GeoLocation getGeoData() {
    return this.geoData;
  }

  /**
   * @param geoData the geoData to set
   */
  public void setGeoData(GeoLocation geoData) {
    this.geoData = geoData;
  }

  public void onTabChange(TabChangeEvent evt) {
    Log.fine(CenterViewControl.class.getName() + ":onTabChange():" + evt.getTab().getId());

    //FacesMessage msg = new FacesMessage("DEBUG", "" + evt.getTab().getTitle());
    //FacesContext.getCurrentInstance().addMessage(null, msg);
    this.tabShown = evt.getTab().getId();
    if (this.tabShown.equalsIgnoreCase("tabNetCenter")) {
      //
    } else if (this.tabShown.equalsIgnoreCase("tabAdrMain")) {
      //
    } else if (this.tabShown.equalsIgnoreCase("tabGeoLoc")) {
      if (this.geoData == null) {
        //
      }

    }

  }

  public String getTabShown() {
    return this.tabShown;
  }

  public boolean isNewAdrMainEnabled() {
    if (this.tabShown.equalsIgnoreCase("tabAdrMain") && this.netCenter.getNetCenterPK().getZoneid() == this.zoneid) {
      return true;
    }
    return false;
  }

  public void onNewAdrMain(ActionEvent evt) {
    Log.fine(CenterViewControl.class.getName() + ":onNewAdrMain():UIComponent=" + evt.getComponent());
    this.adrMain = new AdrMain();
    this.netCenter.setAdrid(null);
  }

  /**
   * Get lon/lat from address or try to get it from geolocation DB.
   *
   * @param adrid
   * @return
   */
  public String getGeoLocation(Integer adrid) {
    Log.fine(CenterViewControl.class.getName() + ":getGeoLocation(" + adrid + ")");
    if (adrid == null) {
      return null;
    }
    //get location from address (if any)
    if (this.adrMain == null || this.adrMain.getAdrid() == null || !this.adrMain.getAdrid().equals(adrid)) {
      //adrMain not yet initialized or we are requesting from rowtoggler ...
      this.adrMain = ejbAdrMainAdmin.readByAdrId(adrid);
    }

    //use full data set if any
    GeoLocation gl = null;
    if (this.adrMain != null) {
      if (this.adrMain.getGeoLocation() != null) {
        try {
          gl = (GeoLocation) DeSerializer.deserialize(this.adrMain.getGeoLocation());
          gl.setData(null); //remove due to GJson to JSON parsing error
        } catch (ObjectHandlerException ex) {
          Logger.getLogger(CenterViewControl.class.getName()).log(Level.WARNING, ex.getMessage());
        }
      }
      if (gl == null && this.adrMain.getGeolat() != null && this.adrMain.getGeolng() != null) {
        //old style just lon and lat - generate GeoLoc dataset
        gl = new GeoLocation(null, "way", this.adrMain.getGeolng(), this.adrMain.getGeolat());
      }
    }
    //TODO show country based on Locale
    return gl != null ? GJson.toJson(gl) : "";
  }

  public void clearGeoLocation(Integer adrid) {
    Log.fine(CenterViewControl.class.getName() + ":clearGeoLocation(" + adrid + ")");
    if (this.adrMain == null || this.adrMain.getAdrid() == null || !this.adrMain.getAdrid().equals(adrid)) {
      this.adrMain = ejbAdrMainAdmin.readByAdrId(adrid);
    }
    if (this.adrMain != null) {
      Log.finer(CenterViewControl.class.getName() + ":clearGeoLocation():" + this.adrMain);
      this.geoData = null;
      this.adrMain.setGeoLocation(null);
      this.adrMain.setGeolat(null);
      this.adrMain.setGeolng(null);
      ejbAdrMainAdmin.save(this.adrMain);
    }
  }

  public void searchGeoLocation(Integer adrid) {
    Log.fine(CenterViewControl.class.getName() + ":searchGeoLocation(" + adrid + ")");
    if (this.adrMain == null || this.adrMain.getAdrid() == null || !this.adrMain.getAdrid().equals(adrid)) {
      this.adrMain = ejbAdrMainAdmin.readByAdrId(adrid);
    }
    if (this.adrMain != null && this.adrMain.getCity() != null) {
      Log.finer(CenterViewControl.class.getName() + ":searchGeoLocation():" + this.adrMain);

      try {
        //get data from Address otherwise use the center of Germany ;)
        String osmurl = "http://nominatim.openstreetmap.org/search/";
        String search = "?format=json&addressdetails=0&limit=1";
        //String charset = "UTF-8";
        //url = new URL(osmurl + URLEncoder.encode(adrMain.getStreet()+" "+adrMain.getCity(), charset).replaceAll("\\+","%20") + search);
        //Log.info("url=" + url.toString());
        //Nominatim accepts " " but not URL encoding
        String q = (StringUtils.isNotBlank(adrMain.getHN()) ? adrMain.getHN() + " " : "");
        q += (StringUtils.isNotBlank(adrMain.getStreet()) ? adrMain.getStreet() + " " : "") + adrMain.getCity();
        String s = URLEncoder.encode(q, "UTF-8").replaceAll("\\+", "%20");
        String surl = osmurl + s + search;
        Log.finer(CenterViewControl.class.getName() + ":searchGeoLocation():" + surl);
        JSONObject json = readJsonFromUrl(surl);
        if (json == null) {
          this.geoData = null;
          this.adrMain.setGeoLocation(null);
          this.adrMain.setGeolat(null);
          this.adrMain.setGeolng(null);
          return;
        }
        Log.finer(CenterViewControl.class.getName() + ":searchGeoLocation():\n" + json.toString());
        this.adrMain.setGeolat(Double.parseDouble("" + json.get("lat")));
        this.adrMain.setGeolng(Double.parseDouble("" + json.get("lon")));
        //prepare GeoLocation object
        this.geoData = new GeoLocation(null, this.adrMain.getGeolng(), this.adrMain.getGeolat());
        //store raw data (the seach result)
        this.geoData.setData(json.toString());
        //update AdrMain
        this.adrMain.setGeoLocation(DeSerializer.serialize(this.geoData));
        //OLD this.adrMain.setGeoLocation(DeSerializer.serialize(json.toString()));
        ejbAdrMainAdmin.save(this.adrMain);

        //RequestContext rctx = RequestContext.getCurrentInstance();
        //rctx.update("frmViewCenter:tvDlgEditCenter:tabGeoLoc");
      } catch (JSONException | NumberFormatException | ObjectHandlerException | UnsupportedEncodingException ex) {
        Logger.getLogger(CenterViewControl.class.getName()).log(Level.WARNING, ex.getMessage());
      }
    }
  }

  public String viewAddress(Integer adrid) {
    AdrMain a = ejbAdrMainAdmin.readByAdrId(adrid);
    if (a != null) {
      StringBuilder sb = new StringBuilder();
      sb.append(a.getTitle()).append("<br/>");
      sb.append(a.getAdr1()).append("<br/>");
      sb.append(a.getAdr2()).append("<br/>");
      sb.append(a.getAdr3()).append("<br/>");
      sb.append(a.getStreet()).append(" ").append(a.getHN()).append("<br/>");
      sb.append(a.getZip()).append(" ").append(a.getCity()).append("<br/>");
      return sb.toString();
    }
    return "- no address -";
  }

  /**
   * Get 'disabled' property of 'add' center button.
   * <p>
   * 'disable' function if it is not the local zone we are working with. To get
   * a list of centers of a zone we should unse the zone view and start a
   * messaging request to the zone host.
   * </p>
   *
   * @return
   */
  public boolean disabledAddCenter() {
    if (netZones != null) {
      return netZones.getZoneid() != moduleCtrl.getLocalZoneID();
    }
    return false;
  }

  /**
   * On loading the view.
   *
   * Or in GET action method (e.g. <f:viewAction action>).
   */
  public void onload() {
    Log.fine(CenterViewControl.class.getName() + ":onLoad()");

  }

  public List<NetCenter> getListCenters() {
    Log.fine(CenterViewControl.class.getName() + ":getListCenters()");
    netZones = sessionCtrl.getZoneObject();
    if (netZones != null) {
      Log.finer(CenterViewControl.class.getName() + ":getListCenters():" + netZones);
      lstNetCenter = ejbCenterAdmin.listCentersByZoneId(netZones.getZoneid());
    } else {
      Log.finer(CenterViewControl.class.getName() + ":getListCenters():no zone as filter selected");
      lstNetCenter = ejbCenterAdmin.listCenters();
    }
    if (lstNetCenter == null || lstNetCenter.isEmpty()) {
      //should not empty, there are a lot of conditions to fill the list
      if (netZones != null) {
        Log.finer(CenterViewControl.class.getName() + ":getListCenters():zid=" + netZones.getZoneid());
        lstNetCenter = ejbCenterAdmin.listCentersByZoneId(netZones.getZoneid());
      } else {
        lstNetCenter = ejbCenterAdmin.listCenters();
      }
    }
    return lstNetCenter;
  }

  public void filterListener(FilterEvent filterEvent) {
    Log.fine(CenterViewControl.class.getName() + ":filterListener():filterEvent component=" + filterEvent.getComponent());
    Map<String, Object> listFilters = filterEvent.getFilters();
    StringBuilder sb = new StringBuilder();
    for (Map.Entry kv : listFilters.entrySet()) {
      String key = (String) kv.getKey();
      String value;
      if (kv.getValue() instanceof String[]) {
        value = Arrays.toString((String[]) kv.getValue());
      } else {
        //value = kv.getValue().getClass().getName();
        value = kv.getValue().toString();
      }
      sb.append(key + "=" + value + "<br/>\n");
    }
    Log.finer(CenterViewControl.class.getName() + ":filterListener():kv of getFilters():" + sb.toString());
    //tring to get filter from DataTable
    DataTable dataTable = (DataTable) filterEvent.getSource();
    List l = dataTable.getFilteredValue();
    Log.finer(CenterViewControl.class.getName() + ":filterListener():List of dataTable.getFilteredValue():" + l);

    if (listFilters.containsKey("globalFilter") && StringUtils.isNotBlank((String) listFilters.get("globalFilter"))) {
      Log.finer(CenterViewControl.class.getName() + ":filterListener():globalFilter=" + listFilters.get("globalFilter"));
      //Map<String, String> params = new HashMap<>();
      //params.put("name", "Test");
      String flt = (String) listFilters.get("globalFilter");
      lstNetCenter = ejbCenterAdmin.listCentersByName(flt);
      lstNetCenterFiltered = ejbCenterAdmin.listCentersByName(flt);
      sb.append(lstNetCenter.size() + " records filtered<br/>\n");
    } else if (listFilters.containsKey("netCenterPK.zoneid")) {
      String[] fltZoneId = (String[]) listFilters.get("netCenterPK.zoneid");
      if (fltZoneId.length > 0) {
        //TODO filter by all selected zone ids
        //lstNetCenter = ejbCenterAdmin.listCentersByZoneId(Integer.parseInt(fltZoneId[0]));
        lstNetCenterFiltered = ejbCenterAdmin.listCentersByZoneId(Integer.parseInt(fltZoneId[0]));
        sb.append("records filtered by " + fltZoneId[0] + "<br/>\n");
      } else {
        lstNetCenter = ejbCenterAdmin.listCenters();
        lstNetCenterFiltered = null;
        sb.append("no records filtered<br/>\n");
      }
    } else {
      lstNetCenter = ejbCenterAdmin.listCenters();
      lstNetCenterFiltered = null;
      sb.append("no records filtered<br/>\n");
    }
    FacesMessage msg = new FacesMessage("Filter", sb.toString());
    FacesContext.getCurrentInstance().addMessage(null, msg);
  }

  /**
   * @return the lstNetCenterFiltered
   */
  public List<NetCenter> getLstNetCenterFiltered() {
    Log.fine(CenterViewControl.class.getName() + ":getLstNetCenterFiltered():" + (lstNetCenterFiltered != null ? lstNetCenterFiltered.toString() : "null"));
    return lstNetCenterFiltered;
  }

  /**
   * @param lstNetCenterFiltered the lstNetCenterFiltered to set
   */
  public void setLstNetCenterFiltered(List<NetCenter> lstNetCenterFiltered) {
    Log.fine(CenterViewControl.class.getName() + ":setLstNetCenterFiltered():" + (lstNetCenterFiltered != null ? lstNetCenterFiltered.toString() : "null"));
    //this.lstNetCenterFiltered = lstNetCenterFiltered;
  }

  //============================================= methods of actions etc.
  public String doSaveNetCenter() {
    Log.fine(CenterViewControl.class.getName() + ":doSaveNetCenter()");
    //update geo lon, lat from fields - the user may have corrected them
    if (this.adrMain != null) {
      try {
        GeoLocation gl = (GeoLocation) DeSerializer.deserialize(this.adrMain.getGeoLocation());
        gl.setLat(this.adrMain.getGeolat());
        gl.setLon(this.adrMain.getGeolng());
        this.adrMain.setGeoLocation(DeSerializer.serialize(gl));
      } catch (ObjectHandlerException ex) {
        Logger.getLogger(CenterViewControl.class.getName()).log(Level.WARNING, ex.getMessage());
      }
    }
    Integer adrid = ejbCenterAdmin.saveAdrMain(this.adrMain);
    if (adrid != null) {
      this.netCenter.setAdrid(this.adrMain.getAdrid());
    }
    NetCenterPK pk = ejbCenterAdmin.saveNetCenter(this.netCenter);
    Log.finer(CenterViewControl.class.getName() + ":doSaveNetCenter():PK=" + pk.toString());
    if (!lstNetCenter.contains(this.netCenter)) {
      lstNetCenter.add(this.netCenter);
    }
    return null;
  }

  public void mnuFltZone(int zid) {
    Log.fine(CenterViewControl.class.getName() + ":mnuFltZone():" + zid);
    if (zid < 0) {
      this.netZones = null;
      sessionCtrl.setZoneObject(null);
      this.lstNetCenter = ejbCenterAdmin.listCenters();
    } else {
      this.netZones = ejbZoneAdmin.readNetZonesByID(zid);
      sessionCtrl.setZoneObject(this.netZones);
      this.lstNetCenter = ejbCenterAdmin.listCentersByZoneId(zid);
    }
  }

  public String doDelete() {
    //NOT ALLOWED by convention
    //if (lstNetCenter.contains(netCenter)) {
    //  lstNetCenter.remove(netCenter);
    //}

    //TODO set status to inactiv, set end date
    return null;
  }

  public String showCenterInfo() {
    Log.fine(CenterViewControl.class.getName() + ":showCenterInfo():" + netCenter.toString());
    return "intern";
  }

  /**
   * List all centers registered at the zone this zone host is configured for.
   *
   * @return
   */
  public List<NetCenter> getRegisteredCenters() {
    Log.fine(CenterViewControl.class.getName() + ":getRegisteredCenters()");
    return ejbCenterAdmin.listCentersByZoneId(moduleCtrl.getLocalZoneID());
  }

  /**
   * @return the zoneid
   */
  public int getZoneId() {
    if (zoneid < 0) {
      zoneid = moduleCtrl.getLocalZoneID();
    }
    return zoneid;
  }

  /**
   * @param zoneid the zoneid to set
   */
  public void setZoneId(int zoneid) {
    this.zoneid = zoneid;
  }

  public String doClose() {
    sessionCtrl.setPage("/inc/desktop.xhtml");
    return "intern";
  }

  public void onClose(CloseEvent event) {
    sessionCtrl.setPage("/inc/desktop.xhtml");
  }

  /**
   * @return the centerid
   */
  public int getCenterId() {
    return centerid;
  }

  /**
   * @param centerid the centerid to set
   */
  public void setCenterId(int centerid) {
    this.centerid = centerid;
  }

  public void doPrepareNewCenterObject() {
    Log.fine(CenterViewControl.class.getName() + ":doPrepareNewCenterObject():ZID/CID:" + zoneid + "/" + centerid);
    centerid = -1; //reset to "new"
    netCenter = new NetCenter(-1, zoneid);
    adrMain = new AdrMain();
    action = "add";
    //return "intern";
    //return "pm:edit?transition=flip";
  }

  /**
   * @return the lstNetZones
   */
  public List<NetZones> getLstNetZones() {
    return lstNetZones;
  }

  private static String readAll(Reader rd) throws IOException {
    StringBuilder sb = new StringBuilder();
    int cp;
    while ((cp = rd.read()) != -1) {
      sb.append((char) cp);
    }
    return sb.toString();
  }

  public static JSONObject readJsonFromUrl(String url) {
    JSONObject json = null;
    InputStream is;
    try {
      is = new URL(url).openStream();
      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
      String resultJson = readAll(rd);
      resultJson = StringUtils.removeStart(resultJson, "[");
      resultJson = StringUtils.removeEnd(resultJson, "]");
      Log.finer("readJsonFromUrl():\n" + resultJson);
      json = new JSONObject(resultJson);
      is.close();
    } catch (IOException | JSONException ex) {
      Log.warning("readJsonFromUrl():" + ex.getMessage());
    }
    return json;
  }

  public boolean isAMQCon(NetCenterPK pk) {
    Log.fine(CenterViewControl.class.getName() + ":isAMQCon():Checking connection for " + pk != null ? pk.toString() : "null");
    return false;
  }
}
