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
package org.gecko.rest.jersey.runtime.httpwhiteboard;

import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_RESOURCE;

import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.Application;

import org.gecko.rest.jersey.provider.JerseyConstants;
import org.gecko.rest.jersey.runtime.JerseyWhiteboardComponent;
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
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.condition.Condition;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

/**
 * This component handles the lifecycle of a {@link JaxRSServiceRuntime}
 * @author Mark Hoffmann
 * @since 30.07.2017
 */
@Component(name="JaxRsHttpWhiteboardRuntimeComponent", immediate=true, configurationPolicy=ConfigurationPolicy.REQUIRE, reference = @Reference(name = "runtimeCondition", service = Condition.class , target = JerseyConstants.JERSEY_RUNTIME_CONDITION))
public class JaxRsHttpWhiteboardRuntimeComponent extends JerseyWhiteboardComponent{

	private static Logger logger = Logger.getLogger("o.e.o.j.JaxRsHttpWhiteboardRuntimeComponent");

	/**
	 * Called on component activation
	 * @param componentContext the component context
	 * @throws ConfigurationException 
	 */
	/* (non-Javadoc)
	 * @see org.gecko.rest.jersey.runtime.JerseyWhiteboardComponent#activate(org.osgi.service.component.ComponentContext)
	 */
	@Activate
	@Override
	public void activate(final ComponentContext componentContext) throws ConfigurationException {
		updateProperties(componentContext);
		if (whiteboard != null) {
			whiteboard.teardown();;
		}
		whiteboard = new HTTPWhiteboardBasedJerseyServiceRuntime();
		whiteboard.initialize(componentContext);
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
	@Reference(name="defaultApplication", cardinality=ReferenceCardinality.AT_LEAST_ONE, policy=ReferencePolicy.DYNAMIC, unbind="unbindDefaultApplication", updated = "modifedDefaultApplication", target="(&(osgi.jaxrs.application.base=*)(osgi.jaxrs.name=.default))")
	public void addDefaultApplication(Application application, Map<String, Object> properties) {
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
	@Reference(name="application", service=Application.class,cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC, unbind="unbindApplication", updated = "modifedApplication", target="(&(osgi.jaxrs.application.base=*)(!(osgi.jaxrs.name=.default)))")
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

	@Reference(service = AnyService.class, target = "(" + JaxrsWhiteboardConstants.JAX_RS_EXTENSION
			+ "=true)", cardinality = MULTIPLE, policy = DYNAMIC)
	public void bindJaxRsExtension(ServiceReference<Object> jaxRsExtensionSR, Map<String, Object> properties) {
		updatedJaxRsExtension(jaxRsExtensionSR, properties);
	}

	public void updatedJaxRsExtension(ServiceReference<Object> jaxRsExtensionSR, Map<String, Object> properties) {
		logger.fine("Handle extension " + jaxRsExtensionSR + " properties: " + properties);
		ServiceObjects<?> so = getServiceObjects(jaxRsExtensionSR);
		dispatcher.addExtension(so, properties);

	}
	public void unbindJaxRsExtension(ServiceReference<Object> jaxRsExtensionSR, Map<String, Object> properties) {
		dispatcher.removeExtension(properties);
	}

	@Reference(service = AnyService.class, target = "(" + JAX_RS_RESOURCE
			+ "=true)", cardinality = MULTIPLE, policy = DYNAMIC)
	public void bindJaxRsResource(ServiceReference<Object> jaxRsExtensionSR, Map<String, Object> properties) {
		updatedJaxRsResource(jaxRsExtensionSR,properties);
	}

	public void updatedJaxRsResource(ServiceReference<Object> jaxRsResourceSR, Map<String, Object> properties) {
		logger.fine("Handle resource " + jaxRsResourceSR + " properties: " + properties);
		ServiceObjects<?> so = getServiceObjects(jaxRsResourceSR);
		dispatcher.addResource(so, properties);

	}
	
	public void unbindJaxRsResource(ServiceReference<Object> jaxRsResourceSR, Map<String, Object> properties) {
		dispatcher.removeResource(properties);
	}
}
