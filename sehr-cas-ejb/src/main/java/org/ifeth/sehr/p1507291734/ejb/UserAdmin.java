/*
 * (C)MDI GmbH
 */
package org.ifeth.sehr.p1507291734.ejb;

import java.util.List;
import java.util.logging.Logger;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.ifeth.sehr.intrasec.entities.UsrMain;

/**
 * Manage user (login) to this host.
 * <p>
 * Note: A User is based on a person record by SEHR concvention.
 * </p>
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@Stateless
@LocalBean
public class UserAdmin {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.ejb");

  @PersistenceContext(unitName = "HibJTAIntraSEC")
  private EntityManager em;

  public UsrMain readUserByID(int id) {
    Log.info(UserAdmin.class.getName() + ":readUserByID()");
    Query query = em.createQuery("Select u from UsrMain u where u.usrid=" + id, UsrMain.class);
    UsrMain entity = null;
    try {
      entity = (UsrMain) query.getSingleResult();
    } catch (PersistenceException pe) {
      Log.warning(UserAdmin.class.getName() + ":readUserByID():" + pe.getMessage());
    }
    return entity;
  }

  public List<UsrMain> listRegisteredUsers() {
    Log.info(UserAdmin.class.getName() + ":listRegisteredUsers()");
    Query query = em.createQuery("select u from UsrMain u", UsrMain.class);
    List<UsrMain> list = null;
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(UserAdmin.class.getName() + ":listRegisteredUsers():" + pe.getMessage());
    }
    return list;
  }

  public List<UsrMain> listUsersByServiceId(int svid) {
    Log.info(UserAdmin.class.getName() + ":listUsersByServiceId()");
    Query query = em.createQuery("select u from UsrMain u where u.usrid in (select s.usrid from UsrServices s where s.svid=" + svid + ")", UsrMain.class);
    List<UsrMain> list = null;
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(UserAdmin.class.getName() + ":listUsersByServiceId():" + pe.getMessage());
    }
    return list;
  }

}
