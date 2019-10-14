/*
 * (C) IFETH
 */
package org.ifeth.sehr.p1507291734.web.control;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@FacesConverter("IdentityTypeConverter")
public class IdentityTypeConverter implements Converter {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");

  @Override
  public Object getAsObject(FacesContext context, UIComponent component, String value) {
    Log.info(IdentityTypeConverter.class.getName() + ":getAsObject():UIComponent=" + component.getId());
    if (value != null && value.trim().length() > 0) {
      Log.log(Level.INFO, "{0}:getAsObject():value={1}", new Object[]{IdentityTypeConverter.class.getName(), value});
      switch (value) {
        case "none":
          return new KeyValueItem((short)0,"none");//(short) 0;
        case "Passport":
          return new KeyValueItem((short)1,"Passport");//(short) 1;
        case "Driver Licence":
          return new KeyValueItem((short)2,"Driver Licence");//(short) 2;
        case "Other":
          return new KeyValueItem((short)9,"Other");//(short) 9;
      }
      return new KeyValueItem((short)0,"none");//(short) 0;
    }
    return null;
  }

  @Override
  public String getAsString(FacesContext context, UIComponent component, Object value) {
    Log.log(Level.INFO, "{0}:getAsString():UIComponent={1}", new Object[]{IdentityTypeConverter.class.getName(), component.getId()});
    if (value != null) {
      Log.log(Level.INFO, "{0}:getAsString():value={1}", new Object[]{IdentityTypeConverter.class.getName(), value});
      if (value instanceof KeyValueItem) {
        KeyValueItem item = (KeyValueItem)value;
        if(item.getKey()==0){
          return "none";
        }
        if(item.getKey()==1){
          return "Passport";
        }
        if(item.getKey()==2){
          return "Driver Licence";
        }
        if(item.getKey()==9){
          return "Other";
        }
      }
    }
    return "";
  }
}
