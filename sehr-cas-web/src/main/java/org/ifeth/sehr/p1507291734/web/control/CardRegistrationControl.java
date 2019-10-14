/*
 * (C) 2015 MDI GmbH
 *
 * Based on an idea of Hans J Haase
 * Development Team: Hans J.
 */
package org.ifeth.sehr.p1507291734.web.control;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import org.apache.commons.lang.StringUtils;
import org.ifeth.sehr.core.exception.GenericSEHRException;
import org.ifeth.sehr.core.exception.ObjectHandlerException;
import org.ifeth.sehr.core.handler.LifeCARDObjectHandler;
import org.ifeth.sehr.core.handler.LifeCARDProcessor;
import org.ifeth.sehr.core.lib.DeSerializer;
import org.ifeth.sehr.core.objects.LifeCardItem;
import org.ifeth.sehr.core.spec.SEHRConstants;
import org.ifeth.sehr.intrasec.entities.LcCad;
import org.ifeth.sehr.intrasec.entities.LcMain;
import org.ifeth.sehr.intrasec.entities.NetCenter;
import org.ifeth.sehr.intrasec.entities.NetZones;
import org.ifeth.sehr.intrasec.entities.PrsMain;
import org.ifeth.sehr.intrasec.entities.UsrMain;
import org.ifeth.sehr.p1507291734.ejb.CenterAdmin;
import org.ifeth.sehr.p1507291734.ejb.LifeCARDAdmin;
import org.ifeth.sehr.p1507291734.ejb.UserAdmin;
import org.ifeth.sehr.p1507291734.ejb.ZoneAdmin;
import org.primefaces.event.CloseEvent;
import org.primefaces.event.FlowEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 * Control of the wizard 'CardRegistration'.
 *
 * @author HansJ
 */
