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
package org.eclipselabs.jaxrs.jersey.runtime.httpwhiteboard;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Application;

import org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider;
import org.eclipselabs.jaxrs.jersey.runtime.JerseyServiceRuntime;
import org.eclipselabs.jaxrs.jersey.runtime.JerseyWhiteboardComponent;
import org.osgi.framework.ServiceRegistration;
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
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntime;
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntimeConstants;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;


/**
 * This component handles the lifecycle of a {@link JaxRSServiceRuntime}
 * @author Mark Hoffmann
 * @since 30.07.2017
 */
@Component(name="JaxRsHttpWhiteboardRuntimeComponent", immediate=true, configurationPolicy=ConfigurationPolicy.REQUIRE)
public class JaxRsHttpWhiteboardRuntimeComponent extends JerseyWhiteboardComponent{

	private static Logger logger = Logger.getLogger("o.e.o.j.JaxRsHttpWhiteboardRuntimeComponent");

	/**
	 * Called on component activation
	 * @param context the component context
	 * @throws ConfigurationException 
	 */
	/* (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.runtime.JerseyWhiteboardComponent#activate(org.osgi.service.component.ComponentContext)
	 */
	@SuppressWarnings("unchecked")
	@Activate
	@Override
	public void activate(final ComponentContext context) throws ConfigurationException {
		if (whiteboard != null) {
			whiteboard.teardown();;
		}
		whiteboard = new HTTPWhiteboardBasedJerseyServiceRuntime();
		dispatcher.setWhiteboardProvider(whiteboard);
		whiteboard.initialize(context);
		String[] urls = whiteboard.getURLs(context);
		// activate and start server
		dispatcher.dispatch();
		whiteboard.startup();
		
		// activate and start server
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put("service.changecount", changeCount.incrementAndGet());
		properties.put(JaxRSServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT, urls);
		String[] service = new String[] {JaxRSServiceRuntime.class.getName(), JaxRsWhiteboardProvider.class.getName()};
		try {
			serviceRuntime = (ServiceRegistration<JaxRSServiceRuntime>) context.getBundleContext().registerService(service, whiteboard, properties);
			whiteboard.updateRuntimeDTO(serviceRuntime.getReference());
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error starting JaxRsRuntimeService ", e);
			if (serviceRuntime != null) {
				serviceRuntime.unregister();
			}
		} 
	}

	/**
	 * Called on component modification
	 * @param context the component context
	 * @throws ConfigurationException 
	 */
	@Modified
	public void modified(ComponentContext context) throws ConfigurationException {
		updateProperties(context);
	}

	/**
	 * Called on component de-activation
	 * @param context the component context
	 */
	@Deactivate
	public void deactivate(ComponentContext context) {
		super.deactivate(context);
	}
	
	/**
	 * Adds a new application
	 * @param application the application to add
	 * @param properties the service properties
	 */
	@Reference(name="application", cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC, unbind="removeApplication")
	public void addApplication(Application application, Map<String, Object> properties) {
		super.addApplication(application, properties);
	}

	/**
	 * Removes a application 
	 * @param application the application to remove
	 * @param properties the service properties
	 */
	public void removeApplication(Application application, Map<String, Object> properties) {
		super.removeApplication(application, properties);
	}
	
	/**
	 * Adds a new resource
	 * @param resource the resource to add
	 * @param properties the service properties
	 */
	@Override
	@Reference(name="resource", cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC, unbind="removeResource", target="(" + JaxRSWhiteboardConstants.JAX_RS_RESOURCE + "=true)")
	public void addResource(Object resource, Map<String, Object> properties) {
		super.addResource(resource, properties);
	}
	
	@Override
	public void removeResource(Object resource, Map<String, Object> properties) {
		super.removeResource(resource, properties);
	}
	
	/**
	 * Adds a new extension
	 * @param extension the extension to add
	 * @param properties the service properties
	 */
	@Reference(name="extension", cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC, unbind="removeExtension", target="(" + JaxRSWhiteboardConstants.JAX_RS_EXTENSION + "='true')")
	public void addExtension(Object extension, Map<String, Object> properties) {
		super.addExtension(extension, properties);
	}

	/**
	 * Removes an extension 
	 * @param extension the extension to remove
	 * @param properties the service properties
	 */
	public void removeExtension(Object extension, Map<String, Object> properties) {
		super.removeExtension(extension, properties);
	}
}
