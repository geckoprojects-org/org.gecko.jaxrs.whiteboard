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
package org.eclipselabs.jaxrs.jersey.factories;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipselabs.jaxrs.jersey.provider.hk2.HK2FactoryProvider;
import org.glassfish.hk2.api.Factory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferenceScope;

/**
 * Provider component the creates HK2 factories for prototype scoped resources
 * @author Mark Hoffmann
 * @since 12.07.2017
 */
@Component(name="JerseyResourceInstanceFactoryProvider", immediate=true)
public class JerseyResourceInstanceFactoryProvider implements HK2FactoryProvider {
	
	private Map<String, ServiceReference<Object>> resourcePrototypeMap = new ConcurrentHashMap<>();
	private volatile ComponentContext ctx;
	
	/**
	 * Activated on component activation
	 * @param ctx the component context
	 */
	@Activate
	public void activate(ComponentContext ctx) {
		this.ctx = ctx;
	}
	
	/**
	 * Called on component de-activation
	 */
	@Deactivate
	public void deactivate() {
		resourcePrototypeMap.clear();
		this.ctx = null;
		
	}

	/**
	 * Adds a new resource service reference to this component 
	 * @param resourceRef the resource reference
	 */
	@Reference(cardinality=ReferenceCardinality.MULTIPLE, 
			policy=ReferencePolicy.DYNAMIC, 
			target="(osgi.jaxrs.resource=*)", 
			scope=ReferenceScope.PROTOTYPE_REQUIRED, 
			unbind = "removeResource")
	public void addResource(ServiceReference<Object> resourceRef) {
		String className = (String) resourceRef.getProperty("osgi.jaxrs.resource");
		if (className != null && !className.isEmpty()) {
			resourcePrototypeMap.put(className, resourceRef);
		}
	}
	
	/**
	 * Removes a given resource service reference
	 * @param resourceRef the reference to be removed
	 */
	public void removeResource(ServiceReference<Object> resourceRef) {
		String className = (String) resourceRef.getProperty("osgi.jaxrs.resource");
		if (className != null && !className.isEmpty()) {
			resourcePrototypeMap.remove(className);
		}
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JerseyFactoryProvider#createResourceFactory(java.lang.Class)
	 */
	public <T> Factory<T> createResourceFactory(Class<T> clazz) {
		if (ctx == null) {
			throw new IllegalStateException("The component is in invalid state, where no bundle context is availabe to create a factory");
		}
		BundleContext bctx = ctx.getBundleContext();
		if (clazz == null) {
			return null;
		}
		String className = clazz.getName();
		ServiceReference<Object> resourceRef = resourcePrototypeMap.get(className);
		if (resourceRef == null) {
			return null;
		} else {
			return new JerseyResourceInstanceFactory<>(bctx, clazz, resourceRef);
		}
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JerseyFactoryProvider#isPrototypeResource(java.lang.Class)
	 */
	public <T> boolean isPrototypeResource(Class<T> clazz) {
		String className = clazz.getName();
		return resourcePrototypeMap.containsKey(className);
	}
}
