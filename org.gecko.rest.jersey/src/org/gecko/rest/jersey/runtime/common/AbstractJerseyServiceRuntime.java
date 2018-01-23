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
package org.gecko.rest.jersey.runtime.common;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Application;

import org.gecko.rest.jersey.binder.PrototypeServiceBinder;
import org.gecko.rest.jersey.dto.DTOConverter;
import org.gecko.rest.jersey.factories.JerseyResourceInstanceFactory;
import org.gecko.rest.jersey.helper.JerseyHelper;
import org.gecko.rest.jersey.provider.JerseyConstants;
import org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider;
import org.gecko.rest.jersey.provider.application.JaxRsResourceProvider;
import org.gecko.rest.jersey.provider.whiteboard.JaxRsWhiteboardProvider;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntime;
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntimeConstants;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.BaseApplicationDTO;
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
	protected final Map<String, JaxRsApplicationProvider> applicationContainerMap = new ConcurrentHashMap<>();
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
	 * @see org.gecko.rest.jersey.provider.whiteboard.JaxRsWhiteboardProvider#initialize(org.osgi.service.component.ComponentContext)
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
	 * @see org.gecko.rest.jersey.provider.whiteboard.JaxRsWhiteboardProvider#teardown()
	 */
	public void teardown() {
		doTeardown();
		if (binder != null) {
			binder.dispose();
		}
	}

	/**
	 * Handles the distinct teardown event
	 */
	protected abstract void doTeardown();

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.whiteboard.JaxRsWhiteboardProvider#updateRuntimeDTO(org.osgi.framework.ServiceReference)
	 */
	public synchronized void updateRuntimeDTO(ServiceReference<?> serviceRef) {
		List<ApplicationDTO> appDTOList = new LinkedList<>();
		applicationContainerMap.forEach((name, ap)->{
			BaseApplicationDTO appDTO = ap.getApplicationDTO();
			if(appDTO instanceof ApplicationDTO) {
				ApplicationDTO curDTO = (ApplicationDTO) appDTO;
				if (name.equals(".default")) {
					runtimeDTO.defaultApplication = curDTO;
				} else {
					appDTOList.add(curDTO);
				}
			} else {
				//TODO: What about the failed DTOs
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
	 * @see org.gecko.rest.jersey.provider.whiteboard.JaxRsWhiteboardProvider#updateRuntimeDTO(org.osgi.service.jaxrs.runtime.dto.RuntimeDTO)
	 */
	@Override
	public void updateRuntimeDTO(RuntimeDTO runtimeDTO) {

	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.whiteboard.JaxRsWhiteboardProvider#registerApplication(org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider)
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
		ResourceConfig config = createResourceConfig(applicationProvider);
		String applicationPath = applicationProvider.getPath();
		doRegisterServletContainer(applicationProvider, applicationPath, config);
		applicationContainerMap.put(applicationProvider.getName(), applicationProvider);
	}

	/**
	 * Handles the distinct operation of adding the given application servlet for the given path 
	 * @param container the container servlet to add
	 * @param path to path to add it for
	 */
	protected abstract void doRegisterServletContainer(JaxRsApplicationProvider provider, String path, ResourceConfig config);

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.whiteboard.JaxRsWhiteboardProvider#unregisterApplication(org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider)
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
		doUnregisterApplication(provider);
	}

	/**
	 * Handles the destinct unregistration of the servlets
	 * @param applicationProvider {@link JaxRsApplicationProvider} to unregister
	 */
	protected abstract void doUnregisterApplication(JaxRsApplicationProvider applicationProvider);

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.whiteboard.JaxRsWhiteboardProvider#reloadApplication(org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider)
	 */
	@Override
	public void reloadApplication(JaxRsApplicationProvider applicationProvider) {
		if (applicationProvider == null) {
			logger.log(Level.WARNING, "No application provider was given to be reloaded");
		}
		logger.log(Level.INFO, "Reload an application provider " + applicationProvider.getName());
		JaxRsApplicationProvider provider = applicationContainerMap.get(applicationProvider.getName());
		if (provider == null) {
			logger.log(Level.WARNING, "No application provider was registered nothing to reload, registering instead");
			registerApplication(applicationProvider);
		} else {
			List<ServletContainer> servletContainers = provider.getServletContainers();
			if(!servletContainers.isEmpty()) {
				logger.log(Level.INFO, "Reload servlet container " + applicationProvider.getName());
				servletContainers.forEach(servletContainer -> {
					try{
						ResourceConfig config = createResourceConfig(provider);
						servletContainer.reload(config);
					} catch(Exception e) {
						//We cant't check if the surrounding container is started, so we have to do it this way
						logger.log(Level.WARNING, "Jetty servlet context handler is not started yet", e);
					}
				});
			} else {
				logger.log(Level.INFO, "No servlet container is available to reload " + applicationProvider.getName());
			}
		}
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.whiteboard.JaxRsWhiteboardProvider#isRegistered(org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider)
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
	 * @see org.gecko.rest.jersey.provider.whiteboard.JaxRsWhiteboardProvider#getName()
	 */
	@Override
	public String getName() {
		return name;
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.whiteboard.JaxRsWhiteboardProvider#getProperties()
	 */
	@Override
	public Map<String, Object> getProperties() {
		Map<String, Object> properties = new HashMap<>();
		Enumeration<String> keys = context.getProperties().keys();
		while(keys.hasMoreElements()) {
			String key = keys.nextElement();
			Object value = context.getProperties().get(key);
			properties.put(key, value);
		}
		return properties;
	}

	/**
	 * Creates a new {@link ResourceConfig} for a given application. this method takes care of registering
	 * Jersey factories for prototype scoped resource services and singletons separately
	 * @param applicationProvider the JaxRs application application provider
	 */
	protected ResourceConfig createResourceConfig(JaxRsApplicationProvider applicationProvider) {
		if (applicationProvider == null) {
			logger.log(Level.WARNING, "Cannot create a resource configuration for null application provider");
			return null;
		}
		Application application = applicationProvider.getJaxRsApplication();
		ResourceConfig config = ResourceConfig.forApplication(application);
		
		PrototypeServiceBinder binder = new PrototypeServiceBinder();
		applicationProvider.getContentProviers().forEach(provider -> {
			logger.info("Register prototype provider for classes " + provider.getObjectClass() + " in the application " + applicationProvider.getName());
			if (context == null) {
				throw new IllegalStateException("Cannot create prototype factories without component context");
			}
			if(provider instanceof JaxRsResourceProvider) {
				Factory<?> factory = new JerseyResourceInstanceFactory<>(provider);
				logger.info("registering for real " + provider.getObjectClass());
				binder.register(provider.getObjectClass(), factory);
			}
		});
		config.register(binder);
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
