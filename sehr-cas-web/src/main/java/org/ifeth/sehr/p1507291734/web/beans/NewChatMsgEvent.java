/*
 * (C) MDI GmbH for the SEHR community
 */
package org.ifeth.sehr.p1507291734.web.beans;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
public class NewChatMsgEvent extends ChatMsgEvent {

  private final String message;

  public NewChatMsgEvent(ChatMsg chatMsg, String message) {
    super(chatMsg);
    this.message = message;
  }

  public String getMessage() {
    return this.message;
  }

}
