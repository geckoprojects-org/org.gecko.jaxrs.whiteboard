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
package org.gecko.rest.jersey.runtime.common;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Application;

import org.gecko.rest.jersey.binder.PrototypeServiceBinder;
import org.gecko.rest.jersey.dto.DTOConverter;
import org.gecko.rest.jersey.factories.InjectableFactory;
import org.gecko.rest.jersey.factories.JerseyExtensionInstanceFactory;
import org.gecko.rest.jersey.factories.JerseyResourceInstanceFactory;
import org.gecko.rest.jersey.helper.JerseyHelper;
import org.gecko.rest.jersey.provider.JerseyConstants;
import org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider;
import org.gecko.rest.jersey.provider.application.JaxRsExtensionProvider;
import org.gecko.rest.jersey.provider.application.JaxRsResourceProvider;
import org.gecko.rest.jersey.provider.whiteboard.JaxRsWhiteboardProvider;
import org.gecko.rest.jersey.runtime.servlet.WhiteboardServletContainer;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntimeConstants;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.BaseApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.BaseDTO;
import org.osgi.service.jaxrs.runtime.dto.ExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceMethodInfoDTO;
import org.osgi.service.jaxrs.runtime.dto.RuntimeDTO;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

/**
 * Implementation of the {@link JaxRSServiceRuntime} for a Jersey implementation
 * @author Mark Hoffmann
 * @since 12.07.2017
 */
public abstract class AbstractJerseyServiceRuntime implements JaxrsServiceRuntime, JaxRsWhiteboardProvider {

	@Deprecated 
	private volatile PrototypeServiceBinder binder;
	private volatile RuntimeDTO runtimeDTO = new RuntimeDTO();
	private volatile String name;
	protected ComponentContext context;
	// hold all resource references of the default application 
	protected final Map<String, JaxRsApplicationProvider> applicationContainerMap = new ConcurrentHashMap<>();
	
//	hold the failed apps, resources and extensions for this whiteboard
	protected final List<FailedApplicationDTO> failedApplications = new LinkedList<>();
	protected final List<FailedResourceDTO> failedResources = new LinkedList<>();
	protected final List<FailedExtensionDTO> failedExtensions = new LinkedList<>();

