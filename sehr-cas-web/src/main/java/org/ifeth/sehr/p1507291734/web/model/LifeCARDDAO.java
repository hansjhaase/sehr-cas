/*
 * (C) IFETH
 */
package org.ifeth.sehr.p1507291734.web.model;

import java.io.IOException;
import java.io.InvalidClassException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.ifeth.sehr.core.exception.ObjectHandlerException;
import org.ifeth.sehr.core.lib.DeSerializer;
import org.ifeth.sehr.core.objects.LifeCardItem;
import org.ifeth.sehr.intrasec.entities.LcMain;
import org.ifeth.sehr.p1507291734.ejb.LifeCARDAdmin;

/**
 * DAO of REST service 'LifeCARDItemResource' and 'LCMainResource'.
 * 
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
public class LifeCARDDAO {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");

  //@EJB - does not work here :( So we're using Intial Context so far...
  private LifeCARDAdmin ejbLifeCARDAdmin;
  private InitialContext ic;
  private static LifeCARDDAO instance;
  private Map<String, LifeCardItem> content = new ConcurrentHashMap<>();

  private LifeCARDDAO() {
    //dummy entries for testing purposes
//    LifeCardItem lcItem = new LifeCardItem();
//    lcItem.setLcid(1);
//    lcItem.setSurname("Doe");
//    lcItem.setFirstname("Jane");
//    content.put("1", lcItem);
//    lcItem = new LifeCardItem();
//    lcItem.setLcid(2);
//    lcItem.setSurname("Doe");
//    lcItem.setFirstname("John");
    //   content.put("2", lcItem);
    try {
      ic = new InitialContext();
      //ejbLifeCARDAdmin = (LifeCARDAdmin) ic.lookup("java:comp/env/ejb/sehr/LifeCardAdmin");
      //ejbLifeCARDAdmin = (LifeCARDAdmin) ic.lookup("LifeCardAdmin");
      ejbLifeCARDAdmin = (LifeCARDAdmin) ic.lookup("java:global/sehr-cas-ear/sehr-cas-ejb-0.2/LifeCardAdmin");
      List<LcMain> registrations = ejbLifeCARDAdmin.listRegistrations("DE", null, null);
      for (LcMain lcMain : registrations) {
        LifeCardItem lcItem = new LifeCardItem();
        if (lcMain.getItem() != null) {
          try {
            lcItem = (LifeCardItem) DeSerializer.deserialize(lcMain.getItem());
          } catch (ObjectHandlerException ex) {
            Logger.getLogger(LifeCARDDAO.class.getName()).log(Level.WARNING, null, ex.getMessage());
            //TODO class - BLOB (item) update required because it is unreadable
            lcItem.setSurname(lcMain.getSurname());
            lcItem.setFirstname(lcMain.getFirstname());
            lcItem.setSts((short) -1); //invalidate current item
          }
        }
        content.put(String.valueOf(lcMain.getLcid()), lcItem);
      }
    } catch (NamingException ex) {
      Log.log(Level.SEVERE, ex.getMessage());
    }
  }

  public static synchronized LifeCARDDAO getInstance() {
    if (instance == null) {
      instance = new LifeCARDDAO();
    }
    return instance;
  }

  //useful methods... 
  public Map<String, LifeCardItem> getList() {
    //refresh list from DB
    List<LcMain> registrations = ejbLifeCARDAdmin.listRegistrations("DE", null, null);
    if(registrations==null || registrations.isEmpty()){
      return null;
    }
    for (LcMain lcMain : registrations) {
      //list only valid entries (records)
      if (lcMain.getItem() != null) {
        LifeCardItem lcItem;
        try {
          Object oLcItem = DeSerializer.deserialize(lcMain.getItem());
          lcItem = (LifeCardItem) oLcItem;
          content.put(String.valueOf(lcMain.getLcid()), lcItem);
//        } catch (InvalidClassException ex) {
//          Logger.getLogger(LifeCARDDAO.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        } catch (ObjectHandlerException ex) {
          Logger.getLogger(LifeCARDDAO.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
      }
    }
    return content;
  }

  public LcMain getLcMain(Integer id) {
    LcMain record = ejbLifeCARDAdmin.getLcMainByLcId(id);
    return record;
  }
}
