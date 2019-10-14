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
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import org.apache.commons.lang.StringUtils;
import org.ifeth.sehr.core.lib.DeSerializer;
import org.ifeth.sehr.core.objects.LifeCardItem;
import org.ifeth.sehr.intrasec.entities.LcCad;
import org.ifeth.sehr.intrasec.entities.LcCadPK;
import org.ifeth.sehr.intrasec.entities.LcMain;
import org.ifeth.sehr.intrasec.entities.PrsMain;
import org.ifeth.sehr.intrasec.entities.UsrMain;
import org.ifeth.sehr.p1507291734.ejb.LifeCARDAdmin;
import org.ifeth.sehr.p1507291734.ejb.UserAdmin;
import org.primefaces.event.CloseEvent;

/**
 * Control of pages handling LifeCARD holder entries.
 *
 * @author HansJ
 */
@Named(value = "cardsDeliveredViewCtrl")
@RequestScoped
public class CardsDeliveredViewControl implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger Log = Logger.getLogger("org.ifeth.p1507291734.web");
  @EJB
  private UserAdmin ejbUserAdmin;
  @EJB
  private LifeCARDAdmin ejbLifeCARDAdmin;
  //@Inject
  //private MobileControl mobileCtrl;
  @Inject
  private SessionControl sessionCtrl;
  private int usrid;
  private int svid; //recid
  private UsrMain usrMain; //a user (a LC holder is also a user by convention)
  private LcMain lcMain; //the administrative record (containing the LC item)
  private LcCad lcCad; //the administrative record of produced cards
  private List<PrsMain> lstPrsMain;
  private List<LcMain> lstLcMain;
  private List<LcCad> lstLcCad;
  private short fltLcStatus = -1; //all
  private boolean fltLifeCARD = false;

  //============================================= constructors, initialization
  public CardsDeliveredViewControl() {
  }

  @PostConstruct
  public void init() {
    usrMain = new UsrMain();
    lstPrsMain = new ArrayList<>();
    lcMain = new LcMain();
    if (sessionCtrl.getLcMain() != null) {
      lcMain = sessionCtrl.getLcMain();
    }
    lstLcMain = new ArrayList<>();
    lcCad = new LcCad();
    lstLcCad = new ArrayList<>();
  }

  //============================================= getter/setter
  public UsrMain getUserObject() {
    return usrMain;
  }

  public void setUserObject(UsrMain object) {
    this.usrMain = object;
  }

  public LcMain getLcMainObject() {
    Log.info(CardsDeliveredViewControl.class.getName() + ":getLcMainObject():" + lcMain.toString());
    return lcMain;
  }

  public String getHolder() {
    Log.info(CardsDeliveredViewControl.class.getName() + ":getHolder():" + lcMain.toString());
    return lcMain.toString();
  }

  public void setLcMainObject(LcMain object) {
    Log.info(CardsDeliveredViewControl.class.getName() + ":setLcMainObject():" + object.toString());
    this.lcMain = object;
  }

  public LcCad getLcCadObject() {
    Log.info(CardsDeliveredViewControl.class.getName() + ":getLcCadObject():" + lcMain.toString());
    return lcCad;
  }

  public void setLcCadObject(LcCad object) {
    Log.info(CardsDeliveredViewControl.class.getName() + ":setLcCadObject():" + object.toString());
    this.lcCad = object;
  }

  public List<PrsMain> getListPatients() {

    if (lstPrsMain == null || lstPrsMain.isEmpty()) {
      lstPrsMain = ejbLifeCARDAdmin.listPatients();
    }

    return lstPrsMain;
  }

  public List<LcCad> getListLcCad() {
    if (lcMain == null) {
      return null;
    }
    if (lstLcCad == null || lstLcCad.isEmpty()) {
      Log.info(CardsDeliveredViewControl.class.getName() + ":getListLcCad():Updating list...");
      lstLcCad = ejbLifeCARDAdmin.listProducedCardsByLcMainID(lcMain.getLcid());
    }
    return lstLcCad;
  }

  public String getLifeCARDItem(LcMain lcMain) {
    if (lcMain == null) {
      return null;
    }
    LifeCardItem item;
    try {
      item = (LifeCardItem) DeSerializer.deserialize(lcMain.getItem());
    } catch (Exception e) {
      return null;
    }
    return item.toString();
  }

  public String getCardStatus(LcCadPK pk) {
    StringBuilder sb = new StringBuilder();
    //FacesContext fctx = FacesContext.getCurrentInstance();

    if (pk == null) {
      return "Error, card sequence number and LifeCard administrative record not set.";
    }
    //System.out.println("pk=" + pk.toString());
    int lcid = pk.getLcid();
    LifeCardItem item = ejbLifeCARDAdmin.getLifeCardItemByLcId(lcid);
    if (item == null) {
      Log.fine(CardsDeliveredViewControl.class.getName() + ":getCardStatus():No card item stored. Card already produced?");
      return "Error, No card item found (the template for printing).";
    }
    //ServletContext sctx = (ServletContext) fctx.getExternalContext().getContext();

    sb.append(item.toString());
    return sb.toString();
  }

  public String getCardInfo(LcCadPK pk) {
    if (pk == null) {
      return "Error, card sequence number and LifeCard administrative record not set.";
    }
    FacesContext fctx = FacesContext.getCurrentInstance();
    //System.out.println("pk=" + pk.toString());
    //int lcid = pk.getLcid();
    //LcMain lcMain= ejbLifeCARDAdmin.getEntityByLcId(lcid);
    LcCad lcCadInfo;
    try {
      lcCadInfo = ejbLifeCARDAdmin.getLcCadByPK(pk);
    } catch (Exception e) {
      Log.warning(CardsDeliveredViewControl.class.getName() + ":getCardInfo():EJB error:" + e.getMessage());
      return "Error on getting card record.";
    }
    if (lcCadInfo == null) {
      Log.info(CardsDeliveredViewControl.class.getName() + ":getCardInfo():No card item stored. Card already produced?");
      return "Error, No administrative record found.";
    }
    ServletContext sctx = (ServletContext) fctx.getExternalContext().getContext();
    StringBuilder sb = new StringBuilder();
    //TODO I18N
    sb.append("Issued by: ").append(lcCadInfo.getCi()).append(", ");
    sb.append("Ser.No/Vendor: ").append(lcCadInfo.getCvSer()).append(lcCadInfo.getCvID()).append(", ");
    //TODO implement valdid to date in LC_CAD
    sb.append("Valid: ").append("n/a");

    return sb.toString();
  }

  //============================================= methods of actions / menues
  public void actionSaveLcCadObject() {
    Log.info(CardsDeliveredViewControl.class.getName() + ":actionSaveLcCadObject()");
    boolean isLcCadInList = false;
    for (LcCad entity : lstLcCad) {
      if (entity.getLcCadPK().equals(lcCad.getLcCadPK())) {
        isLcCadInList = true;
        break;
      }
    }
    if (!isLcCadInList) {
      Log.info(CardsDeliveredViewControl.class.getName() + ":actionSaveLcCadObject():saving LcCad");
      //TODO add to DB
      lstLcCad.add(lcCad);
    } else {
      Log.info(CardsDeliveredViewControl.class.getName() + ":actionSaveLcCadObject():updating LcCad");
      //TODO update DB
    }
    //return "intern";
  }

