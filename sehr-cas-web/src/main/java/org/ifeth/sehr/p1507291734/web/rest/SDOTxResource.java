/*
 * IFETH 2012
 */
package org.ifeth.sehr.p1507291734.web.rest;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.enterprise.context.RequestScoped;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.servlet.ServletContext;
//import static javax.ws.rs.HttpMethod.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.activemq.ActiveMQConnection;
import org.ifeth.sehr.core.exception.ObjectHandlerException;
import org.ifeth.sehr.core.lib.DeSerializer;
import org.ifeth.sehr.core.objects.SEHRDataObject;
import org.ifeth.sehr.p1507291734.web.model.SEHRDataObjectTxHandler;

/**
 * REST Web Service to transfer SEHRDataObject through HTTP.
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@RequestScoped
@Path("sdotx")
public class SDOTxResource {

  @Context
  private ServletContext sctx;

  /**
   *
   * @param queue
   * @return SEHRDataObject
   */
  @GET
  @Path("list/{queue}")
  //@Produces("application/xml")
  @Produces(MediaType.TEXT_XML)
  public List<SEHRDataObject> listMessages(@PathParam("queue") String queue) {
    List<SEHRDataObject> l = new ArrayList<>();
    ActiveMQConnection amqcon = (ActiveMQConnection) sctx.getAttribute("ActiveMQConnection");

    try {
      QueueSession sess = amqcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
      //ActiveMQQueue q = new ActiveMQQueue(queue);
      //QueueBrowser queueBrowser = sess.createBrowser((Queue) q);
      Queue q = sess.createQueue(queue);
      QueueBrowser queueBrowser = sess.createBrowser(q);
      amqcon.start();

      Enumeration<Message> eq = queueBrowser.getEnumeration();
      while (eq.hasMoreElements()) {
        Message message = (Message) eq.nextElement();
        SEHRDataObject sdo = new SEHRDataObject();
        sdo.setObjName(message.getJMSMessageID());
        l.add(sdo);
      }
      sess.close();
    } catch (JMSException ex) {
      Logger.getLogger(SDOTxResource.class.getName()).log(Level.SEVERE, null, ex);
    }
    //System.out.println(l.size() + " messages");
    return l;
  }

  /**
   * Retrieve message with given ID from queue.
   *
   * @param queue
   * @param msgid
   * @return SEHRDataObject
   */
  @GET
  @Path("read/{queue}/{msgid}")
  @Produces("application/xml")
  public SEHRDataObject readMessage(
          @PathParam("queue") String queue,
          @PathParam("msgid") String msgid) {
    ActiveMQConnection amqcon = (ActiveMQConnection) sctx.getAttribute("ActiveMQConnection");
    //SEHRDataObjectTxHandler sdoTxHandler = SEHRDataObjectTxHandler.getInstance();
    SEHRDataObject sdo = new SEHRDataObject();
    try {
      QueueSession sess = amqcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
      //ActiveMQQueue q = new ActiveMQQueue(queue);
      //QueueBrowser queueBrowser = sess.createBrowser((Queue) q);
      Destination q = sess.createQueue(queue);
      MessageConsumer consumer = sess.createConsumer(q, "JMSMessageID='" + msgid + "'");
      //get specified message
      amqcon.start();
      Message message = consumer.receiveNoWait();
      if (message == null) {
        Logger.getLogger(SDOTxResource.class.getName()).warning("No message wit ID " + msgid);
        consumer.close();
        sess.close();
        return null;
      }
      if (message instanceof MapMessage) {
        MapMessage mm = (MapMessage) message;
        Map<String, Object> msgProperties = txMsgHeader2SDOHeader(message);
        //'param' is part of data / body...
        //TODO check specification
        Object oParam = mm.getObject("param");
        if (oParam != null) {
          msgProperties.put("param", oParam);
        }
        sdo.setSDOProperties(msgProperties);
        //TODO process data / body / content
        Object oData = mm.getObject("data");
//        if (oData instanceof Map){
//          try {
//            //WebService does not accept a Map
//            byte[] b = DeSerializer.serialize(oData);
//            sdo.setDataobject(b);
//          } catch (ObjectHandlerException ex) {
//            Logger.getLogger(SDOTxResource.class.getName()).log(Level.SEVERE, null, ex);
//          }
//        }else{
//          sdo.setDataobject(oData);
//        }
        //always serialize...
        try {
          //WebService does not accept some types (like Map)
          byte[] b = DeSerializer.serialize(oData);
          sdo.setDataobject(b);
        } catch (ObjectHandlerException ex) {
          Logger.getLogger(SDOTxResource.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
        //System.out.println("Received and marshalled: " + message.toString());
        Logger.getLogger(SDOTxResource.class.getName()).info("Received and marshalled: " + message.toString());
      } else if (message instanceof ObjectMessage) {
        Logger.getLogger(SDOTxResource.class.getName()).info("ObjectMessage received. byte[] transforming of message: " + message.toString());
        try {
          //WebService does not accept some types (like Map)
          byte[] b = DeSerializer.serialize(((ObjectMessage) message).getObject());
          sdo.setDataobject(b);
        } catch (ObjectHandlerException ex) {
          Logger.getLogger(SDOTxResource.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
        //System.out.println("Received but not marshalled, not a MapMessage: " + message.toString());
      } else if (message instanceof TextMessage) {
        Logger.getLogger(SDOTxResource.class.getName()).info("ObjectMessage received. byte[] transforming of message: " + message.toString());
        sdo.setDataobject(((TextMessage) message).getText());
      } else {
        Logger.getLogger(SDOTxResource.class.getName()).warning("No processor for message: " + message.toString());
      }
      consumer.close();
      sess.close();
    } catch (JMSException ex) {
      Logger.getLogger(SDOTxResource.class.getName()).log(Level.SEVERE, null, ex.getMessage());
      return null;
    }
    return sdo;
  }

  /**
   * Get SDO from cache list (memory).
   *
   * @param id
   * @return SEHRDataObject
   */
  @GET
  @Path("get/{id}")
  @Produces("application/xml")
  public SEHRDataObject getSEHRDataObject(@PathParam("id") int id) {
    //TODO return proper object, selcted by a list the client receives before
    //throw new UnsupportedOperationException();
    SEHRDataObjectTxHandler sdoTxHandler = SEHRDataObjectTxHandler.getInstance();
    SEHRDataObject sdo = sdoTxHandler.getSDO(id);
    //SEHRDataObject sdo  = new SEHRDataObject();
    //sdo.setObjID(IDGenerator.generateID());
    return sdo;
  }

  @POST
  @Consumes(MediaType.APPLICATION_XML)
  @Produces(MediaType.APPLICATION_XML)
  public Response processSDO(SEHRDataObject sdo) throws URISyntaxException {
    if (sdo == null) {
      return Response.status(400).entity("Please add SDO!").build();
    }

    if (sdo.getDataobject() == null) {
      return Response.status(400).entity("Please provide the SDO data !").build();
    }
    SEHRDataObjectTxHandler sdoTxHandler = SEHRDataObjectTxHandler.getInstance();
    sdoTxHandler.cacheSDO(sdo);
    ActiveMQConnection amqCon = (ActiveMQConnection) sctx.getAttribute("ActiveMQConnection");
    if (amqCon != null && !amqCon.isClosed()) {
      String msgID = sdoTxHandler.processSDO(amqCon, sdo);
      //TODO parse result and create a response that includes the ID
      //return Response.created(new URI("/rest/sdotx/get/" + sdo.getObjID())).build();
      return Response.status(201).entity(msgID).build();
    }
    return Response.serverError().entity("Processing error!").build();
  }

  /**
   * DELETE method for resource SDOTxResource
   */
  @DELETE
  public void delete() {
  }

  private Map<String, Object> txMsgHeader2SDOHeader(Message msg) throws JMSException {
    Map<String, Object> msgProperties = new HashMap<>();
    msgProperties.put("MsgType", msg.getStringProperty("MsgType"));
    msgProperties.put("ordertype", msg.getStringProperty("ordertype"));
    msgProperties.put("dataset", msg.getStringProperty("dataset"));
    msgProperties.put("JMSType", msg.getJMSType());
    msgProperties.put("origCenterId", msg.getIntProperty("origCenterId"));
    //we should not use this... Camel rewrites it
    msgProperties.put("JMSReplyTo", msg.getJMSReplyTo().toString());
    if (msg.propertyExists("sehrReplyTo")) {
      //to be discussed: rplyDestType
      //to be discussed: rplyDest
      //instead of 
      msgProperties.put("sehrReplyTo", msg.getStringProperty("sehrReplyTo"));
    }
    if (msg.propertyExists("rcvCenterId")) {
      msgProperties.put("rcvCenterId", msg.getIntProperty("rcvCenterId"));
    }
    //to be discussed: rcvDestType
    //msgProperties.put("rcvDestType", "queue");
    //msgProperties.put("rcvDest", "sehr.9999999.9999980.queue");
    //or msgProperties.put("rcvDest", "queue://sehr.9999999.0306010.queue");
    //msgProperties.put("rcvDest", "topic://sehr.9999999.0306010.queue");
    return msgProperties;
  }
}
