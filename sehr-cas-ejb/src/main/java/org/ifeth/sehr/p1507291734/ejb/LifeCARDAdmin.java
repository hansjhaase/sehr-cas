/*
 * (C) 2012-2016 IFETH
 */
package org.ifeth.sehr.p1507291734.ejb;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.activemq.camel.component.ActiveMQComponent;
//import org.apache.activemq.camel.component.ActiveMQConfiguration;
import org.apache.camel.CamelContext;
import org.apache.commons.lang3.StringUtils;
import org.ifeth.sehr.core.exception.GenericSEHRException;
import org.ifeth.sehr.core.exception.ObjectHandlerException;
import org.ifeth.sehr.core.handler.LifeCARDObjectHandler;
import org.ifeth.sehr.core.handler.LifeCARDProcessor;
import org.ifeth.sehr.core.lib.DeSerializer;
import org.ifeth.sehr.core.objects.LifeCardItem;
import org.ifeth.sehr.core.objects.SEHRConfigurationObject;
import org.ifeth.sehr.core.spec.SEHRConstants;
import org.ifeth.sehr.intrasec.entities.LcCad;
import org.ifeth.sehr.intrasec.entities.LcCadPK;
import org.ifeth.sehr.intrasec.entities.LcMain;
import org.ifeth.sehr.intrasec.entities.NetCenter;
import org.ifeth.sehr.intrasec.entities.NetServices;
import org.ifeth.sehr.intrasec.entities.PrsMain;
import org.ifeth.sehr.intrasec.entities.UsrMain;
import org.ifeth.sehr.intrasec.lib.JPAUtils;
import org.ifeth.sehr.p1507291734.lib.Constants;

/**
 * Manage patient cards based on the LifeCARD(R) project to share EHR with
 * health care professionals.
 * <p>
 * Managing patients is based on the LifeCARD(R) project started in 1989 by the
 * authors Silvia and Hans J Haase. The registration of a patient as a member of
 * a health care community (zone) with with relations (who is treating the
 * patient), access privileges is a basic procedure of SEHR. Managing the EHR's
 * itself is done by modules like 'sehr-care'.</p>
 *
 * @author hansjhaase
 * @see org.ifeth.sehr.core.objects.LifeCardItem
 */
//@Stateless(name = "LifeCARDAdmin")
//@Stateless(mappedName="LifeCARDAdmin")
@Stateless(name = "LifeCardAdmin", mappedName = "ejb/sehr/LifeCardAdmin")
public class LifeCARDAdmin {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.ejb");
  @Resource
  private SessionContext sctx;

  @PersistenceContext(unitName = "HibJTAIntraSEC")
  private EntityManager em;

  //CatID is 5; see SEHR specification and table DEF_CATEGORY
  private static final int CatID = SEHRConstants.EHR_CATID;

  private Properties p;
  private CamelContext camelContext;
  private SEHRConfigurationObject sco;

  private boolean init() {
    InitialContext ic;
    try {
      ic = new InitialContext();
      this.p = (Properties) ic.lookup(Constants.ICPropName);
      this.sco = (SEHRConfigurationObject) ic.lookup(SEHRConstants.icSCO);
    } catch (NamingException ne) {
      Log.severe(LifeCARDAdmin.class.getName() + ":init():" + ne.toString());
      return false;
    }
    return true;
  }

