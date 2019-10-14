/*
 * (C) 2015 MDI GmbH
 *
 * Based on an idea of Hans J Haase
 * Development Team: Hans J.
 */
package org.ifeth.sehr.p1507291734.web.control;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import org.apache.commons.lang.StringUtils;
import org.ifeth.sehr.core.exception.ObjectHandlerException;
import org.ifeth.sehr.core.handler.LifeCARDObjectHandler;
import org.ifeth.sehr.core.lib.DeSerializer;
import org.ifeth.sehr.core.objects.LifeCardItem;
import org.ifeth.sehr.intrasec.entities.LcCad;
import org.ifeth.sehr.intrasec.entities.LcMain;
import org.ifeth.sehr.intrasec.entities.PrsMain;
import org.ifeth.sehr.intrasec.entities.UsrMain;
import org.ifeth.sehr.p1507291734.ejb.LifeCARDAdmin;
import org.ifeth.sehr.p1507291734.ejb.PrsMainAdmin;
import org.ifeth.sehr.p1507291734.ejb.UserAdmin;
import org.primefaces.event.CloseEvent;
import org.primefaces.event.ToggleEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.Visibility;

/**
 * Control of pages handling LifeCARD holder entries.
 *
 * @author HansJ
 */
@Named(value = "cardsRegVwCtrl")
@RequestScoped
public class CardsRegisteredViewControl implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger Log = Logger.getLogger("org.ifeth.p1507291734.web");
  @EJB
  private UserAdmin ejbUserAdmin;
  @EJB
  private PrsMainAdmin ejbPrsMainAdmin;
  @EJB
  private LifeCARDAdmin ejbLifeCARDAdmin;
  //@Inject
  //private MobileControl mobileCtrl;
  @Inject
  private SessionControl sessionCtrl;
  private int usrid;
  private int svid; //recid
  private PrsMain prsMain; //a LC holder is a person)
  private UsrMain usrMain; //a user (is also person by convention)
  private LcMain lcMain; //the administrative record (containing the LC item)
  private List<PrsMain> lstPrsMain;
  private List<LcMain> lstLcMain;
  private short fltLcStatus = -1; //all
  private boolean fltLifeCARD = false;

  //============================================= constructors, initialization
  public CardsRegisteredViewControl() {
  }

  @PostConstruct
  public void init() {
    usrMain = new UsrMain();
    lstPrsMain = new ArrayList<>();
    lcMain = new LcMain();
    lstLcMain = new ArrayList<>();
  }

  //============================================= getter/setter
  public UsrMain getUserObject() {
    return usrMain;
  }

  public void setUserObject(UsrMain object) {
    this.usrMain = object;
  }

  public LcMain getLcMainObject() {
    Log.info(CardsRegisteredViewControl.class.getName() + ":getLcMainObject():" + lcMain.toString());
    return lcMain;
  }

  public void setLcMainObject(LcMain object) {
    Log.info(CardsRegisteredViewControl.class.getName() + ":setLcMainObject():" + object.toString());
    this.lcMain = object;
  }

  public List<PrsMain> getListPatients() {
    Log.info(CardsRegisteredViewControl.class.getName() + ":getListPatients()");
    if (lstPrsMain == null || lstPrsMain.isEmpty()) {
      lstPrsMain = ejbLifeCARDAdmin.listPatients();
    }
    return lstPrsMain;
  }

  public List<LcMain> getListLcMain() {
    Log.info(CardsRegisteredViewControl.class.getName() + ":getListLcMain()");
    if (lstLcMain == null || lstLcMain.isEmpty()) {
      String fltCountry=null;//"DE";
      if (fltLcStatus >= 0) {
        lstLcMain = ejbLifeCARDAdmin.listRegistrations(fltCountry, null, null, fltLcStatus);
      } else {
        lstLcMain = ejbLifeCARDAdmin.listRegistrations(fltCountry, null, null);
      }
    }

    return lstLcMain;
  }

  public String getCardData(LcMain lcMain) {
    if (lcMain == null) {
      return null;
    }
    LifeCardItem item = null;
    try {
      item = (LifeCardItem) DeSerializer.deserialize(lcMain.getItem());
    } catch (ObjectHandlerException e) {
      Log.warning(CardsRegisteredViewControl.class.getName() + ":getCardData():" + lcMain.toString() + ":" + e.getMessage());
      //return null;
    }
    if (item == null) {
      try {
        if (lcMain.getItem() != null) {
          //store for later convertion
          Object oldLcItem = (Object) DeSerializer.deserialize(lcMain.getItem());
          FileOutputStream fos = new FileOutputStream("/tmp/LifeCardItem-" + lcMain.getLcid() + "-" + System.currentTimeMillis() + ".obj");
          ObjectOutputStream oos = new ObjectOutputStream(fos);
          oos.writeObject(oldLcItem);
          oos.close();
        }
      } catch (ObjectHandlerException ex) {
        Logger.getLogger(CardsRegisteredViewControl.class.getName()).log(Level.SEVERE, null, ex);
      } catch (FileNotFoundException ex) {
        Logger.getLogger(CardsRegisteredViewControl.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IOException ex) {
        Logger.getLogger(CardsRegisteredViewControl.class.getName()).log(Level.SEVERE, null, ex);
      }
      //(re-)build item object
      item = new LifeCardItem();
      item.setSurname(lcMain.getSurname());
      item.setFirstname(lcMain.getFirstname());
      //TODO store new object inside lcMain
      Log.warning(CardsRegisteredViewControl.class.getName() + ":getCardData():LCItem recreated...");
    }
    return item.toString();
  }

  /**
   * Creates the card image of the given holder entry.
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
      String lcid = fctx.getExternalContext().getRequestParameterMap().get("lcid");
      if (StringUtils.isBlank(lcid) || StringUtils.isNumeric(lcid) == false) {
        Log.info(CardsRegisteredViewControl.class.getName() + ":getCardImage():Invalid 'lcid'.");
        return null;
      }
      LifeCardItem item = ejbLifeCARDAdmin.getLifeCardItemByLcId(Integer.valueOf(lcid));
      if (item == null) {
        Log.info(CardsRegisteredViewControl.class.getName() + ":getCardImage():No card item stored. Card already produced?");
        return null;
      }
      File fTemplate;
      File fImage;
      String fPathImg = sctx.getRealPath("resources/images");
      FileInputStream fisImage = null;
      try {
        fisImage = new FileInputStream(new File(fPathImg + "/LifeCard-V8_NoImage.png")); //default on error
        //item = (LifeCardItem) DeSerializer.deserialize(lcMain.getItem());
        fTemplate = new File(fPathImg + "/LifeCard-V8.png");
        Log.fine(CardsRegisteredViewControl.class.getName() + ":getCardImage():tplImageFile=" + fTemplate.getAbsolutePath());
        String pathTmp = sctx.getRealPath("WEB-INF") + "/tmp";
        File fPathTmp = new File(pathTmp);
        if (!fPathTmp.exists()) {
          fPathTmp.mkdirs();
        }
        fImage = File.createTempFile("lc-", ".png", fPathTmp);
        LifeCARDObjectHandler.createLifeCARDImage(fTemplate, item, fImage);
        Log.fine(CardsRegisteredViewControl.class.getName() + ":getCardImage():tmpImageFile=" + fImage.getAbsolutePath());
        //Ok, image processed by item attributes
        fisImage = new FileInputStream(fImage);

      } catch (Exception e) {
        Log.warning(CardsRegisteredViewControl.class.getName() + ":getCardImage():" + e.getMessage());
        //fisImage = new FileInputStream(new File(fPathImg + "/LifeCard-V8_Error.png"));
      }
      return fisImage != null ? new DefaultStreamedContent(fisImage) : null;
    }
  }

  public String getPrsMain(Integer prsid) {
    PrsMain prsMain = ejbPrsMainAdmin.readPrsMainByID(prsid);
    return prsMain.toString();
  }

  //============================================= methods of actions etc.
  public void actionSaveLcMainObject() {
    Log.info(CardsRegisteredViewControl.class.getName() + ":actionSaveLcMainObject():" + lcMain.toString());
    FacesContext fctx = FacesContext.getCurrentInstance();
    if (lcMain.getPrsid() == null) {
      fctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Fehler...", "LifeCARD(R) muss einer Person (Patient) zugeordnet sein!"));
      return;
    }
    Log.info(CardsRegisteredViewControl.class.getName() + ":actionSaveLcMainObject():saving lcMain.lcid=" + lcMain.getLcid());
    if (ejbLifeCARDAdmin.saveLcMain(lcMain)) {
      if (!lstLcMain.contains(lcMain)) {
        //TODO add to DB
        lstLcMain.add(lcMain);
      }
    }
    //return "intern";
  }

  public String doSaveLcMainObject() {
    Log.info(CardsRegisteredViewControl.class.getName() + ":doSaveLcMainObject():" + lcMain.toString());
    Log.info(CardsRegisteredViewControl.class.getName() + ":doSaveLcMainObject():lcMain.surname=" + lcMain.getSurname());
    if (!lstLcMain.contains(lcMain)) {
      Log.info(CardsRegisteredViewControl.class.getName() + ":doSaveLcMainObject():saving lcMain.lcid=" + lcMain.getLcid());
      //TODO add to DB
      lstLcMain.add(lcMain);
    }

    return "intern";
  }

  public void doDeleteLcMainObject() {
    if (lstLcMain.contains(lcMain)) {
      //TODO remove from DB
      lstLcMain.remove(lcMain);
    }
  }

  public String doPrepareNewLcMainObject() {
    Log.info(CardsRegisteredViewControl.class.getName() + ":doPrepareNewLcMainObject()");
    sessionCtrl.setLcMain(null);
    sessionCtrl.setPage("/inc/wizCardRegistration.xhtml");
    return "intern";
  }

  public String showLcMainInfo() {
    Log.info(CardsRegisteredViewControl.class.getName() + ":showLcMainInfo():" + lcMain.toString());
    return "intern";
  }

  public Integer getCountLcCad(Integer lcid) {
    List<LcCad> list = ejbLifeCARDAdmin.listProducedCardsByLcMainID(lcid);
    return list.size();
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

  public void onRowToggle(ToggleEvent event) {
    //event INVOKE_APPLICATION
    Log.info("onRowToggle():event.getVisibility()=" + event.getVisibility());
    if (event.getVisibility() == Visibility.VISIBLE) {
      LcMain lc = (LcMain) event.getData();
      System.out.println(lc != null ? lc.getLcid() : "LcMain is null");
    }
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
    this.lstLcMain = new ArrayList(); //clear list
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
    Log.info("btnFltLifeCard():lstLcMain size: " + lstLcMain.size());
    //we're using ActionListener...
    //return "intern"; //do not use this as page control
  }
}
