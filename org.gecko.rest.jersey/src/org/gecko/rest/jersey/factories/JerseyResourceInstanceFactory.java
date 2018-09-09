/**
 * Copyright (c) 2012 - 2018 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.gecko.rest.jersey.factories;

import java.util.HashSet;
import java.util.Set;

import org.gecko.rest.jersey.binder.PrototypeServiceBinder;
import org.gecko.rest.jersey.provider.application.JaxRsApplicationContentProvider;
import org.glassfish.hk2.api.Factory;
import org.osgi.framework.ServiceObjects;

/**
 * HK2 creation factory for JaxRs resource instance. These factory instances will be bound using the {@link PrototypeServiceBinder}.
 * The factory is responsible to create or releasing a certain JaxRs resource instances, at request time.
 * @param <T> the type of the resource, which is the class type
 * @author Mark Hoffmann
 * @since 12.07.2017
 */
public class JerseyResourceInstanceFactory<T> implements Factory<T> {

	private volatile Set<T> instanceCache = new HashSet<>();
	private JaxRsApplicationContentProvider provider;
	private ServiceObjects<T> serviceObjects;

	/**
	 * Creates a new instance. A service reference will be cached lazily, on the first request
	 * @param clazz the resource class
	 */
	public JerseyResourceInstanceFactory(JaxRsApplicationContentProvider provider) {
		this.provider = provider;
		serviceObjects = provider.getProviderObject();;
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
	@Override
	public T provide() {
		try {
			// If the service objects is null, the service is obviously gone and we return null to avoid exception in jersey
			if (serviceObjects == null) {
				return null;
			}
			T instance = serviceObjects.getService();
			synchronized (instanceCache) {
				instanceCache.add(instance);
			}
			return (T)instance;
		} catch (Exception e) {
			if (e instanceof IllegalStateException) {
				throw e;
			}
			throw new IllegalStateException("Cannot create prototype instance for class " + provider.getId(), e);
		}
	}

	/**
	 * Cleans up all resources. If a service reference was given via constructor, the reference is now <code>null</code>
	 * After calling dispose, a new instance has to be created
	 */
	public void dispose() {
		// release all cached service instances
		if (serviceObjects != null) {
			instanceCache.forEach((i) -> serviceObjects.ungetService(i));
		}
		instanceCache.clear();
	}
	
	/**
	 * Return the size of the cached instances
	 * @return the size of the cached instances
	 */
	public int getCacheInstanceCount() {
		return instanceCache.size();
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
				serviceObjects.ungetService(instance);
			} catch (Exception e) {
				if (e instanceof IllegalStateException) {
					throw e;
				}
				throw new IllegalStateException("Error disposing instance " + instance, e);
			}
		}
	}

}