	private Logger logger = Logger.getLogger("o.e.o.j.serviceRuntime");
	private ServiceRegistration<JaxrsServiceRuntime> serviceRuntime;
	private AtomicLong changeCount = new AtomicLong();

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.runtime.JaxRSServiceRuntime#getRuntimeDTO()
	 */
	@Override
	public RuntimeDTO getRuntimeDTO() {
		synchronized (runtimeDTO) {
			return runtimeDTO;
		}
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
	
	/* (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.whiteboard.JaxRsWhiteboardProvider#startup()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void startup() {
		doStartup();
		Dictionary<String, Object> properties = getRuntimeProperties();
		String[] service = new String[] {JaxrsServiceRuntime.class.getName(), JaxRsWhiteboardProvider.class.getName()};
		try {
			synchronized (runtimeDTO) {
				serviceRuntime = (ServiceRegistration<JaxrsServiceRuntime>) context.getBundleContext().registerService(service, this, properties);
				updateRuntimeDTO(serviceRuntime.getReference());
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error starting JaxRsRuntimeService ", e);
			if (serviceRuntime != null) {
				serviceRuntime.unregister();
			}
		} 
	}

	/**
	 * Merges all available properties and adds a fitting changecount
	 * @return the properties that can be assigned to the changecount
	 */
	private Dictionary<String, Object> getRuntimeProperties() {
		Dictionary<String, Object> properties = new Hashtable<>();
		getProperties().entrySet().forEach(e -> properties.put(e.getKey(), e.getValue()));
		properties.put(JaxrsServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT, getURLs(context));
		properties.put(JaxrsWhiteboardConstants.JAX_RS_NAME, name);
		properties.put("service.changecount", changeCount.incrementAndGet());
		return properties;
	}
	
	/**
	 * Updates the properties and the changecount of the registered Runtime
	 */
	protected void updateRuntimeProperties() {
		if (serviceRuntime != null) {
			Dictionary<String, Object> properties = getRuntimeProperties();
			serviceRuntime.setProperties(properties);
			if (serviceRuntime.getReference() != null) {
				synchronized (runtimeDTO) {
					updateRuntimeDTO(serviceRuntime.getReference());
				}
			}
		}
	}

	/**
	 * Handles the actual implementation specific Startup
	 */
	protected abstract void doStartup();
	
	/**
	 * Handles the distinct intilization 
	 * @param context the {@link ComponentContext} to use
	 */
	protected abstract void doInitialize(ComponentContext context) ;

	/* (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.whiteboard.JaxRsWhiteboardProvider#modified(org.osgi.service.component.ComponentContext)
	 */
	@Override
	public void modified(ComponentContext context) throws ConfigurationException {
		doModified(context);
		applicationContainerMap.clear();
		updateRuntimeProperties();
	}
	
	protected abstract void doModified(ComponentContext context) throws ConfigurationException ;

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.whiteboard.JaxRsWhiteboardProvider#teardown()
	 */
	public void teardown() {
		if (serviceRuntime != null) {
			try {
				serviceRuntime.unregister();
			} catch (IllegalStateException ise) {
				logger.log(Level.SEVERE, "JaxRsRuntime was already unregistered", ise);
			} catch (Exception ise) {
				logger.log(Level.SEVERE, "Error unregsitering JaxRsRuntime", ise);
			}
		}
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
//	public synchronized void updateRuntimeDTO(ServiceReference<?> serviceRef) {
//		List<ApplicationDTO> appDTOList = new LinkedList<>();
//		List<FailedApplicationDTO> fappDTOList = new LinkedList<>();
//		applicationContainerMap.forEach((name, ap) -> {
//			BaseApplicationDTO appDTO = ap.getApplicationDTO();
//			if (appDTO instanceof ApplicationDTO) {
//				ApplicationDTO curDTO = (ApplicationDTO) appDTO;
//				if (curDTO.name.equals(".default")) {
//					runtimeDTO.defaultApplication = curDTO;
//				} else {
//					appDTOList.add(curDTO);
//				}
//			} else if (appDTO instanceof FailedApplicationDTO) {
//				fappDTOList.add((FailedApplicationDTO) appDTO);
//			}
//		});
//		if (serviceRef != null) {
//			ServiceReferenceDTO srDTO = DTOConverter.toServiceReferenceDTO(serviceRef);
//			runtimeDTO.serviceDTO = srDTO;
//			// the defaults application service id is the same, like this, because it comes
//			// from here
//			// runtimeDTO.defaultApplication.serviceId = srDTO.id;
//		}
//		runtimeDTO.applicationDTOs = appDTOList.toArray(new ApplicationDTO[appDTOList.size()]);
//		runtimeDTO.failedApplicationDTOs = fappDTOList.toArray(new FailedApplicationDTO[fappDTOList.size()]);
//
//		// TODO: handle FailedExtensionDTO and FailedResourceDTO in RuntimeDTO
//		runtimeDTO.failedExtensionDTOs = new FailedExtensionDTO[0];
//		runtimeDTO.failedResourceDTOs = new FailedResourceDTO[0];
//
//	}
	
	public synchronized void updateRuntimeDTO(ServiceReference<?> serviceRef) {
		
		synchronized (runtimeDTO) {
			List<ApplicationDTO> appDTOList = new LinkedList<>();
			
			applicationContainerMap.forEach((name, ap) -> {
				BaseApplicationDTO appDTO = ap.getApplicationDTO();
				if (appDTO instanceof ApplicationDTO) {
					ApplicationDTO curDTO = (ApplicationDTO) appDTO;
					if (curDTO.name.equals(".default") || curDTO.base.equals("/")) {
						runtimeDTO.defaultApplication = curDTO;
					} else {
						appDTOList.add(curDTO);
					}
				} 	
			});
			
			if (serviceRef != null) {
				ServiceReferenceDTO srDTO = DTOConverter.toServiceReferenceDTO(serviceRef);
				runtimeDTO.serviceDTO = srDTO;
				// the defaults application service id is the same, like this, because it comes
				// from here
				// runtimeDTO.defaultApplication.serviceId = srDTO.id;
			}
			runtimeDTO.applicationDTOs = appDTOList.toArray(new ApplicationDTO[appDTOList.size()]);		
			
//			We need to add the ResourceDTO which uses NameBinding with the corresponding Extension, for all app plus the default one		
			setExtResourceForNameBinding(runtimeDTO.applicationDTOs);
			setExtResourceForNameBinding(new ApplicationDTO[] {runtimeDTO.defaultApplication});

//			add the failed apps, resources and extensions DTOs	
			runtimeDTO.failedApplicationDTOs = failedApplications.toArray(new FailedApplicationDTO[failedApplications.size()]);
			runtimeDTO.failedExtensionDTOs = failedExtensions.toArray(new FailedExtensionDTO[failedExtensions.size()]); 
			runtimeDTO.failedResourceDTOs = failedResources.toArray(new FailedResourceDTO[failedResources.size()]); 
		}				
	}
	
	private void setExtResourceForNameBinding(ApplicationDTO[] apps) {
		for(ApplicationDTO aDTO : apps) {				
			Map<String, Set<ResourceDTO>> extResNameBind = new HashMap<>();
			for(ResourceDTO rDTO : aDTO.resourceDTOs) {
				for(ResourceMethodInfoDTO mDTO : rDTO.resourceMethods) {
					if(mDTO.nameBindings != null && mDTO.nameBindings.length > 0) {
						for(String n : mDTO.nameBindings) {
							for(ExtensionDTO extDTO : aDTO.extensionDTOs) {
								if(extDTO.nameBindings != null && extDTO.nameBindings.length > 0) {
									for(String en : extDTO.nameBindings) {
										if(n.equals(en)) {
											if(!extResNameBind.containsKey(extDTO.name)) {
												extResNameBind.put(extDTO.name, new HashSet<ResourceDTO>());
											}
											extResNameBind.get(extDTO.name).add(rDTO);
										}
									}
								}
							}
						}
					}
				}
			}
			for(ExtensionDTO extDTO : aDTO.extensionDTOs) {
				if(extResNameBind.containsKey(extDTO.name)) {
					extDTO.filteredByName = extResNameBind.get(extDTO.name).toArray(new ResourceDTO[0]);
				}
			}
		}
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
//	@Override
//	public void registerApplication(JaxRsApplicationProvider applicationProvider) {
//		if (applicationProvider == null) {
//			logger.log(Level.WARNING, "Cannot register an null application provider");
//			return;
//		}
//		if (applicationContainerMap.containsKey(applicationProvider.getId())) {
//			logger.log(Level.SEVERE, "There is already an application registered with name: " + applicationProvider.getId());
//			throw new IllegalStateException("There is already an application registered with name: " + applicationProvider.getId());
//		}
//		ResourceConfig config = createResourceConfig(applicationProvider);
//		String applicationPath = applicationProvider.getPath();
//		doRegisterServletContainer(applicationProvider, applicationPath, config);
//		applicationContainerMap.put(applicationProvider.getId(), applicationProvider);
//		
//		if(!applicationProvider.isDefault()) {
//			//registration of the default container does not warrant a servicechangecount 
//			updateRuntimeProperties();
//		}
//	}
	
	
	@Override
	public void registerApplication(JaxRsApplicationProvider applicationProvider) {
		if (applicationProvider == null) {
			logger.log(Level.WARNING, "Cannot register an null application provider");
			return;
		}
		if (applicationContainerMap.containsKey(applicationProvider.getId())) {
			logger.log(Level.SEVERE, "There is already an application registered with name: " + applicationProvider.getId());
			throw new IllegalStateException("There is already an application registered with name: " + applicationProvider.getId());
		}
//		ResourceConfig config = createResourceConfig(applicationProvider);
//		ResourceConfigWrapper config = createResourceConfig(applicationProvider);
		String applicationPath = applicationProvider.getPath();
		doRegisterServletContainer(applicationProvider, applicationPath);
		applicationContainerMap.put(applicationProvider.getId(), applicationProvider);
		
//		if(!applicationProvider.isDefault()) {
//			//registration of the default container does not warrant a servicechangecount 
//			updateRuntimeProperties();
//		}
	}

	/**
	 * Handles the distinct operation of adding the given application servlet for the given path 
	 * @param container the container servlet to add
	 * @param path to path to add it for
	 */
	protected abstract void doRegisterServletContainer(JaxRsApplicationProvider provider, String path, ResourceConfig config);
	protected abstract void doRegisterServletContainer(JaxRsApplicationProvider provider, String path);

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.whiteboard.JaxRsWhiteboardProvider#unregisterApplication(org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider)
	 */
//	@Override
//	public void unregisterApplication(JaxRsApplicationProvider applicationProvider) {
//		if (applicationProvider == null) {
//			logger.log(Level.WARNING, "Cannot unregister an null application provider");
//			return;
//		}
//		JaxRsApplicationProvider provider = null;
//		synchronized (applicationContainerMap) {
//			provider = applicationContainerMap.remove(applicationProvider.getId());
//		}
//		if (provider == null) {
//			logger.log(Level.WARNING, "There is no application registered with the name: " + applicationProvider.getName());
//			return;
//		}
//		doUnregisterApplication(provider);
//		updateRuntimeProperties();
//	}
	
	@Override
	public void unregisterApplication(JaxRsApplicationProvider applicationProvider) {
		if (applicationProvider == null) {
			logger.log(Level.WARNING, "Cannot unregister an null application provider");
			return;
		}
		JaxRsApplicationProvider provider = null;
		synchronized (applicationContainerMap) {
			provider = applicationContainerMap.remove(applicationProvider.getId()); //we are keeping track of the failed app elsewhere
		}
		if (provider == null) {
			logger.log(Level.WARNING, "There is no application registered with the name: " + applicationProvider.getName());
			return;
		}
		doUnregisterApplication(provider);
//		updateRuntimeProperties();
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
//	@Override
//	public void reloadApplication(JaxRsApplicationProvider applicationProvider) {
//		if (applicationProvider == null) {
//			logger.log(Level.WARNING, "No application provider was given to be reloaded");
//		}
//		logger.log(Level.INFO, "Reload an application provider " + applicationProvider.getName());
//		JaxRsApplicationProvider provider = applicationContainerMap.get(applicationProvider.getId());
//		if (provider == null) {
//			logger.log(Level.INFO, "No application provider was registered nothing to reload, registering instead for " + applicationProvider.getId());
//			registerApplication(applicationProvider);
//		} else {
//			List<ServletContainer> servletContainers = provider.getServletContainers();
//			if(!servletContainers.isEmpty()) {
//				logger.log(Level.FINE, "Reload servlet container for application " + applicationProvider.getName());
//
//				List<ServletContainer> copyList = new ArrayList<>(servletContainers);
//				
//				copyList.forEach(servletContainer -> {
//					try{
//						ResourceConfig config = createResourceConfig(provider);
//						servletContainer.reload(config);
//					} catch(Exception e) {
//						//We cant't check if the surrounding container is started, so we have to do it this way
//						logger.log(Level.WARNING, "Jetty servlet context handler is not started yet", e);
//					}
//				});
//			} else {
//				logger.log(Level.INFO, "-- No servlet container is available to reload " + applicationProvider.getName());
//			}
//			updateRuntimeProperties();
//		}
//	}
	
	@Override
	public void reloadApplication(JaxRsApplicationProvider applicationProvider) {
		if (applicationProvider == null) {
			logger.log(Level.WARNING, "No application provider was given to be reloaded");
		}
		logger.log(Level.INFO, "Reload an application provider " + applicationProvider.getName());
		JaxRsApplicationProvider provider = applicationContainerMap.get(applicationProvider.getId());
		if (provider == null) {
			logger.log(Level.INFO, "No application provider was registered nothing to reload, registering instead for " + applicationProvider.getId());
			registerApplication(applicationProvider);
		} else {
			applicationContainerMap.put(applicationProvider.getId(), applicationProvider);
			List<ServletContainer> servletContainers = provider.getServletContainers();
			if(!servletContainers.isEmpty()) {
				logger.log(Level.FINE, "Reload servlet container for application " + applicationProvider.getName());

				List<ServletContainer> copyList = new ArrayList<>(servletContainers);
				
				copyList.forEach(servletContainer -> {
					try{
						ResourceConfigWrapper config = createResourceConfig(provider);
						
						((WhiteboardServletContainer) servletContainer).reloadWrapper(config);
					} catch(Exception e) {
						//We cant't check if the surrounding container is started, so we have to do it this way
						logger.log(Level.WARNING, "Jetty servlet context handler is not started yet", e);
					}
				});
			} else {
				logger.log(Level.INFO, "-- No servlet container is available to reload " + applicationProvider.getName());
			}
//			updateRuntimeProperties();
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
		return applicationContainerMap.containsKey(provider.getId());
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
		if (serviceRuntime != null) {
			String[] runtimeKeys = serviceRuntime.getReference().getPropertyKeys();
			for (String k : runtimeKeys) {
				properties.put(k, serviceRuntime.getReference().getProperty(k));
			}
		}
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
	protected ResourceConfigWrapper createResourceConfig(JaxRsApplicationProvider applicationProvider) {
		if (applicationProvider == null) {
			logger.log(Level.WARNING, "Cannot create a resource configuration for null application provider");
			return null;
		}
		Application application = applicationProvider.getJaxRsApplication();
//		logger.log(Level.INFO, "Create configuration for application " + applicationProvider.getId() + " Singletons: " + application.getSingletons() + ", Classes: " + application.getClasses());
		ResourceConfigWrapper wrapper = new ResourceConfigWrapper();
		ResourceConfig config = ResourceConfig.forApplication(application);
		wrapper.config = config;
		
		PrototypeServiceBinder resBinder = new PrototypeServiceBinder();
		AtomicBoolean resRegistered = new AtomicBoolean(false);
		
		PrototypeServiceBinder extBinder = new PrototypeServiceBinder();
		AtomicBoolean extRegistered = new AtomicBoolean(false);
		
		applicationProvider.getContentProviers().stream().sorted().forEach(provider -> {					
			logger.info("Register prototype provider for classes " + provider.getObjectClass() + " in the application " + applicationProvider.getId());
			logger.info("Register prototype provider for name " + provider.getName() + " id " + provider.getId() + " rank " + provider.getServiceRank());
			if (context == null) {
				throw new IllegalStateException("Cannot create prototype factories without component context");
			}
			InjectableFactory<?> factory = null;
			if(provider instanceof JaxRsResourceProvider) {
				resRegistered.set(true);
				factory = new JerseyResourceInstanceFactory<>(provider);
				resBinder.register(provider.getObjectClass(), factory);
			}
			else if(provider instanceof JaxRsExtensionProvider) {
				extRegistered.set(true);
				factory = new JerseyExtensionInstanceFactory<>(provider);
				extBinder.register(provider.getObjectClass(), factory);
			}
			if(factory != null) {
				wrapper.factories.add(factory);
			}
		});
		if (resRegistered.get()) {
			config.register(resBinder);
		}
		if(extRegistered.get()) {
			config.register(extBinder);
		}
		return wrapper;
	}

	/**
	 * Updates the fields that are provided by service properties.
	 * @param ctx the component context
	 * @throws ConfigurationException thrown when no context is available or the expected property was not provided 
	 */
	protected void updateProperties(ComponentContext ctx) throws ConfigurationException {
		if (ctx == null) {
			throw new ConfigurationException(JaxrsServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT, "No component context is availble to get properties from");
		}
		name = JerseyHelper.getPropertyWithDefault(ctx, JaxrsWhiteboardConstants.JAX_RS_NAME, null);
		if (name == null) {
			name = JerseyHelper.getPropertyWithDefault(ctx, JerseyConstants.JERSEY_WHITEBOARD_NAME, null);
			if (name == null) {
				throw new ConfigurationException(JaxrsWhiteboardConstants.JAX_RS_NAME, "No name was defined for the whiteboard");
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
	
	
	public synchronized void updateFailedContents(Map<String, JaxRsApplicationProvider> failedAppProviders, 
			Map<String, JaxRsResourceProvider> failedResourcesProviders, 
			Map<String, JaxRsExtensionProvider> failedExtensionsProviders) {

		failedApplications.clear();
		failedResources.clear();
		failedExtensions.clear();
		
		failedAppProviders.values().stream().forEach(p-> {
			BaseApplicationDTO dto = p.getApplicationDTO();
			if(dto instanceof FailedApplicationDTO) {
				failedApplications.add((FailedApplicationDTO) dto);
			}
			else {
				throw new IllegalStateException("Failed Application Provider " + p.getName() + " does not have a FailedApplicationDTO");
			}
		});

		failedResourcesProviders.values().stream().forEach(p-> {
			BaseDTO dto = p.getResourceDTO();
			if(dto instanceof FailedResourceDTO) {
				failedResources.add((FailedResourceDTO) dto);
			}
			else {
				throw new IllegalStateException("Failed Resource Provider " + p.getName() + " does not have a FailedResourceDTO");
			}
		});

		failedExtensionsProviders.values().stream().forEach(p-> {
			BaseDTO dto = p.getExtensionDTO();
			if(dto instanceof FailedExtensionDTO) {
				failedExtensions.add((FailedExtensionDTO) dto);
			}
			else {
				throw new IllegalStateException("Failed Extension Provider " + p.getName() + " does not have a FailedExtensionDTO");
			}
		});

		updateRuntimeProperties();
	}
}