// +++ we are using actionListener, not action  
//  public String doSaveLcCadObject() {
//    return "intern";
//  }
  public void actionDeleteLcCadObject() {
    //delete not allowed so far, just deactivate...
    boolean isLcCadInList = false;
    for (LcCad entity : lstLcCad) {
      if (entity.getLcCadPK().equals(lcCad.getLcCadPK())) {
        isLcCadInList = true;
        break;
      }
    }
    if (isLcCadInList) {
      int i = ejbLifeCARDAdmin.processCardDeactivation(lcCad);
      //lstLcCad.remove(lcCad);
    }
  }

  public void actionListenerDeactivateLcCad(ActionEvent event) {
    Log.info(CardsDeliveredViewControl.class.getName() + ":actionListenerDeactivateLcCad():Event Source:" + event.getSource() + ", PhaseId=" + event.getPhaseId().getName());
    Object o = event.getSource();
  }

  public String actionDeactivateLcCad() {
    if (lcCad == null || lcCad.getLcCadPK() == null) {
      Log.info(CardsDeliveredViewControl.class.getName() + ":actionDeactivateLcCad():LcCad (or PK) is null");
      return null;
    }
    Log.info(CardsDeliveredViewControl.class.getName() + ":actionDeactivateLcCad():pk=" + lcCad.getLcCadPK());

    //delete not allowed so far, just deactivate...
    int i = ejbLifeCARDAdmin.processCardDeactivation(lcCad);
    FacesContext fctx = FacesContext.getCurrentInstance();
    fctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Updated...", ""));
    return null;
  }

  public String doPrepareNewLcCadObject() {
    lcCad = new LcCad();
    return "intern";
  }

  public String showLcCadInfo() {
    Log.info(CardsDeliveredViewControl.class.getName() + ":showLcCadInfo():" + lcCad.toString());
    return "intern";
  }

  public String getStatusImg() {
    String img = "Maennchen-orange16x16.png";
    if (lcMain != null) {
      if (lcMain.getSts() == 1) {
        img = "Maennchen-gruen16x16.png"; //ok, card shipped
      }
    }
    return img;
  }

  /**
   * @return the svid
   */
  public int getServiceId() {
    return svid;
  }

  /**
   * @param svid
   */
  public void setServiceId(int svid) {
    this.svid = svid;
  }

  /**
   * @return the usrid
   */
  public int getUsrid() {
    return usrid;
  }

  /**
   * @param usrid the usrid to set
   */
  public void setUsrid(int usrid) {
    this.usrid = usrid;
  }

  public String actionClose() {
    sessionCtrl.setPage("/inc/desktop.xhtml");
    return "intern";
  }

  public void onClose(CloseEvent event) {
    sessionCtrl.setPage("/inc/desktop.xhtml");
  }

  /**
   * @return the fltLifeCard
   */
  public boolean isFltLifeCard() {
    return fltLifeCARD;
  }

  /**
   * @param fltLC the fltLifeCARD to set
   */
  public void setFltLifeCard(boolean fltLC) {
    this.fltLifeCARD = fltLC;
  }

  /**
   * @param fltLC
   */
  public void btnFltLifeCard(boolean fltLC) {
//    Map<String, Object> sessMap = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
//    for (Map.Entry<String, Object> e : sessMap.entrySet()) {
//      System.out.println(e.getKey());
//    }
    fltLifeCARD = fltLC;
    if (fltLC) {
      lstLcMain = ejbLifeCARDAdmin.listCards(null); //all status
    } else {
      lstLcMain = ejbLifeCARDAdmin.listRegistrations("DE", null, null);
    }
    //System.out.println("lstLcMain size: " + lstLcMain.size());
    //return "intern"; //ActionListener, do not use this page control
  }
}
