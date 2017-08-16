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

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * 
 * @author Mark Hoffmann
 * @since 15.07.2017
 */
@ApplicationPath("/")
public class JerseyApplication extends Application {
	
	private volatile Map<ServiceReference<?>, Class<?>> classesMap = new ConcurrentHashMap<>();
	private volatile Map<ServiceReference<?>, Object> singletonMap = new ConcurrentHashMap<>();
	private final String whiteboardName;
	private final BundleContext context;
	private final Logger log = Logger.getLogger("o.e.o.j.application");
	
	public JerseyApplication(String whiteboardName, BundleContext context) {
		this.whiteboardName = whiteboardName;
		this.context = context;
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
	public String getWhiteboardName() {
		return whiteboardName;
	}
	
	/**
	 * Adds a service reference for a resource to the default application
	 * @param resourceRef the reference to register
	 */
	public void addResourceReference(ServiceReference<?> resourceRef) {
		if (resourceRef == null) {
			if (log != null) {
				log.log(Level.WARNING, "A null service reference was given to register as a JaxRs resource");
			}
			return;
		}
		if (context == null) {
			throw new IllegalStateException("No bundle context was given to the JerseyApplication");
		}
		if (whiteboardName == null) {
			throw new IllegalStateException("No whiteboard name was given to the JerseyApplication");
		}
		String resource = (String) resourceRef.getProperty(JaxRSWhiteboardConstants.JAX_RS_RESOURCE);
		String extension = (String) resourceRef.getProperty(JaxRSWhiteboardConstants.JAX_RS_EXTENSION);
		if (!Boolean.parseBoolean(resource) && !Boolean.parseBoolean(extension)) {
			if (log != null) {
				log.log(Level.WARNING, "The given service reference is not marked as JaxRs resource or extension. Expected property 'osgi.jaxrs.resource=true' or 'osgi.jaxrs.extension=true'");
			}
			return;
		}
		String whiteboardTarget = (String) resourceRef.getProperty(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET);
		if (!whiteboardName.equalsIgnoreCase(whiteboardTarget)) {
			if (log != null) {
				log.log(Level.WARNING, "The given service reference of the resource is not targeted to this whiteboard name");
			}
			return;
		}
		String scope = (String) resourceRef.getProperty("service.scope");
		if (Constants.SCOPE_PROTOTYPE.equalsIgnoreCase(scope)) {
			ServiceObjects<?> so = context.getServiceObjects(resourceRef);
			if (so == null) {
				if (log != null) {
					log.log(Level.WARNING, "The prototype service of the given service references has gone");
				}
			} else {
				Object o = so.getService();
				if (o == null) {
					if (log != null) {
						log.log(Level.WARNING, "The prototype service of the given service references has gone");
					}
				} else {
					classesMap.put(resourceRef, o.getClass());
				}
			}
		} else {
			Object service = context.getService(resourceRef);
			if (service == null) {
				if (log != null) {
					log.log(Level.WARNING, "The service of the given service references has gone");
				}
			} else {
				singletonMap.put(resourceRef, service);
			}
		}
	}
	
	/**
	 * Removed a resource service reference from the default application
	 * @param resourceRef the service reference of the resource to be removed
	 */
	public void removeResourceReference(ServiceReference<?> resourceRef) {
		if (resourceRef == null) {
			if (log != null) {
				log.log(Level.WARNING, "A null service reference was given to unregister as a JaxRs resource");
			}
			return;
		}
		synchronized (classesMap) {
			classesMap.remove(resourceRef);
		}
		synchronized (singletonMap) {
			singletonMap.remove(resourceRef);
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
