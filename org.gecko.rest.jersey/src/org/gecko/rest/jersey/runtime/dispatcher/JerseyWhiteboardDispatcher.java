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
package org.gecko.rest.jersey.runtime.dispatcher;

import static java.util.function.Predicate.not;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.core.Application;

import org.gecko.rest.jersey.helper.DispatcherHelper;
import org.gecko.rest.jersey.provider.JaxRsConstants;
import org.gecko.rest.jersey.provider.application.JaxRsApplicationContentProvider;
import org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider;
import org.gecko.rest.jersey.provider.application.JaxRsExtensionProvider;
import org.gecko.rest.jersey.provider.application.JaxRsProvider;
import org.gecko.rest.jersey.provider.application.JaxRsResourceProvider;
import org.gecko.rest.jersey.provider.application.JaxRsWhiteboardDispatcher;
import org.gecko.rest.jersey.provider.whiteboard.JaxRsWhiteboardProvider;
import org.gecko.rest.jersey.runtime.application.JerseyApplicationProvider;
import org.gecko.rest.jersey.runtime.application.JerseyExtensionProvider;
import org.gecko.rest.jersey.runtime.application.JerseyResourceProvider;
import org.gecko.rest.jersey.runtime.common.AbstractJerseyServiceRuntime;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceObjects;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.runtime.dto.FailedExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedResourceDTO;

/**
 * Implementation of the dispatcher.
 * It has the following tasks:
 * - It assigns resources and extensions to applications
 * - Unassignable resources and extension are defaulting to the default application
 * Applications:
 * - There are new applications, that need to be registered, if they are not empty
 * - There are existing applications that need to be updated on change, if they are not a legacy application 
 * - There are existing applications that needs to be unregistered, because they are removed
 * - There are existing applications that needs to be unregistered, because they don't, match to the whiteboard anymore
 * - There are existing applications that needs to be unregistered, because they are empty
 * Resources and Extensions
 * - There are resources that come and fit to an application, that fits to the whiteboard
 * - There are resources that come and fit to an application, that don't fit to the whiteboard, so they defaulting to the default application
 * - There are resources that come and don't fit to an application, so they defaulting to the default application
 * - There are resources that come and don't fit to an application but the whiteboard, these defaulting to the default application
 * - There are resources that come and fit to an application but dont't fit to an whiteboard these are failed resources 
 * - There are existing resources that don't fit to an application anymore. They have to be removed from the old application and assigned 
 * to the new one or the default application, if nothing matches.
 * - There are existing resources that fit to an application which don't fit to the whiteboard anymore. They have to be removed from the old application and assigned 
 * to the default application.
 * - There are existing resource that fit to an application, but the whiteoard target 
 * - There are resources that are removed
 * @author Mark Hoffmann
 * @since 12.10.2017
 */
public class JerseyWhiteboardDispatcher implements JaxRsWhiteboardDispatcher {

	private static final Logger logger = Logger.getLogger("jersey.dispatcher");
	private JaxRsWhiteboardProvider whiteboard;
	private volatile Map<String, JaxRsApplicationProvider> applicationProviderCache = new ConcurrentHashMap<>();
	private volatile Map<String, JaxRsResourceProvider> resourceProviderCache = new ConcurrentHashMap<>();
	private volatile Map<String, JaxRsExtensionProvider> extensionProviderCache = new ConcurrentHashMap<>();
	private volatile Set<JaxRsApplicationProvider> removedApplications = new HashSet<>();
	
//	Maps to keep track of failing services to update the runtime DTO at the end of the doDispatch
	private volatile Map<String, JaxRsApplicationProvider> failedApplications = new ConcurrentHashMap<>();
	private volatile Map<String, JaxRsResourceProvider> failedResources = new ConcurrentHashMap<>();
	private volatile Map<String, JaxRsExtensionProvider> failedExtensions = new ConcurrentHashMap<>();	
	
	private volatile Set<JaxRsResourceProvider> removedResources = new HashSet<>();
	private volatile Set<JaxRsExtensionProvider> removedExtensions = new HashSet<>();
	private volatile boolean dispatching = false;
	// The implicit default application
	private volatile JaxRsApplicationProvider defaultProvider;
	// The shadowed default application with name '.default'
	private volatile JaxRsApplicationProvider currentDefaultProvider;
	private ReentrantLock lock = new ReentrantLock();
	private AtomicBoolean lockedChange = new AtomicBoolean();
	private boolean batchMode = false;


	public JerseyWhiteboardDispatcher() {
	}

	/* (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsWhiteboardDispatcher#setBatchMode(boolean)
	 */
	public void setBatchMode(boolean batchMode) {
		this.batchMode = batchMode;
	}
	