@Named(value = "cardRegCtrl")
@ViewScoped
public class CardRegistrationControl implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger Log = Logger.getLogger("org.ifeth.p1507291734.web");
  @EJB
  private ZoneAdmin ejbZoneAdmin;
  @EJB
  private CenterAdmin ejbCenterAdmin;
  @EJB
  private UserAdmin ejbUserAdmin;
  @EJB
  private LifeCARDAdmin ejbLifeCARDAdmin;
  //@Inject
  //private MobileControl mobileCtrl;
  @Inject
  private SessionControl sessionCtrl;
  @Inject
  private ModuleControl moduleCtrl;
  
  private int usrid;
  private int svid; //recid
  private NetZones netZones; //the zone the holder is assigned to
  private NetCenter netCenter; //the zone the holder is assigned to
  private PrsMain prsMain; //a person (the basic object)
  private UsrMain usrMain; //a LC holder has an account to manage his card
  private LcMain lcMain; //the administrative record (containing the LC item)
  private LifeCardItem lcItem; //the card data fpr printing and LcCad binding
  private LcCad lcCad;
  private List<String> lHostnames;
  private List<KeyValueItem> listIdentTypes;
  private KeyValueItem keyValueItem;
  private Map<Short, String> mIdentTypes;
  private boolean skip;

  //============================================= constructors, initialization
  public CardRegistrationControl() {
  }

  @PostConstruct
  public void init() {
    listIdentTypes = new ArrayList<>();
//    mIdentTypes = LifeCARDObjectHandler.listIdentConstants();
    mIdentTypes = listIdentConstants();
    for (Map.Entry<Short, String> entry : mIdentTypes.entrySet()) {
      listIdentTypes.add(new KeyValueItem(entry.getKey(), entry.getValue()));
    }

    lHostnames = new ArrayList();
    //see SEHR LDAP specification; by convention the host the card has 
    //been registered and the card is managed within a health network
    String hostname = "n/a";
    lHostnames.add(hostname); //default
    try {
      hostname = InetAddress.getLocalHost().getHostName();
      lHostnames.add(hostname);
    } catch (UnknownHostException ex) {
      Logger.getLogger(CardRegistrationControl.class.getName()).log(Level.WARNING, null, ex);
    }
    hostname = (String) moduleCtrl.getProperty("SEHRHost");
    if (StringUtils.isNotBlank(hostname) && !lHostnames.contains(hostname)) {
      lHostnames.add(hostname);
    }
    //create default objects
    usrMain = new UsrMain();
    keyValueItem = new KeyValueItem((short) 0, "none");
    lcCad = new LcCad();
    //TODO get AppToken from NetServices by query
    //currently there are only a few application providers for the LifeCARD
    lcCad.setCai("p1312270921"); //JMedViewer
    //lcItem = new LifeCardItem(); //prepared on tab selection

    //check for editing entry or creating one
    if (sessionCtrl.getLcMain() != null) {
      lcMain = sessionCtrl.getLcMain();
    } else {
      lcMain = new LcMain();
      lcMain.setEHNDomain(moduleCtrl.getDomain());

      lcMain.setHostid(lHostnames.get(lHostnames.size() - 1));
      lcMain.setCountry(StringUtils.upperCase(moduleCtrl.getCountryCode()));
      if (moduleCtrl.getLocalZoneID() > 0) {
        lcMain.setZoneid(moduleCtrl.getLocalZoneID());
      }

    }
  }

  /**
   * @return the prsMain
   */
  public PrsMain getPrsMain() {
    return prsMain;
  }

  /**
   * @param prsMain the prsMain to set
   */
  public void setPrsMain(PrsMain prsMain) {
    this.prsMain = prsMain;
  }

  //============================================= getter/setter
  public UsrMain getUsrMain() {
    return usrMain;
  }

  public void setUsrMain(UsrMain object) {
    this.usrMain = object;
  }

  /**
   * @return the netZones
   */
  public NetZones getNetZones() {
    if (this.lcMain != null && this.lcMain.getZoneid() != null) {
      if (netZones == null || !netZones.getZoneid().equals(this.lcMain.getZoneid())) {
        netZones = ejbZoneAdmin.readNetZonesByID(this.lcMain.getZoneid());
      }
    }
    return netZones;
  }

  /**
   * @param netZones the netZones to set
   */
  public void setNetZones(NetZones netZones) {
    this.netZones = netZones;
    if (this.lcMain != null && netZones != null) {
      this.lcMain.setZoneid(netZones.getZoneid());
    }
  }

  /**
   * @return the netCenter
   */
  public NetCenter getNetCenter() {
    if (this.lcMain != null && this.lcMain.getCenterid() != null) {
      if (netCenter == null || !netCenter.getNetCenterPK().getCenterid().equals(this.lcMain.getCenterid())) {
        netCenter = ejbCenterAdmin.readNetCenterByID(this.lcMain.getCenterid());
      }
    }
    return netCenter;
  }

  /**
   * @param netCenter the netCenter to set
   */
  public void setNetCenter(NetCenter netCenter) {
    this.netCenter = netCenter;
    if (this.lcMain != null && netCenter != null) {
      this.lcMain.setCenterid(netCenter.getNetCenterPK().getCenterid());
    }
  }

  public LcMain getLcMain() {
    Log.info(CardRegistrationControl.class.getName() + ":getLcMain():" + lcMain.toString());
    return lcMain;
  }

  public void setLcMain(LcMain object) {
    Log.info(CardRegistrationControl.class.getName() + ":setLcMain():" + object.toString());
    this.lcMain = object;
  }

  /**
   * @return the LifeCardItem object
   */
  public LifeCardItem getLcItem() {
    //if (lcItem == null) {
    //  lcItem = new LifeCardItem();
    //}
    return lcItem;
  }

  /**
   * @param lcItem the lcItem to set
   */
  public void setLcItem(LifeCardItem lcItem) {
    this.lcItem = lcItem;
  }

  /**
   * @return the lcCad
   */
  public LcCad getLcCad() {
    return lcCad;
  }

  /**
   * @param lcCad the lcCad to set
   */
  public void setLcCad(LcCad lcCad) {
    this.lcCad = lcCad;
  }

  /**
   * @return the keyValueItem
   */
  public KeyValueItem getKeyValueItem() {
    if (keyValueItem == null) {
      if (lcItem != null && listIdentConstants().containsKey(lcItem.getIdentType())) {
        keyValueItem = new KeyValueItem(lcItem.getIdentType(), listIdentConstants().get(lcItem.getIdentType()));
      }
    }
    return keyValueItem;
  }

  /**
   * @param keyValueItem the keyValueItem to set
   */
  public void setKeyValueItem(KeyValueItem keyValueItem) {
    this.keyValueItem = keyValueItem;
    if (keyValueItem != null) {
      lcItem.setIdentType(keyValueItem.getKey());
    }
  }

  public String getCardData(LcMain lcMain) {
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

  public void handleZoneSelect(SelectEvent event) {
    Object item = event.getObject();
    //+++ zone already set... 
    this.netZones = (NetZones) item;
    //TODO show message if selected zone differs from configured one
    FacesMessage msg = new FacesMessage("Selected Zone", "" + this.netZones.toString());
    FacesContext.getCurrentInstance().addMessage(null, msg);
    this.netZones = (NetZones) item;
  }

  public void handleCenterSelect(SelectEvent event) {
    Object item = event.getObject();
    //+++ zone already set... 
    this.netCenter = (NetCenter) item;
    //FacesMessage msg = new FacesMessage("Selected Center", "" + this.netCenter);
    //FacesContext.getCurrentInstance().addMessage(null, msg);
  }

  /**
   * Creates the card image preview with the data of the holder.
   *
   * @return
   */
  public StreamedContent getCardImage() {
//    String cardTemplate = "LifeCard-V8.png";
//    if (lcMain == null) {
//      return null;
//    }
    FacesContext fctx = FacesContext.getCurrentInstance();
    if (fctx.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
      // In this phase we're just rendering the HTML 
      // We return a stub StreamedContent so that it will generate right URL.
      return new DefaultStreamedContent();
    } else {
      ServletContext sctx = (ServletContext) fctx.getExternalContext().getContext();
      if (lcItem == null) {
        return null;
      }
      File fTemplate;
      File fImage;
      try {
        //item = (LifeCardItem) DeSerializer.deserialize(lcMain.getItem());
        String fPathImg = sctx.getRealPath("resources/images");
        fTemplate = new File(fPathImg + "/LifeCard-V8.png");
        Log.fine(CardRegistrationControl.class.getName() + ":getCardImage():tplImageFile=" + fTemplate.getAbsolutePath());
        String pathTmp = sctx.getRealPath("WEB-INF") + "/tmp";
        File fPathTmp = new File(pathTmp);
        if (!fPathTmp.exists()) {
          fPathTmp.mkdirs();
        }
        fImage = File.createTempFile("lc-", ".png", fPathTmp);
        Log.fine(CardRegistrationControl.class.getName() + ":getCardImage():tmpImageFile=" + fImage.getAbsolutePath());
        LifeCARDObjectHandler.createLifeCARDImage(fTemplate, lcItem, fImage);
        return new DefaultStreamedContent(new FileInputStream(fImage));
      } catch (Exception e) {
        Log.warning(CardRegistrationControl.class.getName() + ":getCardImage():" + e.getMessage());
        return null;
      }
    }
  }

  public String readUsrMain(int usrid) {
    UsrMain u = ejbUserAdmin.readUserByID(usrid);
    return u.toString();
  }

  public List<NetZones> cplZoneId(String query) {
    List<NetZones> l = ejbZoneAdmin.listZonesByTitle(query);
    return l;
  }

  public List<String> cplHostID(String query) {

    List<String> results = new ArrayList<>();
    for (Iterator<String> it = lHostnames.iterator(); it.hasNext();) {
      String s = it.next();
      if (s.contains(query)) {
        results.add(s);
      }
    }
    return results;
  }

  public List<NetCenter> cplCenterId(String query) {
    Map<String, String> params = new HashMap();
    if (StringUtils.isNotBlank(query)) {
      params.put("name", "%" + query.trim() + "%");
    }
    if (this.netZones != null && this.netZones.getZoneid() != null) {
      params.put("zoneid", Integer.toString(this.netZones.getZoneid()));
    }
    List<NetCenter> l = ejbCenterAdmin.listCentersByParams(params);
    return l;
  }

  //============================================= methods of actions etc.
  public void onSaveLcMain() {
    Log.fine(CardRegistrationControl.class.getName() + ":onSaveLcMain():" + lcMain.toString());
    FacesMessage msg;
    try {
      lcMain.setItem(DeSerializer.serialize(lcItem));
    } catch (ObjectHandlerException ex) {
      Logger.getLogger(CardRegistrationControl.class.getName()).log(Level.WARNING, ex.getMessage());
      msg = new FacesMessage("Error", "Error saving " + lcMain.getSurname());
      FacesContext.getCurrentInstance().addMessage(null, msg);
      return;
    }
    
    if (ejbLifeCARDAdmin.saveLcMain(lcMain)) {
      msg = new FacesMessage("Successful", "Registred Holder :" + lcMain.getSurname());
    } else {
      msg = new FacesMessage("Error", "Error saving " + lcMain.getSurname());
    }
    FacesContext.getCurrentInstance().addMessage(null, msg);
  }

  public void alCheckPatIdent(ActionEvent event) {
    //Log.fine(CardRegistrationControl.class.getName() + ":alCheckPatIdent():event: " + event);
    Log.fine(CardRegistrationControl.class.getName() + ":alCheckPatIdent():event PhaseId: " + event.getPhaseId());
    Log.fine(CardRegistrationControl.class.getName() + ":alCheckPatIdent():LcMain: " + lcMain);
    //TODO check if an EHR is already stored on the zone EHR_MAIN...
    //workflow: patient is managed by a center
    //patient (or physician) wants to have an managed care record set
    //physician register patient and managed case (on zone)
    //the a EHR entry will be created
    //after that process a card can be applied to share EHR within a zone or globally
    //TODO send pat id to zone, center to verify if EHR is available...
    //(but this is only a request, the patient should always be registered by a center first, not a zone...)
  }

  public void onCancelLcMain() {
    Log.fine(CardRegistrationControl.class.getName() + ":onCancelLcMain():" + lcMain.toString());
    FacesMessage msg = new FacesMessage("Cancelled", "Cancellation of " + lcMain.getSurname());
    FacesContext.getCurrentInstance().addMessage(null, msg);
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

  public List<KeyValueItem> getListIdentTypes() {
    return listIdentTypes;
  }

  public String actionClose() {
    sessionCtrl.setPage("/inc/desktop.xhtml");
    return "intern";
  }

  public void onClose(CloseEvent event) {
    sessionCtrl.setPage("/inc/desktop.xhtml");
  }

  public boolean isSkip() {
    return skip;
  }

  public void setSkip(boolean skip) {
    this.skip = skip;
  }

  public String onFlowProcess(FlowEvent event) {
    Log.fine(CardRegistrationControl.class.getName() + ":onFlowProcess():oldStep=" + event.getOldStep());
    Log.fine(CardRegistrationControl.class.getName() + ":onFlowProcess():newStep=" + event.getNewStep());
    if (event.getNewStep().equalsIgnoreCase("tbCardPrinting")) {
      if (lcItem == null) {
        lcItem = new LifeCardItem();
        //there is no plastic card known at this point to assign
        lcItem.setLcid(0);
        lcItem.setSurname(lcMain.getSurname());
        lcItem.setFirstname(lcMain.getFirstname());
        lcItem.setMiddle(lcMain.getMiddle());
        lcItem.setTitle(lcMain.getTitle());
        lcItem.setDoB(lcMain.getDob());
        //these data are not part of the DB record but only of a written statement
        lcItem.setProblem(null);
        lcItem.setEMConFQName(null);
        lcItem.setEMConPhone(null);
        //In any case invalidate existing status in LcMain on generating a 
        //printed card this way...
        lcItem.setSts(SEHRConstants.LifeCARD_STS_NEW);

        String ctry = lcMain.getCountry();
        Integer zid = lcMain.getZoneid();
        Integer cid = lcMain.getCenterid();
        Integer pid = lcMain.getPatid();
        try {
          lcItem.setLcPrintnumber(LifeCARDObjectHandler.buildNumber(lcItem, ctry != null ? ctry : "de", zid, cid, pid));
        } catch (GenericSEHRException ex) {
          Logger.getLogger(LifeCARDObjectHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
    if (skip) {
      skip = false;   //reset in case user goes back
      return "tbConfirm";
    } else {
      return event.getNewStep();
    }
  }

  //TODO use LifeCARDObjectHandler.listIdentConstants()
  private Map<Short, String> listIdentConstants() {
    Map<Short, String> c = new HashMap<>();
    c.put(SEHRConstants.LifeCARD_IDENT_NONE, "none");
    c.put(SEHRConstants.LifeCARD_IDENT_DRIVERLIC, "Driver Licence");
    c.put(SEHRConstants.LifeCARD_IDENT_OTHER, "Other");
    c.put(SEHRConstants.LifeCARD_IDENT_PASSPORT, "Passport");
    return c;
  }
}
