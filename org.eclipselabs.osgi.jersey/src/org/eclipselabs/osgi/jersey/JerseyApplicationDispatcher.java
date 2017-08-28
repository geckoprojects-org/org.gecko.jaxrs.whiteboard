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
import java.util.Set;

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
 * Dispatcher component that assigns all resources and extensions to applications
 * @author Mark Hoffmann
 * @since 28.08.2017
 */
@Component(name="JerseyApplicationDispatcher", service=JaxRsApplicationDispatcher.class, immediate=true)
public class JerseyApplicationDispatcher implements JaxRsApplicationDispatcher {
	
	private volatile Set<ServiceReference<Application>> applicationCache = new HashSet<>();
	private volatile Set<ServiceReference<?>> resourceCache = new HashSet<>();
	private volatile Set<JaxRsJerseyRuntime> runtimeCache = new HashSet<>();
	private volatile BundleContext bundleContext = null;
	
	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsApplicationDispatcher#getApplications()
	 */
	@Override
	public Set<ServiceReference<Application>> getApplications() {
		return Collections.unmodifiableSet(applicationCache);
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
	 * @see org.eclipselabs.osgi.jersey.JaxRsApplicationDispatcher#getRuntimes()
	 */
	@Override
	public Set<JaxRsJerseyRuntime> getRuntimes() {
		return Collections.unmodifiableSet(runtimeCache);
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
	@Reference(name="runtime", cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC, unbind="removeRuntime")
	public void addRuntime(JaxRsJerseyRuntime runtimeRef) {
		boolean added = runtimeCache.add(runtimeRef);
		if (added) {
			updateRuntimeOnAdd(runtimeRef);
		}
	}
	
	/**
	 * Removes a jersey runtime 
	 * @param runtime the runtime to remove
	 */
	public void removeRuntime(JaxRsJerseyRuntime runtimeRef) {
		boolean removed = runtimeCache.remove(runtimeRef);
		if (removed) {
			updateRuntimeOnRemove(runtimeRef);
		}
	}
	
	/**
	 * Adds a new application
	 * @param application the application to add
	 */
	@Reference(name="application", cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC, unbind="removeApplication")
	public void addApplication(ServiceReference<Application> applicationRef) {
		boolean added = applicationCache.add(applicationRef);
		if (added) {
			updateApplicationOnAdd(applicationRef);
		}
	}
	
	/**
	 * Removes a application 
	 * @param application the application to remove
	 */
	public void removeApplication(ServiceReference<Application> applicationRef) {
		boolean removed = applicationCache.remove(applicationRef);
		if (removed) {
			updateApplicationOnRemove(applicationRef);
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
	 * Called, when a new runtime was added
	 * @param runtime the runtaime that was added
	 */
	private void updateRuntimeOnAdd(JaxRsJerseyRuntime runtime) {
		Collections.unmodifiableSet(applicationCache).forEach((appRef)->doAddApplication(runtime, appRef));
	}

	private void updateRuntimeOnRemove(JaxRsJerseyRuntime runtime) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Called when an new application reference was added.
	 * @param applicationRef the new application reference
	 */
	private void updateApplicationOnAdd(ServiceReference<Application> applicationRef) {
		Collections.unmodifiableSet(runtimeCache).forEach((runtime)->doAddApplication(runtime, applicationRef));
	}
	
	/**
	 * Adds the given application reference to the given runtime
	 * @param runtime the runtime to add the application
	 * @param applicationRef the application reference to be added to the runtime
	 */
	private void doAddApplication(JaxRsJerseyRuntime runtime, ServiceReference<Application> applicationRef) {
		Application application = bundleContext.getService(applicationRef);
		JaxRsApplicationProvider provider = new JerseyApplicationProvider("", application);
		runtime.registerApplication(provider);
	}

	private void updateApplicationOnRemove(ServiceReference<Application> applicationRef) {
		// TODO Auto-generated method stub
		
	}

	private void updateResourceOnAdd(ServiceReference<?> resource) {
		// TODO Auto-generated method stub
		
	}

	private void updateResourceOnRemove(ServiceReference<?> resource) {
		// TODO Auto-generated method stub
		
	}

}
