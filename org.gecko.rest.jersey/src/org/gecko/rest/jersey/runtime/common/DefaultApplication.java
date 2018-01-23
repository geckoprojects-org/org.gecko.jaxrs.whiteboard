/**
 * Copyright (c) 2012 - 2017 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.gecko.rest.jersey.runtime.common;

import javax.ws.rs.core.Application;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * The default Appplication for a Whiteboard 
 * @author Juergen Albert
 */
@Component(service = Application.class, scope = ServiceScope.PROTOTYPE, property = {
		JaxRSWhiteboardConstants.JAX_RS_NAME + "=.default",
		JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE + "=/"
})
public class DefaultApplication extends Application {

}
