/*
 * (C) IFETH
 *
 * Refactored from 'sehr-access' EJB to implement it as a maven managed 
 * component in EAR package.
 */
package org.ifeth.sehr.p1507291734.ejb;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.commons.lang3.StringUtils;
import org.ifeth.sehr.core.lib.IDGenerator;
import org.ifeth.sehr.core.objects.UserSessionObject;
import org.ifeth.sehr.intrasec.entities.UsrMain;
import org.ifeth.sehr.intrasec.lib.JPAUtils;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@Stateless(name="AccessControl")
public class AccessControl{

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.ejb");

  private static final SimpleDateFormat sdfSQL = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  
  // --- status ACC_USRLOG
  //-1=invalid, 0=valid, 1=logged out, reloging required
  private static final short LOGGEDIN=0;
  private static final short LOGGEDOUT=1;
  
  // --- key/password generator
  private static final int PWDLEN = 6;
  private static final String PASSWDCHARS = "abcdefghijklmnopqrstuvwxyz0123456789$*+{[]}";

  @PersistenceContext(unitName = "HibJTAIntraSEC")
  private EntityManager em;
  
  @EJB
  private ACLManager acl;

  public String getRandomPassword() {
    int n = PASSWDCHARS.length();
    char[] cbuf = new char[PWDLEN];
    for (int i = 0; i
            < PWDLEN; i++) {
      int randint = (int) (Math.random() * n);
      cbuf[i] = PASSWDCHARS.charAt(randint);
    }
    return new String(cbuf);
  }

  /**
   * Check login to a SEHR location (zone).
   *
   * <p>
   * This method checks if a user is allowed to use SEHR. It does not check or
   * allow the login to a service. <br/>
   * <u>Note:</u>Not all users need the allowance to login to a zone. Most of
   * the users are just using services of a zone.
   * </p>
   *
   * @param username
   * @param pass
   * @param locid - zoneid
   * @param sessionid
   * @return
   */
  public UserSessionObject login2SEHRLocation(String username, String pass, int locid, String sessionid) {
    Log.fine(AccessControl.class.getName()+":login2SEHRLocation():Login2Zone/Center' requested by "+username);
//    Query query = em.createNativeQuery("select count(usrid) from usr_main");
//    int n = (Integer) query.getSingleResult();
//    Log.log(Level.FINEST, "UserSessionBean:Login2SEHRLocation(): {0} users found", n);
//    if (n == 0) {
//      Log.warning(" Login failed (no user records)");
//      return null;
//    }
    if (StringUtils.isBlank(sessionid)) {
      sessionid = Integer.toString(IDGenerator.generateID());
    }
    String sql = "select usrid, status, lastlogin from login2sehrzone("
            + "'"+username+"'"
            + ",'" + pass +"'"
            + "," + locid
            + ",'" + sessionid+"'"
            + ")";
    Log.log(Level.FINEST, AccessControl.class.getName()+":Login2SEHRLocation():Processing native sql={0}", sql);
    Query query = em.createNativeQuery(sql);
    Object[] result = (Object[]) query.getSingleResult();
    if (result == null) {
      Log.warning(AccessControl.class.getName()+":login2SEHRLocation():Login failed (no result returned)");
      return null;
    }
    int status = (Integer) result[1];
    int usrid = (Integer) result[0];
    java.sql.Timestamp lastLoginDTS = (java.sql.Timestamp) result[2];
    if (status > 0) {
      Log.log(Level.INFO, AccessControl.class.getName()+":Login2SEHRLocation():Login failed (status={0})", status);
      return null;
    }
    //not 'find', we do not need a record for modifying
    Query qryUsrMain = em.createQuery("select u from UsrMain u where u.usrid=:usrid",UsrMain.class);
    qryUsrMain.setParameter("usrid", usrid);
    UsrMain um = (UsrMain) JPAUtils.getSingleResult(qryUsrMain.getResultList());
    if (um == null) {
      Log.warning(AccessControl.class.getName()+":login2SEHRLocation():Oupps - User object not found.");
      return null;
    }
    //System.out.println(aclUser.getUserRoles("full").size());

    UserSessionObject userSession = new UserSessionObject();
    userSession.setUsrid(usrid);
    userSession.setSessionid(sessionid);
    userSession.setStatus(status);
    userSession.setUsrMain(um);
    if (lastLoginDTS != null) {
      userSession.setLastloggedin(lastLoginDTS);
    }

    Map permMap = acl.getUserACL(usrid);
    userSession.setUserPerms(permMap);
    Map rolesMap = acl.getUserRoles(usrid, "id_name");
    Log.log(Level.FINE, AccessControl.class.getName()+":login2SEHRLocation():{0} user role(s)", rolesMap.size());
    userSession.setUserRoles(rolesMap);

    Log.log(Level.INFO, AccessControl.class.getName()+":login2SEHRLocation():Login passed, last login was {0}", lastLoginDTS);
    return userSession;
  }
  
  public boolean logout(int usrid) {
    
    Date dts = new Date();
    try {
      Query query = em.createNativeQuery("update acc_usrlog set status="+LOGGEDOUT+", logoutdts='" + sdfSQL.format(dts) + "' where usrid=" + usrid);
      query.executeUpdate();    
      //TODO do other stuff if required
    } catch (Exception e) {
      Log.log(Level.WARNING, AccessControl.class.getName()+":logout(userid={0}):Logout from zone failed. Error: {1}", new Object[]{usrid, e.getMessage()});
      return false;
    }
    return true;
  }
}
