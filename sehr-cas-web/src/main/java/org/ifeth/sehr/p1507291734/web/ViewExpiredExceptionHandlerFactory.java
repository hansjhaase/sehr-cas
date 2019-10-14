/*
 * (C) 2015 MDI GmbH for the SEHR Community
 * Licensed under the European Union Public Licence - EUPL v.1.1 ("License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://ec.europa.eu/idabc/servlets/Doc?id=31979
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Refactored from 'sehr-saf' on 3.08.2015
 */
package org.ifeth.sehr.p1507291734.web;

import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerFactory;
 
/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
public class ViewExpiredExceptionHandlerFactory  extends ExceptionHandlerFactory {
 
    private ExceptionHandlerFactory parent;
 
    public ViewExpiredExceptionHandlerFactory(ExceptionHandlerFactory parent) {
        this.parent = parent;
    }
 
    @Override
    public ExceptionHandler getExceptionHandler() {
        ExceptionHandler result = parent.getExceptionHandler();
        result = new ViewExpiredExceptionHandler(result);
 
        return result;
    }
 
 
}

