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
package org.eclipselabs.osgi.jersey.runtime;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipselabs.osgi.jersey.JaxRsApplicationProvider;
import org.eclipselabs.osgi.jersey.JaxRsJerseyHandler;
import org.eclipselabs.osgi.jersey.JerseyConstants;
import org.eclipselabs.osgi.jersey.JerseyHelper;
import org.eclipselabs.osgi.jersey.application.JerseyApplication;
import org.eclipselabs.osgi.jersey.application.JerseyApplicationProvider;
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
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntime;
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntimeConstants;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;


/**
 * This component handles the lifecycle of a {@link JaxRSServiceRuntime}
 * @author Mark Hoffmann
 * @since 30.07.2017
 */
@Component(name="JaxRsRuntimeComponent", immediate=true, configurationPolicy=ConfigurationPolicy.REQUIRE)
public class JaxRsRuntimeComponent {

	Logger logger = Logger.getLogger("o.e.o.j.runtimeComponent");
	private volatile ServiceRegistration<JerseyServiceRuntime> serviceRuntime = null;
	private volatile AtomicLong changeCount = new AtomicLong();
	private volatile JaxRsJerseyHandler runtime = null;
	private volatile String name;
	private volatile boolean strict = false;
	private volatile ComponentContext context;
	private ServiceReference<JaxRsApplicationProvider> defaultApplicationRef;
	private final List<ServiceReference<?>> resourcesRefList = new LinkedList<>();
	private final List<ServiceReference<?>> extensionRefList = new LinkedList<>();
	private JaxRsApplicationProvider defaultApplication;

	/**
	 * Called on component activation
	 * @param context the component context
	 * @throws ConfigurationException 
	 */
	@SuppressWarnings("unchecked")
	@Activate
	public void activate(ComponentContext context) throws ConfigurationException {
		this.context = context;
		updateProperties(context);
		if (runtime != null) {
			runtime.teardown();;
		}
		runtime = new JerseyServiceRuntime();
		String[] urls = runtime.getURLs(context);
		// activate and start server
		runtime.initialize(context);
		if (defaultApplication == null) {
			defaultApplication = new JerseyApplicationProvider(name, new JerseyApplication(name, context.getBundleContext()));
		}
		// now register default application
		runtime.registerApplication(defaultApplication);
		runtime.startup();
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put("service.changecount", changeCount.incrementAndGet());
		properties.put(JaxRSServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT, urls);
		String[] service = new String[] {JaxRSServiceRuntime.class.getName(), JaxRsJerseyHandler.class.getName()};
		try {
			serviceRuntime = (ServiceRegistration<JerseyServiceRuntime>) context.getBundleContext().registerService(service, runtime, properties);
			runtime.updateRuntimeDTO(serviceRuntime.getReference());
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error starting JaxRsRuntimeService ", e);
		} finally {
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
		if (runtime != null) {
			runtime.teardown();
			runtime = null;
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
	
//	@Reference(name="defaultApplication", cardinality=ReferenceCardinality.MANDATORY, updated="modifyDefaultUpplication", unbind="unsetDefaultApplication")
	public void setDefaultApplication(ServiceReference<JaxRsApplicationProvider>defaultAppProviderRef) {
		this.defaultApplicationRef = defaultAppProviderRef;
	}
	public void unsetDefaultApplication(ServiceReference<JaxRsApplicationProvider>defaultAppProviderRef) {
		this.defaultApplicationRef = defaultAppProviderRef;
	}
	public void modifyDefaultApplication(ServiceReference<JaxRsApplicationProvider>defaultAppProviderRef) {
		this.defaultApplicationRef = defaultAppProviderRef;
	}

//	/**
//	 * Called, when a new resource implementation, that matches this filter criteria can be added
//	 * Left the target filter out, it should be propagated via configuration admin
//	 * rootResource.target=(&(osgi.jaxrs.resource=true)(osgi.jaxrs.whiteboard.target=${osgi.jaxrs.name}))
//	 * @param jaxrsResourceRef the service reference instance to add
//	 */
//	@Reference(name="rootResource", cardinality=ReferenceCardinality.MULTIPLE, 
//			policy=ReferencePolicy.DYNAMIC)
//	public void addResource(ServiceReference<Object> jaxrsResourceRef) {
//		boolean added = resourcesRefList.add(jaxrsResourceRef);
//		System.out.println("added " + jaxrsResourceRef.getProperty("component.name"));
//		if (defaultApplication != null && added) {
//			defaultApplication..get.addResourceReference(jaxrsResourceRef);
//			runtime.reloadApplication(defaultApplication);
//		}
//		if (added) {
//			incrementChangeCount();
//			ServiceReference<?> serviceRef = serviceRuntime == null ? null : serviceRuntime.getReference();
//			runtime.updateRuntimeDTO(serviceRef);
//		}
//	}

	/**
	 * Increments the change count and updates the service properties
	 */
	private void incrementChangeCount() {
		long changecount = changeCount.incrementAndGet();
		if (serviceRuntime != null) {
			ServiceReference<JerseyServiceRuntime> ref = serviceRuntime.getReference();
			Dictionary<String, Object> newProperties = new Hashtable<>();
			for (String p : ref.getPropertyKeys()) {
				if (p.equals("service.changecount")) {
					newProperties.put(p, Long.valueOf(changecount));
				} else {
					newProperties.put(p, ref.getProperty(p));
				}
			}
			serviceRuntime.setProperties(newProperties);
		}
	}

//	/**
//	 * Called when removing
//	 * @param jaxrsResourceRef
//	 */
//	public void removeResource(ServiceReference<?> jaxrsResourceRef) {
//		boolean removed = resourcesRefList.remove(jaxrsResourceRef);
//		if (defaultApplication != null && removed) {
//			defaultApplication.removeResourceReference(jaxrsResourceRef);
//			runtime.reloadApplication(defaultApplication);
//		}
//		if (removed) {
//			ServiceReference<?> serviceRef = serviceRuntime == null ? null : serviceRuntime.getReference();
//			runtime.updateRuntimeDTO(serviceRef);
//		}
//	}

	/**
	 * Updates the fields that are provided by service properties.
	 * @param ctx the component context
	 * @throws ConfigurationException thrown when no context is available or the expected property was not provided 
	 */
	private void updateProperties(ComponentContext ctx) throws ConfigurationException {
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
		strict = JerseyHelper.getPropertyWithDefault(ctx, JerseyConstants.JERSEY_STRICT_MODE, false);
	}

}
