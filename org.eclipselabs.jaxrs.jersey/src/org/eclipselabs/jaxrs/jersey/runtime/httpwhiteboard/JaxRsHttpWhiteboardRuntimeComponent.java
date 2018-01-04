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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Application;

import org.eclipselabs.jaxrs.jersey.helper.ReferenceCollector;
import org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider;
import org.eclipselabs.jaxrs.jersey.runtime.JerseyWhiteboardComponent;
import org.osgi.framework.ServiceReference;
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

/**
 * This component handles the lifecycle of a {@link JaxRSServiceRuntime}
 * @author Mark Hoffmann
 * @since 30.07.2017
 */
@Component(name="JaxRsHttpWhiteboardRuntimeComponent", immediate=true, configurationPolicy=ConfigurationPolicy.REQUIRE)
public class JaxRsHttpWhiteboardRuntimeComponent extends JerseyWhiteboardComponent{

	private static Logger logger = Logger.getLogger("o.e.o.j.JaxRsHttpWhiteboardRuntimeComponent");

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	private ReferenceCollector collector;
	
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
		collector.connect(dispatcher);
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
	public void addApplication(ServiceReference<Application> ref) {
		super.addApplication(ref);
	}

	/**
	 * Removes a application 
	 * @param application the application to remove
	 * @param properties the service properties
	 */
	public void removeApplication(ServiceReference<Application> ref) {
		super.removeApplication(ref);
	}
}
