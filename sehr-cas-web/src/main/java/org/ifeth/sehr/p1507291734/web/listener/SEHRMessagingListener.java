package org.ifeth.sehr.p1507291734.web.listener;

import com.google.common.eventbus.EventBus;
import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.advisory.AdvisorySupport;
import org.ifeth.sehr.p1507291734.lib.Constants;
import org.ifeth.sehr.p1507291734.lib.LoggerUtility;
import org.ifeth.sehr.p1507291734.web.beans.ChatMsg;
import org.ifeth.sehr.p1507291734.web.beans.NewChatMsgEvent;

/**
 * Very basic messaging handler.
 *
 * This handler is based on JMedS4C.
 */
public class SEHRMessagingListener implements MessageListener {

  private static final Logger logger = Logger.getLogger("org.ifeth.sehr.p1507291734.web");

  private static final String FS = System.getProperty("file.separator");
  private static final String MSGDIR = System.getProperty("java.io.tmpdir") + FS + "SEHR" + FS;

  //XMPP like actions are: 
  //- messaging (a plain message from-to single or group user/center/zone)
  //- iq (interactive query), requst-response, 
  //- set/get presence (user/connection status)
  //types of messages (see SEHRDataObjectConstants, MessageType enums):
  //-- message
  //-- notification (sending notes; publish, subscribe use cases),
  //iq examples
  //-- request (get / set)
  //   --- get: query, list, item
  //   --- set: item
  //-- response
  //   --- Object
  //   --- List<Objects>
  //   --- String (canbe a JSON, xml string)
  //
  public static final String PROPMSGTYPE = "MsgType"; //see SEHR spec

  /**
   * Types of messages.
   *
   *
   * @deprecated use SEHRDataObjectConstants, MessageType enums
   */
  public enum MessageType {
    MESSAGE, /*EIS pattern: message*/
    COMMAND, /*EIS pattern: command*/
    NOTIFICATION, /*EIS pattern: a note, memo, alert (high prio), sms or ack etc*/
    REQUEST, /*EIS pattern: request/response*/
    RESPONSE, /*EIS pattern: request/response*/
    FILE, /*EIS pattern: document*/
    ORDERENTRY, /*EIS pattern: document*/
    HL7, /*EIS pattern: request/response - response is the ACK*/
    RECORD, /*EIS pattern: document*/
    ICAL, /*EIS pattern: event, iCal message, a TASK, EVENT*/
    NUL /*undefined*/
  }

  //--- global and common resuable (singleton) runtime objects
  private Properties p;
  //private ActiveMQConnectionFactory connectionFactory;
  private final ActiveMQConnection amqCon;
  //--- session/thread related objects
  private Session jmsSession;
  //... current chat room
  private Destination curDestination = null;
  private MessageConsumer curConsumer;
  private MessageProducer curProducer;
  //private TextMessage chatMessage;
  private String chatNick = "Guest";
  private int debug;
  //types of chat messages
  public static short PUBLIC = 0, PRIVATE_ZONE = 1,
          PRIVATE_CENTER = 2, PRIVATE_USER = 3, ADMIN = 9;
  private int zoneid = -1; //none before initialization
  private String sZoneID = null;
  /* The session has to be assigned to a concrete WEB or GUI user session! */
  private String sessID = null;
  private String origUUID = null;
  private final EventBus eventBus; //internal bus for events (not for HTML5)

  public SEHRMessagingListener(EventBus evtBus, String sess, ActiveMQConnection amqCon) {
    this.eventBus = evtBus;
    this.sessID = sess;
    this.amqCon = amqCon;
  }

