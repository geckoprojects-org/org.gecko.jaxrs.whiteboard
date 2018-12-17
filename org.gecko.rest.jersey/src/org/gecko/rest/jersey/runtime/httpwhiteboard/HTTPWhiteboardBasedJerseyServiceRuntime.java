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
package org.gecko.rest.jersey.runtime.httpwhiteboard;

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

import org.gecko.rest.jersey.helper.JerseyHelper;
import org.gecko.rest.jersey.provider.JerseyConstants;
import org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider;
import org.gecko.rest.jersey.runtime.common.AbstractJerseyServiceRuntime;
import org.gecko.rest.jersey.runtime.servlet.WhiteboardServletContainer;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * Implementation of the {@link JaxRSServiceRuntime} for a Jersey implementation
 * @author Mark Hoffmann
 * @since 12.07.2017
 */
//@Capability(namespace = ImplementationNamespace.IMPLEMENTATION_NAMESPACE, 
//version = JaxrsWhiteboardConstants.JAX_RS_WHITEBOARD_SPECIFICATION_VERSION, 
//name = JaxrsWhiteboardConstants.JAX_RS_WHITEBOARD_IMPLEMENTATION, 
//attribute= { 
//		"uses:=\"javax.ws.rs,javax.ws.rs.sse,javax.ws.rs.core,javax.ws.rs.ext,javax.ws.rs.client,javax.ws.rs.container,org.osgi.service.jaxrs.whiteboard\"",
//		"provider=jersey", 
//		"http.whiteboard=true"
//})
public class HTTPWhiteboardBasedJerseyServiceRuntime extends AbstractJerseyServiceRuntime {

	private final Map<String, ServiceRegistration<Servlet>> applicationServletRegistrationMap = new ConcurrentHashMap<>();
	private Logger logger = Logger.getLogger("o.e.o.j.HTTPWhiteboardBasedJerseyServiceRuntime");
	private Filter httpContextSelect;
	private Filter httpWhiteboardTarget;
	private String basePath;
	
	/* (non-Javadoc)
	 * @see org.gecko.rest.jersey.runtime.common.AbstractJerseyServiceRuntime#doInitialize(org.osgi.service.component.ComponentContext)
	 */
	@Override
	protected void doInitialize(ComponentContext context) {
	}
	
	/* (non-Javadoc)
	 * @see org.gecko.rest.jersey.runtime.common.AbstractJerseyServiceRuntime#doStartup()
	 */
	@Override
	protected void doStartup() {
		//Nothing todo here
	}
	
	/* (non-Javadoc)
	 * @see org.gecko.rest.jersey.runtime.common.AbstractJerseyServiceRuntime#doModified(org.osgi.service.component.ComponentContext)
	 */
	@Override
	public void doModified(ComponentContext context) throws ConfigurationException {
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
		return endpoint + basePath + path;
	}
	
	/* (non-Javadoc)
	 * @see org.gecko.rest.jersey.runtime.common.AbstractJerseyServiceRuntime#doRegisterServletContainer(org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider, java.lang.String, org.glassfish.jersey.server.ResourceConfig)
	 */
	@Override
	protected void doRegisterServletContainer(JaxRsApplicationProvider provider, String path, ResourceConfig config) {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, basePath + path);
		String target = (String) context.getProperties().get(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET);
		if(target != null){
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, target);
		}
		String select = (String) context.getProperties().get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT);
		if(select != null){
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, select);
		}
		
		ServiceRegistration<Servlet> serviceRegistration = context.getBundleContext().registerService(Servlet.class, new PrototypeServiceFactory<Servlet>() {

			@Override
			public Servlet getService(Bundle bundle, ServiceRegistration<Servlet> registration) {
				ResourceConfig config = createResourceConfig(provider);
				ServletContainer container = new WhiteboardServletContainer(config, provider);
				provider.addServletContainer(container);
				return container;
			}

			@Override
			public void ungetService(Bundle bundle, ServiceRegistration<Servlet> registration, Servlet service) {
				provider.removeServletContainer((ServletContainer) service);
			}
			
		}, props);
		
		applicationServletRegistrationMap.put(provider.getId(), serviceRegistration);
	}
	
	@Override
	protected void doUnregisterApplication(JaxRsApplicationProvider applicationProvider) {
		ServiceRegistration<Servlet> serviceRegistration = applicationServletRegistrationMap.remove(applicationProvider.getId());
		if(serviceRegistration != null) {
			serviceRegistration.unregister();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.gecko.rest.jersey.runtime.common.AbstractJerseyServiceRuntime#doUpdateProperties(org.osgi.service.component.ComponentContext)
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
		
		basePath = JerseyHelper.getPropertyWithDefault(ctx, JerseyConstants.JERSEY_CONTEXT_PATH, "");
		if(basePath.length() > 0) {
			if(!basePath.startsWith("/")) {
				basePath = "/" + basePath;
			}
			if(basePath.endsWith("/")) {
				basePath = basePath.substring(0, basePath.length() - 1);
			} else if(basePath.endsWith("/*")) {
				basePath = basePath.substring(0, basePath.length() - 2);
			}
		}
		
	}

	/* (non-Javadoc)
	 * @see org.gecko.rest.jersey.runtime.common.AbstractJerseyServiceRuntime#doTeardown()
	 */
	@Override
	protected void doTeardown() {
	}

}