  /**
   * Store a new LifeCARD entry on this zone using given parameters.
   *
   * <p>
   * A card entry has to be stored on a zone based SEHR host for privacy
   * reasons. There is no global database containing sensitive medical data and
   * there should never be a single company holding those records by SEHR
   * convention. Only the patient is responsible for sharing his data.
   * </p>
   * <p>
   * By the given parameters a world wide unique record will be created for a
   * patient. The ID of a card is globally unique because the country (e.g. DE),
   * the zoneid, the id of a facility (center id) and the id of the patient at
   * this facility (at the time of registration) are unique by this combination.
   * </p>
   * <p>
   * <b>Important!</b> Use 'saveLCItem(LifeCardItem, lcid)' to store the item
   * object for printing/producing the card with the given LcMain record by the
   * record id 'lcid'. Ask the holder before by presenting the data for
   * verification of the data pronted on the card.
   * </p>
   *
   * @param surname
   * @param firstname
   * @param title
   * @param middle
   * @param idpassport
   * @param dob
   * @param country
   * @param zid
   * @param cid
   * @param pid
   * @param ICEProblem
   * @param ICEContact
   * @param ICEContactPhone
   * @return LifeCardItem object that has been generated and stored at LcMain
   */
  public LifeCardItem registerByParams(String surname, String firstname, String title, String middle, String idpassport, long dob, String country, int zid, int cid, int pid, String ICEProblem, String ICEContact, String ICEContactPhone) {
    if (!init()) {
      return null;
    }
    if (StringUtils.isBlank(country)) {
      country = "ZZ"; //undefined/user defined by ISO 3166-1 alpha 2 convention
    }
    //--- build LC object
    LifeCardItem lcItem = new LifeCardItem();
    lcItem.setSurname(surname);
    lcItem.setFirstname(firstname);
    lcItem.setMiddle(middle);
    lcItem.setTitle(title);
    lcItem.setDoB(new Date(dob));
    lcItem.setProblem(ICEProblem);
    lcItem.setEMConFQName(ICEContact);
    lcItem.setEMConPhone(ICEContactPhone);
    lcItem.setIdentNo(idpassport);
    lcItem.setIdentType(SEHRConstants.LifeCARD_IDENT_PASSPORT);
    lcItem.setSts(SEHRConstants.LifeCARD_STS_NEW); //new, not yet registered

    //--- save to db
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd"); //sql date field
    String d1 = sdf.format(new Date(dob));

    String sql = "SELECT p.STATUS, p.LCID ";
    sql += "FROM NEW_LCMAIN('" + country + "'," + zid + "," + cid + "," + pid + ", '" + surname + "', '" + firstname + "', '" + title + "', '" + middle + "', '" + d1 + "') p";
    Query qryRegister = em.createNativeQuery(sql);
    Object[] result = (Object[]) qryRegister.getSingleResult();
    //<0 error codes, 0 ok, >0 card already exist/shipped, 
    //1 card, shipped 
    //8 locked, e.g. stolen card, holder died etc., 
    //9 deleted, out of usage
    lcItem.setSts((Short) result[0]);
    if (lcItem.getSts() != 0 || result[1] == null) {
      Log.info("registerByParams():Card already exists or another error occured (sts=" + lcItem.getSts() + ")");
      return lcItem;
    }
    //--- put db related data to the LC object
    lcItem.setLcid(0); //cardid!! no card yet produced and assigned
    String lcnumber = country + "-" + String.format("%07d", zid) + "-" + String.format("%07d", cid) + "-" + String.format("%08d", pid);
    lcItem.setLcPrintnumber(lcnumber);
    //store object
    LcMain lcMain = getLcMainByLcId((Integer) result[1]);
    try {
      lcMain.setItem(DeSerializer.serialize(lcItem));
      saveLcMain(lcMain);
    } catch (ObjectHandlerException ex) {
      Logger.getLogger(LifeCARDAdmin.class.getName()).log(Level.SEVERE, null, ex.getMessage());
    }

    return lcItem;
  }

  /**
   * List services processing LifeCARD(R) entries bind to this zone host.
   * <p>
   * The list elements are containing the URLs to a WEB host (GUI) and the
   * ApacheMQ broker. The centerid is 0 on this administrative record by
   * convention. If there are centers using the service there is a record for
   * each center with the centerid set. The DefModule is not used because the
   * zone may use a LifeCARD app on another zone. The type of app handling a
   * LifeCARD is defined by catid and not by a DefModule reference.
   * </p>
   *
   * @param zid
   * @return
   */
  public List<NetServices> listServices(int zid) {
    List<NetServices> listLCServices = new ArrayList<>();

    String sql = "SELECT a.* FROM net_services a";
    sql += " WHERE a.centerid<=0 and a.catid=" + CatID;
    if (zid > 0) {
      sql += " AND a.zoneid=" + zid;
    }

    Query query = em.createNativeQuery(sql, NetServices.class);
    try {
      listLCServices = query.getResultList();
      if (listLCServices != null && !listLCServices.isEmpty()) {
        for (NetServices netServices : listLCServices) {
          //TODO do some checks... verify SEHRAuthKey, GUID of service etc...
          if (StringUtils.isBlank(netServices.getSehrauthkey())) {
            Log.warning(netServices.getTitle() + ": Invalid SEHRAuthKey!");
          }
        }
      }
    } catch (PersistenceException pe) {
      Log.warning(LifeCARDAdmin.class.getName() + ":listServices():" + pe.getMessage());
    }
    return listLCServices;
  }

