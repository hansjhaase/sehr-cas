/*
 * (C) MDI GmbH for the SEHR community
 */
package org.ifeth.sehr.p1507291734.web.beans;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
public abstract class ChatMsgEvent {

  private final ChatMsg chatMsg;

  public ChatMsgEvent(ChatMsg chatMsg) {
    this.chatMsg = chatMsg;
  }

  public ChatMsg getChatMsg() {
    return this.chatMsg;
  }

}