	/* (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsWhiteboardDispatcher#batchDispatch()
	 */
	public void batchDispatch() {
		if (isDispatching() && batchMode) {
			doDispatch();
		}
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsWhiteboardDispatcher#setWhiteboardProvider(org.gecko.rest.jersey.provider.whiteboard.JaxRsWhiteboardProvider)
	 */
	@Override
	public void setWhiteboardProvider(JaxRsWhiteboardProvider whiteboard) {
		if (isDispatching()) {
			throw new IllegalStateException("Error setting whiteboard provider, when dispatching is active");
		}
		this.whiteboard = whiteboard;
		if(currentDefaultProvider != null) {
			whiteboard.registerApplication(currentDefaultProvider);
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsWhiteboardDispatcher#getWhiteboardProvider()
	 */
	@Override
	public JaxRsWhiteboardProvider getWhiteboardProvider() {
		return whiteboard;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsWhiteboardDispatcher#getApplications()
	 */
	@Override
	public Set<JaxRsApplicationProvider> getApplications() {
		return Collections.unmodifiableSet(new HashSet<>(applicationProviderCache.values()));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsWhiteboardDispatcher#getResources()
	 */
	@Override
	public Set<JaxRsResourceProvider> getResources() {
		return Collections.unmodifiableSet(new HashSet<>(resourceProviderCache.values()));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsWhiteboardDispatcher#getExtensions()
	 */
	@Override
	public Set<JaxRsExtensionProvider> getExtensions() {
		return Collections.unmodifiableSet(new HashSet<>(extensionProviderCache.values()));
	}

	
	@Override
	public void addApplication(Application application, Map<String, Object> properties) {
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(application, properties);
		/*
		 * Section 151.6.1 The default application can be replaced, with another one using another base path
		 */
		if(provider.isDefault()) {
			defaultProvider = provider;
			currentDefaultProvider = provider;
			if(whiteboard != null) {
				whiteboard.registerApplication(currentDefaultProvider);
				currentDefaultProvider.markUnchanged();
				checkDispatch();
			}
			return;
		} 
		String key = provider.getId();
		if (!applicationProviderCache.containsKey(key)) {
			logger.info("Adding Application with id " + provider.getName());
			applicationProviderCache.put(key, provider);
			checkDispatch();
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsWhiteboardDispatcher#removeApplication(javax.ws.rs.core.Application, java.util.Map)
	 */
	@Override
	public void removeApplication(Application application, Map<String, Object> properties) {
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(null, properties);
		String key = provider.getId();
		JaxRsApplicationProvider removed = applicationProviderCache.remove(key);
		logger.fine("Removing Application with name " + provider.getName());
		if (removed != null) {
			logger.info("Removed Application with name " + provider.getName());
			removedApplications.add(removed);
			checkDispatch();
		} 
	}

	/* (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsWhiteboardDispatcher#addResource(org.osgi.framework.ServiceReference)
	 */
	@Override
	public void addResource(ServiceObjects<?> serviceObject, Map<String, Object> properties) {
		JaxRsResourceProvider provider = new JerseyResourceProvider<>(serviceObject, properties);		
		String key = provider.getId();
		if(serviceObject == null) {
			logger.log(Level.WARNING, "Dispatcher cannot add resource with id: " + key + "!");
			return;
		}
		if(provider.getResourceDTO() instanceof FailedResourceDTO) {
			if(!failedResources.containsKey(provider.getId())) {
				failedResources.put(provider.getId(), provider);
			}			
			if (isDispatching() && !batchMode) {
				doDispatch();
			}
			else {
				if(whiteboard instanceof AbstractJerseyServiceRuntime) {
					AbstractJerseyServiceRuntime ajsr = (AbstractJerseyServiceRuntime) whiteboard;
					ajsr.updateFailedContents(failedApplications, failedResources, failedExtensions);
					reset(failedApplications, failedResources, failedExtensions);
				}
			}
		}
		else if (!resourceProviderCache.containsKey(key)) {
			logger.info("Added resource " + key + " name: " + provider.getName());
			resourceProviderCache.put(key, provider);
			checkDispatch();
		}
		else {
//			This is the case in which the resource service properties have been modified
			resourceProviderCache.put(key, provider);
			checkDispatch();
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsWhiteboardDispatcher#removeResource(java.lang.Object, java.util.Map)
	 */
	@Override
	public void removeResource(Map<String, Object> properties) {
		JaxRsResourceProvider provider = new JerseyResourceProvider<Object>(null, properties);
		String key = provider.getId();
		JaxRsResourceProvider removed = resourceProviderCache.remove(key);
		if (removed != null) {
			removedResources.add(removed);
			checkDispatch();
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsWhiteboardDispatcher#addExtension(java.lang.Object, java.util.Map)
	 */
	@Override
	public void addExtension(ServiceObjects<?> serviceObject, Map<String, Object> properties) {
		JaxRsExtensionProvider provider = new JerseyExtensionProvider<>(serviceObject, properties);
		String key = provider.getId();
		if(provider.getExtensionDTO() instanceof FailedExtensionDTO) {
			if(!failedExtensions.containsKey(provider.getId())) {
				failedExtensions.put(provider.getId(), provider);
			}			
			if (isDispatching() && !batchMode) {
				doDispatch();
			}
			else {
				if(whiteboard instanceof AbstractJerseyServiceRuntime) {
					AbstractJerseyServiceRuntime ajsr = (AbstractJerseyServiceRuntime) whiteboard;
					ajsr.updateFailedContents(failedApplications, failedResources, failedExtensions);
					reset(failedApplications, failedResources, failedExtensions);
				}
			}
		}
		else if (!extensionProviderCache.containsKey(key)) {
			logger.info("Added extension " + key + " name: " + provider.getName());
			extensionProviderCache.put(key, provider);
			checkDispatch();
		}
		else {
//			This is the case in which the extension service properties have been modified
			extensionProviderCache.put(key, provider);
			checkDispatch();
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsWhiteboardDispatcher#removeExtension(java.lang.Object, java.util.Map)
	 */
	@Override
	public void removeExtension(Map<String, Object> properties) {
		JaxRsExtensionProvider provider = new JerseyExtensionProvider<Object>(null, properties);
		String key = provider.getId();
		JaxRsExtensionProvider removed = extensionProviderCache.remove(key);
		logger.fine("Remove extension " + key + " name: " + provider.getName());
		if (removed != null) {
			logger.info("Removed extension " + key + " name: " + provider.getName());
			removedExtensions.add(removed);
			checkDispatch();
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsWhiteboardDispatcher#dispatch()
	 */
	@Override
	public void dispatch() {
		// add default application
		if (whiteboard == null) {
			throw new IllegalStateException("Dispatcher cannot be used without a whiteboard provider");
		}
		dispatching = true;
		doDispatch();
	}



	@Override
	public void deactivate() {
		if (!isDispatching()) {
			return;
		}
		try {
			lock.tryLock(5, TimeUnit.SECONDS);
			dispatching = false;
			whiteboard.unregisterApplication(currentDefaultProvider);
			applicationProviderCache.values().forEach((app)->{
				if (whiteboard.isRegistered(app)) {
					whiteboard.unregisterApplication(app);
				}
			});
			currentDefaultProvider = null;
			defaultProvider = null;
			applicationProviderCache.clear();
			resourceProviderCache.clear();
			extensionProviderCache.clear();
		} catch (InterruptedException e) {
			logger.log(Level.SEVERE, "Interrupted deactivate call of the dispatcher", e);
		} finally {
			lock.unlock();
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsWhiteboardDispatcher#isDispatching()
	 */
	@Override
	public boolean isDispatching() {
		return dispatching;
	}

	/**
	 * Checks the execution of doDispatch, in case it is active
	 */
	private void checkDispatch() {
		if (isDispatching() && !batchMode) {
			doDispatch();
		}
	}
	
	/**
	 * Does the dispatching work. We lock the dispatch work. If we dont get a lock, because there is currently work in progress,
	 * we mark lockedChange so, that we know that there, is still work to do.
	 */
	private void doDispatch() {
		if (lock.tryLock()) {			
			Collection<JaxRsApplicationProvider> applications = Collections.unmodifiableCollection(applicationProviderCache.values());
			Collection<JaxRsResourceProvider> resources = Collections.unmodifiableCollection(resourceProviderCache.values());
			Collection<JaxRsExtensionProvider> extensions = Collections.unmodifiableCollection(extensionProviderCache.values());
			Collection<JaxRsApplicationProvider> remApplications = getRemovedList(removedApplications);
			Collection<JaxRsResourceProvider> remResources = getRemovedList(removedResources);
			Collection<JaxRsExtensionProvider> remExtensions = getRemovedList(removedExtensions);
			try {				
				/*
				 * Unregister all applications that are declared as deleted.
				 * Further remove all resources and extension from applications that are declared as deleted
				 */
				remApplications.forEach((remApp)->{
					if(whiteboard.isRegistered(remApp)) {
						/*
						 * 151.5.5 Whiteboard extension services must be released by the JAX-RS whiteboard when the application 
						 * with which they have been registered is removed from the whiteboard, even if this is only a 
						 * temporary situation. 
						 */
						for(JaxRsApplicationContentProvider c : remApp.getContentProviers()) {
							if(c instanceof JaxRsExtensionProvider) {								
								if(extensionProviderCache.containsKey(c.getId())) {
									removedExtensions.add((JaxRsExtensionProvider)c);
								}
							}
							if(c instanceof JaxRsResourceProvider && c.isSingleton()) {								
								if(resourceProviderCache.containsKey(c.getId())) {
									removedResources.add((JaxRsResourceProvider)c);
								}
							}
						}
						
						unassignContent(Collections.singleton(remApp), remApp.getContentProviers());						
						whiteboard.unregisterApplication(remApp);
					}
				});
				unassignContent(applications, remResources);
				unassignContent(applications, remExtensions);
				/*
				 * Determine all applications, resources and extension that fit to the whiteboard.
				 * We only work with those, because all these are possible candidates for the whiteboard
				 */
				List<JaxRsApplicationProvider> applicationCandidates = applications.stream().
						filter((app)->app.canHandleWhiteboard(getWhiteboardProvider().getProperties())).
						collect(Collectors.toList());
				
				Map<Boolean, List<JaxRsResourceProvider>> resourceCandidatesMap = resources.stream().collect(Collectors
						.partitioningBy((r) -> r.canHandleWhiteboard(getWhiteboardProvider().getProperties()),Collectors.toUnmodifiableList()));

				Map<Boolean, List<JaxRsExtensionProvider>> extensionCandidatesMap = extensions.stream().collect(Collectors
						.partitioningBy((e) -> e.canHandleWhiteboard(getWhiteboardProvider().getProperties()),Collectors.toUnmodifiableList()));
				
				
				
				unassignContent(applicationCandidates, resourceCandidatesMap.get(Boolean.FALSE));
				unassignContent(applicationCandidates, extensionCandidatesMap.get(Boolean.FALSE));		
								
				/*
				 * Go over all applications and filter application with same path (shadowed) ordered by service rank (highest first)
				 * Check substitution of an application through the matched one 
				 * No matching applications should be stored in the failedApplication list. All failed application from
				 * this step should be filtered out of the application list
				 * #19 151.6.1
				 */
				applicationCandidates = checkPathProperty(applicationCandidates);		
						
//				Check the osgi.jaxrs.name property and filter out services with same name and lower rank
				List<JaxRsProvider> candidates = checkNameProperty(applicationCandidates, resourceCandidatesMap.get(Boolean.TRUE), extensionCandidatesMap.get(Boolean.TRUE));
				applicationCandidates = candidates.stream()
						.filter(JaxRsApplicationProvider.class::isInstance)
						.map(JaxRsApplicationProvider.class::cast)
						.collect(Collectors.toUnmodifiableList());
				
				List<JaxRsResourceProvider> resourceCandidates = candidates.stream()
						.filter(JaxRsResourceProvider.class::isInstance)
						.map(JaxRsResourceProvider.class::cast)
						.collect(Collectors.toUnmodifiableList());
				
				List<JaxRsExtensionProvider> extensionCandidates = candidates.stream()
						.filter(JaxRsExtensionProvider.class::isInstance)
						.map(JaxRsExtensionProvider.class::cast)
						.collect(Collectors.toUnmodifiableList());			
				
				
//				Assign extension to apps and report a failure DTO for those extensions which have not been assigned to any app
				Set<JaxRsApplicationContentProvider> noMatchingExt = 
						assignContent(applications, applicationCandidates, extensionCandidates);
				
				noMatchingExt.stream().filter(JerseyExtensionProvider.class::isInstance)
						.map(JerseyExtensionProvider.class::cast).forEach(p -> {

							p.updateStatus(DTOConstants.FAILURE_REASON_REQUIRED_APPLICATION_UNAVAILABLE);
							if (!failedExtensions.containsKey(p.getId())) {
								failedExtensions.put(p.getId(), p);
							}

						});
				
				
//				check for osgi.jaxrs.extension.select properties in apps and extensions
//				If such property exists we should check that the corresponding extensions are available,
//				otherwise the service should result in a failure DTO
				applicationCandidates = checkExtensionSelect(applicationCandidates);	
				
				/*
				 * Determine all default applications. We are only interested in the highest ranked one, that
				 * will substitute the implicit default application. All other default applications are added 
				 * to the failed application list
				 * Section 151.6.1
				 * 
				 * Go over all applications and filter application with name '.default' ordered by service rank (highest first)
				 * Check substitution of defaultProvider through this application
				 * No matching applications should be stored in the failedApplication list. All failed application from
				 * this step should be filtered out of the application list
				 * #18 151.6.1
				 */
				Set<JaxRsApplicationProvider> defaultApplications = DispatcherHelper.getDefaultApplications(applicationCandidates);
				final Optional<JaxRsApplicationProvider> defaultApplication = defaultApplications.stream().findFirst();
				
				if(defaultApplication.isPresent()) {
				
				defaultApplications
					.stream()
					.skip(1)// the default app
					.forEach(a-> {
						if(a instanceof JerseyApplicationProvider) {
							((JerseyApplicationProvider) a).updateStatus(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
						}
						if(!failedApplications.containsKey(a.getId())) {
							failedApplications.put(a.getId(), a);
						}
					});
				} 
				
//				Filter out from the application list the default ones which have been added to the failed list
				applicationCandidates = applicationCandidates.stream().filter(a -> !failedApplications.containsKey(a.getId()))
						.collect(Collectors.toUnmodifiableList());	
				
				substituteDefaultApplication(defaultApplication);
				
//				If we did not replace the .default app with any other, we need to apply the extension.select to the contents of the .default app
				if(!defaultApplication.isPresent()) {
					checkExtensionSelect(Collections.singletonList(currentDefaultProvider));
				}				
				
//				Assign resources to apps and report a failure DTO for those resources which have not been added to any app
				Set<JaxRsApplicationContentProvider> noMatchingRes = 
						assignContent(applications, applicationCandidates, resourceCandidates);
				
				noMatchingRes.stream().forEach(e -> {
					if(e instanceof JerseyResourceProvider) {
						JerseyResourceProvider<?> p = (JerseyResourceProvider<?>) e;
						p.updateStatus(DTOConstants.FAILURE_REASON_REQUIRED_APPLICATION_UNAVAILABLE);
						if(!failedResources.containsKey(p.getId())) {
							failedResources.put(p.getId(), p);
						}
					}
				});
				
//				check for osgi.jaxrs.extension.select properties in apps and resources
//				If such property exists we should check that the corresponding extensions are available,
//				otherwise the service should result in a failure DTO
				checkExtensionSelectForResources(applicationCandidates);
				
//				If we did not replace the .default app with any other, we need to apply the extension.select to the contents of the .default app
				if(!defaultApplication.isPresent()) {
					checkExtensionSelectForResources(Collections.singletonList(currentDefaultProvider));
				}		
				
//				Remove the default app from the app candidates, because it will be registered in a separate step
				applicationCandidates = applicationCandidates.stream().filter(a-> !a.getId().equals(defaultProvider.getId()))
						.collect(Collectors.toUnmodifiableList());

				List<JaxRsApplicationProvider> finalApplicationCandidates = applicationCandidates;					
				
//				First we unregister the app that need to be unregistered
				applications.forEach((app)->{
					if (!finalApplicationCandidates.contains(app)) {
						if (whiteboard.isRegistered(app)) {
							logger.info("Unregistering application " + app.getId());
							whiteboard.unregisterApplication(app);
						}
						app.markUnchanged();
					}					
				});
				
//				Then we register/reload the app which are in the applicationCandidates list
				applications.forEach((app)->{
					if (finalApplicationCandidates.contains(app)) {
						if (whiteboard.isRegistered(app)) {			
							if (app.isChanged()) {
								logger.info("Re-loading application APP " + app.getId());
								whiteboard.reloadApplication(app);
							}
						} else {
							logger.info("Registering application " + app.getId());
							whiteboard.registerApplication(app);
						}
						app.markUnchanged();
					}					
				});
				
//				We register/reload the default application, if needed
				if(!whiteboard.isRegistered(currentDefaultProvider)) {
					whiteboard.registerApplication(currentDefaultProvider);
					currentDefaultProvider.markUnchanged();
				} else 	if (currentDefaultProvider != null && currentDefaultProvider.isChanged()) {
					if (whiteboard.isRegistered(currentDefaultProvider)) {
						whiteboard.reloadApplication(currentDefaultProvider);
					}
					currentDefaultProvider.markUnchanged();
				}
				
				if(whiteboard instanceof AbstractJerseyServiceRuntime) {
					AbstractJerseyServiceRuntime ajsr = (AbstractJerseyServiceRuntime) whiteboard;
					ajsr.updateFailedContents(failedApplications, failedResources, failedExtensions);
					reset(failedApplications, failedResources, failedExtensions);
				}				
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				lock.unlock();
			}
			// re-trigger, if there was a change during lock
			if (lockedChange.compareAndSet(true, false)) {
				doDispatch();
			}
		} else {
			lockedChange.compareAndSet(false, true);
		}
	}

	/**
	 * @param failedApplications
	 * @param failedResources
	 * @param failedExtensions
	 */
	private void reset(Map<String, JaxRsApplicationProvider> failedApplications,
			Map<String, JaxRsResourceProvider> failedResources,
			Map<String, JaxRsExtensionProvider> failedExtensions) {

		failedApplications.values().stream().filter(JerseyApplicationProvider.class::isInstance)
		.map(JerseyApplicationProvider.class::cast).forEach(a -> a.updateStatus(JaxRsConstants.NO_FAILURE));
		
		failedResources.values().stream().filter(JerseyResourceProvider.class::isInstance)
		.map(JerseyResourceProvider.class::cast).forEach(a -> a.updateStatus(JaxRsConstants.NO_FAILURE));
		
		failedExtensions.values().stream().filter(JerseyExtensionProvider.class::isInstance)
		.map(JerseyExtensionProvider.class::cast).forEach(a -> a.updateStatus(JaxRsConstants.NO_FAILURE));

		failedApplications.clear();
		failedResources.clear();
		failedExtensions.clear();		
	}

	/**
	 * 151.6.1: The base URI for each application within the whiteboard must be unique. 
	 * If two or more applications targeting the same whiteboard are registered with the same base URI 
	 * then only the highest ranked service will be made available. 
	 * All other application services with that URI will have a failure DTO created for them. 
	 * 
	 * @param applicationCandidates the candidates apps. They are already ordered by rank because they passed before through
	 * the checkNameProperty method
	 * 
	 * @return the surviving apps after this check
	 */
	private List<JaxRsApplicationProvider> checkPathProperty(List<JaxRsApplicationProvider> applicationCandidates) {
		
		logger.fine("App Candidates size BEFORE ordering " + applicationCandidates.size());
		
		applicationCandidates = applicationCandidates.stream()
				.sorted()
				.collect(Collectors.toUnmodifiableList());
		
		logger.fine("App Candidates size AFTER ordering " + applicationCandidates.size());

		
		for(int i = 0; i < applicationCandidates.size(); i++) {
			JaxRsApplicationProvider a1 = applicationCandidates.get(i);
			String path = a1.getPath();
			for(int j = i+1; j < applicationCandidates.size(); j++) {
				JaxRsApplicationProvider a2 = applicationCandidates.get(j);
				if(path.equals(a2.getPath())) {
					failedApplications.put(a2.getId(), a2);										
					if(a2 instanceof JerseyApplicationProvider) {
						JerseyApplicationProvider a = (JerseyApplicationProvider) a2;
						logger.fine("Failing DTO status for App " + a.getId());						
						a.updateStatus(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
					}					
				}
			}
		}
		return applicationCandidates.stream().filter(a->!failedApplications.containsKey(a.getId())).collect(Collectors.toList());
	}

	/**
	 * Check extension dependencies for resources. If a dependency is not satisfied, the resource is 
	 * removed from the app. 
	 * 
	 * @param applicationCandidates
	 */
	private void checkExtensionSelectForResources(
			List<JaxRsApplicationProvider> applicationCandidates) {
		
		Map<JaxRsProvider, Set<String>> dependencyMap = new HashMap<JaxRsProvider, Set<String>>();
		
		for(JaxRsApplicationProvider app : applicationCandidates) {

			Collection<JaxRsApplicationContentProvider> contents = app.getContentProviers();
			
//			get the extensions which have been added to this app
			List<JaxRsExtensionProvider> extensions = contents.stream()
					.filter(JaxRsExtensionProvider.class::isInstance)
					.map(JaxRsExtensionProvider.class::cast)
					.collect(Collectors.toList());
			
//			get the resources which have been added to this app
			List<JaxRsResourceProvider> resources = contents.stream()
					.filter(JaxRsResourceProvider.class::isInstance)
					.map(JaxRsResourceProvider.class::cast)
					.collect(Collectors.toList());
			
			for(JaxRsResourceProvider res : resources) {
				if(res.requiresExtensions()) {
					dependencyMap.put(res, new HashSet<String>());
					List<Filter> extFilters = res.getExtensionFilters();	
					for(Filter filter : extFilters) {					
						boolean match = false;
						for(JaxRsExtensionProvider ext : extensions) {
							if(filter.matches(ext.getProperties())) {
								match = true;
								break;
							}
						}
						if(match == false) {
							if(!filter.matches(app.getProviderProperties())
									&& !filter.matches(getWhiteboardProvider().getProperties())) {
								removeContentFromApplication(app, res);
								if(!failedResources.containsKey(res.getId())) {
									failedResources.put(res.getId(), res);
								}								
								if(res instanceof JerseyResourceProvider) {
									JerseyResourceProvider<?> a = (JerseyResourceProvider<?>) res;
									a.updateStatus(DTOConstants.FAILURE_REASON_REQUIRED_EXTENSIONS_UNAVAILABLE);
								}
							}
						}	
					}
				}
			}			
		}
	}

	/**
	 * Check the osgi.jaxrs.extension.select for apps and extensions. 
	 * If an app requires an extension and this is not present, then all the extensions previously added to that app
	 * will be removed and the app itself will be unregistered and recorded as a failure DTO.
	 * If an extension requires another extension which is not present, the extension is removed from the app. In this
	 * case we then check recursively if the removal of such extension causes other extensions or the app itself to be
	 * unsatisfied.
	 * 
	 * @param applicationCandidates the app candidates
	 * @return the set of surviving apps after this check
	 */
	private List<JaxRsApplicationProvider> checkExtensionSelect(List<JaxRsApplicationProvider> applicationCandidates) {
		
		Map<JaxRsProvider, Set<String>> dependencyMap = new HashMap<JaxRsProvider, Set<String>>();
		
		for(JaxRsApplicationProvider app : applicationCandidates) {
		
//			get the extensions which have been added to this app
			Collection<JaxRsApplicationContentProvider> contents = app.getContentProviers();
			List<JaxRsExtensionProvider> extensions = contents.stream()
					.filter(JaxRsExtensionProvider.class::isInstance)
					.map(JaxRsExtensionProvider.class::cast)
					.collect(Collectors.toList());
			
//			check if the app itself requires some ext. If so, check if they are among the contents.
//			If not, the application should be put in the failed ones and all the ext should be removed from the app
			if(app.requiresExtensions()) {
				dependencyMap.put(app, new HashSet<String>());
				List<Filter> extFilters = app.getExtensionFilters();			
				
				for(Filter filter : extFilters) {					
					boolean match = false;
					for(JaxRsExtensionProvider ext : extensions) {
						if(filter.matches(ext.getProperties())) {
							match = true;
							dependencyMap.get(app).add(ext.getId());
							break;
						}
					}
					if(match == false) {	
						if(!filter.matches(getWhiteboardProvider().getProperties())) {
							for(JaxRsExtensionProvider ext : extensions) {							
								removeContentFromApplication(app, ext);
								if(!failedExtensions.containsKey(ext.getId())) {
									failedExtensions.put(ext.getId(), ext);
								}								
								if(ext instanceof JerseyExtensionProvider) {
									JerseyExtensionProvider<?> e = (JerseyExtensionProvider<?>) ext;
									e.updateStatus(DTOConstants.FAILURE_REASON_REQUIRED_EXTENSIONS_UNAVAILABLE);
								}
							}
							if(!failedApplications.containsKey(app.getId())) {
								failedApplications.put(app.getId(), app);	
							}	
							if(app instanceof JerseyApplicationProvider) {
								JerseyApplicationProvider a = (JerseyApplicationProvider) app;
								a.updateStatus(DTOConstants.FAILURE_REASON_REQUIRED_EXTENSIONS_UNAVAILABLE);
							}	
							break;
						}					
					}
					
				}				
			}
//			If the app survives previous step, we should check all the extensions for extension requirement
//			If a required ext is not there we remove the extension which was asking for it from the app
//			We also check if previous passing extension needed that ext. In that case we unregistered also
//			those ones recursively. If the app needed one of the removed extension we remove the app
			if(!failedApplications.containsKey(app.getId())) {
				List<JaxRsExtensionProvider> extensionsCopy = new LinkedList<>();
				extensionsCopy.addAll(extensions);
				for(JaxRsExtensionProvider ext : extensions) {
					if(ext.requiresExtensions()) {
						dependencyMap.put(ext, new HashSet<String>());
						List<Filter> extFilters = ext.getExtensionFilters();	
						for(Filter filter : extFilters) {					
							boolean match = false;
							for(JaxRsExtensionProvider ext2 : extensionsCopy) {
								if(filter.matches(ext2.getProperties())) {
									match = true;
									dependencyMap.get(ext).add(ext2.getId());
									break;
								}
							}
							if(match == false) {
								if(filter.matches(app.getProviderProperties())) {
									match = true;
									dependencyMap.get(ext).add(app.getId());
								}
								else if(filter.matches(getWhiteboardProvider().getProperties())) {
									match = true;
									dependencyMap.get(ext).add(getWhiteboardProvider().getName());
								}
								else {
									removeExtensionDependency(dependencyMap, ext, extensionsCopy, app);		
									if(failedApplications.containsKey(app.getId())) {
										break;
									}
								}								
							}							
						}				
					}
				}
			}			
		}	
		return applicationCandidates.stream().filter(a -> !failedApplications.containsKey(a.getId())).collect(Collectors.toList());
	}
	
	private void removeExtensionDependency(Map<JaxRsProvider, Set<String>> dependencyMap, 
			JaxRsExtensionProvider ext, List<JaxRsExtensionProvider> extensionsCopy, JaxRsApplicationProvider app) {
		
		if(dependencyMap.get(app) != null && dependencyMap.get(app).contains(ext.getId())) {
			for(JaxRsExtensionProvider extension : extensionsCopy) {
				removeContentFromApplication(app, extension);				
				if(!failedExtensions.containsKey(extension.getId())) {
					failedExtensions.put(extension.getId(), extension);
				}
				if(extension instanceof JerseyExtensionProvider) {
					JerseyExtensionProvider<?> e = (JerseyExtensionProvider<?>) extension;
					e.updateStatus(DTOConstants.FAILURE_REASON_REQUIRED_EXTENSIONS_UNAVAILABLE);
				}
			}
			if(!failedApplications.containsKey(app.getId())) {
				failedApplications.put(app.getId(), app);	
			}
			if(app instanceof JerseyApplicationProvider) {
				JerseyApplicationProvider a = (JerseyApplicationProvider) app;
				a.updateStatus(DTOConstants.FAILURE_REASON_REQUIRED_EXTENSIONS_UNAVAILABLE);
			}	
		}
		else {
			removeContentFromApplication(app, ext);
			if(!failedExtensions.containsKey(ext.getId())) {
				failedExtensions.put(ext.getId(), ext);
			}
			if(ext instanceof JerseyExtensionProvider) {
				JerseyExtensionProvider<?> e = (JerseyExtensionProvider<?>) ext;
				e.updateStatus(DTOConstants.FAILURE_REASON_REQUIRED_EXTENSIONS_UNAVAILABLE);
			}
			extensionsCopy.remove(ext);
			dependencyMap.forEach((k,v)-> {
				if(v.contains(ext.getId())) {
					if(k instanceof JaxRsExtensionProvider) {						
							removeExtensionDependency(dependencyMap, (JaxRsExtensionProvider) k, extensionsCopy, app);											
					}					
				}
			});			
		}
	}

	
	/**
	 * Check the osgi.jaxrs.name property of all services associated with a whiteboard.
	 * If two or more services have the same name property, only the highest ranked one should be
	 * kept. The others should result in a failure DTO 
	 * 
	 * @param applicationCandidates
	 * @param resourceCandidates
	 * @param extensionCandidates
	 * @return a set of JaxRsProvider containing the surviving services
	 */
	private List<JaxRsProvider> checkNameProperty(List<JaxRsApplicationProvider> applicationCandidates,
			List<JaxRsResourceProvider> resourceCandidates, List<JaxRsExtensionProvider> extensionCandidates) {			
		
		List<JaxRsProvider> allCandidates = new ArrayList<JaxRsProvider>();
		allCandidates.addAll(applicationCandidates);
		allCandidates.addAll(resourceCandidates);
		allCandidates.addAll(extensionCandidates);		
		
		logger.fine("App Candidates BEFORE NAME SORT " + allCandidates.size());
		
		allCandidates = allCandidates.stream()
				.sorted(Comparator.naturalOrder())
				.collect(Collectors.toUnmodifiableList());
		
			
		List<JaxRsProvider> failures = new ArrayList<JaxRsProvider>();
		for(int i = 0; i < allCandidates.size(); i++) {
			JaxRsProvider p = allCandidates.get(i);
			String name = p.getName();
			for(int j = i+1; j < allCandidates.size(); j++) {
				JaxRsProvider p2 = allCandidates.get(j);	
				if(name.equals(p2.getName())) {
					logger.info("Adding failure " + p2.getId() + " with name " + p2.getName() + " compared with " + p.getId());
					failures.add(p2);						
				}
			}
		}
		logger.fine("Failures after name sort " + failures.size());
		failures.stream().forEach(f-> {
			
			if(f instanceof JaxRsApplicationProvider) {
				if(!failedApplications.containsKey(f.getId())) {
					failedApplications.put(f.getId(), (JaxRsApplicationProvider) f);	
				}
				if(f instanceof JerseyApplicationProvider) {
					JerseyApplicationProvider a = (JerseyApplicationProvider) f;
					a.updateStatus(DTOConstants.FAILURE_REASON_DUPLICATE_NAME);
				}
			}
			else if(f instanceof JaxRsResourceProvider) {
				if(!failedResources.containsKey(f.getId())) {
					failedResources.put(f.getId(), (JaxRsResourceProvider) f);
				}
				if(f instanceof JerseyResourceProvider) {
					JerseyResourceProvider<?> r = (JerseyResourceProvider<?>) f;
					r.updateStatus(DTOConstants.FAILURE_REASON_DUPLICATE_NAME);
				}
			}
			else if(f instanceof JaxRsExtensionProvider) {
				if(!failedExtensions.containsKey(f.getId())) {
					failedExtensions.put(f.getId(), (JaxRsExtensionProvider) f);
				}
				
				if(f instanceof JerseyExtensionProvider) {
					JerseyExtensionProvider<?> e = (JerseyExtensionProvider<?>) f;
					e.updateStatus(DTOConstants.FAILURE_REASON_DUPLICATE_NAME);
				}
			}			
		});
		
		return allCandidates.stream().filter(not(failures::contains)).collect(Collectors.toList());	
	}
	
	

	/**
	 * Removes content, that's services has been disappeared.
	 * @param applications all applications
	 * @param content the content
	 */
	private void unassignContent(Collection<JaxRsApplicationProvider> applications, Collection<? extends JaxRsApplicationContentProvider> content) {
		applications.forEach((app)->{
			content.forEach((c)->{
				if (removeContentFromApplication(app, c)) {
					logger.info("Removed content " + c.getName() + " from application " + app.getName());
				}
			});
		});
		content.forEach((c)->{
			if (removeContentFromApplication(currentDefaultProvider, c)) {
				logger.info("Removed content " + c.getName() + " from default application");
			}
		});
	}

	private Set<JaxRsApplicationContentProvider> assignContent(Collection<JaxRsApplicationProvider> applications, 
			Collection<JaxRsApplicationProvider> candidates,
			Collection<? extends JaxRsApplicationContentProvider> content) {
		
		Set<JaxRsApplicationContentProvider> notAddedContents = new HashSet<>();
		
		// determine all content that match an application and returns the ones that found a match
		Set<JaxRsApplicationContentProvider> contentCandidates = content.
				stream().
				map(this::cloneContent).
				filter((c)->{
					AtomicBoolean matched = new AtomicBoolean(false);
					applications.forEach((app)->{
						if (candidates.contains(app) && 
								c != null &&
								c.canHandleApplication(app)) {
							boolean added = addContentToApplication(app, c);
							if (added) {
								logger.info("Added content " + c.getName() + " to application " + app.getName() + " " + c.getObjectClass());
							}
							if (!matched.get()) {
								matched.set(added);
							} 
						} else {
							if (removeContentFromApplication(app, c)) {
								logger.info("Removed content " + c.getName() + " from application " + app.getName());
							}
						}
					});
					return matched.get();
				}).collect(Collectors.toSet());		
		
		/* 
		 * Add all other content to the default application or remove it, if the content fits to an other application now
		 * For that we have to use a comparator that compares the content provider by name
		 */
		Comparator<JaxRsApplicationContentProvider> comparator = JaxRsApplicationContentProvider.getComparator();
		final Set<JaxRsApplicationContentProvider> cc = new TreeSet<JaxRsApplicationContentProvider>(comparator);
		cc.addAll(contentCandidates);
		content.stream().
		map(this::cloneContent).forEach((c)->{
			if (cc.contains(c)) {
				if (c.canHandleApplication(currentDefaultProvider)) {
					if (addContentToApplication(currentDefaultProvider, c)) {
					}
				} else {
					if (removeContentFromApplication(currentDefaultProvider, c)) {
						logger.fine("Removed content candidate " + c.getName() + " from default application");
					}
				}
			} else {
				if(c.canHandleDefaultApplication(currentDefaultProvider)) {
					if (addContentToApplication(currentDefaultProvider, c)) {
						logger.info("Added content " + c.getName() + " to current default application " + currentDefaultProvider.getName() + " " + c.getObjectClass());
					} 				
					else {
						logger.info("Current default app cannot handle " + c.getName());
						notAddedContents.add(c);
					}
				}		
				else {
					logger.info("No suitable app found for " + c.getName());
					notAddedContents.add(c);
				}
				
			}
		});
		
		return notAddedContents;
	}

	/**
	 * Adds content instance to an application 
	 * @param application the application to add the content for
	 * @param content the content to add
	 * @return <code>true</code>, if adding was successful
	 */
	private boolean addContentToApplication(JaxRsApplicationProvider application, JaxRsApplicationContentProvider content) {
		if (content instanceof JaxRsResourceProvider) {
			return application.addResource((JaxRsResourceProvider) content);
		}
		if (content instanceof JaxRsExtensionProvider) {
			return application.addExtension((JaxRsExtensionProvider) content);
		}
		logger.warning("unhandled JaxRsApplicationContentProvider. coult not add application " + application + " to content " + content);
		return false;
	}

	/**
	 * Removes a content instance from an application 
	 * @param application the application to remove the content for
	 * @param content the content to remove
	 * @return <code>true</code>, if removal was successful
	 */
	private boolean removeContentFromApplication(JaxRsApplicationProvider application, JaxRsApplicationContentProvider content) {
		if (content instanceof JaxRsResourceProvider) {
			return application.removeResource((JaxRsResourceProvider) content);
		}
		if (content instanceof JaxRsExtensionProvider) {
			return application.removeExtension((JaxRsExtensionProvider) content);
		}
		logger.warning("unhandled JaxRsApplicationContentProvider. Can not remove application " + application + " for content " + content);

		return false;
	}

	/**
	 * Clones the given source and returns the new cloned instance
	 * @param source the object to be cloned
	 * @return the cloned instance
	 */
	private JaxRsApplicationContentProvider cloneContent(JaxRsApplicationContentProvider source) {
		if (source == null) {
			return null;
		}
		try {
			return (JaxRsApplicationContentProvider) source.clone();
		} catch (CloneNotSupportedException e) {
			logger.log(Level.SEVERE, "Cannot clone object " + source.getId() + " because it is not clonable", e);
		}
		return null;
	}
	
	private void substituteDefaultApplication(Optional<JaxRsApplicationProvider> newDefaultProvider) {
		/*
		 * We check, if an un-registration of the application is really necessary
		 */
		boolean unregisterNeeded = false;
		Long providerSID = newDefaultProvider.isPresent() ? newDefaultProvider.get().getServiceId() : null;
		Long currentSID = currentDefaultProvider != null ? currentDefaultProvider.getServiceId() : null;
		
		if (currentSID != null) {
			if (providerSID != null) {
				unregisterNeeded = providerSID != currentSID;
			} else {
				unregisterNeeded = false; //if another default app is not found we do not need to unregister the basic .default one
			}
		}
		if (whiteboard != null && 
				unregisterNeeded && 
				whiteboard.isRegistered(currentDefaultProvider)) {
			whiteboard.unregisterApplication(currentDefaultProvider);
			/*			
			 * If the previous .default is unregistered is because it is shadowed by another app, 
			 * so we need to record this as a failure DTO
			 */		
			if(currentDefaultProvider instanceof JerseyApplicationProvider) {
				JerseyApplicationProvider ap = (JerseyApplicationProvider) currentDefaultProvider;
				ap.updateStatus(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
				failedApplications.put(currentDefaultProvider.getId(), currentDefaultProvider);
			}
		}
		
		currentDefaultProvider = newDefaultProvider.orElse(defaultProvider);
	}

	/**
	 * Copies the list of removed objects and removes all copied objects from it
	 * @param originalCollection the original collection
	 * @return the copied collection as unmodifiable collection or <code>null</code>
	 */
	private <T> Collection<T> getRemovedList(Collection<T> originalCollection) {
		if (originalCollection == null) {
			return null;
		}
		Set<T> removed;
		synchronized (originalCollection) {
			removed = new HashSet<>(originalCollection);
			originalCollection.removeAll(removed);
		}
		return removed;
	}

	/* (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsWhiteboardDispatcher#getBatchMode()
	 */
	@Override
	public boolean getBatchMode() {
		return batchMode;
	}
	


	
}
