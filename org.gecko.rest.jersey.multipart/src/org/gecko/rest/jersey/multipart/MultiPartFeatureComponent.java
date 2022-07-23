/**
 * Copyright (c) 2012 - 2022 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.gecko.rest.jersey.multipart;

import java.util.logging.Logger;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsExtension;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsWhiteboardTarget;

@JaxrsExtension
@JaxrsName("MultiPartFeatureExtension")
@Component(name = "MultiPartFeatureComponent", property = {"multipart=true"})
@JaxrsApplicationSelect("(!(disableMultipart=true))")
@JaxrsWhiteboardTarget("(!(disableMultipart=true))")
public class MultiPartFeatureComponent implements Feature{
	
	private Logger logger = Logger.getLogger(MultiPartFeatureComponent.class.getName()); 

	@Override
	public boolean configure(FeatureContext context) {
		context.register(MultiPartFeature.class);
		logger.fine("Registering MultiPartFeature!");
		return true;
	}
}