  @Override
  public void onMessage(Message message) {
    try {
      logger.log(Level.FINE, SEHRMessagingListener.class.getName() + ":onMessage():Message received - JMSType:" + message.getJMSType());
      if (message.getStringProperty(PROPMSGTYPE).equalsIgnoreCase("message")) {
        ChatMsg chatMsg = SEHRMessagingListener.UnmarshallingIncomingMessage(message);
        //'ChatMsgType' is a MUST attribute indication a simple text chat
        //JMedS4C/SEHR based text message
        if (chatMsg.getType() != null) {
          String msg = null;
          logger.log(Level.FINEST, SEHRMessagingListener.class.getName() + ":onMessage():ChatMsgType=" + chatMsg.getType());
          if (chatMsg.getOrigUUID() != null && chatMsg.getOrigUUID().equals(this.origUUID)) {
            //prepare output text for own message sent and received back from topic
            msg = chatMsg.getFrom();
            if (chatMsg.getPrivateToNick() != null) {
//            aset = sc.addAttribute(SimpleAttributeSet.EMPTY,
//                    StyleConstants.Foreground, Color.decode("#008423"));//green
              msg += "@U:" + chatMsg.getPrivateToNick();
            } else if (chatMsg.getCenterTo() != null) {
              msg += "@C:" + String.format("%07d", chatMsg.getCenterTo());
            } else {
              msg += "@*";
            }
//            aset = sc.addAttribute(SimpleAttributeSet.EMPTY,
//                    StyleConstants.Foreground, Color.decode("#797979"));//gray
            msg += ": " + chatMsg.getText();

          } else {
            //display received from other source
            if (chatMsg.getType() == PRIVATE_USER) {
                //aset = sc.addAttribute(SimpleAttributeSet.EMPTY,
              //        StyleConstants.Foreground, Color.decode("#008423"));//green
              msg = chatMsg.getFromPrivateToText("@", ":");
            } else if (chatMsg.getType() == PRIVATE_CENTER) {
                //int cid = Integer.parseInt(p.getProperty("centerID", "0"));
              //if (textMessage.getStringProperty("ChatMsgCenterTo").equalsIgnoreCase(String.format("%07d", cid))) {
              //aset = sc.addAttribute(SimpleAttributeSet.EMPTY,
              //        StyleConstants.Foreground, Color.CYAN);
              msg = chatMsg.getFrom() + "@" + chatMsg.getCenterTo() + ":" + chatMsg.getText();

            } else if (chatMsg.getType() == PUBLIC) {
                //public message
              //aset = sc.addAttribute(SimpleAttributeSet.EMPTY,
              //        StyleConstants.Foreground, Color.BLACK); //Color.decode("#003366")
              msg = chatMsg.getFrom() + ": " + chatMsg.getText();
            }
          }

            //TODO Store in DB to present msgs as a history from DB
          //store (to dir) and notify other components about new message
          this.eventBus.post(new NewChatMsgEvent(chatMsg, msg));

          Writer fw = null;
          String chatFile = MSGDIR + this.sessID;
          try {
            fw = new FileWriter(chatFile, true);
            fw.append(msg + System.getProperty("line.separator")); // e.g. "\n"
          } catch (IOException e) {
            logger.log(Level.WARNING, "'" + chatFile + "' file error:" + e.getMessage());
          } finally {
            if (fw != null) {
              try {
                fw.close();
              } catch (IOException e) {
                logger.severe(e.getMessage());
              }
            }
          }

        }
      } else {
        //other SEHR message
        logger.log(Level.WARNING, SEHRMessagingListener.class.getName() + ":onMessage():Other SEHR message: {0}", message.getClass().getName());
      }
    } catch (JMSException jmse) {
      logger.log(Level.WARNING, SEHRMessagingListener.class.getName() + ":onMessage():JMSException{0}", jmse.getMessage());
    }
  }

  public boolean activateAdvisory() {
    if (curDestination == null) {
      return false;
    }
    try {
      //Destination advisoryDestination = AdvisorySupport.getConnectionAdvisoryTopic();
      //Destination advisoryDestination = AdvisorySupport.getDestinationAdvisoryTopic(currentDestination);
      Destination advisoryDestination = AdvisorySupport.getConsumerAdvisoryTopic(curDestination);
      //Destination advisoryDestination = AdvisorySupport.getNoConsumersAdvisoryTopic(currentDestination);
      MessageConsumer advisory = jmsSession.createConsumer(advisoryDestination);
      advisory.setMessageListener(this);
    } catch (JMSException ex) {
      logger.log(Level.SEVERE, this.getClass().getName() + ":activateAdvisory():JMS error " + ex.getMessage());
      return false;
    }
    return true;
  }

