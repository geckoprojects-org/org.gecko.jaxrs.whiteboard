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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.core.Application;

import org.gecko.rest.jersey.helper.DispatcherHelper;
import org.gecko.rest.jersey.provider.application.JaxRsApplicationContentProvider;
import org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider;
import org.gecko.rest.jersey.provider.application.JaxRsExtensionProvider;
import org.gecko.rest.jersey.provider.application.JaxRsResourceProvider;
import org.gecko.rest.jersey.provider.application.JaxRsWhiteboardDispatcher;
import org.gecko.rest.jersey.provider.whiteboard.JaxRsWhiteboardProvider;
import org.gecko.rest.jersey.runtime.application.JerseyApplicationProvider;
import org.gecko.rest.jersey.runtime.application.JerseyExtensionProvider;
import org.gecko.rest.jersey.runtime.application.JerseyResourceProvider;
import org.osgi.framework.ServiceObjects;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

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
	private volatile Set<JaxRsApplicationProvider> failedApplications = new HashSet<>();
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

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsWhiteboardDispatcher#addApplication(javax.ws.rs.core.Application, java.util.Map)
	 */
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
			}
			return;
		} else if (provider.getName().equals(".default") && currentDefaultProvider != null) {
			Object providerBase = provider.getApplicationProperties().get(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE);
			Object currentBase = currentDefaultProvider.getApplicationProperties().get(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE);
			if (providerBase != null && !providerBase.equals(currentBase)) {
				currentDefaultProvider.updateApplicationBase((String) providerBase);
				if (whiteboard != null) {
					whiteboard.unregisterApplication(currentDefaultProvider);
					whiteboard.registerApplication(currentDefaultProvider);
					currentDefaultProvider.markUnchanged();
				}
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
		logger.info("Removing Application with name " + provider.getName());
		if (removed != null) {
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
		logger.fine("Dispatcher add resource with id: " + key + " and class " + provider.getObjectClass().getName());
		resourceProviderCache.put(key, provider);
		checkDispatch();
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
		logger.info("Add extension " + key + " name: " + provider.getName());
		if (!extensionProviderCache.containsKey(key)) {
			logger.info("Added extension " + key + " name: " + provider.getName());
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
		logger.info("Remove extension " + key + " name: " + provider.getName());
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

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsWhiteboardDispatcher#deactivate()
	 */
	@Override
	public void deactivate() {
		if (!isDispatching()) {
			return;
		}
		try {
			lock.tryLock(5, TimeUnit.SECONDS);
			dispatching = false;
			whiteboard.unregisterApplication(currentDefaultProvider);
//			whiteboard.unregisterApplication(currentShadowed);
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
				 * Determine all default applications. We are only interested in the highest ranked one, that
				 * will substitute the implicit default application. All other default applications are added 
				 * to the failed application list
				 * Section 151.6.1
				 */
				Set<JaxRsApplicationProvider> defaultApplications = DispatcherHelper.getDefaultApplications(applications);
				final Optional<JaxRsApplicationProvider> defaultApplication = defaultApplications.stream().findFirst();
				defaultApplications
					.stream()
					.filter(app->defaultApplication.isPresent() && !app.equals(defaultApplication.get()))
					.forEach(failedApplications::add);
//				substituteDefaultApplication(defaultApplication, Optional.empty());
				
				/*
				 * Go over all applications and filter application with name '.default' ordered by service rank (highest first)
				 * Check substitution of defaultProvider through this application
				 * No matching applications should be stored in the failedApplication list. All failed application from
				 * this step should be filtered out of the application list
				 * #18 151.6.1
				 */
				/*
				 * Go over all applications and filter application with same path (shadowed) ordered by service rank (highest first)
				 * Check substitution of an application through the matched one 
				 * No matching applications should be stored in the failedApplication list. All failed application from
				 * this step should be filtered out of the application list
				 * #19 151.6.1
				 */
				
				
				/*
				 * Unregister all applications that are declared as deleted.
				 * Further remove all resources and extension from applications that are declared as deleted
				 */
				remApplications.forEach((remApp)->{
					if(whiteboard.isRegistered(remApp)) {
						whiteboard.unregisterApplication(remApp);
					}
				});
				unassignContent(applications, remResources);
				unassignContent(applications, remExtensions);

				/*
				 * Determine all applications, resources and extension that fit to the whiteboard.
				 * We only work with those, because all theses are possible candidates for the whiteboard
				 */
				Set<JaxRsApplicationProvider> applicationCandidates = applications.stream().
						filter((app)->app.canHandleWhiteboard(getWhiteboardProvider().getProperties())).
						collect(Collectors.toSet());
				Set<JaxRsResourceProvider> resourceCandidates = resources.stream().
						filter((r)->r.canHandleWhiteboard(getWhiteboardProvider().getProperties())).
						collect(Collectors.toSet());
				Set<JaxRsExtensionProvider> extensionCandidates = extensions.stream().
						filter((e)->e.canHandleWhiteboard(getWhiteboardProvider().getProperties())).
						collect(Collectors.toSet());

				/*
				 * Assign all resources and extension of our candidates to the applications
				 */
				assignContent(applications, applicationCandidates, resourceCandidates);
				assignContent(applications, applicationCandidates, extensionCandidates);

				/*
				 * 
				 */
				// apply all changes to the whiteboard
				applications.forEach((app)->{
					// the application fits to the whiteboard
					if (applicationCandidates.contains(app)) {
						if (whiteboard.isRegistered(app)) {
							// unregister applications that are now empty
							//							if (app.isEmpty()) {
							//								whiteboard.unregisterApplication(app);
							//							} else 
							if (app.isChanged()) {
								whiteboard.reloadApplication(app);
							}
						} else {

							// we don't register empty applications and legacy application in general or changed applications 
							//							if (!app.isEmpty() && app.isChanged()) {
							whiteboard.registerApplication(app);
							//							}
						}
						// the application doesn't fit to the whiteboard anymore
					} else {
						if (whiteboard.isRegistered(app)) {
							whiteboard.unregisterApplication(app);
						}
					}
					// reset change marker
					app.markUnchanged();
				});
				if(!whiteboard.isRegistered(currentDefaultProvider)) {
					whiteboard.registerApplication(currentDefaultProvider);
					currentDefaultProvider.markUnchanged();
				} else 	if (currentDefaultProvider != null &&  currentDefaultProvider.isChanged()) {
					if (whiteboard.isRegistered(currentDefaultProvider)) {
						whiteboard.reloadApplication(currentDefaultProvider);
					}
					currentDefaultProvider.markUnchanged();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				lock.unlock();
			}
			// retrigger, if there was a change during lock
			if (lockedChange.compareAndSet(true, false)) {
				doDispatch();
			}
		} else {
			lockedChange.compareAndSet(false, true);
		}
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

	/**
	 * Assigns the given content to the given applications. If content does not match to an application,
	 * it will be assigned to the default application
	 * @param applications the applications
	 * @param candidates the application candidates
	 * @param content the content to assign
	 */
	private void assignContent(Collection<JaxRsApplicationProvider> applications, Collection<JaxRsApplicationProvider> candidates, Collection<? extends JaxRsApplicationContentProvider> content) {
		// determine all content that match an application
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
		 * For that we have to use a comparator that compares the content provider byname
		 */
		Comparator<JaxRsApplicationContentProvider> comparator = JaxRsApplicationContentProvider.getComparator();
		final Set<JaxRsApplicationContentProvider> cc = new TreeSet<JaxRsApplicationContentProvider>(comparator);
		cc.addAll(contentCandidates);
		content.stream().
		map(this::cloneContent).forEach((c)->{
			if (cc.contains(c)) {
				if (c.canHandleApplication(currentDefaultProvider)) {
					if (addContentToApplication(currentDefaultProvider, c)) {
						logger.info("Added content candidate " + c.getName() + " to default application");
					}
				} else {
					if (removeContentFromApplication(currentDefaultProvider, c)) {
						logger.info("Removed content candidate " + c.getName() + " from default application");
					}
				}
			} else {
				if (addContentToApplication(currentDefaultProvider, c)) {
					logger.info("Added content " + c.getName() + " to default application " + currentDefaultProvider.getName() + " " + c.getObjectClass());
				} 
			}
		});
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
		logger.warning("unhandled JaxRsApplicationContentProvider. coult not remove application " + application + " to content " + content);

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
	
	/**
	 * Substitutes the current default provider with the provider from the parameter. If the parameter is <code>null</code>,
	 * it is expected to reuse the original implicit default provider
	 * @param newDefaultProvider the new provider or <code>null</code> 
	 * @param shadowedProvider the shadow application provider
	 */
	private void substituteDefaultApplication(Optional<JaxRsApplicationProvider> newDefaultProvider, Optional<JaxRsApplicationProvider> shadowedProvider) {
		/*
		 * We check, if an un-registration of the application is really necessary
		 */
		boolean unregisterNeeded = false;
		Long shadowSID = shadowedProvider.isPresent() ? shadowedProvider.get().getServiceId() : null;
		Long providerSID = newDefaultProvider.isPresent() ? newDefaultProvider.get().getServiceId() : null;
		Long currentSID = currentDefaultProvider != null ? currentDefaultProvider.getServiceId() : null;
		
		if (currentSID != null) {
			if (providerSID != null) {
				unregisterNeeded = providerSID != currentSID;
			} else {
				unregisterNeeded = true;
			}
		}
		if (whiteboard != null && 
				unregisterNeeded && 
				whiteboard.isRegistered(currentDefaultProvider)) {
			whiteboard.unregisterApplication(currentDefaultProvider);
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
