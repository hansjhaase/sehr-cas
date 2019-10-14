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
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;
import org.ifeth.sehr.intrasec.entities.PrsMain;
import org.ifeth.sehr.intrasec.entities.UsrMain;
import org.ifeth.sehr.p1507291734.ejb.LifeCARDAdmin;
import org.ifeth.sehr.p1507291734.ejb.UserAdmin;
import org.primefaces.event.CloseEvent;

/**
 * Control of pages handling user (account) entries.
 *
 * @author HansJ
 */
@Named(value = "userViewCtrl")
@RequestScoped
public class UserViewControl implements Serializable {

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
  private PrsMain prsMain;
  private UsrMain usrMain;
  private List<UsrMain> lstUsrMain;
  private boolean fltLifeCard = false;

  //============================================= constructors, initialization
  public UserViewControl() {
  }

  @PostConstruct
  public void init() {
    prsMain = new PrsMain();
    usrMain = new UsrMain();
    usrMain.setPrsMain(prsMain);
    lstUsrMain = new ArrayList<>();
  }

  //============================================= getter/setter
  public UsrMain getUserObject() {
    if(usrMain.getPrsMain()==null){
      prsMain = new PrsMain();
      usrMain.setPrsMain(prsMain);
    }
    return usrMain;
  }

  public void setUserObject(UsrMain object) {
    this.usrMain = object;
    if(object.getPrsMain()==null){
      prsMain = new PrsMain();
      usrMain.setPrsMain(prsMain);
    }
  }

  public List<UsrMain> getListUsers() {

    if (lstUsrMain == null || lstUsrMain.isEmpty()) {
      if (fltLifeCard) {
        lstUsrMain = ejbLifeCARDAdmin.listLCUserAccounts();
      } else {
        lstUsrMain = ejbUserAdmin.listRegisteredUsers();
      }
    }

    return lstUsrMain;
  }

  //============================================= methods of actions etc.
  public String doSaveUserObject() {
    if (!lstUsrMain.contains(usrMain)) {
      //TODO add to DB
      lstUsrMain.add(usrMain);
    }

    return "intern";
  }

  public void doDeleteUser() {
    if (lstUsrMain.contains(usrMain)) {
      //TODO remove from DB
      lstUsrMain.remove(usrMain);
    }
  }

  public String doPrepareNewUserObject() {
    prsMain = new PrsMain();
    usrMain = new UsrMain();
    usrMain.setPrsMain(prsMain);
    return "intern";
  }

  public String showUserInfo() {
    Log.info(UserViewControl.class.getName() + ":showUserInfo():" + usrMain.toString());
    return "intern";
  }

  /**
   * @return the zoneid
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
    return fltLifeCard;
  }

  /**
   * @param fltLifeCard the fltLifeCard to set
   */
  public void setFltLifeCard(boolean fltLifeCard) {
    this.fltLifeCard = fltLifeCard;
  }

  /**
   * @param fltLC
   */
  public void btnFltLifeCard(boolean fltLC) {
//    Map<String, Object> sessMap = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
//    for (Map.Entry<String, Object> e : sessMap.entrySet()) {
//      System.out.println(e.getKey());
//    }
    fltLifeCard = fltLC;
    if (fltLC) {
      lstUsrMain = ejbLifeCARDAdmin.listLCUserAccounts();
    } else {
      lstUsrMain = ejbUserAdmin.listRegisteredUsers();
    }
    Log.info(UserViewControl.class.getName() + ":btnFltLifeCard():list size: " + lstUsrMain.size());
    //return "intern"; //ActionListener, do not use this page control
  }
}