  /**
   * Creates chat session for the given zone.
   *
   * @return AMQ session handle
   */
  public Session createSession(Integer zid) {
    if (this.sessID == null || amqCon == null || amqCon.isClosed()) {
      logger.log(Level.FINE, SEHRMessagingListener.class.getName() + ":createSession():A client (WEB or GUI) session ID or connection is required.");
      return null;
    }
    //get environment settings
    InitialContext ic;
    try {
      ic = new InitialContext();
      p = (Properties) ic.lookup(Constants.ICPropName);
      debug = Integer.parseInt(p.getProperty("debug", "2"));
      LoggerUtility.assignLevelByDebug(debug, logger);
      //+++ other messages will not be processed...
      //if(debug==9){
      //  activateAdvisory();
      //}
      logger.log(Level.FINE, this.getClass().getName() + ":createSession():debug={0}, logger={1}", new Object[]{debug, logger.getLevel()});
    } catch (NamingException e) {
      logger.severe(this.getClass().getName() + ":createSession():JNDI error:" + e.toString());
      //debug = 9; //on errors switch debugging on
      return null;
    }

    if (zid == null) {
      //get local zone ID this service is running for
      this.zoneid = Integer.parseInt(p.getProperty("zoneid", "9999999"));
    } else {
      this.zoneid = zid;
    }
    this.sZoneID = String.format("%07d", this.zoneid);

    //create root directory for chat files if required
    File dir = new File(MSGDIR);
    if (!dir.exists()) {
      if (!dir.mkdirs()) {
        return null;
      }
    }

    try {
      // create a chat session
      this.jmsSession = amqCon.createSession(false, Session.AUTO_ACKNOWLEDGE);
      logger.log(Level.FINE, this.getClass().getName() + ":createSession():JMS chat session established to {0}", this.sZoneID);
      //joinRoom("public", "Guest"); //default is zoneID.puplic as guest
      // now that everything is ready to go, (re)start the connection
      //this.amqCon.start();
      //flag messages of this handler session
      this.origUUID = UUID.randomUUID().toString();
    } catch (JMSException ex) {
      logger.log(Level.WARNING, this.getClass().getName() + ":createSession():JMS Error{0}", ex.toString());
      return null;
    }

    Writer fw;
    String chatFile = MSGDIR + this.sessID;
    try {
      fw = new FileWriter(chatFile, true);
      fw.append(System.getProperty("line.separator")); // e.g. "\n"
    } catch (IOException e) {
      logger.log(Level.WARNING, "'" + chatFile + "' file creation error:" + e.getMessage());
    }

    return this.jmsSession;
  }

  /**
   * Join room to chat. Prefix of all room names is 'sehr.[ZoneID].chat.-' and
   * then -.public (visible to all users of a zone) -.center.[centerID] (visible
   * to all users of a center) -.user.[userID]> (of IntraSec) for a private
   * chat) The chat is a zone related service but if there are zone bridges the
   * message will be sent to other zones!
   *
   * @param room 'public', 'center.[CENTERID]' or 'user.[USERID of UsrMain]'
   * @param nick
   */
  public void joinRoom(String room, String nick) {
    if (this.jmsSession == null) {
      logger.log(Level.WARNING, this.getClass().getName() + ":joinRoom():No AMQ session!");
      return;
    }
    this.chatNick = nick;
    if (!room.matches("^[^\\.].*")) {
      logger.log(Level.WARNING, this.getClass().getName() + ":joinRoom():Invalid room syntax!");
      return;
    }
    String topicRoom = "sehr." + this.sZoneID + ".chat." + room;

    if (curDestination != null) {
      sendMsg("hat den Chatraum verlassen.");
      try {
        curConsumer.close();
        curProducer.close();
      } catch (JMSException e) {
        logger.log(Level.WARNING, this.getClass().getName() + ":joinRoom():{0}", e.toString());
      }
    }

    try {
      curDestination = jmsSession.createTopic(topicRoom);
      curConsumer = jmsSession.createConsumer(curDestination);
      curConsumer.setMessageListener(this);
      if (amqCon.isStarted()) {
        amqCon.start();
      }
      // create default/public producer
      curProducer = jmsSession.createProducer(curDestination);
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      //sendMsg("joined the room (" + sdf.format(new Date()) + ")");
      sendMsg("ist nun erreichbar (" + sdf.format(new Date()) + ")");
    } catch (JMSException e) {
      logger.log(Level.WARNING, this.getClass().getName() + ":joinRoom():{0}", e.toString());
    }
  }

