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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
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
import org.ifeth.sehr.core.i18n.CountryCode;
import org.ifeth.sehr.core.lib.DeSerializer;
import org.ifeth.sehr.core.objects.LifeCardItem;
import org.ifeth.sehr.core.spec.SEHRConstants;
import org.ifeth.sehr.intrasec.entities.LcCad;
import org.ifeth.sehr.intrasec.entities.LcCadPK;
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
 * Control of the wizard 'Card Production (wizCardProd.xhtml)' .
 *
 * @author HansJ
 */
@Named(value = "cardProdCtrl")
@ViewScoped
public class CardProductionControl implements Serializable {

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
  private NetCenter netCenter; //the center the holder (patient) is assigned to
  private NetCenter issuer; //the center that issues cards of a zone
  private PrsMain prsMain; //the person record of the LC 
  private UsrMain usrMain; //a LC holder has an account to manage his card
  private LcMain lcMain; //the administrative record (containing the LC item)
  private LifeCardItem lcItem; //the card data fpr printing and LcCad binding
  private LcCad lcCad;
  private List<String> lHostnames;
  private List<KeyValueItem> listIdentTypes;
  private List<KeyValueItem> listStatusTypes;
  private KeyValueItem keyValueItem;
  private KeyValueItem keyValueStatus;
  private Map<Short, String> mIdentTypes;
  private boolean skip;

  //============================================= constructors, initialization
  public CardProductionControl() {
  }

