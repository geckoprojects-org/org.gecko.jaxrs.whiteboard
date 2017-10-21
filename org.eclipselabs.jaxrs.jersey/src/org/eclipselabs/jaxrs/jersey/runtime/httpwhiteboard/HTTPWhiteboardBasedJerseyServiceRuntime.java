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
package org.eclipselabs.jaxrs.jersey.runtime.httpwhiteboard;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.servlet.Servlet;

import org.eclipselabs.jaxrs.jersey.helper.JerseyHelper;
import org.eclipselabs.jaxrs.jersey.runtime.common.AbstractJerseyServiceRuntime;
import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntime;

import aQute.bnd.annotation.headers.ProvideCapability;
import aQute.bnd.annotation.headers.RequireCapability;

/**
 * Implementation of the {@link JaxRSServiceRuntime} for a Jersey implementation
 * @author Mark Hoffmann
 * @since 12.07.2017
 */
@ProvideCapability(ns = ImplementationNamespace.IMPLEMENTATION_NAMESPACE, 
version="1.0", 
value = "osgi.implementation=\"osgi.jaxrs\";provider=jersey;http.whiteboard=true", 
uses= {"javax.ws.rs", "javax.ws.rs.client", "javax.ws.rs.container", "javax.ws.rs.core", "javax.ws.rs.ext", "org.osgi.service.jaxrs.whiteboard"})
@RequireCapability(ns = ImplementationNamespace.IMPLEMENTATION_NAMESPACE, filter = "(osgi.implementation=osgi.http)")
public class HTTPWhiteboardBasedJerseyServiceRuntime extends AbstractJerseyServiceRuntime {

	private final Map<ServletContainer, ServiceRegistration<Servlet>> applicationServletContainerMap = new ConcurrentHashMap<>();
	private Logger logger = Logger.getLogger("o.e.o.j.HTTPWhiteboardBasedJerseyServiceRuntime");
	private Filter httpContextSelect;
	private Filter httpWhiteboardTarget;
	
	/* (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.runtime.common.AbstractJerseyServiceRuntime#doInitialize(org.osgi.service.component.ComponentContext)
	 */
	@Override
	protected void doInitialize(ComponentContext context) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider#startup()
	 */
	@Override
	public void startup() {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider#modified(org.osgi.service.component.ComponentContext)
	 */
	@Override
	public void modified(ComponentContext context) throws ConfigurationException {
		// TODO Auto-generated method stub
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsJerseyHandler#getURLs(org.osgi.service.component.ComponentContext)
	 */
	@SuppressWarnings("unchecked")
	public String[] getURLs(ComponentContext context) {
		BundleContext bundleContext = context.getBundleContext();
		
		//first look which http whiteboards would fit
		
		List<String> baseUris = new LinkedList<>();
		
		try {
			final Collection<ServiceReference<ServletContextHelper>> contextReferences = bundleContext.getServiceReferences(ServletContextHelper.class, httpContextSelect != null ? httpContextSelect.toString() : null);
			final Map<String, Filter> pathWithFilter = new HashMap<>();
			contextReferences.forEach(sr -> {
				String path = (String) sr.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH);
				String whiteboardTarget = (String) sr.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET);
				if(whiteboardTarget == null) {
					pathWithFilter.put(path, null);
				} else {
					try {
						Filter targetFilter = bundleContext.createFilter(whiteboardTarget);
						pathWithFilter.put(path, targetFilter);
					} catch (InvalidSyntaxException e) {
						logger.warning("There is a ServletContext with an invalid " + HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET + " out there with service ID "+ sr.getProperty(Constants.SERVICE_ID));
					}
				}
			});
			
			Collection<ServiceReference<HttpServiceRuntime>> whiteboardReferences = bundleContext.getServiceReferences(HttpServiceRuntime.class, httpWhiteboardTarget != null ? httpWhiteboardTarget.toString() : null);
			whiteboardReferences.forEach(ref -> {
				pathWithFilter.forEach((path, target) -> {
					if(target == null || target.match(ref)) {
						Object object = ref.getProperty(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT);
						if(object instanceof String){
							String endpoint = object.toString();
							baseUris.add(buildEndPoint(endpoint, path));
						} else if(object instanceof Collection){
							Collection<String> endpoints = (Collection<String>) object;
							for(String endpoint : endpoints){
								baseUris.add(buildEndPoint(endpoint, path));
							}
						}
					}
				});
			});
			
		} catch (InvalidSyntaxException e1) {
			// will not happen. We have already checked at this point
		}
		
		return baseUris.toArray(new String[0]);
	}
	

	private String buildEndPoint(String endpoint, String path) {
		if(!endpoint.endsWith("/")) {
			endpoint += "/";
		}
		if(path.startsWith("/")) {
			path = path.substring(1); 
		}
		return endpoint + path;
	}

	/* (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.runtime.common.AbstractJerseyServiceRuntime#doRegisterServletContainer(org.glassfish.jersey.servlet.ServletContainer, java.lang.String)
	 */
	@Override
	protected void doRegisterServletContainer(ServletContainer container, String path) {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, path);
		String target = (String) context.getProperties().get(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET);
		if(target != null){
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, target);
		}
		String select = (String) context.getProperties().get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT);
		if(select != null){
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, select);
		}
		
		//TODO: We have to register this as a prototype service, but how?O
		
		ServiceRegistration<Servlet> serviceRegistration = context.getBundleContext().registerService(Servlet.class, container, props);
		
		applicationServletContainerMap.put(container, serviceRegistration);
	}

	
	/* (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.runtime.common.AbstractJerseyServiceRuntime#doUnregisterApplication(org.glassfish.jersey.servlet.ServletContainer)
	 */
	@Override
	protected void doUnregisterApplication(ServletContainer container) {
		ServiceRegistration<Servlet> registration = null;
		synchronized (applicationServletContainerMap) {
			registration = applicationServletContainerMap.remove(container);
			
		}
		registration.unregister();
	}

	/* (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.runtime.common.AbstractJerseyServiceRuntime#doUpdateProperties(org.osgi.service.component.ComponentContext)
	 */
	@Override
	protected void doUpdateProperties(ComponentContext ctx) throws ConfigurationException {
		String contextSelect = JerseyHelper.getPropertyWithDefault(ctx, HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, null);
		try {
			httpContextSelect = contextSelect != null ? ctx.getBundleContext().createFilter(contextSelect) : null;
		} catch (InvalidSyntaxException e) {
			throw new ConfigurationException(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "Invalid filter syntax: " + e.getMessage());
		}
		String whiteboardTarget = JerseyHelper.getPropertyWithDefault(ctx, HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, null);
		try {
			httpWhiteboardTarget = whiteboardTarget != null ? ctx.getBundleContext().createFilter(whiteboardTarget) : null;
		} catch (InvalidSyntaxException e) {
			throw new ConfigurationException(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "Invalid filter syntax: " + e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.runtime.common.AbstractJerseyServiceRuntime#doTeardown()
	 */
	@Override
	protected void doTeardown() {
	}
}
