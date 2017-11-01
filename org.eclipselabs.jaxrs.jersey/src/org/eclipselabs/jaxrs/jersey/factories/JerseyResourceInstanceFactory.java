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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipselabs.jaxrs.jersey.binder.PrototypeServiceBinder;
import org.glassfish.hk2.api.Factory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Reference;

/**
 * HK2 creation factory for JaxRs resource instance. These factory instances will be bound using the {@link PrototypeServiceBinder}.
 * The factory is responsible to create or releasing a certain JaxRs resource instances, at request time.
 * @param <T> the type of the resource, which is the class type
 * @author Mark Hoffmann
 * @since 12.07.2017
 */
public class JerseyResourceInstanceFactory<T> implements Factory<T> {

	private final BundleContext bctx;
	private final Class<T> clazz;
	private ServiceReference<Object> resourceReference;
	private volatile Set<Object> instanceCache = new HashSet<>();

	/**
	 * Creates a new instance. A service reference will be cached lazily, on the first request
	 * @param bctx the bundle context
	 * @param clazz the resource class
	 */
	public JerseyResourceInstanceFactory(BundleContext bctx, Class<T> clazz) {
		this.bctx = bctx;
		this.clazz = clazz;
		this.resourceReference = null;
	}

	/**
	 * Creates a new instance. The given service reference will be taken, to determine the {@link ServiceObjects}.
	 * If the given service reference is not of scope prototype, the provide method will return <code>null</code>,
	 * in the same manner like, if it does not find service with the scope 
	 * @param bctx the bundle context
	 * @param clazz the resource class
	 * @param resourceReference the service reference to the resource service
	 */
	public JerseyResourceInstanceFactory(BundleContext bctx, Class<T> clazz, ServiceReference<Object> resourceReference) {
		this.bctx = bctx;
		this.clazz = clazz;
		this.resourceReference = resourceReference;
	}

	/* (non-Javadoc)
	 * @see org.glassfish.hk2.api.Factory#dispose(java.lang.Object)
	 */
	@Override
	public void dispose(T object) {
		disposeInstance(object);
	}

	/* (non-Javadoc)
	 * @see org.glassfish.hk2.api.Factory#provide()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T provide() {
		if (bctx == null) {
			throw new IllegalStateException("Cannot create instances because bundle context is not available");
		}
		if (clazz == null) {
			throw new IllegalStateException("Cannot create instances because not class was given");
		}
		try {
			ServiceObjects<Object> soInstance = getServiceObjects();
			// If the service objects is null, the service is obviously gone and we return null to avoid exception in jersey
			if (soInstance == null) {
				return null;
			}
			Object instance = soInstance.getService();
			synchronized (instanceCache) {
				instanceCache.add(instance);
			}
			return (T)instance;
		} catch (Exception e) {
			if (e instanceof IllegalStateException) {
				throw e;
			}
			throw new IllegalStateException("Cannot create prototype instance for class " + clazz.getName(), e);
		}
	}

	/**
	 * Cleans up all resources. If a service reference was given via constructor, the reference is now <code>null</code>
	 * After calling dispose, a new instance has to be created
	 */
	public void dispose() {
		// release all cached service instances
		ServiceObjects<Object> soInstance = getServiceObjects();
		if (soInstance != null) {
			instanceCache.forEach((i) -> soInstance.ungetService(i));
		}
		instanceCache.clear();
		resourceReference = null;
	}
	
	/**
	 * Return the size of the cached instances
	 * @return the size of the cached instances
	 */
	public int getCacheInstanceCount() {
		return instanceCache.size();
	}

	/**
	 * Returns a service reference for the given class. If already an {@link ServiceReference} was set
	 * @return the service {@link Reference} or otherwise an {@link IllegalStateException} will be thrown
	 */
	private ServiceReference<Object> getServiceReference() {
		if (bctx == null) {
			throw new IllegalStateException("Cannot create instances because bundle context is not available");
		}
		if (clazz == null) {
			throw new IllegalStateException("Cannot create instances because not class was given");
		}
		ServiceReference<Object> reference = resourceReference;
		if (resourceReference == null) {
			Collection<ServiceReference<Object>> serviceReferences;
			try {
				serviceReferences = bctx.getServiceReferences(Object.class, "(osgi.jaxrs.resource=true)");
				if (serviceReferences.isEmpty()) {
					throw new IllegalStateException("There was no service found for class: " + clazz.getSimpleName());
				} else {
					reference = serviceReferences.iterator().next();
				}
			} catch (InvalidSyntaxException e) {
				throw new IllegalStateException("Cannot execute a service lookup because of invalid syntax: ", e);
			}
		}
		if (reference == null) {
			throw new IllegalStateException("The resulting service reference is null");
		}
		Object scope = reference.getProperty(Constants.SERVICE_SCOPE);
		if ("prototype".equals(scope)) {
			if (!reference.equals(resourceReference)) {
				resourceReference = reference;
			}
		} else {
			throw new IllegalStateException("The resulting service reference is not of service scope prototype");
		}
		return resourceReference;
	}

	/**
	 * Returns the {@link ServiceObjects} instance or <code>null</code>, in case the service is already gone
	 * @return the {@link ServiceObjects} instance or <code>null</code>
	 */
	private ServiceObjects<Object> getServiceObjects() {
		try {
			ServiceReference<Object> reference = getServiceReference();
			ServiceObjects<Object> soInstance = bctx.getServiceObjects(reference);
			if (soInstance == null) {
				return null;
			} else {
				return soInstance;
			}
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Disposes a service instance. If it is a prototype instance, it will be removed from the cache.
	 * @param instance the instance to be released
	 */
	private void disposeInstance(T instance) {
		if (instance == null) {
			return;
		}
		if (instanceCache.remove(instance)) {
			try {
				ServiceObjects<Object> soInstance = getServiceObjects();
				soInstance.ungetService(instance);
			} catch (Exception e) {
				if (e instanceof IllegalStateException) {
					throw e;
				}
				throw new IllegalStateException("Error disposing instance " + instance, e);
			}
		}
	}

}
