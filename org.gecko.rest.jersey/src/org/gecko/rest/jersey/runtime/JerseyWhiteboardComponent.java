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
package org.gecko.rest.jersey.runtime;

import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.Application;

import org.gecko.rest.jersey.helper.JerseyHelper;
import org.gecko.rest.jersey.helper.ReferenceCollector;
import org.gecko.rest.jersey.provider.JerseyConstants;
import org.gecko.rest.jersey.provider.whiteboard.JaxRsWhiteboardProvider;
import org.gecko.rest.jersey.runtime.dispatcher.JerseyWhiteboardDispatcher;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntimeConstants;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

/**
 * A configurable component, that establishes a whiteboard
 * @author Mark Hoffmann
 * @since 11.10.2017
 */
@Component(name="JaxRsWhiteboardComponent", immediate=true, configurationPolicy=ConfigurationPolicy.REQUIRE)
public class JerseyWhiteboardComponent {

	Logger logger = Logger.getLogger("o.e.o.j.runtimeComponent");
	private volatile String name;
	protected JerseyWhiteboardDispatcher dispatcher = new JerseyWhiteboardDispatcher();
	protected volatile JaxRsWhiteboardProvider whiteboard;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	ReferenceCollector collector;
	
	/**
	 * Called on component activation
	 * @param context the component context
	 * @throws ConfigurationException 
	 */
	@Activate
	public void activate(ComponentContext context) throws ConfigurationException {
		updateProperties(context);
		
		if (whiteboard != null) {
			whiteboard.teardown();;
		}
		whiteboard = new JerseyServiceRuntime();
		// activate and start server
		whiteboard.initialize(context);
//		dispatcher.setBatchMode(true);
		dispatcher.setWhiteboardProvider(whiteboard);
		collector.connect(dispatcher);
		dispatcher.dispatch();
		whiteboard.startup();
	}

	/**
	 * Called on component modification
	 * @param context the component context
	 * @throws ConfigurationException 
	 */
	@Modified
	public void modified(ComponentContext context) throws ConfigurationException {
		updateProperties(context);
		whiteboard.modified(context);
		dispatcher.dispatch();
	}

	/**
	 * Called on component de-activation
	 * @param context the component context
	 */
	@Deactivate
	public void deactivate(ComponentContext context) {
		if (dispatcher != null) {
			collector.disconnect(dispatcher);
			dispatcher.deactivate();
		}
		if (whiteboard != null) {
			whiteboard.teardown();
			whiteboard = null;
		}
	}
	
	/**
	 * Adds a new default application
	 * @param application the application to add
	 * @param properties the service properties
	 */
	@Reference(name="defaultApplication", cardinality=ReferenceCardinality.AT_LEAST_ONE, policy=ReferencePolicy.DYNAMIC, unbind="removeDefaultApplication", updated = "modifedDefaultApplication", target="(osgi.jaxrs.name=.default)")
	public void addDefaultApplication(Application application, Map<String, Object> properties) {
		dispatcher.addApplication(application, properties);
	}

	/**
	 * Modifies a default application
	 * @param application the application to add
	 * @param properties the service properties
	 */
	public void modifedDefaultApplication(Application application, Map<String, Object> properties) {
		dispatcher.removeApplication(application, properties);
		dispatcher.addApplication(application, properties);
	}

	/**
	 * Removes a default application 
	 * @param application the application to remove
	 * @param properties the service properties
	 */
	public void removeDefaultApplication(Application application, Map<String, Object> properties) {
		dispatcher.removeApplication(application, properties);
	}
	
	/**
	 * Adds a new application
	 * @param application the application to add
	 * @param properties the service properties
	 */
	@Reference(name="application", cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC, unbind="removeApplication", updated = "modifedApplication", target="(!(osgi.jaxrs.name=.default))")
	public void addApplication(Application application, Map<String, Object> properties) {
		dispatcher.addApplication(application, properties);
	}
	
	/**
	 * Adds a new application
	 * @param application the application to add
	 * @param properties the service properties
	 */
	public void modifedApplication(Application application, Map<String, Object> properties) {
		dispatcher.removeApplication(application, properties);
		dispatcher.addApplication(application, properties);
	}
	
	/**
	 * Removes a application 
	 * @param application the application to remove
	 * @param properties the service properties
	 */
	public void removeApplication(Application application, Map<String, Object> properties) {
		dispatcher.removeApplication(application, properties);
	}

	/**
	 * Updates the fields that are provided by service properties.
	 * @param ctx the component context
	 * @throws ConfigurationException thrown when no context is available or the expected property was not provided 
	 */
	protected void updateProperties(ComponentContext ctx) throws ConfigurationException {
		if (ctx == null) {
			throw new ConfigurationException(JaxrsServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT, "No component context is availble to get properties from");
		}
		name = JerseyHelper.getPropertyWithDefault(ctx, JaxrsWhiteboardConstants.JAX_RS_NAME, null);
		if (name == null) {
			name = JerseyHelper.getPropertyWithDefault(ctx, JerseyConstants.JERSEY_WHITEBOARD_NAME, null);
			if (name == null) {
				throw new ConfigurationException(JaxrsWhiteboardConstants.JAX_RS_NAME, "No name was defined for the whiteboard");
			}
		}
	}

}
