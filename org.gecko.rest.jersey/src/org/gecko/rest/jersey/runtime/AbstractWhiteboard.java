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
 *     Stefan Bishof - API and implementation
 *     Tim Ward - implementation
 */
package org.gecko.rest.jersey.runtime;

import static org.gecko.rest.jersey.provider.JerseyConstants.JERSEY_WHITEBOARD_NAME;
import static org.osgi.service.jakartars.runtime.JakartarsServiceRuntimeConstants.JAKARTA_RS_SERVICE_ENDPOINT;
import static org.osgi.service.jakartars.whiteboard.JakartarsWhiteboardConstants.JAKARTA_RS_NAME;

import java.util.logging.Logger;

import org.gecko.rest.jersey.helper.JerseyHelper;
import org.gecko.rest.jersey.provider.JerseyConstants;
import org.gecko.rest.jersey.provider.application.JakartarsWhiteboardDispatcher;
import org.gecko.rest.jersey.provider.whiteboard.JakartarsWhiteboardProvider;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.condition.Condition;

/**
 * A configurable component, that establishes a whiteboard
 * @author Mark Hoffmann
 * @since 11.10.2017
 */

@Component(name = "JakartarsWhiteboardComponent", 
	reference = @Reference(name = "runtimeCondition", service = Condition.class , target = JerseyConstants.JERSEY_RUNTIME_CONDITION),
	configurationPolicy = ConfigurationPolicy.REQUIRE

)
public abstract class AbstractWhiteboard {

	Logger logger = Logger.getLogger("o.e.o.j.runtimeComponent");
	private volatile String name;
	

	protected final JakartarsWhiteboardDispatcher dispatcher= new JerseyWhiteboardDispatcher();
	
	protected volatile JakartarsWhiteboardProvider whiteboard;
	
	/**
	 * Updates the fields that are provided by service properties.
	 * @param ctx the component context
	 * @throws ConfigurationException thrown when no context is available or the expected property was not provided 
	 */
	protected void updateProperties(ComponentContext ctx) throws ConfigurationException {
		if (ctx == null) {
			throw new ConfigurationException(JAKARTA_RS_SERVICE_ENDPOINT, "No component context is availble to get properties from");
		}
		name = JerseyHelper.getPropertyWithDefault(ctx, JAKARTA_RS_NAME, null);
		if (name == null) {
			name = JerseyHelper.getPropertyWithDefault(ctx, JERSEY_WHITEBOARD_NAME, JERSEY_WHITEBOARD_NAME);
			if (name == null) {
				throw new ConfigurationException(JAKARTA_RS_NAME, "No name was defined for the whiteboard");
			}
		}
	}
	
	protected ServiceObjects<?> getServiceObjects(ServiceReference<?> reference) {
		return reference.getBundle().getBundleContext().getServiceObjects(reference);
	}
	
}