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
package org.eclipselabs.jaxrs.jersey.runtime;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Application;

import org.eclipselabs.jaxrs.jersey.helper.JerseyHelper;
import org.eclipselabs.jaxrs.jersey.helper.ReferenceCollector;
import org.eclipselabs.jaxrs.jersey.provider.JerseyConstants;
import org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider;
import org.eclipselabs.jaxrs.jersey.runtime.dispatcher.JerseyWhiteboardDispatcher;
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
 * A configurable component, that establishes a whiteboard
 * @author Mark Hoffmann
 * @since 11.10.2017
 */
@Component(name="JaxRsWhiteboardComponent", immediate=true, configurationPolicy=ConfigurationPolicy.REQUIRE)
public class JerseyWhiteboardComponent {

	Logger logger = Logger.getLogger("o.e.o.j.runtimeComponent");
	protected volatile ServiceRegistration<JaxRSServiceRuntime> serviceRuntime = null;
	protected volatile AtomicLong changeCount = new AtomicLong();
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
	@SuppressWarnings("unchecked")
	@Activate
	public void activate(ComponentContext context) throws ConfigurationException {
		updateProperties(context);
		
		if (whiteboard != null) {
			whiteboard.teardown();;
		}
		whiteboard = new JerseyServiceRuntime();
		String[] urls = whiteboard.getURLs(context);
		// activate and start server
		whiteboard.initialize(context);
//		dispatcher.setBatchMode(true);
		dispatcher.setWhiteboardProvider(whiteboard);
		collector.connect(dispatcher);
		dispatcher.dispatch();
		whiteboard.startup();
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
		changeCount.set(0);
		if (dispatcher != null) {
			collector.disconnect(dispatcher);
			dispatcher.deactivate();
		}
		if (whiteboard != null) {
			whiteboard.teardown();
			whiteboard = null;
		}
		if (serviceRuntime != null) {
			try {
				serviceRuntime.unregister();
			} catch (IllegalStateException ise) {
				logger.log(Level.SEVERE, "JaxRsRuntime was already unregistered", ise);
			} catch (Exception ise) {
				logger.log(Level.SEVERE, "Error unregsitering JaxRsRuntime", ise);
			}
		}
	}
	/**
	 * Adds a new application
	 * @param application the application to add
	 * @param properties the service properties
	 */
	@Reference(name="application", cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC, unbind="removeApplication")
	public void addApplication(Application application, Map<String, Object> properties) {
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
			throw new ConfigurationException(JaxRSServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT, "No component context is availble to get properties from");
		}
		name = JerseyHelper.getPropertyWithDefault(ctx, JaxRSWhiteboardConstants.JAX_RS_NAME, null);
		if (name == null) {
			name = JerseyHelper.getPropertyWithDefault(ctx, JerseyConstants.JERSEY_WHITEBOARD_NAME, null);
			if (name == null) {
				throw new ConfigurationException(JaxRSWhiteboardConstants.JAX_RS_NAME, "No name was defined for the whiteboard");
			}
		}
	}

}
