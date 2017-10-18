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
package org.eclipselabs.jaxrs.jersey.runtime.common;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Application;

import org.eclipselabs.jaxrs.jersey.binder.PrototypeServiceBinder;
import org.eclipselabs.jaxrs.jersey.dto.DTOConverter;
import org.eclipselabs.jaxrs.jersey.factories.JerseyResourceInstanceFactory;
import org.eclipselabs.jaxrs.jersey.helper.JaxRsHelper;
import org.eclipselabs.jaxrs.jersey.helper.JerseyHelper;
import org.eclipselabs.jaxrs.jersey.provider.JerseyConstants;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider;
import org.eclipselabs.jaxrs.jersey.provider.osgi.PrototypeResourceProvider;
import org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntime;
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntimeConstants;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.RuntimeDTO;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * Implementation of the {@link JaxRSServiceRuntime} for a Jersey implementation
 * @author Mark Hoffmann
 * @since 12.07.2017
 */
public abstract class AbstractJerseyServiceRuntime implements JaxRSServiceRuntime, JaxRsWhiteboardProvider {

	private volatile PrototypeServiceBinder binder;
	private volatile RuntimeDTO runtimeDTO = new RuntimeDTO();
	private volatile String name;
	protected ComponentContext context;
	// hold all resource references of the default application 
	private final Map<String, JaxRsApplicationProvider> applicationContainerMap = new ConcurrentHashMap<>();
	private Logger logger = Logger.getLogger("o.e.o.j.serviceRuntime");

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.runtime.JaxRSServiceRuntime#getRuntimeDTO()
	 */
	@Override
	public RuntimeDTO getRuntimeDTO() {
		return runtimeDTO;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider#initialize(org.osgi.service.component.ComponentContext)
	 */
	@Override
	public void initialize(ComponentContext context) throws ConfigurationException {
		this.context = context;
		updateProperties(context);
		if (binder != null) {
			binder.dispose();
		}
		binder = new PrototypeServiceBinder();
		doInitialize(context);
	}

	/**
	 * Handles the distinct intilization 
	 * @param context the {@link ComponentContext} to use
	 */
	protected abstract void doInitialize(ComponentContext context) ;

		
	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider#teardown()
	 */
	public void teardown() {
		binder.dispose();
		doTeardown();
	}

	/**
	 * Handles the distinct teardown event
	 */
	protected abstract void doTeardown();
	
	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider#updateRuntimeDTO(org.osgi.framework.ServiceReference)
	 */
	public synchronized void updateRuntimeDTO(ServiceReference<?> serviceRef) {
		List<ApplicationDTO> appDTOList = new LinkedList<>();
		applicationContainerMap.forEach((name, ap)->{
			ApplicationDTO appDTO = ap.getApplicationDTO();
			if (name.equals(".default")) {
				runtimeDTO.defaultApplication = appDTO;
			} else {
				appDTOList.add(appDTO);
			}
		});
		if (serviceRef != null) {
			ServiceReferenceDTO srDTO = DTOConverter.toServiceReferenceDTO(serviceRef);
			runtimeDTO.serviceDTO = srDTO;
			// the defaults application service id is the same, like this, because it comes from here
			//			runtimeDTO.defaultApplication.serviceId = srDTO.id;
		}
		runtimeDTO.applicationDTOs = appDTOList.toArray(new ApplicationDTO[appDTOList.size()]);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider#updateRuntimeDTO(org.osgi.service.jaxrs.runtime.dto.RuntimeDTO)
	 */
	@Override
	public void updateRuntimeDTO(RuntimeDTO runtimeDTO) {

	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider#registerApplication(org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider)
	 */
	@Override
	public void registerApplication(JaxRsApplicationProvider applicationProvider) {
		if (applicationProvider == null) {
			logger.log(Level.WARNING, "Cannot register an null application provider");
			return;
		}
		if (applicationContainerMap.containsKey(applicationProvider.getName())) {
			logger.log(Level.SEVERE, "There is already an application registered with name: " + applicationProvider.getName());
			throw new IllegalStateException("There is already an application registered with name: " + applicationProvider.getName());
		}
		Application application = applicationProvider.getJaxRsApplication();
		ResourceConfig config = createResourceConfig(application);
		ServletContainer container = new ServletContainer(config);
		applicationProvider.setServletContainer(container);
		String applicationPath = applicationProvider.isDefault() ? JaxRsHelper.getServletPath(applicationProvider.getJaxRsApplication()) : JaxRsHelper.toServletPath(applicationProvider.getPath());
		doRegisterServletContainer(container, applicationPath);
		
		applicationContainerMap.put(applicationProvider.getName(), applicationProvider);
	}

	/**
	 * Handles the distinct operation of adding the given application servlet for the given path 
	 * @param container the container servlet to add
	 * @param path to path to add it for
	 */
	protected abstract void doRegisterServletContainer(ServletContainer container, String path);

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider#unregisterApplication(org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider)
	 */
	@Override
	public void unregisterApplication(JaxRsApplicationProvider applicationProvider) {
		if (applicationProvider == null) {
			logger.log(Level.WARNING, "Cannot unregister an null application provider");
			return;
		}
		JaxRsApplicationProvider provider = null;
		synchronized (applicationContainerMap) {
			provider = applicationContainerMap.remove(applicationProvider.getName());
		}
		if (provider == null) {
			logger.log(Level.WARNING, "There is no application registered with the name: " + applicationProvider.getName());
			return;
		}
		ServletContainer container = provider.getServletContainer();
		doUnregisterApplication(container);
	}

	/**
	 * Handles the destinct unregistration of the servlet
	 * @param container the Jersey Servlet to unregister
	 */
	protected abstract void doUnregisterApplication(ServletContainer container);

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider#reloadApplication(org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider)
	 */
	@Override
	public void reloadApplication(JaxRsApplicationProvider applicationProvider) {
		if (applicationProvider == null) {
			logger.log(Level.WARNING, "No application provider was given to be reloaded");
		}
		synchronized (applicationContainerMap) {
			JaxRsApplicationProvider provider = applicationContainerMap.get(applicationProvider.getName());
			if (provider == null) {
				logger.log(Level.WARNING, "No application provider was registered nothing to reload, registering instead");
				registerApplication(applicationProvider);
			} else {
				ServletContainer servletContainer = provider.getServletContainer();
				if (servletContainer != null) {
					ResourceConfig config = createResourceConfig(provider.getJaxRsApplication());
//					if (contextHandler != null && contextHandler.isStarted()) {
						servletContainer.reload(config);
//					} else {
//						logger.log(Level.WARNING, "Jetty servlet context handler is not started yet");
//					}
				}
			}
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider#isRegistered(org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider)
	 */
	@Override
	public boolean isRegistered(JaxRsApplicationProvider provider) {
		if (provider == null) {
			return false;
		}
		return applicationContainerMap.containsKey(provider.getName());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider#getProperties()
	 */
	@Override
	public Map<String, Object> getProperties() {
		Map<String, Object> properties = new HashMap<>();
		Enumeration<String> keys = context.getProperties().keys();
		while(keys.hasMoreElements()) {
			String key = keys.nextElement();
			Object value = context.getProperties().get(keys.nextElement());
			properties.put(key, value);
		}
		return properties;
	}

	/**
	 * Creates a new {@link ResourceConfig} for a given application. this method takes care of registering
	 * Jersey factories for prototype scoped resource services and singletons separately
	 * @param application the JaxRs application
	 */
	private ResourceConfig createResourceConfig(Application application) {
		if (application == null) {
			logger.log(Level.WARNING, "Cannot create a resource configuration for null application");
			return null;
		}
		ResourceConfig config = ResourceConfig.forApplication(application);
		// prepare factory creation to forward prototype functionality to Jersey
		if (application instanceof PrototypeResourceProvider) {
			System.out.println("Register a prototype provider like application");
			if (context == null) {
				throw new IllegalStateException("Cannot create prototype factories without component context");
			}
			PrototypeResourceProvider prp = (PrototypeResourceProvider) application;
			BundleContext bctx = context.getBundleContext();
			Set<Class<?>> classes = prp.getPrototypeResourceClasses();
			classes.forEach((c)->{
				Factory<?> factory = new JerseyResourceInstanceFactory<>(bctx, c);
				binder.register(c, factory);
			});
			config.register(binder);
		}
		return config;
	}

	/**
	 * Updates the fields that are provided by service properties.
	 * @param ctx the component context
	 * @throws ConfigurationException thrown when no context is available or the expected property was not provided 
	 */
	protected void updateProperties(ComponentContext ctx) throws ConfigurationException {
		if (ctx == null) {
			throw new ConfigurationException(JaxRSServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT, "No component context is availble to get properties from");
		}
		name = JerseyHelper.getPropertyWithDefault(ctx, JaxRSWhiteboardConstants.JAX_RS_NAME, null);
		if (name == null) {
			name = JerseyHelper.getPropertyWithDefault(ctx, JerseyConstants.JERSEY_WHITEBOARD_NAME, null);
			if (name == null) {
				throw new ConfigurationException(JaxRSWhiteboardConstants.JAX_RS_NAME, "No name was defined for the whiteboard");
			}
		}
		doUpdateProperties(ctx);
	}

	/**
	 * Handles the distinct update properties event
	 * @param ctx the {@link ComponentContext} to use
	 * @throws ConfigurationException 
	 */
	protected abstract void doUpdateProperties(ComponentContext ctx) throws ConfigurationException;
}
