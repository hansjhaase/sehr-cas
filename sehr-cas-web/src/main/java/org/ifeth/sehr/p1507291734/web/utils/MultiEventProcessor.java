/*
 * (C) MDI GmbH fpr the SEHR community
 */
package org.ifeth.sehr.p1507291734.web.utils;

import com.google.common.eventbus.Subscribe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.ifeth.sehr.intrasec.entities.UsrMain;
import org.ifeth.sehr.p1507291734.web.beans.ChatMsg;
import org.ifeth.sehr.p1507291734.web.beans.NewChatMsgEvent;
import org.ifeth.sehr.p1507291734.web.listener.SEHRMessagingListener;

/**
 *
 * @author Hans J Haase &lt;hansjhaase@mdigmbh.de&gt;
 */
public class MultiEventProcessor {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");

  private org.primefaces.push.EventBus pfEventBus;

  private final List<UsrMain> listChatUsers = new ArrayList<>();
  //private final List<ChatMsg> listChatMsgPublic = new ArrayList<>();
  //may change during a session
  private final Map<String, List<ChatMsg>> mapChatRooms = new HashMap<>();

  private final int zoneid;

  public MultiEventProcessor(int zoneid) {
    this.zoneid = zoneid;
  }

  @Subscribe
  public void handleChatMsgEvent(NewChatMsgEvent event) {
    Log.info(MultiEventProcessor.class.getName() + "(EventBus):ChatMsg received :" + event.toString());
    int i = 0;
    //process 'room'
    ChatMsg cm = event.getChatMsg();
    if (cm.getType() == SEHRMessagingListener.PUBLIC) {
      List<ChatMsg> listChatMsgPublic;
      //use public room of the zone (by default 'sehr.ZONE_ID.chat.public')      
      if (!mapChatRooms.containsKey("public")) {
        listChatMsgPublic = new ArrayList<>();
        mapChatRooms.put("public", listChatMsgPublic);
      } else {
        listChatMsgPublic = mapChatRooms.get("public");
      }
      listChatMsgPublic.add(cm);
      i = listChatMsgPublic.size();
    } else if (cm.getType() == SEHRMessagingListener.PRIVATE_USER) {
      String roomUserID = String.format("%08d", cm.getPrivateToUserID());
      List<ChatMsg> listChatMsgPrivate;
      if (!mapChatRooms.containsKey(roomUserID)) {
        listChatMsgPrivate = new ArrayList<>();
        mapChatRooms.put(roomUserID, listChatMsgPrivate);
      } else {
        listChatMsgPrivate = mapChatRooms.get(roomUserID);
      }
      listChatMsgPrivate.add(cm);
      i = listChatMsgPrivate.size();
    }
    //TODO refactor to real UsrMain entry... center etc.
    if (!this.listChatUsers.contains(cm.getFrom() + "@Z:" + String.format("%07d", zoneid))) {
      UsrMain usrMain = new UsrMain();
      usrMain.setUsrid(-1);
      usrMain.setUsrname(cm.getFrom());
      this.listChatUsers.add(usrMain);
    }

    if (this.pfEventBus != null) {
      this.pfEventBus.publish(i);
    }
  }

  public List<ChatMsg> listChatMsgPublic() {
    return listChatMsgOfRoom("public");
  }

  public List<ChatMsg> listChatMsgOfRoom(String room) {
    List<ChatMsg> listChatMsg;
    if (!mapChatRooms.containsKey(room)) {
      return null;
    }
    listChatMsg = mapChatRooms.get(room);
    return listChatMsg;
  }

  public List<String> listChatRooms() {
    if (mapChatRooms.isEmpty()) {
      return null;
    }
    List<String> lRooms = new ArrayList<>();
    for (String room : mapChatRooms.keySet()) {
      lRooms.add(room);
    }
    return lRooms;
  }

  public List<UsrMain> listChatUsers() {
    return this.listChatUsers;
  }

  public void setWEBSocketProcessor(org.primefaces.push.EventBus pfEventBus) {
    this.pfEventBus = pfEventBus;
  }
}
