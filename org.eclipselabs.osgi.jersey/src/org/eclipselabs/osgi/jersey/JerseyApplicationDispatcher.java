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
package org.eclipselabs.osgi.jersey;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.Application;

import org.eclipselabs.osgi.jersey.application.JerseyApplicationProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * Dispatcher component that assigns all resources and extensions to applications and a given whiteboard
 * @author Mark Hoffmann
 * @since 28.08.2017
 */
@Component(name="JerseyApplicationDispatcher", service=JaxRsApplicationDispatcher.class, immediate=true)
public class JerseyApplicationDispatcher implements JaxRsApplicationDispatcher {
	
	private volatile Map<String, JaxRsApplicationProvider> applicationProviderCache = new ConcurrentHashMap<>();
	private volatile Set<Application> applicationCache = new HashSet<>();
	private volatile Set<ServiceReference<?>> resourceCache = new HashSet<>();
	private volatile JaxRsJerseyRuntime runtime;
	private volatile BundleContext bundleContext = null;
	
	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsApplicationDispatcher#getApplications()
	 */
	@Override
	public Set<JaxRsApplicationProvider> getApplications() {
		return Collections.unmodifiableSet(new HashSet<>(applicationProviderCache.values()));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsApplicationDispatcher#getResources()
	 */
	@Override
	public Set<ServiceReference<?>> getResources() {
		return Collections.unmodifiableSet(resourceCache);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsApplicationDispatcher#getRuntime()
	 */
	@Override
	public JaxRsJerseyRuntime getRuntime() {
		return runtime;
	}

	/**
	 * Called on component activation
	 * @param context the component context
	 */
	@Activate
	public void activate(ComponentContext context) {
		bundleContext = context.getBundleContext();
	}
	
	/**
	 * Called on component de-activation
	 * @param context the component context
	 */
	@Deactivate
	public void deactivate(ComponentContext context) {
		
	}
	
	/**
	 * Adds a new jersey runtime 
	 * @param runtime the runtime to add
	 */
	@Reference(name="runtime", cardinality=ReferenceCardinality.MANDATORY, policy=ReferencePolicy.STATIC, unbind="removeRuntime")
	public void addRuntime(JaxRsJerseyRuntime runtimeRef) {
		runtime = runtimeRef;
	}
	
	/**
	 * Removes a jersey runtime 
	 * @param runtime the runtime to remove
	 */
	public void removeRuntime(JaxRsJerseyRuntime runtimeRef) {
		runtime = null;
	}
	
	/**
	 * Adds a new application
	 * @param application the application to add
	 * @param properties the service properties
	 */
	@Reference(name="application", cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC, unbind="removeApplication")
	public void addApplication(Application application, Map<String, Object> properties) {
		boolean added = applicationCache.add(application);
		if (added) {
			updateApplicationOnAdd(application, properties);
		}
	}
	
	/**
	 * Removes a application 
	 * @param application the application to remove
	 * @param properties the service properties
	 */
	public void removeApplication(Application application, Map<String, Object> properties) {
		boolean removed = applicationCache.remove(application);
		if (removed) {
			updateApplicationOnRemove(application, properties);
		}
	}
	
	/**
	 * Adds a JaxRs resource or extension 
	 * @param resource the resource or extension to add
	 */
	@Reference(name="resource", cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC, unbind="removeResource", updated="modifyResource", target="(|(" + JaxRSWhiteboardConstants.JAX_RS_RESOURCE + "=true)(" + JaxRSWhiteboardConstants.JAX_RS_EXTENSION + "=true))")
	public void addResource(ServiceReference<Object> resourceRef) {
		boolean added = resourceCache.add(resourceRef);
		if (added) {
			updateResourceOnAdd(resourceRef);
		}
	}
	
	/**
	 * Removes a JaxRs resource or extension
	 * @param resource the resource or extension to remove
	 */
	public void removeResource(ServiceReference<Object> resourceRef) {
		boolean removed = resourceCache.remove(resourceRef);
		if (removed) {
			updateResourceOnRemove(resourceRef);
		}
	}
	
	/**
	 * Called when an new application reference was added.
	 * @param application the new application
	 * @param properties the service properties
	 */
	private void updateApplicationOnAdd(Application application, Map<String, Object> properties) {
		JaxRsApplicationProvider provider = doAddApplication(application, properties);
		if (provider.canHandleWhiteboard(runtime.getProperties())) {
			runtime.registerApplication(provider);
		}
	}
	
	/**
	 * Adds the given application reference to the given runtime
	 * @param runtime the runtime to add the application
	 * @param application the application reference to be added to the runtime
	 * @param properties the service properties
	 * @return the application provider
	 */
	private JaxRsApplicationProvider doAddApplication(Application application, Map<String, Object> properties) {
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(application, properties);
		String name = provider.getName();
		applicationProviderCache.put(name, provider);
		return provider;
	}

	/**
	 * Returns the application name or generates one
	 * @param properties the service properties
	 * @return the application name or a generated one
	 */
	private String getApplicationName(Map<String, Object> properties) {
		String name = null;
		if (properties != null) {
			Long serviceId = (Long) properties.get("service.id");
			Long componentId = (Long) properties.get("component.id");
			name = (String) properties.get(JaxRSWhiteboardConstants.JAX_RS_NAME);
			if (name == null) {
				if (serviceId != null) {
					name = ".sid" + serviceId.toString();
				} else if (componentId != null) {
					name = ".cid" + componentId.toString();
				} 
			}
		}
		return name == null ? "." + UUID.randomUUID().toString() : name;
	}

	private void updateApplicationOnRemove(Application application, Map<String, Object> properties) {
		String name = JerseyApplicationProvider.getApplicationName(properties);
		JaxRsApplicationProvider provider = null;
		if (!applicationProviderCache.containsKey(name)) {
			Optional<JaxRsApplicationProvider> first = applicationProviderCache.values().stream().filter((p)->p.getJaxRsApplication().equals(application)).findFirst();
			if (first.isPresent()) {
				provider = applicationProviderCache.remove(first.get().getName());
			}
		} else {
			provider = applicationProviderCache.remove(name);
		}
		if (provider != null) {
			runtime.unregisterApplication(provider);
		} 
		
	}

	private void updateResourceOnAdd(ServiceReference<?> resource) {
		// TODO Auto-generated method stub
		
	}

	private void updateResourceOnRemove(ServiceReference<?> resource) {
		// TODO Auto-generated method stub
		
	}

}