  public boolean isSession() {
    return (jmsSession != null);
  }

  public boolean isJoined() {
    return (curDestination != null);
  }

  public void sendMsg(String msg) {
    sendMsg(msg, SEHRMessagingListener.PUBLIC, -1, -1, -1);
  }

  public void sendMsg(String msg, short chatMsgType, int chatMsgZoneTo, int chatMsgCenterTo, int chatMsgPrivateTo) {
    TextMessage chatMessage; //or MapMessage
    try {
      String targetRoom = null;
      String targetZoneId = this.sZoneID; //default: use current by joinRoom
      chatMessage = jmsSession.createTextMessage();
      //body
      chatMessage.setText(msg);
      //header
      chatMessage.setStringProperty(PROPMSGTYPE, "message");
      chatMessage.setStringProperty("ChatMsgZoneFrom", this.sZoneID);
      if (chatMsgZoneTo > 0) {
        if (this.zoneid != chatMsgZoneTo) {
          targetZoneId = String.format("%07d", chatMsgZoneTo); //to other zone
          chatMessage.setStringProperty("ChatMsgZoneTo", targetZoneId);
          //TODO implement XNET routing
          targetRoom = "sehr." + targetZoneId + ".chat.public"; //is zone based target
        }
      }
      //is it a message for a given center (non-public within zone)?
      if (chatMsgCenterTo > 0) {
        chatMessage.setStringProperty("ChatMsgCenterTo", String.format("%07d", chatMsgCenterTo));
        targetRoom = "sehr." + targetZoneId + ".chat.center." + String.format("%07d", chatMsgCenterTo);
      }
      //and is it a private message to a registered user of the SEHR zone?
      //the UsrID is unique over the zone (but not over other zones!)
      if (chatMsgPrivateTo > 0) {
        //TODO use and get real nick from DB
        chatMessage.setStringProperty("ChatMsgPrivateTo", String.format("%08d", chatMsgPrivateTo));
        chatMessage.setIntProperty("ChatMsgPprivateToUserID", chatMsgPrivateTo);
        targetRoom = "sehr." + targetZoneId + ".chat.user." + String.format("%08d", chatMsgPrivateTo);
      }
      chatMessage.setShortProperty("ChatMsgType", chatMsgType);
      chatMessage.setStringProperty("ChatMsgFrom", this.chatNick);
      //TODO implement FromUserID based on a SEHR zone registration
      if (targetRoom != null) {
        //define another (second) producer for the new target if required
        Destination targetDest = jmsSession.createTopic(targetRoom);
        MessageProducer producer = jmsSession.createProducer(targetDest);
        //producer.setTimeToLive(30000);
        producer.send(chatMessage);
        logger.log(Level.FINEST, this.getClass().getName() + ":sendMsg():message {0} sent to {1}", new Object[]{chatMessage, targetRoom});
        producer.close();
      } else {
        //chatProducer.setTimeToLive(30000);
        curProducer.send(chatMessage);
        logger.log(Level.FINEST, this.getClass().getName() + ":sendMsg():message {0} sent to {1}", new Object[]{chatMessage, curProducer.getDestination()});
      }

    } catch (JMSException ex) {
      logger.log(Level.WARNING, this.getClass().getName() + ":sendMsg():JMS Error:{0}", ex.toString());
      try {
        jmsSession.close();
      } catch (JMSException ex2) {
        //ignore error on errors
      } finally {
        jmsSession = null; //invalidate session on errors!
      }
    }
  }

