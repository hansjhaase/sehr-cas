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
import org.ifeth.sehr.intrasec.entities.PrsMain;
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
public class PrsMainAdmin {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.ejb");

  @PersistenceContext(unitName = "HibJTAIntraSEC")
  private EntityManager em;

  public PrsMain readPrsMainByID(Integer id) {
    Log.info(PrsMainAdmin.class.getName() + ":readPrsMainByID()");
    Query query = em.createQuery("Select p from PrsMain p where p.prsid=" + id, PrsMain.class);
    PrsMain entity = null;
    try {
      entity = (PrsMain) query.getSingleResult();
    } catch (PersistenceException pe) {
      Log.warning(PrsMainAdmin.class.getName() + ":readPrsMainByID():" + pe.getMessage());
    }
    return entity;
  }

  public List<PrsMain> list() {
    Log.info(PrsMainAdmin.class.getName() + ":list()");
    Query query = em.createQuery("select p from PrsMain u", UsrMain.class);
    List<PrsMain> list = null;
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(PrsMainAdmin.class.getName() + ":list():" + pe.getMessage());
    }
    return list;
  }

  public List<PrsMain> listByStatus(short status) {
    Log.info(PrsMainAdmin.class.getName() + ":listByStatus()");
    Query query = em.createNamedQuery("PrsMain.findByStatus", PrsMain.class);
    query.setParameter("status", status);
    List<PrsMain> list = null;
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(PrsMainAdmin.class.getName() + ":listByStatus():" + pe.getMessage());
    }
    return list;
  }

}
