/*
 * (C)IFETH
 */
package org.ifeth.sehr.p1507291734.ejb;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.ifeth.sehr.intrasec.entities.AdrMain;
import org.ifeth.sehr.intrasec.entities.NetCenter;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@Stateless
@LocalBean
public class AdrMainAdmin {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.ejb");

  @PersistenceContext(unitName = "HibJTAIntraSEC")
  private EntityManager em;

  public List<AdrMain> listAdrMain(short adrtype) {
    Log.fine(AdrMainAdmin.class.getName() + ":listAdrMain():adrtype=" + adrtype);
    List<AdrMain> list;
    Query query = em.createQuery("SELECT a FROM AdrMain a WHERE a.adrtype = :adrtype", AdrMain.class);
    query.setParameter("adrtype", adrtype);
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(AdrMainAdmin.class.getName() + ":listAdrMain():" + pe.getMessage());
      return null;
    }
    return list;
  }

  public AdrMain readByAdrId(int adrid) {
    AdrMain adrMain = null;
    Query query = em.createNamedQuery("AdrMain.findByAdrid", AdrMain.class);
    query.setParameter("adrid", adrid);

    try {
      adrMain = (AdrMain) query.getSingleResult();
    } catch (PersistenceException pe) {
      Log.warning(AdrMainAdmin.class.getName() + ":findByAdrId():" + pe.getMessage());
    }
    return adrMain;
  }

  public List<AdrMain> listAdressesByParams(Map<String, String> params) {
    Log.info(AdrMainAdmin.class.getName() + ":listAdressesByParams():params " + (params != null ? params.size() : "none"));

    List<AdrMain> list = null;
    CharSequence csql;
    String sql = "SELECT a.* FROM adr_main a ";
    if (params != null) {
      sql += "WHERE ";
      for (Map.Entry<String, String> entry : params.entrySet()) {
        switch (entry.getKey()) {
          case "match":
            sql += "a.match like '%" + entry.getValue() + "%' ";
            break;
          case "adr1":
            sql += "a.adr1=" + entry.getValue() + " ";
            break;
        }
        sql += "AND ";
      }
      sql = sql.substring(0, sql.length() - 4); //remove AND
    }
    Log.fine(AdrMainAdmin.class.getName() + ":listAdressesByParams():sql=" + sql);

    //Query query = em.createQuery("SELECT n FROM NetCenter n WHERE n.name like :name", NetCenter.class);
    //query.setParameter("name", "%" + filter + "%");
    Query query = em.createNativeQuery(sql, AdrMain.class);
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(AdrMainAdmin.class.getName() + ":listAdressesByParams():" + pe.getMessage());
    }
    return list;
  }

  public Integer save(AdrMain adrMain) {
    Log.fine(AdrMainAdmin.class.getName() + ":save():" + adrMain.toString());
    try {
      if (adrMain.getAdrid() != null && adrMain.getAdrid().equals(-1)) {
        adrMain.setAdrid(null);
      }
      if (adrMain.getAdrid() == null || em.find(AdrMain.class, adrMain.getAdrid()) == null) {
        em.persist(adrMain);
      } else {
        adrMain = em.merge(adrMain);
      }
    } catch (PersistenceException pe) {
      Log.warning(AdrMainAdmin.class.getName() + ":save():" + pe.getMessage());
    }
    return adrMain.getAdrid();
  }
}
