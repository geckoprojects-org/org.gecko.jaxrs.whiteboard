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
package org.eclipselabs.osgi.jersey.application;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.eclipselabs.osgi.jersey.JaxRsResourceProvider;

/**
 * 
 * @author Mark Hoffmann
 * @since 15.07.2017
 */
@ApplicationPath("/")
public class JerseyApplication extends Application {

	private volatile Map<String, Class<?>> classesMap = new ConcurrentHashMap<>();
	private volatile Map<String, Object> singletonMap = new ConcurrentHashMap<>();
	private final String applicationName;
	private final Logger log = Logger.getLogger("o.e.o.j.application");

	public JerseyApplication(String applicationName) {
		this.applicationName = applicationName;
	}

	/* 
	 * (non-Javadoc)
	 * @see javax.ws.rs.core.Application#getClasses()
	 */
	@Override
	public Set<Class<?>> getClasses() {
		return Collections.unmodifiableSet(new HashSet<>(classesMap.values()));
	}

	/* 
	 * (non-Javadoc)
	 * @see javax.ws.rs.core.Application#getSingletons()
	 */
	@Override
	public Set<Object> getSingletons() {
		return Collections.unmodifiableSet(new HashSet<>(singletonMap.values()));
	}

	/**
	 * Returns the name of the whiteboard
	 * @return the name of the whiteboard
	 */
	public String getApplicationName() {
		return applicationName;
	}

	/**
	 * Adds a resource provider to the application
	 * @param resourceProvider the provider to register
	 */
	public void addResource(JaxRsResourceProvider resourceProvider) {
		if (resourceProvider == null) {
			if (log != null) {
				log.log(Level.WARNING, "A null service resource provider was given to register as a JaxRs resource");
			}
			return;
		}
		String name = resourceProvider.getName();
		if (resourceProvider.isSingleton()) {
			Object resource = resourceProvider.getResource();
			singletonMap.put(name, resource);
		} else {
			Class<?> resourceClass = resourceProvider.getResourceClass();
			classesMap.put(name, resourceClass);
		}
	}

	/**
	 * Removes a resource from the application
	 * @param resourceProvider the provider of the resource to be removed
	 */
	public void removeResource(JaxRsResourceProvider resourceProvider) {
		if (resourceProvider == null) {
			if (log != null) {
				log.log(Level.WARNING, "A null resource provider was given to unregister as a JaxRs resource");
			}
			return;
		}
		String name = resourceProvider.getName();
		if (resourceProvider.isSingleton()) {
			synchronized (singletonMap) {
				singletonMap.remove(name);
			}
		} else {
			synchronized (classesMap) {
				classesMap.remove(name);
			}
		}
	}

	/**
	 * Cleans up all resources
	 */
	public void dispose() {
		classesMap.clear();
		singletonMap.clear();
	}

}