  @PostConstruct
  public void init() {
    listIdentTypes = new ArrayList<>();
    //mIdentTypes = LifeCARDObjectHandler.listIdentConstants();
    mIdentTypes = listIdentConstants();
    for (Map.Entry<Short, String> entry : mIdentTypes.entrySet()) {
      listIdentTypes.add(new KeyValueItem(entry.getKey(), entry.getValue()));
    }
    listStatusTypes = new ArrayList<>();
    for (Map.Entry<Short, String> entry : listStatusConstants().entrySet()) {
      listStatusTypes.add(new KeyValueItem(entry.getKey(), entry.getValue()));
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
      Logger.getLogger(CardProductionControl.class.getName()).log(Level.WARNING, null, ex);
    }
    hostname = (String) moduleCtrl.getProperty("SEHRHost");
    if (StringUtils.isNotBlank(hostname) && !lHostnames.contains(hostname)) {
      lHostnames.add(hostname);
    }
    //create default objects
    usrMain = new UsrMain();
    keyValueItem = new KeyValueItem((short) -1, "n/a");
    lcCad = new LcCad();
    //TODO get AppToken from NetServices by query
    //currently there are only a few application providers for the LifeCARD
    lcCad.setCai("p1312270921"); //JMedViewer
    //lcItem = new LifeCardItem(); //prepared on tab selection

    //check for entry to work with
    if (sessionCtrl.getLcMain() != null) {
      lcMain = sessionCtrl.getLcMain();
      if (lcMain.getPrsid() >= 0) {
        prsMain = ejbLifeCARDAdmin.getPrsMainByPrsId(lcMain.getPrsid());
      }
      if (prsMain == null) {
        prsMain = new PrsMain();
        prsMain.setLastname(lcMain.getSurname());
        prsMain.setFirstname(lcMain.getFirstname());
        prsMain.setDob(lcMain.getDob());
      }
    } else {
      lcMain = null;
      //in this use case it is an error if there is no LcMain entity!
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

  /**
   * @return the issuer
   */
  public NetCenter getIssuer() {
    return issuer;
  }

  /**
   * @param issuer the issuer to set
   */
  public void setIssuer(NetCenter issuer) {
    this.issuer = issuer;
  }

  public LcMain getLcMain() {
    Log.info(CardProductionControl.class.getName() + ":getLcMain():" + lcMain);
    return lcMain;
  }

  public void setLcMain(LcMain object) {
    Log.info(CardProductionControl.class.getName() + ":setLcMain():" + object.toString());
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

  /**
   * @return the keyValueStatus
   */
  public KeyValueItem getLcCadStatus() {
    if (keyValueStatus == null) {
      if (lcCad != null && listStatusConstants().containsKey(lcCad.getStatus())) {
        keyValueStatus = new KeyValueItem(lcCad.getStatus(), listStatusConstants().get(lcCad.getStatus()));
      }
    }
    return keyValueStatus;
  }

  /**
   * @param kvStatus the keyValueStatus to set
   */
  public void setLcCadStatus(KeyValueItem kvStatus) {
    this.keyValueStatus = kvStatus;
    if (kvStatus != null) {
      lcCad.setStatus(kvStatus.getKey());
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
    this.netCenter = (NetCenter) item;
    //FacesMessage msg = new FacesMessage("Selected Center", "" + this.netCenter);
    //FacesContext.getCurrentInstance().addMessage(null, msg);
  }

  public void handleIssuerSelect(SelectEvent event) {
    Object item = event.getObject();
    this.issuer = (NetCenter) item;
    //FacesMessage msg = new FacesMessage("Selected Issuer", "" + this.issuer);
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
        Log.fine(CardProductionControl.class.getName() + ":getCardImage():tplImageFile=" + fTemplate.getAbsolutePath());
        String pathTmp = sctx.getRealPath("WEB-INF") + "/tmp";
        File fPathTmp = new File(pathTmp);
        if (!fPathTmp.exists()) {
          fPathTmp.mkdirs();
        }
        fImage = File.createTempFile("lc-", ".png", fPathTmp);
        Log.fine(CardProductionControl.class.getName() + ":getCardImage():tmpImageFile=" + fImage.getAbsolutePath());
        LifeCARDObjectHandler.createLifeCARDImage(fTemplate, lcItem, fImage);
        return new DefaultStreamedContent(new FileInputStream(fImage));
      } catch (Exception e) {
        Log.warning(CardProductionControl.class.getName() + ":getCardImage():" + e.getMessage());
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

  public void onSaveLcMain() {
    Log.info(CardProductionControl.class.getName() + ":onSaveLcMain():" + lcMain.toString());
    //TODO update (save) LcMain entity
    FacesMessage msg = new FacesMessage("Successful", "Registred Holder :" + lcMain.getSurname());
    FacesContext.getCurrentInstance().addMessage(null, msg);
    //return "intern";
  }

  public void onSaveLcCad() {
    Log.info(CardProductionControl.class.getName() + ":onSaveLcCad():Saving with status " + lcCad.getStatus());

    if (lcCad.getStatus() >= SEHRConstants.LifeCARD_STS_CRDPRD) {
      if (lcCad.getCvID() == null || StringUtils.isBlank(lcCad.getCvSer())) {
        FacesMessage msg = new FacesMessage("Verification Failure", "Serial No and Vendor ID are required!");
        FacesContext.getCurrentInstance().addMessage(null, msg);
        return;
      }
      if (lcCad.getStatus() == SEHRConstants.LifeCARD_STS_CRDPRD) {
        //--- generate HashCode for verification purposes
        //a manipulation of the vendor, LcMain ID or issuer will be detected 
        final int prime = 19;
        int result = prime + lcCad.getCiiIi(); //vendor
        result = prime * result + lcCad.getCiiIi(); //issuer;
        result = prime * result + lcCad.getLcCadPK().getLcid(); //admin record ID;
        lcCad.setCiiCheckdigit(result);
        Log.info(CardProductionControl.class.getName() + ":onSaveLcCad():Status 'Card Produced'; checkdigit=" + lcCad.getCiiCheckdigit());
      }
    }
    FacesMessage msg;
    if (ejbLifeCARDAdmin.saveLcCad(lcCad)) {
      //assign status to LcItem and LcMain
      if (lcMain != null && lcItem != null) {
        lcItem.setSts(lcCad.getStatus());
        lcMain.setSts(lcCad.getStatus());
        try {
          lcMain.setItem(DeSerializer.serialize(lcItem));
          if (ejbLifeCARDAdmin.saveLcMain(lcMain)) {
            Log.info(CardProductionControl.class.getName() + ":onFlowProcess():Admin record with printing item saved:" + lcItem.toString());
          } else {
            Log.warning(CardProductionControl.class.getName() + ":onFlowProcess():Error saving " + lcItem.toString());
          }
        } catch (ObjectHandlerException ex) {
          Log.warning(CardProductionControl.class.getName() + ":onFlowProcess():" + ex.getMessage());
        }
      }
      msg = new FacesMessage("Registred Card", lcCad.getCi() + " (" + lcCad.getCiiCheckdigit() + ")");
      FacesContext.getCurrentInstance().addMessage(null, msg);
    } else {
      msg = new FacesMessage("Card Registration Failure", lcCad.toString());
      FacesContext.getCurrentInstance().addMessage(null, msg);
    }
    //return "intern";
  }

  public void alCheckPatIdent(ActionEvent event) {
    //Log.info(CardRegistrationControl.class.getName() + ":alCheckPatIdent():event: " + event);
    Log.info(CardProductionControl.class.getName() + ":alCheckPatIdent():event PhaseId: " + event.getPhaseId());
    Log.info(CardProductionControl.class.getName() + ":alCheckPatIdent():LcMain: " + lcMain);
    //TODO check if an EHR is already stored on the zone EHR_MAIN...
    //workflow: patient is managed by a center
    //patient (or physician) wants to have an managed care record set
    //physician register patient and managed case (on zone)
    //the a EHR entry will be created
    //after that process a card can be applied to share EHR within a zone or globally
    //TODO send pat id to zone, center to verify if EHR is avaiable...
    //(but this is only a request, the patient should always be registered by a center first, not a zone...)
  }

  public void onCancelLcMain() {
    Log.info(CardProductionControl.class.getName() + ":onCancelLcMain():" + lcMain);
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

  public List<KeyValueItem> getListStatusTypes() {
    return listStatusTypes;
  }

  public String actionClose() {
    sessionCtrl.setPage("/inc/desktop.xhtml");
    return "intern";
  }

  public void onClose(CloseEvent event) {
    //on close show LcMain list
    sessionCtrl.setLcMain(null); //remove working object
    sessionCtrl.setPage("/inc/viewCardsRegistered.xhtml");
  }

  public boolean isSkip() {
    return skip;
  }

  public void setSkip(boolean skip) {
    this.skip = skip;
  }

  public String onFlowProcess(FlowEvent event) {
    Log.info(CardProductionControl.class.getName() + ":onFlowProcess():old-/newStep " + event.getOldStep() + "/" + event.getNewStep());
    //Log.info(CardProductionControl.class.getName() + ":onFlowProcess():newStep=" + event.getNewStep());
    //--- processing current tab (we are leaving)
    //save printing item object on leaving the item related tab
    if (event.getOldStep().equalsIgnoreCase("tbCardPrinting")) {
      if (lcItem != null) {
        try {
          lcMain.setItem(DeSerializer.serialize(lcItem));
          if (ejbLifeCARDAdmin.saveLcMain(lcMain)) {
            Log.info(CardProductionControl.class.getName() + ":onFlowProcess():Admin record with printing item saved:" + lcItem.toString());
          } else {
            Log.warning(CardProductionControl.class.getName() + ":onFlowProcess():Error saving " + lcItem.toString());
          }
        } catch (ObjectHandlerException ex) {
          Log.warning(CardProductionControl.class.getName() + ":onFlowProcess():" + ex.getMessage());
        }
      }
    } else if (event.getOldStep().equalsIgnoreCase("tbCardProvider")) {
      if (this.issuer == null) {
        FacesMessage msg = new FacesMessage("Verification Failure", "Select Issuer!");
        FacesContext.getCurrentInstance().addMessage(null, msg);
        return event.getOldStep();
      }
      lcCad.setCiiIi(this.issuer.getNetCenterPK().getCenterid());
    }

    //--- preparing content of a tab 
    if (event.getNewStep().equalsIgnoreCase("tbCardHolder")) {
      if (prsMain == null) {
        prsMain = new PrsMain();
        prsMain.setLastname(lcMain.getSurname());
        prsMain.setFirstname(lcMain.getFirstname());
        prsMain.setDob(lcMain.getDob());
      }
    } else if (event.getNewStep().equalsIgnoreCase("tbPatientAssignment")) {
      if (StringUtils.isBlank(lcMain.getEHNDomain())) {
        lcMain.setEHNDomain(moduleCtrl.getDomain());
      }
    } else if (event.getNewStep().equalsIgnoreCase("tbCardPrinting")) {
      if (lcMain.getItem() != null) {
        try {
          lcItem = (LifeCardItem) DeSerializer.deserialize(lcMain.getItem());
        } catch (ObjectHandlerException ex) {
          Logger.getLogger(CardProductionControl.class.getName()).log(Level.SEVERE, null, ex);
          lcItem = null;
        }
      }
      if (lcItem == null) {
        lcItem = new LifeCardItem();
        //there is no plastic card known at this point to assign
        lcItem.setLcid(0);
        lcItem.setSurname(lcMain.getSurname());
        lcItem.setFirstname(lcMain.getFirstname());
        lcItem.setMiddle(lcMain.getMiddle());
        lcItem.setTitle(lcMain.getTitle());
        lcItem.setDoB(lcMain.getDob());
        //these data are not part of the DB record; only of a written statement
        lcItem.setProblem(null);
        lcItem.setEMConFQName(null);
        lcItem.setEMConPhone(null);
        //in any case invalidate existing status in LcMain on generating a 
        //card entry this way...
        //this control is for the production phase, so the status is 
        //'card ordered/production'
        lcItem.setSts(SEHRConstants.LifeCARD_STS_CRDORD);

        String ctry = lcMain.getCountry();
        Integer zid = lcMain.getZoneid();
        Integer cid = lcMain.getCenterid();
        Integer pid = lcMain.getPatid();
        try {
          lcItem.setLcPrintnumber(LifeCARDObjectHandler.buildNumber(lcItem, ctry != null ? ctry : "de", zid, cid, pid));
          lcMain.setItem(DeSerializer.serialize(lcItem));
          //do not store here - store on final confirmation or leaving the tab
        } catch (ObjectHandlerException | GenericSEHRException ex) {
          Logger.getLogger(CardProductionControl.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
      Log.info(CardProductionControl.class.getName() + ":onFlowProcess():new LifeCardItem:" + lcItem.toString());
    } else if (event.getNewStep().equalsIgnoreCase("tbCardProvider")) {
      //at this point always set/sync ID with registration settings
      lcCad.setCi(lcItem.getLcPrintnumber()); //see IntraSEC description

      if (lcCad.getCiiMii() == null) {
        lcCad.setCiiMii((short) 80); //see IntraSEC description
      }
      //card application issuer
      if (lcCad.getCaiCat() == null) {
        lcCad.setCaiCat((short) 16); //currently an issuer related usage
      }
      if (lcCad.getCaiCav() == null) {
        lcCad.setCaiCav((short) 64); //currently LifeCARD version 6.4.x
      }
      if (lcCad.getCiiMii() == null) {
        lcCad.setCiiMii((short) 80); //see IntraSEC description
      }
      if (lcCad.getCvCT() == null) {
        lcCad.setCvCT((short) 1); //1=USB; see IntraSEC description
      }
      if (lcCad.getStatus() == null) {
        //card ordered/production requested
        lcCad.setStatus(SEHRConstants.LifeCARD_STS_CRDORD);
      }
      if (lcCad.getCiiCc() == null) {
        //country code; see IntraSEC description; default = system's locale
        Locale loc = Locale.getDefault();
        CountryCode c = CountryCode.getByLocale(loc);
        short cc = (short) c.getNumeric();
        lcCad.setCiiCc((short) cc);
      }
      Log.info(CardProductionControl.class.getName() + ":onFlowProcess():Before 'tbCardProvider':" + lcCad.toString());
    } else if (event.getNewStep().equalsIgnoreCase("tbConfirm")) {
      if (lcCad.getLcCadPK() == null) {
        //get next PK; assign (prepare) physical card entry to lcItem
        LcCadPK pk = ejbLifeCARDAdmin.nextLcCadPK(lcMain);
        lcCad.setLcCadPK(pk);
      }
      lcItem.setLcid(lcCad.getLcCadPK().getLcid());
    }

    if (skip) {
      skip = false;   //reset in case user goes back
      return "tbConfirm";
    } else {
      return event.getNewStep();
    }
  }

  public void statusSelectionChanged(final AjaxBehaviorEvent event) {
    Log.info(CardProductionControl.class.getName() + ":statusSelectionChanged():':" + event);
    Log.info(CardProductionControl.class.getName() + ":statusSelectionChanged():':lcCad status=" + lcCad.getStatus());
  }

  public String getCardNumber() {
    String number = "n/a";
    if (lcMain != null) {
      number = lcMain.toString(); //returns the number 'XX-7-7-8'
    }
    return number;
  }

  //TODO use LifeCARDProcessor.IdentConst2Text()
  private Map<Short, String> listIdentConstants() {
    Map<Short, String> c = new HashMap<>();
    c.put(SEHRConstants.LifeCARD_IDENT_NONE, "none");
    c.put(SEHRConstants.LifeCARD_IDENT_DRIVERLIC, "Driver Licence");
    c.put(SEHRConstants.LifeCARD_IDENT_OTHER, "Other");
    c.put(SEHRConstants.LifeCARD_IDENT_PASSPORT, "Passport");
    return c;
  }

  //TODO use LifeCARDProcessor.StatusConst2Text()
  private Map<Short, String> listStatusConstants() {
    Map<Short, String> c = new HashMap<>();
    c.put(SEHRConstants.LifeCARD_STS_NEW, "New Account (in progress)");
    c.put(SEHRConstants.LifeCARD_STS_REG, "Registration Phase");
    c.put(SEHRConstants.LifeCARD_STS_REGVFYD, "Reg./Person Verified");
    c.put(SEHRConstants.LifeCARD_STS_CRDORD, "Card Ordered (for Production)");
    c.put(SEHRConstants.LifeCARD_STS_CRDPRD, "Card Produced");
    c.put(SEHRConstants.LifeCARD_STS_CRDSHP, "Card Shipped (to Holder)");
    c.put(SEHRConstants.LifeCARD_STS_CRDRCV, "Card Received (by Holder)");
    c.put(SEHRConstants.LifeCARD_STS_CRDLKP, "Card Locked by Holder/Patient");
    c.put(SEHRConstants.LifeCARD_STS_CRDLKI, "Card Locked by Issuer");
    c.put(SEHRConstants.LifeCARD_STS_OC, "Cancelled/Inactivated");
    return c;
  }
}
