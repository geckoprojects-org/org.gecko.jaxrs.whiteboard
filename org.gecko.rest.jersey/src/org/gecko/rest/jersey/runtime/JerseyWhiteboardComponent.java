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
import static org.osgi.service.component.annotations.ReferenceCardinality.AT_LEAST_ONE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.jakartars.runtime.JakartarsServiceRuntimeConstants.JAKARTA_RS_SERVICE_ENDPOINT;
import static org.osgi.service.jakartars.whiteboard.JakartarsWhiteboardConstants.JAKARTA_RS_EXTENSION;
import static org.osgi.service.jakartars.whiteboard.JakartarsWhiteboardConstants.JAKARTA_RS_NAME;
import static org.osgi.service.jakartars.whiteboard.JakartarsWhiteboardConstants.JAKARTA_RS_RESOURCE;

import java.util.Map;
import java.util.logging.Logger;

import org.gecko.rest.jersey.helper.JerseyHelper;
import org.gecko.rest.jersey.provider.JerseyConstants;
import org.gecko.rest.jersey.provider.application.JakartarsWhiteboardDispatcher;
import org.gecko.rest.jersey.provider.whiteboard.JakartarsWhiteboardProvider;
import org.gecko.rest.jersey.runtime.dispatcher.JerseyWhiteboardDispatcher;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.AnyService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.condition.Condition;
import org.osgi.service.jakartars.whiteboard.JakartarsWhiteboardConstants;

import jakarta.ws.rs.core.Application;

/**
 * A configurable component, that establishes a whiteboard
 * @author Mark Hoffmann
 * @since 11.10.2017
 */

@Component(name = "JakartarsWhiteboardComponent", 
	reference = @Reference(name = "runtimeCondition", service = Condition.class , target = JerseyConstants.JERSEY_RUNTIME_CONDITION),
	configurationPolicy = ConfigurationPolicy.REQUIRE

)
public class JerseyWhiteboardComponent {

	Logger logger = Logger.getLogger("o.e.o.j.runtimeComponent");
	private volatile String name;
	

	protected JakartarsWhiteboardDispatcher dispatcher= new JerseyWhiteboardDispatcher();
	
	protected volatile JakartarsWhiteboardProvider whiteboard;
	
	/**
	 * Called on component activation
	 * @param componentContext the component context
	 * @throws ConfigurationException 
	 */
	@Activate
	public void activate(ComponentContext componentContext) throws ConfigurationException {
		updateProperties(componentContext);
		
		if (whiteboard != null) {
			whiteboard.teardown();
		}
		whiteboard = new JerseyServiceRuntime();
		// activate and start server
		whiteboard.initialize(componentContext);
//		dispatcher.setBatchMode(true);
		dispatcher.setWhiteboardProvider(whiteboard);
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
		dispatcher.dispatch();
		whiteboard.modified(context);
	}

	/**
	 * Called on component de-activation
	 * @param context the component context
	 */
	@Deactivate
	public void deactivate(ComponentContext context) {
		if (dispatcher != null) {
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
	@Reference(cardinality = AT_LEAST_ONE, policy = DYNAMIC, target = "(&(" + JakartarsWhiteboardConstants.JAKARTA_RS_APPLICATION_BASE + "=*)(" + JakartarsWhiteboardConstants.JAKARTA_RS_NAME + "=.default))")
	public void bindDefaultApplication(Application application, Map<String, Object> properties) {
		dispatcher.addApplication(application, properties);
	}

	/**
	 * Modifies a default application
	 * @param application the application to add
	 * @param properties the service properties
	 */
	public void updatedDefaultApplication(Application application, Map<String, Object> properties) {
		dispatcher.removeApplication(application, properties);
		dispatcher.addApplication(application, properties);
	}

	/**
	 * Removes a default application 
	 * @param application the application to remove
	 * @param properties the service properties
	 */
	public void unbindDefaultApplication(Application application, Map<String, Object> properties) {
		dispatcher.removeApplication(application, properties);
	}
	
	/**
	 * Adds a new application
	 * @param application the application to add
	 * @param properties the service properties
	 */
	@Reference(service = Application.class, cardinality = MULTIPLE, policy = DYNAMIC, target = "(&(" + JakartarsWhiteboardConstants.JAKARTA_RS_APPLICATION_BASE + "=*)(!(" + JakartarsWhiteboardConstants.JAKARTA_RS_NAME + "=.default)))")
	public void bindApplication(Application application, Map<String, Object> properties) {
		dispatcher.addApplication(application, properties);
	
	}
	
	/**
	 * Adds a new application
	 * @param application the application to add
	 * @param properties the service properties
	 */
	public void updatedApplication(Application application, Map<String, Object> properties) {
		dispatcher.removeApplication(application, properties);
		dispatcher.addApplication(application, properties);
	}
	
	/**
	 * Removes a application 
	 * @param application the application to remove
	 * @param properties the service properties
	 */
	public void unbindApplication(Application application, Map<String, Object> properties) {
		dispatcher.removeApplication(application, properties);
	}

	@Reference(service = AnyService.class, target = "(" + JAKARTA_RS_EXTENSION
			+ "=true)", cardinality = MULTIPLE, policy = DYNAMIC)
	public void bindJakartarsExtension(ServiceReference<Object> jaxRsExtensionSR, Map<String, Object> properties) {

		updatedJakartarExtension(jaxRsExtensionSR, properties);
	}

	public void updatedJakartarExtension(ServiceReference<Object> jaxRsExtensionSR, Map<String, Object> properties) {
		logger.fine("Handle extension " + jaxRsExtensionSR + " properties: " + properties);
		ServiceObjects<?> so = getServiceObjects(jaxRsExtensionSR);
		dispatcher.addExtension(so, properties);

	}

	public void unbindJaxRsExtension(ServiceReference<Object> jaxRsExtensionSR, Map<String, Object> properties) {
		dispatcher.removeExtension(properties);
	}

	@Reference(service = AnyService.class, target = "(" + JAKARTA_RS_RESOURCE
			+ "=true)", cardinality = MULTIPLE, policy = DYNAMIC)
	public void bindJakartarsResource(ServiceReference<Object> jaxRsExtensionSR, Map<String, Object> properties) {
		updatedJakartarsResource(jaxRsExtensionSR, properties);
	}

	public void updatedJakartarsResource(ServiceReference<Object> jaxRsResourceSR, Map<String, Object> properties) {
		logger.fine("Handle resource " + jaxRsResourceSR + " properties: " + properties);
		ServiceObjects<?> so = getServiceObjects(jaxRsResourceSR);
		dispatcher.addResource(so, properties);

	}

	public void unbindJakartarsResource(ServiceReference<Object> jaxRsResourceSR, Map<String, Object> properties) {
		dispatcher.removeResource(properties);
	}
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
