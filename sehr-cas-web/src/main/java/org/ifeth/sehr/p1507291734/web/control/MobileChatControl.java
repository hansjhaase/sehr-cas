/*
 * (C) MDI GmbH
 */
package org.ifeth.sehr.p1507291734.web.control;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSession;
import org.ifeth.sehr.intrasec.entities.UsrMain;
import org.ifeth.sehr.p1507291734.web.beans.ChatMsg;
import org.ifeth.sehr.p1507291734.web.listener.SEHRMessagingListener;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@Named(value = "mobChatCtrl")
@SessionScoped
public class MobileChatControl implements Serializable {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");

  @Inject
  private ModuleControl moduleCtrl;
  @Inject
  private MobileControl mobileCtrl;
  private ChatMsg chatMsg;
  private List<ChatMsg> roomMsg = new ArrayList();
  private String text = "";
  private String room = "public"; //by default 'public'
  private UsrMain curUserToChat;

  /**
   * @return the chatMsg
   */
  public ChatMsg getChatMsg() {
    return chatMsg;
  }

  /**
   * @param chatMsg the chatMsg to set
   */
  public void setChatMsg(ChatMsg chatMsg) {
    this.chatMsg = chatMsg;
  }

  /**
   * Get chat rooms for viewing.
   *
   * @return the publicMsg
   */
  public List<UsrMain> getListUsers() {
    return mobileCtrl.getListUsers();
  }

  public List<String> getListRooms() {
    return mobileCtrl.getListRooms();
  }

  /**
   * Get all messages of a private (room) chat for viewing.
   *
   * @return the privateMsg
   */
  public List<ChatMsg> getListRoomChatMsg() {
    //TODO add room Filter... processing, currently only one list 
    //is updated by EventBus
    this.roomMsg = mobileCtrl.getListChatMsg(this.room);
    return this.roomMsg;
  }

  public void doSendMsg() {
    //get handler currently from current (a valid) session scope...
    FacesContext fctx = FacesContext.getCurrentInstance();
    HttpSession sess = (HttpSession) fctx.getExternalContext().getSession(false);
    SEHRMessagingListener chatHdl = (SEHRMessagingListener) sess.getAttribute("ChatHandler");
    if (chatHdl.isSession()) {
      //TODO implement chat via sehr.xnet.DOMAIN.chat to all...
      if (room.equalsIgnoreCase("public")) {
        //send public inside zone
        chatHdl.sendMsg(this.text, SEHRMessagingListener.PUBLIC, moduleCtrl.getLocalZoneID(), -1, -1);
      } else {
        chatHdl.sendMsg(this.text, SEHRMessagingListener.PRIVATE_USER, moduleCtrl.getLocalZoneID(), -1, curUserToChat.getUsrid());
      }
      this.text = "";
    }
    //return "pm:vwChatRoom";
  }

  /**
   * @return the text
   */
  public String getText() {
    return text;
  }

  /**
   * @param text the text to set
   */
  public void setText(String text) {
    this.text = text;
  }

  public String joinRoom() {
    Log.finer(MobileChatControl.class.getName() + ":joinRoom():room=" + this.room);
    FacesContext fctx = FacesContext.getCurrentInstance();
    HttpSession sess = (HttpSession) fctx.getExternalContext().getSession(false);
    SEHRMessagingListener chatHdl = (SEHRMessagingListener) sess.getAttribute("ChatHandler");
    chatHdl.joinRoom(this.room, mobileCtrl.getUsername());
    return "pm:vwChatRoom";
  }

  /**
   * @return the room
   */
  public String getRoom() {
    return room;
  }

  /**
   * @param room the room to set
   */
  public void setRoom(String room) {
    this.room = room;
  }

  /**
   * @return the curUserToChat
   */
  public UsrMain getCurUserToChat() {
    return curUserToChat;
  }

  /**
   * @param curUserToChat the curUserToChat to set
   */
  public void setCurUserToChat(UsrMain curUserToChat) {
    this.curUserToChat = curUserToChat;
  }

}