  public void closeSession() {
    if (jmsSession == null) {
      return;
    }
    try {
      if (this.curDestination != null) {
        //TODO I18N
        sendMsg("Chat session closed.");
        this.curConsumer.close();
        this.curProducer.close();
      }
      if (jmsSession.getTransacted()) {
        jmsSession.commit();
      }
      jmsSession.close();
    } catch (JMSException ex) {
      logger.log(Level.WARNING, SEHRMessagingListener.class.getName() + ":closeSession():JMS Error:{0}", ex.toString());
    }
    jmsSession = null;

    File fChatFile = new File(MSGDIR + this.sessID);
    if (fChatFile.canWrite()) {
      fChatFile.delete();
    }

  }

  public String getMessages(String sessionID) {
    StringBuilder contents = new StringBuilder();
    String chatFile = MSGDIR + this.sessID;
    try {
      //use buffering, reading one line at a time
      //FileReader always assumes default encoding is OK!
      BufferedReader input = new BufferedReader(new FileReader(chatFile));
      try {
        String line; //not declared within while loop
        while ((line = input.readLine()) != null) {
          contents.append(line);
          contents.append(System.getProperty("line.separator"));
        }
      } finally {
        input.close();
      }
    } catch (IOException ex) {
      logger.severe("ChatHandler:getMessages():" + ex.getMessage());
    }
    return contents.toString();
  }

  public void setStatus(String s) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    if (s.equalsIgnoreCase("afk")) {
      sendMsg("AFK (" + sdf.format(new Date()) + ")");
    } else if (s.equalsIgnoreCase("dnd")) {
      sendMsg("DND (" + sdf.format(new Date()) + ")");
    } else {
      //sendMsg("ist wieder anwesend... (" + sdf.format(new Date()) + ")");      
    }
  }

  public static ChatMsg UnmarshallingIncomingMessage(Message t) throws JMSException {
    UUID objID = UUID.randomUUID();
    ChatMsg chatMsg = new ChatMsg(null, objID.toString());
    //a MUST attribute
    if (t.propertyExists("ChatMsgType")) {
      chatMsg.setType(t.getShortProperty("ChatMsgType"));
    }
    if (t.propertyExists("ChatMsgFrom")) {
      chatMsg.setFrom(t.getStringProperty("ChatMsgFrom"));
    }
    if (t.propertyExists("ChatMsgPrivateTo")) {
      //the nick, not usefull for unique processes!
      chatMsg.setPrivateToNick(t.getStringProperty("ChatMsgPrivateTo"));
    }
    //TODO implement userid of SEHR or another unique ID procedure
    if (t.propertyExists("ChatMsgprivateToUserID")) {
      chatMsg.setPrivateToUserID(t.getIntProperty("ChatMsgPprivateToUserID"));
    }
    if (t.propertyExists("ChatMsgCenterFrom")) {
      chatMsg.setCenterFrom(t.getIntProperty("ChatMsgCenterFrom"));
    }
    if (t.propertyExists("ChatMsgCenterTo")) {
      chatMsg.setCenterTo(t.getIntProperty("ChatMsgCenterTo"));
    }
    if (t.propertyExists("ChatMsgZoneFrom")) {
      chatMsg.setZoneFrom(t.getIntProperty("ChatMsgZoneFrom"));
    }
    if (t.propertyExists("ChatMsgZoneTo")) {
      chatMsg.setZoneTo(t.getIntProperty("ChatMsgZoneTo"));
    }
    if (t.propertyExists("ChatMsgZoneTo")) {
      chatMsg.setZoneTo(t.getIntProperty("ChatMsgZoneTo"));
    }
    if (t instanceof TextMessage) {
      chatMsg.setText(((TextMessage) t).getText());
    } else if (t instanceof MapMessage) {
      chatMsg.setText(((MapMessage) t).getString("message"));
    }

    return chatMsg;
  }
}