  /**
   * Get the highest XNET level component root, country, domain or local (zone).
   *
   * <p>
   * The XNET component used to send a message to a LifeCARD(R) queue depends on
   * the level this SEHR-CAS is running on as well as on level processings.
   * </p>
   * <ul>
   * <li>Running as <b>root</b> level the component will be
   * 'XNetLevel'='xroot'.</li>
   * <li>Running as <b>country</b> level the component will be
   * 'XNetLevel'='xctry' and it depends on the configuration of the broker to
   * send a message to 'sehr.lc.[COUNTRY].[ZID].[CID].[PID].queue'</li>
   * <li>Running as <b>domain</b> level the component will be 'XNetLevel'='xdom'
   * and it depends on the configuration of the broker to send a message to
   * 'sehr.lc.[COUNTRY].[ZID].[CID].[PID].queue'</li>
   * <li>Running as <b>zone</b> level the component will be 'XNetLevel'='xzone'
   * and it depends on the configuration of the broker to send a message to
   * 'sehr.lc.[COUNTRY].[ZID.[CID].[PID].queue'</li>
   * </ul>
   * <p>
   * For details processing a message from a facility to the patient study
   * https://www.lucidchart.com/documents/edit/466188e0-525b-2b84-a73d-35ea0a00c5b2/0
   * </p>
   *
   * @return
   */
  public Map<String, Object> getXNetComponent() {
    if (!init()) {
      Log.info(LifeCARDAdmin.class.getName() + ":getXNetComponent():Initialization failed.");
      return null;
    }
    Map<String, Object> xnetComp = new HashMap<>();
    xnetComp.put("XNETStarted", false);
    ActiveMQComponent amqComp = null;
    //SEHR XNET root is a top level domain that ties SEHR communities together
    InitialContext ic;
    try {
      ic = new InitialContext();
      this.camelContext = (CamelContext) ic.lookup("XNetContext");
      if (this.camelContext != null && this.camelContext.getStatus().isStarted()) {
        if (StringUtils.isNotBlank(p.getProperty("sehrxnetrooturl", ""))) {
          amqComp = (ActiveMQComponent) this.camelContext.getComponent("xroot");
          xnetComp.put("XNETLevel", "xroot");
          if (amqComp != null) {
            //We can send messages to a LC queue;
            //Reading from a LC queue depends on the app the card holder uses;
            //The holder has to login and be verified to get his messages;
            //The 'xroot' configuration allows this CAS module to send 
            //messages out to the SEHR world using this camel context;
            xnetComp.put("XNETStarted", amqComp.isStarted());
            xnetComp.put("ActiveMQStatus", amqComp.getStatus());
          }
        } else if (StringUtils.isNotBlank(p.getProperty("sehrxnetcountryurl", ""))) {
          amqComp = (ActiveMQComponent) this.camelContext.getComponent("xctry");
          xnetComp.put("XNETLevel", "xctry");
          if (amqComp != null) {
            //It seems to be that we can send messages to a national LC queue
            //But this depends on the configuration of the country broker
            xnetComp.put("XNETStarted", amqComp.isStarted());
            //TODO Check broker for a route to 'xroot' also
            xnetComp.put("ActiveMQStatus", amqComp.getStatus());
          }
        } else if (StringUtils.isNotBlank(p.getProperty("sehrxnetdomainurl", ""))) {
          amqComp = (ActiveMQComponent) this.camelContext.getComponent("xdom");
          xnetComp.put("XNETLevel", "xdom");
          if (amqComp != null) {
            //It seems to be that we can send messages to a LC queue for the 
            //whole managed domain
            //To send dmessages to a LC queue depends on the broker settings
            xnetComp.put("XNETStarted", amqComp.isStarted());
            //TODO Check broker for a route to 'xroot'  or 'xctry'
            xnetComp.put("ActiveMQStatus", amqComp.getStatus());
          }
        } else if (StringUtils.isNotBlank(p.getProperty("sehrxnetzoneurl", ""))) {
          amqComp = (ActiveMQComponent) this.camelContext.getComponent("xzone");
          xnetComp.put("XNETLevel", "xzone"); //the bottom level
          if (amqComp != null) {
            //There is a local broker (of the zone, the community level, WAN)
            //To send dmessages to other LC queues depends on the broker 
            //settings
            xnetComp.put("XNETStarted", amqComp.isStarted());
            //TODO Check broker for a route to 'xroot' otherwise there
            //will be no interchange with patients outside of the zone
            xnetComp.put("ActiveMQStatus", amqComp.getStatus());
          }
        }
        xnetComp.put("ActiveMQComponent", amqComp);
      } else {
        Log.info(LifeCARDAdmin.class.getName() + ":getXNetComponent():No routing (Camel) context.");
        xnetComp.put("ActiveMQStatus", "No routing context.");
      }

    } catch (NamingException ex) {
      Log.warning(LifeCARDAdmin.class.getName() + ":getXNetComponent():" + ex.getMessage());
    }
//    //Finally check if this zone (this SEHR-CAS is running for) has centers 
//    //that have registered the LifeCARD service (module) to use the XNET for 
//    //exchanging data (of/to/with the patient).
//    List<NetCenter> list = getServiceCenter(Integer.parseInt(p.getProperty("zoneID", "0")));
//    xnetComp.put("ListLCServiceCenter", list);

    return xnetComp;
  }

