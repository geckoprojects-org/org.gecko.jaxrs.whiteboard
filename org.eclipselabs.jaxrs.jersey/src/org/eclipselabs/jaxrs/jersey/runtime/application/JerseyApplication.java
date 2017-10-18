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
package org.eclipselabs.jaxrs.jersey.runtime.application;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Application;

import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationContentProvider;

/**
 * Special JaxRs application implementation that holds and updates all resource and extension given by the application provider
 * @author Mark Hoffmann
 * @since 15.07.2017
 */
public class JerseyApplication extends Application {

	private volatile Map<String, Class<?>> classesMap = new ConcurrentHashMap<>();
	private volatile Map<String, Object> singletonMap = new ConcurrentHashMap<>();
	private final String applicationName;
	private final Logger log = Logger.getLogger("jersey.application");

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
	 * Adds a content provider to the application
	 * @param contentProvider the provider to register
	 * @return <code>true</code>, if content was added
	 */
	public boolean addContent(JaxRsApplicationContentProvider contentProvider) {
		if (contentProvider == null) {
			if (log != null) {
				log.log(Level.WARNING, "A null service content provider was given to register as a JaxRs resource or extension");
			}
			return false;
		}
		String name = contentProvider.getName();
		if (contentProvider.isSingleton()) {
			Object resource = contentProvider.getObject();
			Object result = singletonMap.put(name, resource);
			return !resource.equals(result) || result == null;
		} else {
			Class<?> resourceClass = contentProvider.getObjectClass();
			Object result = classesMap.put(name, resourceClass);
			return !resourceClass.equals(result) || result == null;
		}
	}

	/**
	 * Removes a content from the application
	 * @param contentProvider the provider of the contents to be removed
	 * @return Return <code>true</code>, if the content was removed
	 */
	public boolean removeContent(JaxRsApplicationContentProvider contentProvider) {
		if (contentProvider == null) {
			if (log != null) {
				log.log(Level.WARNING, "A null resource provider was given to unregister as a JaxRs resource");
			}
			return false;
		}
		String name = contentProvider.getName();
		if (contentProvider.isSingleton()) {
			synchronized (singletonMap) {
				return singletonMap.remove(name) != null;
			}
		} else {
			synchronized (classesMap) {
				return classesMap.remove(name) != null;
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
