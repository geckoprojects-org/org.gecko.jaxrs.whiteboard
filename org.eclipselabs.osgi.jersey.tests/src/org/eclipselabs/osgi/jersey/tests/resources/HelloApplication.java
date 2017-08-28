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
package org.eclipselabs.osgi.jersey.tests.resources;

import javax.ws.rs.core.Application;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * 
 * @author Mark Hoffmann
 * @since 28.08.2017
 */
@Component(name="HelloApplication", service=Application.class, immediate=true, property= {JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE + "=test"})
public class HelloApplication extends Application {

}
