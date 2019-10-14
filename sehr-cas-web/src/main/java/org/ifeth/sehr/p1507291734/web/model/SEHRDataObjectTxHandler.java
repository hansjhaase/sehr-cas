/*
 * (C) IFETH
 */
package org.ifeth.sehr.p1507291734.web.model;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.activemq.ActiveMQConnection;
import org.apache.commons.lang.StringUtils;
import org.ifeth.sehr.core.exception.GenericSEHRException;
import org.ifeth.sehr.core.exception.ObjectHandlerException;
import org.ifeth.sehr.core.lib.DeSerializer;
import org.ifeth.sehr.core.objects.SEHRDataObject;
import org.ifeth.sehr.p1507291734.ejb.LifeCARDAdmin;

/**
 * DAO of REST service 'SEHRTXResource'.
 * <p>
 * The handler is responsible to transfer SEHRDataObject between REST WS and
 * ApacheMQ messaging queues if there is no port 61616 etc available - only a
 * standard http (port 80).
 * </p>
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
public class SEHRDataObjectTxHandler {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");

  private static final AtomicInteger counter = new AtomicInteger();

  public static int nextValue() {
    return counter.getAndIncrement();
  }

  private Map<Integer, SEHRDataObject> sdoCache = new ConcurrentHashMap<>();

  //@EJB - does not work here :( So we're using Intial Context so far...
  //private LifeCARDAdmin ejbLifeCARDAdmin;
  private InitialContext ic;
  private static SEHRDataObjectTxHandler instance;

  private SEHRDataObjectTxHandler() {
    //*** dummy entry for testing purposes ***
    SEHRDataObject sdo = new SEHRDataObject();
    Integer id = nextValue();
    sdo.setObjID(id);

    Map<String, Object> msgProperties = new HashMap<>();
    msgProperties.put("MsgType", "orderentry");
    msgProperties.put("ordertype", "pt");
    msgProperties.put("dataset", "Entity#Field");
    msgProperties.put("JMSType", "NONSEHR#JMED");
    msgProperties.put("origCenterId", 0601311);
    //we should not use this... Camel rewrites it
    msgProperties.put("JMSReplyTo", "sehr.9999999.0601311.queue");
    //to be discussed: rplyDestType
    //to be discussed: rplyDest
    //instead of 
    msgProperties.put("sehrReplyTo", "queue://sehr.9999999.0601311.queue");

    msgProperties.put("rcvCenterId", 99999980);
    //to be discussed: rcvDestType
    msgProperties.put("rcvDestType", "queue");
    msgProperties.put("rcvDest", "sehr.9999999.9999980.queue");
    //or msgProperties.put("rcvDest", "queue://sehr.9999999.0306010.queue");
    //msgProperties.put("rcvDest", "topic://sehr.9999999.0306010.queue");
    sdo.setSDOProperties(msgProperties);
    Map data = new HashMap();
    data.put("NoteXml#mndid", -1);
    //byte[] b = "I'm a JMed NOTE_XML or VDT data set".getBytes();
    //JMSS4C uses Entity#Field data set; this is based on a HashMap, 
    //so we must convert them to a byte stream
    //otherwise JaxB throws an error...
    //see also
    //https://www.evernote.com/Home.action#n=ae013491-35a9-47af-93d8-b931a2b6b64f&ses=4&sh=2&sds=5&
    if (data instanceof Map) {
      byte[] b;
      try {
        b = DeSerializer.serialize(data);
        sdo.setDataobject(b);
      } catch (ObjectHandlerException ex) {
        Logger.getLogger(SEHRDataObjectTxHandler.class.getName()).log(Level.SEVERE, null, ex);
      }
    } else {
      sdo.setDataobject(data);
    }
    //*** end of testing object ***
    sdoCache.put(id, sdo);
    Logger.getLogger(SEHRDataObjectTxHandler.class.getName()).log(Level.INFO, null, "SEHRDataObject for testing added, id=" + id);
    try {
      ic = new InitialContext();
      //ejbLifeCARDAdmin = (LifeCARDAdmin) ic.lookup("java:comp/env/ejb/sehr/LifeCardAdmin");
      //ejbLifeCARDAdmin = (LifeCARDAdmin) ic.lookup("LifeCardAdmin");
      //ejbLifeCARDAdmin = (LifeCARDAdmin) ic.lookup("java:global/sehr-cas-ear/sehr-cas-ejb-0.2/LifeCardAdmin");
      //List<LcMain> registrations = ejbLifeCARDAdmin.listRegistrations("DE", null, null);
      //for (LcMain lcMain : registrations) {
      //  LifeCardItem lcItem = new LifeCardItem();
      //  if (lcMain.getItem() != null) {
      //    try {
      //      lcItem = (LifeCardItem) DeSerializer.deserialize(lcMain.getItem());
      //    } catch (ObjectHandlerException ex) {
      //      Logger.getLogger(SEHRDataObjectTxHandler.class.getName()).log(Level.WARNING, null, ex.getMessage());
      //      //TODO class - BLOB (item) update required because it is unreadable
      //      lcItem.setSurname(lcMain.getSurname());
      //      lcItem.setFirstname(lcMain.getFirstname());
      //      lcItem.setSts((short) -1); //invalidate current item
      //    }
      //  }
      //  content.put(String.valueOf(lcMain.getLcid()), lcItem);
      //}
    } catch (NamingException ex) {
      Log.log(Level.SEVERE, ex.getMessage());
    }
  }

  public static synchronized SEHRDataObjectTxHandler getInstance() {
    if (instance == null) {
      instance = new SEHRDataObjectTxHandler();
    }
    return instance;
  }

  public SEHRDataObject getSDO(Integer id) {
    SEHRDataObject record = sdoCache.get(id);
    return record;
  }

  public Integer cacheSDO(SEHRDataObject sdo) {
    if (sdo.getObjID() == null) {
      Integer id = nextValue();
      sdo.setObjID(id);
    }
    sdoCache.put(sdo.getObjID(), sdo);
    return sdo.getObjID();
  }

  /**
   * Process and forward incoming SDO to a queue.
   *
   * @param amqCon
   * @param sdo
   * @return Message ID
   */
  public String processSDO(ActiveMQConnection amqCon, SEHRDataObject sdo) {
    //TODO refactor to use SEHRObjectHandler.forward2Broker() !

    String msgID = null; //reference to sent message for further processing
    Map<String, Object> msgProperties = sdo.getSDOProperties();
    try {
      Session session = amqCon.createSession(false, Session.AUTO_ACKNOWLEDGE);
      MapMessage msg = session.createMapMessage();
      String msgType = (String) msgProperties.get("MsgType");
      String orderType = (String) msgProperties.get("ordertype");
      String dataSet = (String) msgProperties.get("dataset");
      String JMSType = (String) msgProperties.get("JMSType");
      int origCenterId = (int) msgProperties.get("origCenterId");
      //we should not use this... Camel rewrites it
      String replyTo = (String) msgProperties.get("JMSReplyTo");
      //to be discussed: rplyDestType
      //to be discussed: rplyDest
      //instead of 
      String sehrReplyTo = (String) msgProperties.get("sehrReplyTo");
      //, "queue://sehr.9999999.0601311.queue");

      String rcvCenterId = (String) msgProperties.get("rcvCenterId");
      //to be discussed: rcvDestType
      String rcvDestType = (String) msgProperties.get("rcvDestType");//, "queue");
      String rcvDest = (String) msgProperties.get("rcvDest");//, "sehr.9999999.9999980.queue");
      Destination destination = session.createQueue(rcvDest);
      Destination rplyDest = session.createQueue(replyTo);
      MessageProducer producer = session.createProducer(destination);

      //do not use note_xml entity pojo 
      //see patterns for async and messaging over a lot of potential receivers 
      //ObjectMessage msg = (ObjectMessage) session.createObjectMessage();
      //NoteXML notexml = new NoteXML();
      //... set fields
      //SEHRDataObject sdo = new SEHRDataObject();
      //sdo.setDataobject(notexml);
      //... use standardized SEHR object for transfers
      //msg.setObject(sdo);
      //inform receiver where to reply in case of errors or confirming
      msg.setJMSReplyTo(rplyDest);
      //msg.setJMSType("JMED");
      //https://www.evernote.com/shard/s306/sh/ae013491-35a9-47af-93d8-b931a2b6b64f/6bc3e7174397c1894a77cb1a2a1c4fe4
      msg.setJMSType(JMSType); //since 8/2015

      //'orderentry' due to processing data as order on target
      msg.setStringProperty("MsgType", msgType);
      msg.setStringProperty("ordertype", orderType); //pharmacy treatment
      //data originator (producer, sender of the message)
      msg.setIntProperty("origCenterId", origCenterId);
      //TODO get name of center (same as/from SEHR registration)
      msg.setStringProperty("origCenterName", "n/a");
      //NOTE_XML record transfer as Entity#Field mapping
      msg.setStringProperty("dataset", dataSet);
      Object oData = null;
      if (StringUtils.startsWithIgnoreCase(dataSet, "Entity#Field")) {
        //special use case: the Map object has been packed for HTTP/XML REST
        if (sdo.getDataobject() instanceof byte[]) {
          oData = (Map) DeSerializer.deserialize((byte[]) sdo.getDataobject());
        } else if (sdo.getDataobject() instanceof Map) {
          oData = sdo.getDataobject();
        } else {
          Logger.getLogger(SEHRDataObjectTxHandler.class.getName()).log(Level.SEVERE, null, "Invalid object type for 'Entity#Field': 'Map' required!");
        }
        if (oData != null) {
          msg.setObject("data", oData);
        } else {
          Logger.getLogger(SEHRDataObjectTxHandler.class.getName()).log(Level.SEVERE, null, "No data!");
        }
      } else {
        //POJO#, SER#, XML# can be used straight forward
        oData = sdo.getDataobject();
      }

      if (oData != null) {
        msg.setObject("data", oData);
        String subject = "JMed-SEHR Message via SEHR WS";
        msg.setStringProperty("subject", subject);
        producer.send(msg);
        msgID = msg.getJMSMessageID();
      }
    } catch (JMSException ex) {
      Logger.getLogger(SEHRDataObjectTxHandler.class.getName()).log(Level.SEVERE, null, ex.getMessage());
      return null;
    } catch (ObjectHandlerException ex) {
      Logger.getLogger(SEHRDataObjectTxHandler.class.getName()).log(Level.SEVERE, null, ex.getMessage());
    }
    return msgID;
  }
}
