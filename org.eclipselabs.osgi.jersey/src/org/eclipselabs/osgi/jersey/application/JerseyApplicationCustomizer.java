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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Jersey service tracker custimzer
 * @author Mark Hoffmann
 * @since 16.07.2017
 */
public class JerseyApplicationCustomizer<S, T> implements ServiceTrackerCustomizer<S, T> {
	
	private static final Logger logger = Logger.getLogger("JerseyApplicationCustomizer");
	private volatile String whiteboardName;
	private volatile BundleContext context;
	private volatile JerseyApplicationProvider provider;
	
	public JerseyApplicationCustomizer(BundleContext context, String whiteboardName, JerseyApplicationProvider provider) {
		this.context = context;
		this.whiteboardName = whiteboardName;
		this.provider = provider;
	}

	@Override
	public T addingService(ServiceReference<S> reference) {
		String resource = (String) reference.getProperty(JaxRSWhiteboardConstants.JAX_RS_RESOURCE);
		// service is no valid resource
		if (!"true".equalsIgnoreCase(resource)) {
			logger.warning("Service reference is no valid resource");
			return null;
		}
		String applicationSelect =  (String) reference.getProperty(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_SELECT);
		if (applicationSelect != null) {
			try {
				Filter targetFilter = FrameworkUtil.createFilter(applicationSelect);
			} catch (InvalidSyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Map<String, Object> properties = new HashMap<>();
//			properties.putAll(provider.getProperties());
			logger.warning("Service reference is a resource but is assigned to an application");
			return null;
		}
		String target = (String) reference.getProperty(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET);
		if (target != null) {
			try {
				Filter targetFilter = FrameworkUtil.createFilter(target);
				Map<String, Object> properties = new HashMap<>();
				properties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, whiteboardName);
				if (targetFilter.matches(properties)) {
					logger.info("Service reference is assigned to the default application");
				}
			} catch (InvalidSyntaxException e) {
				e.printStackTrace();
				return null;
			}
		} 
		String scope = (String) reference.getProperty("service.scope");
		if (Constants.SCOPE_PROTOTYPE.equalsIgnoreCase(scope)) {
			return (T) context.getServiceObjects(reference);
		} else {
			return (T) context.getService(reference);
		}
	}

	@Override
	public void modifiedService(ServiceReference<S> reference, T service) {
		System.out.println("modified " + reference.getProperty("component.name"));
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removedService(ServiceReference<S> reference, T service) {
		System.out.println("removed " + reference.getProperty("component.name"));
		// TODO Auto-generated method stub
		
	}

}
