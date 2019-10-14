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
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import org.apache.commons.lang.StringUtils;
import org.ifeth.sehr.p1507291734.web.beans.MQClients;
import org.ifeth.sehr.p1507291734.web.listener.AdvConnectionListener;
import org.ifeth.sehr.intrasec.entities.NetCenter;
import org.ifeth.sehr.p1507291734.ejb.CenterAdmin;

/**
 * Control of mobile pages to view and manage facilities of a zone.
 *
 * @author HansJ
 */
@Named(value = "mobCenterCtrl")
@SessionScoped
public class MobileCenterControl implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger Log = Logger.getLogger("org.ifeth.p1507291734.web");
  private static final String MONITORCFG = "/WEB-INF/ZoneAdv.map";

  @EJB
  private ZoneAdmin zoneAdmin;
  @EJB
  private CenterAdmin centerAdmin;
  @Inject
  private ModuleControl moduleCtrl;
  @Inject
  private MobileControl mobileCtrl;

  private int zoneid=-1; //selected zone we are working with; -1 = none
  private int centerid;
  private NetCenter netCenter;
  private List<NetCenter> lstNetCenter;

  //============================================= constructors, initialization
  public MobileCenterControl() {
  }

  @PostConstruct
  public void init() {
    netCenter = new NetCenter();
    lstNetCenter = new ArrayList<>();
  }

  //============================================= getter/setter
  public NetCenter getCenterObject() {
    return netCenter;
  }

  public void setCenterObject(NetCenter object) {
    this.netCenter = object;
  }

  /**
   * Returns a list of known (currently) bound centers via AMQ.
   *
   * @return
   */
  public List<NetCenter> getListBoundCenters() {
    Log.finer(MobileCenterControl.class.getName() + ":getListBoundCenters()");
    ServletContext ctx = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();

    AdvConnectionListener adv = (AdvConnectionListener) ctx.getAttribute(AdvConnectionListener.class.getName());
    if (adv != null) {
      moduleCtrl.listConnectedCenter().clear();
      for (MQClients c : adv.getMQClients().values()) {
        String s = c.getClientId();
        if (s != null) {
          if (s.toLowerCase().startsWith("centerid")) {
            //int i = s.toLowerCase().indexOf("centerid");
            String cid = s.substring(9, 9 + 7);
            //TODO get more details of a connected center
            NetCenter nc = centerAdmin.readNetCenterByID(Integer.parseInt(cid));
            if (nc == null) {
              int unknowncid = Integer.parseInt(cid);
              nc = new NetCenter(unknowncid,0);
              nc.setName("!unknown/unregistered!");
            }
            //TODO compare a keycode against registered center in the local DB
            moduleCtrl.addConnectedCenter(nc);
          }
        }
      }
    }

    lstNetCenter = moduleCtrl.listConnectedCenter();
    return lstNetCenter;
  }

  //============================================= methods of actions etc.
  public String doSaveCenterObject() {
    Log.finer(MobileCenterControl.class.getName() + ":doSaveCenterObject()");

    return "pm:vwCenter";//?transition=flip";
  }

  public String doPrepareNewCenterObject() {
    Log.fine(MobileCenterControl.class.getName() + ":doPrepareNewCenterObject()");
    netCenter = new NetCenter();
    mobileCtrl.setXhtmlPage("/mobile/index?ui-page=vwCenterEdit");
    mobileCtrl.setViewPage("pm:vwCenterEdit");
    return "/mobile/index?ui-page=vwCenterEdit";
  }

  public String showCenterInfo() {
    Log.info(MobileCenterControl.class.getName() + ":showCenterInfo():" + netCenter.toString());
    mobileCtrl.setXhtmlPage("/mobile/centerList?ui-page=vwCenterInfo");
    mobileCtrl.setViewPage("pm:vwCenterInfo");
    return "/mobile/centerList?ui-page=vwCenterInfo";
  }

  /**
   * Return a list of centers of a given zone by ZoneID.
   *
   * @return
   */
  public List<NetCenter> getListRegisteredCenters() {
    Log.info(MobileCenterControl.class.getName() + ":getRegisteredCenters():Local ZoneID is:" + moduleCtrl.getLocalZoneID());
    if (zoneid < 0) {
      zoneid = moduleCtrl.getLocalZoneID();
    }
    return zoneAdmin.listActiveCenters(zoneid);
  }

  /**
   * @return the zoneid
   */
  public int getCenterId() {
    return centerid;
  }

  /**
   * @param zoneid the zoneid to set
   */
  public void setCenterId(int cid) {
    this.centerid = cid;
  }

  public String checkLoginStatus() {
    Log.finest(MobileCenterControl.class.getName() + ":checkLoginStatus():Username=" + mobileCtrl.getUsername());
    if (StringUtils.isBlank(mobileCtrl.getUsername())) {
      return "/mobile/login";
    }
    //TODO implement switch to last page that has been visited...
    return "pm:vwCenterOfZoneList";
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
}
