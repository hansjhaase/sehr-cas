/*
 * (C) IFETH Development Team
 */
package org.ifeth.sehr.p1507291734.web.control;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;
import org.ifeth.sehr.intrasec.entities.AdrMain;
import org.ifeth.sehr.p1507291734.ejb.AdrMainAdmin;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@FacesConverter(forClass = AdrMain.class, value="converterAdrMain")
public class AdrMainConverter implements Converter {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");

  @EJB
  private AdrMainAdmin ejbAdrMainAdmin;

  @Override
  public Object getAsObject(FacesContext context, UIComponent component, String value) {
    Log.info(AdrMainConverter.class.getName() + ":getAsObject():UIComponent=" + component.getId()+", value="+value);
    if (value != null && value.trim().length() > 0) {
      String[] t = value.split("\\|");
      try {
        int adrid = Integer.parseInt(t[0]);
        if (adrid >= 0) {
          AdrMain entity = ejbAdrMainAdmin.readByAdrId(adrid);
          return entity;
        }
      } catch (NumberFormatException nfe) {
        Log.warning(AdrMainConverter.class.getName() + ":getAsObject():Exception:" + nfe.getMessage());
        //throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "Conversion Error", nfe.getMessage()));
      }
    }
    return null;
  }

  @Override
  public String getAsString(FacesContext context, UIComponent component, Object value) {
    Log.log(Level.FINEST, "{0}:getAsString():UIComponent={1}, value={2}", new Object[]{AdrMainConverter.class.getName(), component.getId(), value});
    
    if (value != null) {
      if (value instanceof AdrMain) {
        AdrMain adrMain = (AdrMain) value;
        return adrMain.getAdrid() + "|" + adrMain.getTitle();
      }
    }
    return null;
  }
}