  /**
   * Get the EIS context (Apache Camel) to process messages.
   * <p>
   * To produce or process messages SEHR uses 'Apache Camel'.
   * </p>
   *
   * @return CamelContext e.g. to produce a message.
   */
  public CamelContext getXNetContext() {
    CamelContext cctx;
    Map<String, Object> xnetComp = getXNetComponent();
    if (xnetComp == null || xnetComp.isEmpty()) {
      Log.info(LifeCARDAdmin.class.getName() + ":getXNetContext()():No valid XNET routing handle found");
      return null;
    }
    //+++ do not just return the context...
    //return this.camelContext;
    ActiveMQComponent amqComp = (ActiveMQComponent) xnetComp.get("ActiveMQComponent");
    if (amqComp == null) {
      Log.info(LifeCARDAdmin.class.getName() + ":getXNetContext()():No valid XNET handle found (No AMQ component)");
      return null;
    }
    //... get and check the context of the highest identified processing
    cctx = amqComp.getCamelContext();
    //TODO check connection... otherwise throw error "XNetConnectionError"
    if (cctx == null || cctx.isSuspended()) {
      Log.log(Level.WARNING, LifeCARDAdmin.class.getName() + ":getXNetContext():" + (cctx == null ? "XNET context not present (null)" : "XNET context suspended " + cctx.isSuspended()));
      return null;
    }
    return cctx;
  }

