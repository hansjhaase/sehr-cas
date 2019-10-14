/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ifeth.sehr.p1507291734.web.control;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Logger;
import javax.inject.Named;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.servlet.ServletContext;
import org.ifeth.sehr.core.handler.LifeCARDObjectHandler;
import org.ifeth.sehr.core.objects.LifeCardItem;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@Named(value = "downloadController")
@RequestScoped
public class DownloadController {

  private static final long serialVersionUID = 1L;
  private static final Logger Log = Logger.getLogger("org.ifeth.p1507291734.web");

  public StreamedContent getCardOverlayImage(LifeCardItem lcItem) {
    Log.fine(DownloadController.class.getName() + ":getCardOverlayImage():" + lcItem);
    FacesContext fctx = FacesContext.getCurrentInstance();
    if (fctx.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
      // In this phase we're just rendering the HTML 
      // We return a stub StreamedContent so that it will generate right URL.
      return new DefaultStreamedContent();
    } else {
      ServletContext sctx = (ServletContext) fctx.getExternalContext().getContext();
      if (lcItem == null) {
        return null;
      }
      File fTemplate;
      File fImage;
      try {
        //item = (LifeCardItem) DeSerializer.deserialize(lcMain.getItem());
        String fPathImg = sctx.getRealPath("resources/images");
        fTemplate = new File(fPathImg + "/LifeCard-V8.png");
        Log.fine(DownloadController.class.getName() + ":getCardOverlayImage():fTemplate=" + fTemplate.getAbsolutePath());
        String pathTmp = sctx.getRealPath("WEB-INF") + "/tmp";
        File fPathTmp = new File(pathTmp);
        if (!fPathTmp.exists()) {
          fPathTmp.mkdirs();
        }
        fImage = File.createTempFile("lc-", ".png", fPathTmp);
        Log.fine(DownloadController.class.getName() + ":getCardOverlayImage():tmpImageFile=" + fImage.getAbsolutePath());
        File fPrintOvl = LifeCARDObjectHandler.createLifeCARDImage(fTemplate, lcItem, fImage)[1];
        return new DefaultStreamedContent(new FileInputStream(fPrintOvl), "image/png", "lc-"+lcItem.getLcPrintnumber()+".png");
        //InputStream stream = this.getClass().getResourceAsStream("/anyfile.txt");
        //return new DefaultStreamedContent(stream, "text/plain", "downloaded_file.txt");
      } catch (Exception e) {
        Log.warning(DownloadController.class.getName() + ":getCardOverlayImage():" + e.getMessage());
        return null;
      }
    }

  }

}
