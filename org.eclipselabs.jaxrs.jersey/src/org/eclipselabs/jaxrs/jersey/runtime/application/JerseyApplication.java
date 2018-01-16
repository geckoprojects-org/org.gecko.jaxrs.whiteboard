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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Application;

import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationContentProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsExtensionProvider;
import org.eclipselabs.jaxrs.jersey.runtime.application.feature.WhiteboardFeature;

/**
 * Special JaxRs application implementation that holds and updates all resource and extension given by the application provider
 * @author Mark Hoffmann
 * @since 15.07.2017
 */
public class JerseyApplication extends Application {

	private volatile Map<String, Class<?>> classes = new ConcurrentHashMap<>();
	private volatile Map<String, Object> singletons = new ConcurrentHashMap<>();
	private volatile Map<String, JaxRsExtensionProvider> extensions = new ConcurrentHashMap<>();
	private volatile Map<String, JaxRsApplicationContentProvider> contentProviders = new ConcurrentHashMap<>();
	private final String applicationName;
	private final Logger log = Logger.getLogger("jersey.application");
	private Map<String, Object> properties;
	private Application sourceApplication;

	public JerseyApplication(String applicationName) {
		this.applicationName = applicationName;
	}

	public JerseyApplication(String applicationName, Application sourceApplication) {
		this.applicationName = applicationName;
		this.sourceApplication = sourceApplication;
		properties = Collections.unmodifiableMap(sourceApplication.getProperties());
	}

	@Override
	public Map<String, Object> getProperties() {
		return properties;
	}
	
	/* 
	 * (non-Javadoc)
	 * @see javax.ws.rs.core.Application#getClasses()
	 */
	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>> resutlClasses = new HashSet<>();
		resutlClasses.addAll(classes.values());
		resutlClasses.addAll(sourceApplication.getClasses());
		return Collections.unmodifiableSet(resutlClasses);
	}

	/* 
	 * (non-Javadoc)
	 * @see javax.ws.rs.core.Application#getSingletons()
	 */
	@Override
	public Set<Object> getSingletons() {
		Set<Object> resutlSingletons = new HashSet<>();
		resutlSingletons.addAll(singletons.values());
		resutlSingletons.addAll(sourceApplication.getSingletons());
		if(!extensions.isEmpty()) {
			resutlSingletons.add(new WhiteboardFeature(extensions));
		}
		return Collections.unmodifiableSet(resutlSingletons);
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
	@SuppressWarnings("unchecked")
	public boolean addContent(JaxRsApplicationContentProvider contentProvider) {
		if (contentProvider == null) {
			if (log != null) {
				log.log(Level.WARNING, "A null service content provider was given to register as a JaxRs resource or extension");
			}
			return false;
		}
		
		String name = contentProvider.getName();
		contentProviders.put(name, contentProvider);
		if(contentProvider instanceof JaxRsExtensionProvider) {
			Class<?> resourceClass = contentProvider.getObjectClass();
			JaxRsExtensionProvider result = extensions.put(name, (JaxRsExtensionProvider) contentProvider);
			return  result == null || !resourceClass.equals(result.getObjectClass());
		} else if (contentProvider.isSingleton()) {
			Class<?> resourceClass = contentProvider.getObjectClass();
			Object result = singletons.get(name);
			if(result == null || !result.getClass().equals(resourceClass)){
				result = singletons.put(name, contentProvider.getServiceObjects().getService());
				if(result != null) {
					contentProvider.getServiceObjects().ungetService(result);
				}
				return true;
			}
			return false;
		} else {
			Class<?> resourceClass = contentProvider.getObjectClass();
			Object result = classes.put(name, resourceClass);
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
		if(contentProvider instanceof JaxRsExtensionProvider) {
			synchronized (extensions) {
				extensions.remove(name);
			}
		} else if (contentProvider.isSingleton()) {
			synchronized (singletons) {
				singletons.remove(name);
			}
		} else {
			synchronized (classes) {
				classes.remove(name);
			}
		}
		synchronized (contentProviders) {
			return contentProviders.remove(name) != null;
		}
	}

	/**
	 * Cleans up all resources
	 */
	public void dispose() {
		contentProviders.clear();
		extensions.clear();
		classes.clear();
		singletons.clear();
	}

	/**
	 * @return a Collection of contentProviders
	 */
	public Collection<JaxRsApplicationContentProvider> getContentProviders(){
		return contentProviders.values();
	}
	
}