  /**
   * Get a list of all patients that have been registered for a LifeCARD.
   * <p>
   * By SEHR specification there are relationships between persons, e.g. a
   * patient (person 1) allows a physician (person 2) to send (and update) a
   * medication plan for him. A health professional (a pharmacist, person 3)
   * follows (wants to get/read) the medical planning of this patient for an
   * continuous interaction checking. So a LifeCARD(R) record must have a
   * PrsMain and PatMain reference. A UsrMain reference is optional and used to
   * login for administrative purposes.
   * </p>
   * <p>
   * N.B.: A PrsMain record contains the PatMain entity if any - see PrsMain
   * entity.</p>
   *
   * @return List
   */
  public List<PrsMain> listPatients() {
    Log.info(LifeCARDAdmin.class.getName() + ":listPatients()");

    List<PrsMain> list;
    String sql = "SELECT p.* FROM prs_main p where p.prsid IN (SELECT l.prsid FROM lc_main l where l.prsid>0 and l.prsid is not null)";
    Query query = em.createNativeQuery(sql, PrsMain.class);
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(LifeCARDAdmin.class.getName() + ":listPatients():" + pe.getMessage());
      return null;
    }
    return list;
  }

  public List<UsrMain> listLCUserAccounts() {
    Log.info(LifeCARDAdmin.class.getName() + ":listLCUserAccounts()");

    List<UsrMain> list;
    String sql = "SELECT u.* FROM usr_main u where u.usrid IN (SELECT l.prsid FROM lc_main l where l.prsid>0 and l.prsid is not null)";
    Query query = em.createNativeQuery(sql, UsrMain.class);
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(LifeCARDAdmin.class.getName() + ":listLCUserAccounts():" + pe.getMessage());
      return null;
    }
    return list;
  }

  public List<LcCad> listProducedCardsByLcMainID(int lcid) {
    List<LcCad> list;
    Query query = em.createNamedQuery("LcCad.findByLcid", LcCad.class);
    query.setParameter("lcid", lcid);
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(LifeCARDAdmin.class.getName() + ":listRegistrations():" + pe.getMessage());
      return null;
    }
    return list;
  }

  public List<LcMain> listRegistrations(String country, Integer zid, Integer cid) {
    return listRegistrations(country, zid, cid, null);
  }

  /**
   * List LcMain (Registration) records by status.
   * <p>
   * <b>Note:</b>The record includes the card item object to be used for
   * printing. The card item object also contains the ICE related data.
   * </p>
   *
   * @param country
   * @param zid
   * @param cid
   * @param status
   * @return
   */
  public List<LcMain> listRegistrations(String country, Integer zid, Integer cid, Short status) {
    List<LcMain> list;
    if (country == null) {
      country = Locale.getDefault().getCountry();
    }
    String sql = "SELECT l.* FROM Lc_Main l WHERE";
    if (country != null) {
      sql += " l.country='" + country + "' AND";
    }
    if (zid != null) {
      sql += " l.zoneid=" + zid + " AND";
    }
    if (cid != null) {
      sql += " l.centerid=" + cid+" AND";
    }
    if (status != null && status >= 0) {
      sql += " l.sts=" + status;
    }
    sql=StringUtils.trim(sql);
    sql=StringUtils.removeEndIgnoreCase(sql, "WHERE");
    sql=StringUtils.removeEndIgnoreCase(sql, "AND");
    Log.info(LifeCARDAdmin.class.getName() + ":listRegistrations():sql=" + sql);
    Query query = em.createNativeQuery(sql, LcMain.class);
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(LifeCARDAdmin.class.getName() + ":listRegistrations():" + pe.getMessage());
      return null;
    }
    return list;
  }

  /**
   * Get LifeCARD(R) administrative records only for entries where a card has
   * been assigned.
   * <p>
   * <b>Note:</b>If the status parameter is not 'null' only the records
   * (holders) with the given status of the plastic card will be listed.
   * </p>
   *
   * @param status
   * @return
   */
  public List<LcMain> listCards(Short status) {
    List<LcMain> list;
    //String sql = "SELECT distinct(l.lcid), l.* FROM lc_main l, lc_cad c WHERE l.lcid=c.lcid";
    //same result, but faster
    String where = "";
    if (status != null) {
      where = "WHERE l.status=" + status;
    }
    String sql = "SELECT distinct(l.lcid), l.* " + where + " FROM lc_main l JOIN lc_cad c on l.lcid=c.lcid";

    Log.finer(LifeCARDAdmin.class.getName() + ":listCards():sql=" + sql);
    Query query = em.createNativeQuery(sql, LcMain.class);
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(LifeCARDAdmin.class.getName() + ":listCards():" + pe.getMessage());
      return null;
    }
    return list;
  }

  /**
   * Get a list of centers using a LifeCARD(R) compatible app (if modid>0 a
   * locally deployed module defined in DefModule).
   * <p>
   * This list is based on a query that is listing LifeCARD modules by category
   * SEHRConstants.EHR_CATID in the NetServices table.
   * </p>
   *
   * @param zid - if >0 query given zone only
   * @param modid - if >0 query given module only
   * @return
   */
  public List<NetCenter> listCenters(int zid, int modid) {
    List<NetCenter> list;
    String wcZID = "";
    String selIN;
    if (zid > 0) {
      wcZID = " AND s.ZONEID=" + zid;
    }
    String sql = "SELECT c.* FROM Net_center c WHERE c.CENTERID IN ";
    if (modid > 0) {
      selIN = "(SELECT s.CENTERID FROM NET_SERVICES s, DEF_MODULE d WHERE s.MODID=d.MODID AND d.MODID=" + modid + " AND d.CATID=" + CatID + wcZID + ")";
    } else {
      selIN = "(SELECT s.CENTERID FROM NET_SERVICES s WHERE s.CATID=" + CatID + wcZID + ")";
    }
    sql += selIN;
    Query query = em.createNativeQuery(sql, NetCenter.class);
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.log(Level.WARNING, "{0}:listCenters()sql={1}:{2}", new Object[]{LifeCARDAdmin.class.getName(), sql, pe.getMessage()});
      return null;
    }
    return list;
  }

  /**
   * Get the administrative record including the LifeCARD(R) item object.
   * <p>
   * The LifeCARD(R) item is stored as a byte object (BLOB).
   * </p>
   *
   * @param lcid the id of the administrative record LC_MAIN
   * @return
   */
  public LcMain getLcMainByLcId(int lcid) {
    LcMain lcMain;
    Query query = em.createNamedQuery("LcMain.findByLcid", LcMain.class);
    query.setParameter("lcid", lcid);
    try {
      lcMain = (LcMain) JPAUtils.getSingleResult(query.getResultList());
    } catch (PersistenceException pe) {
      Log.warning(LifeCARDAdmin.class.getName() + ":getEntityByLcId():" + pe.getMessage());
      return null;
    }
    return lcMain;
  }

  /**
   * Get the person record assigned to the LifeCARD registration.
   * <p>
   * Each LifeCARD(R) registration is assigned to a 'real' person.
   * </p>
   *
   * @param prsid the prsid of the administrative record LC_MAIN
   * @return
   */
  public PrsMain getPrsMainByPrsId(int prsid) {
    PrsMain prsMain;
    Query query = em.createNamedQuery("PrsMain.findByPrsid", PrsMain.class);
    query.setParameter("prsid", prsid);
    try {
      prsMain = (PrsMain) JPAUtils.getSingleResult(query.getResultList());
    } catch (PersistenceException pe) {
      Log.warning(LifeCARDAdmin.class.getName() + ":getPrsMainByLcId():" + pe.getMessage());
      return null;
    }
    return prsMain;
  }

  /**
   * Get the administrative record including the LifeCARD(R) item object by
   * using the current number.
   * <p>
   * The holder may have multiple cards during a period of time. A card can be
   * stolen, out of date or may have a technically defect. By querying the
   * current card number by 'XX-0000000-0000000-00000000' we will get the
   * current LifeCard(R) item object representing the current plastic card. So
   * we need a subselection to get the assigned administrative record from
   * LC_MAIN.
   * </p>
   * <p>
   * N.B.: The LifeCARD(R) item is stored as a byte object (BLOB).
   * </p>
   *
   * @param number the number printed on the plastic card
   * @return
   */
  public LcMain getLcMainByNumber(String number) {
    LcMain lcMain = null;
    LifeCARDProcessor lcProc = LifeCARDProcessor.getInstance(sco, this.em.getEntityManagerFactory());
    try {
      lcMain = lcProc.searchByNumber(number);
    } catch (GenericSEHRException ex) {
      Logger.getLogger(LifeCARDAdmin.class.getName()).log(Level.SEVERE, null, ex);
    }

    return lcMain;
  }

  /**
   * Get the LifeCARD(R) object (the personal and public data printed on the
   * card) by the record ID of the administrative entity (LcMain).
   *
   * @param lcid the id of the administrative record LC_MAIN
   * @return LifeCardItem
   */
  public LifeCardItem getLifeCardItemByLcId(int lcid) {
    LifeCardItem lcItem = null;
    Log.info(LifeCARDAdmin.class.getName() + ":getLifeCardItemByLcId():lcid=" + lcid);
    LcMain lcMain = getLcMainByLcId(lcid);

    if (lcMain.getItem() != null) {
      try {
        lcItem = (LifeCardItem) DeSerializer.deserialize(lcMain.getItem());
      } catch (ObjectHandlerException ex) {
        Log.warning(LifeCARDAdmin.class.getName() + ":getLifeCardItemByLcId():" + ex.getMessage());
        try {

          //store for later convertion
          Object oldLcItem = (Object) DeSerializer.deserialize(lcMain.getItem());
          FileOutputStream fos = new FileOutputStream("/tmp/LifeCardItem-" + lcMain.getLcid() + "-" + System.currentTimeMillis() + ".obj");
          ObjectOutputStream oos = new ObjectOutputStream(fos);
          oos.writeObject(oldLcItem);
          oos.close();

        } catch (IOException | ObjectHandlerException ioex) {
          Logger.getLogger(LifeCARDAdmin.class.getName()).log(Level.SEVERE, null, ioex);
        }
        return null;
      }
    }
    return lcItem;
  }

  public LcCad getLcCadByPK(LcCadPK pk) {
    LcCad lcCad;
    Query query = em.createQuery("SELECT l FROM LcCad l WHERE l.lcCadPK.lcid = :lcid AND l.lcCadPK.cardid=:cardid", LcCad.class);
    query.setParameter("lcid", pk.getLcid());
    query.setParameter("cardid", pk.getCardid());
    try {
      lcCad = (LcCad) query.getSingleResult();
    } catch (PersistenceException pe) {
      Log.warning(LifeCARDAdmin.class.getName() + ":getLcCadByPK():" + pe.getMessage());
      return null;
    }
    return lcCad;
  }

  /**
   * Get the card item (the personal and public data printed on the card) by the
   * LifeCARD(R) registration number.
   *
   * @param ctry
   * @param zid
   * @param cid
   * @param pid
   * @return Item (e.g. for printing card) or 'null'
   */
  public LifeCardItem getLifeCardItem(String ctry, int zid, int cid, int pid) {
    LifeCardItem lcItem = null;
    LcMain lcMain;
    //Query query = em.createNamedQuery("LcMain.findByNumber", LcMain.class);
    //Query query = em.createQuery("SELECT l FROM LcMain l WHERE UPPER(l.country)=:ctry AND l.zoneid=:zid AND l.centerid=:cid AND l.patid=:pid", LcMain.class);
    Query query = em.createNativeQuery("SELECT l.* FROM Lc_Main l WHERE UPPER(l.country)=:ctry AND l.zoneid=:zid AND l.centerid=:cid AND l.patid=:pid", LcMain.class);
    query.setParameter("ctry", ctry.toUpperCase());
    query.setParameter("zid", zid);
    query.setParameter("cid", cid);
    query.setParameter("pid", pid);
    try {
      lcMain = (LcMain) JPAUtils.getSingleResult(query.getResultList());
    } catch (PersistenceException pe) {
      Log.warning(LifeCARDAdmin.class.getName() + ":getLifeCardItem():" + pe.getMessage());
      return null;
    }
    if (lcMain != null) {
      if (lcMain.getItem() != null) {
        try {
          lcItem = (LifeCardItem) DeSerializer.deserialize(lcMain.getItem());
        } catch (ObjectHandlerException ex) {
          Log.warning(LifeCARDAdmin.class.getName() + ":getLifeCardItem():" + ex.getMessage());
        }
      }
      //try to build item object
      if (lcItem == null) {
        //LcItem not yet stored...? DB record seems to be inconsistent
        LifeCARDProcessor lcProc = LifeCARDProcessor.getInstance(this.sco, this.em.getEntityManagerFactory());
        lcItem = lcProc.createLifeCardItem(lcMain);
      }
    } else {
      String printNumber = "";
      try {
        printNumber = LifeCARDObjectHandler.buildNumber(null, ctry, zid, cid, pid);
      } catch (GenericSEHRException ex) {
        //Logger.getLogger(LifeCARDAdmin.class.getName()).log(Level.SEVERE, null, ex);
      }
      Log.warning(LifeCARDAdmin.class.getName() + ":getLifeCardItem():No LcMain record for " + printNumber);
    }
    return lcItem;
  }

  public int processCardDeactivation(LcCad lcCad) {
    if (!init()) {
      return 0;
    }
    LcCadPK pk = lcCad.getLcCadPK();
    //native works, JPQL not
    //Query query = em.createNativeQuery("UPDATE lc_cad l SET l.sts=CAST(90 as SMALLINT) WHERE l.lcid = :lcid AND l.cardid=:cardid");
    Query query = em.createNativeQuery("UPDATE lc_cad l SET l.sts=90 WHERE l.lcid = :lcid AND l.cardid=:cardid");
    query.setParameter("lcid", pk.getLcid());
    query.setParameter("cardid", pk.getCardid());
    int i;
    try {
      i = query.executeUpdate();
      em.flush();
    } catch (PersistenceException pe) {
      Log.warning(LifeCARDAdmin.class.getName() + ":processCardDeactivation():" + pe.getMessage());
      return 0;
    }
    Log.fine(LifeCARDAdmin.class.getName() + ":processCardDeactivation():updated LcCad recs=" + i);
    return i;
  }

  public Object[] processCardOrder(LcMain lcMain) {
    if (!init()) {
      return null;
    }
    //SEHRConfigurationObject sco = new SEHRConfigurationObject();
    //sco.setZoneid(this.p.getProperty("zoneid", "0000000"));
    //looks up in 'java:comp/env/'
    //SEHRConfigurationObject sco = (SEHRConfigurationObject) sctx.lookup("SEHRConfigurationObject");
    LifeCARDProcessor fac = LifeCARDProcessor.getInstance(this.sco, this.em.getEntityManagerFactory());
    Object[] o = fac.processCardOrder(lcMain);
    return o;
  }

  /**
   * Add or update LcMain record.
   *
   * @param lcMain
   * @return
   */
  public boolean saveLcMain(LcMain lcMain) {
    try {
      if (lcMain.getLcid()!=null && lcMain.getLcid() > 0) {
        em.merge(lcMain);
      } else {
        em.persist(lcMain);
      }
    } catch (PersistenceException pe) {
      Log.warning(LifeCARDAdmin.class.getName() + "saveLcMain():" + pe.getMessage());
      return false;
    }
    return true;
  }

  /**
   * Get next PK (card issue number).
   *
   * @param lcMain
   * @return
   */
  public LcCadPK nextLcCadPK(LcMain lcMain) {
    //TODO implement library from LifeCARDProcessor
    //LifeCARDProcessor fac = LifeCARDProcessor.getInstance(this.sco, this.em, true);
    //return fac.nextLcCadPK(LcMain lcMain); 
    LcCadPK pk = new LcCadPK();
    Query qryCardSeq = em.createNativeQuery("select max(c.cardid) as cs from lc_cad c where c.lcid=" + lcMain.getLcid());
    try {
      Integer i = (Integer) qryCardSeq.getSingleResult();
      pk.setLcid(lcMain.getLcid());
      pk.setCardid(i == null ? 1 : ++i);
    } catch (PersistenceException ex) {
      Log.warning(LifeCARDAdmin.class.getName() + ":nextLcCadPK():" + ex.getMessage());
      return null;
    }
    Log.fine(LifeCARDAdmin.class.getName() + ":nextLcCadPK():" + pk.toString());
    return pk;
  }

  public boolean saveLcCad(LcCad lcCad) {
    if (lcCad.getLcCadPK() == null) {
      Log.info(LifeCARDAdmin.class.getName() + "saveLcCad():No PK (use nextLcCadPK() to get a valid record entry).");
      return false;
    }
    try {
      lcCad.setChanged(new Date());
      if (em.find(lcCad.getClass(), lcCad.getLcCadPK()) != null) {
        Log.info(LifeCARDAdmin.class.getName() + "saveLcCad():Updating " + lcCad.toString());
        em.merge(lcCad);
      } else {
        Log.info(LifeCARDAdmin.class.getName() + "saveLcCad():Creating " + lcCad.toString());
        em.persist(lcCad);
      }
    } catch (PersistenceException pe) {
      Log.info(LifeCARDAdmin.class.getName() + "saveLcCad():" + pe.getMessage());
      return false;
    }
    return true;
  }
}
