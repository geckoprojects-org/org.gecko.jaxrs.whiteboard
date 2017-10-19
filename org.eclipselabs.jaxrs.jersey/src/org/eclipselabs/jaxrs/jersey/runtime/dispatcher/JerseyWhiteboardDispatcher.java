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
package org.eclipselabs.jaxrs.jersey.runtime.dispatcher;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.ws.rs.core.Application;

import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationContentProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsExtensionProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsResourceProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsWhiteboardDispatcher;
import org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider;
import org.eclipselabs.jaxrs.jersey.runtime.application.JerseyApplicationProvider;
import org.eclipselabs.jaxrs.jersey.runtime.application.JerseyExtensionProvider;
import org.eclipselabs.jaxrs.jersey.runtime.application.JerseyResourceProvider;

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

	private JaxRsWhiteboardProvider whiteboard;
	private volatile Map<String, JaxRsApplicationProvider> applicationProviderCache = new ConcurrentHashMap<>();
	private volatile Map<String, JaxRsResourceProvider> resourceProviderCache = new ConcurrentHashMap<>();
	private volatile Map<String, JaxRsExtensionProvider> extensionProviderCache = new ConcurrentHashMap<>();
	private volatile Set<JaxRsApplicationProvider> removedApplications = new HashSet<>();
	private volatile Set<JaxRsResourceProvider> removedResources = new HashSet<>();
	private volatile Set<JaxRsExtensionProvider> removedExtensions = new HashSet<>();
	private volatile boolean dispatching = false;
	private volatile JaxRsApplicationProvider defaultProvider;
	private ReentrantLock lock = new ReentrantLock();
	private AtomicBoolean lockedChange = new AtomicBoolean();

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.application.JaxRsWhiteboardDispatcher#setWhiteboardProvider(org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider)
	 */
	@Override
	public void setWhiteboardProvider(JaxRsWhiteboardProvider whiteboard) {
		if (isDispatching()) {
			throw new IllegalStateException("Error setting whiteboard provider, when dispatching is active");
		}
		this.whiteboard = whiteboard;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.application.JaxRsWhiteboardDispatcher#getWhiteboardProvider()
	 */
	@Override
	public JaxRsWhiteboardProvider getWhiteboardProvider() {
		return whiteboard;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.application.JaxRsWhiteboardDispatcher#getApplications()
	 */
	@Override
	public Set<JaxRsApplicationProvider> getApplications() {
		return Collections.unmodifiableSet(new HashSet<>(applicationProviderCache.values()));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.application.JaxRsWhiteboardDispatcher#getResources()
	 */
	@Override
	public Set<JaxRsResourceProvider> getResources() {
		return Collections.unmodifiableSet(new HashSet<>(resourceProviderCache.values()));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.application.JaxRsWhiteboardDispatcher#getExtensions()
	 */
	@Override
	public Set<JaxRsExtensionProvider> getExtensions() {
		return Collections.unmodifiableSet(new HashSet<>(extensionProviderCache.values()));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.application.JaxRsWhiteboardDispatcher#addApplication(javax.ws.rs.core.Application, java.util.Map)
	 */
	@Override
	public void addApplication(Application application, Map<String, Object> properties) {
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(application, properties);
		String name = provider.getName();
		if (!applicationProviderCache.containsKey(name)) {
			applicationProviderCache.put(name, provider);
			checkDispatch();
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.application.JaxRsWhiteboardDispatcher#removeApplication(javax.ws.rs.core.Application, java.util.Map)
	 */
	@Override
	public void removeApplication(Application application, Map<String, Object> properties) {
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(application, properties);
		String name = provider.getName();
		JaxRsApplicationProvider removed = applicationProviderCache.remove(name);
		if (removed != null) {
			removedApplications.add(removed);
			checkDispatch();
		} 
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.application.JaxRsWhiteboardDispatcher#addResource(java.lang.Object, java.util.Map)
	 */
	@Override
	public void addResource(Object resource, Map<String, Object> properties) {
		JaxRsResourceProvider provider = new JerseyResourceProvider<Object>(resource, properties);
		String name = provider.getName();
		if (!resourceProviderCache.containsKey(name)) {
			resourceProviderCache.put(name, provider);
			checkDispatch();
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.application.JaxRsWhiteboardDispatcher#removeResource(java.lang.Object, java.util.Map)
	 */
	@Override
	public void removeResource(Object resource, Map<String, Object> properties) {
		JaxRsResourceProvider provider = new JerseyResourceProvider<Object>(resource, properties);
		String name = provider.getName();
		JaxRsResourceProvider removed = resourceProviderCache.remove(name);
		if (removed != null) {
			removedResources.add(removed);
			checkDispatch();
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.application.JaxRsWhiteboardDispatcher#addExtension(java.lang.Object, java.util.Map)
	 */
	@Override
	public void addExtension(Object extension, Map<String, Object> properties) {
		JaxRsExtensionProvider provider = new JerseyExtensionProvider<Object>(extension, properties);
		String name = provider.getName();
		if (!extensionProviderCache.containsKey(name)) {
			extensionProviderCache.put(name, provider);
			checkDispatch();
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.application.JaxRsWhiteboardDispatcher#removeExtension(java.lang.Object, java.util.Map)
	 */
	@Override
	public void removeExtension(Object extension, Map<String, Object> properties) {
		JaxRsExtensionProvider provider = new JerseyExtensionProvider<Object>(extension, properties);
		String name = provider.getName();
		JaxRsExtensionProvider removed = extensionProviderCache.remove(name);
		if (removed != null) {
			removedExtensions.add(removed);
			checkDispatch();
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.application.JaxRsWhiteboardDispatcher#dispatch()
	 */
	@Override
	public void dispatch() {
		// add default application
		if (whiteboard == null) {
			throw new IllegalStateException("Dispatcher cannot be used without a whiteboard provider");
		}
		defaultProvider = new JerseyApplicationProvider(".default", new Application(), "/");
		whiteboard.registerApplication(defaultProvider);
		dispatching = true;
		doDispatch();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.application.JaxRsWhiteboardDispatcher#deactivate()
	 */
	@Override
	public void deactivate() {
		if (!isDispatching()) {
			return;
		}
		dispatching = false;
		whiteboard.unregisterApplication(defaultProvider);
		applicationProviderCache.values().forEach((app)->{
			if (whiteboard.isRegistered(app)) {
				whiteboard.unregisterApplication(app);
			}
		});
		defaultProvider = null;
		applicationProviderCache.clear();
		resourceProviderCache.clear();
		extensionProviderCache.clear();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.application.JaxRsWhiteboardDispatcher#isDispatching()
	 */
	@Override
	public boolean isDispatching() {
		return dispatching;
	}

	/**
	 * Checks the execution of doDispatch, in case it is active
	 */
	private void checkDispatch() {
		if (isDispatching()) {
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
							if (app.isEmpty()) {
								whiteboard.unregisterApplication(app);
								// legacy application don't need a reload at all, all others are only reloaded, if they have changed
							} else if (!app.isLegacy() && app.isChanged()) {
								whiteboard.reloadApplication(app);
							}
						} else {
							// we don't register empty applications and legacy application in general or changed applications 
							if (!app.isEmpty() && 
									(app.isLegacy() || app.isChanged())) {
								whiteboard.registerApplication(app);
							}
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
				if (defaultProvider.isChanged()) {
					if (whiteboard.isRegistered(defaultProvider)) {
						whiteboard.reloadApplication(defaultProvider);
					}
					defaultProvider.markUnchanged();
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
			content.forEach((c)->removeContentFromApplication(app, c));
		});
		content.forEach((c)->removeContentFromApplication(defaultProvider, c));
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
				stream().map((c)->{
					try {
						return (JaxRsApplicationContentProvider) c.clone();
					} catch (CloneNotSupportedException e) {
						e.printStackTrace();
					}
					return null;
				}).
				filter((c)->{
					AtomicBoolean matched = new AtomicBoolean(false);
					applications.forEach((app)->{
						if (candidates.contains(app) && 
								c != null &&
								c.canHandleApplication(app)) {
							if (!matched.get()) {
								matched.set(addContentToApplication(app, c));
							} else {
								addContentToApplication(app, c);
							}
						} else {
							removeContentFromApplication(app, c);
						}
					});
					return matched.get();
				}).collect(Collectors.toSet());
		// add all other content to the default application or remove it, if the content fits to an other application now
		content.stream().map((c)->{
			try {
				return (JaxRsApplicationContentProvider) c.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			return null;
		}).forEach((c)->{
			if (contentCandidates.contains(c)) {
				if (c.canHandleApplication(defaultProvider)) {
					addContentToApplication(defaultProvider, c);
				} else {
					removeContentFromApplication(defaultProvider, c);
				}
			} else {
				addContentToApplication(defaultProvider, c);
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
		if (application.isLegacy()) {
			return false;
		}
		if (content instanceof JaxRsResourceProvider) {
			return application.addResource((JaxRsResourceProvider) content);
		}
		if (content instanceof JaxRsExtensionProvider) {
			return application.addExtension((JaxRsExtensionProvider) content);
		}
		return false;
	}

	/**
	 * Removes a content instance from an application 
	 * @param application the application to remove the content for
	 * @param content the content to remove
	 * @return <code>true</code>, if removal was successful
	 */
	private boolean removeContentFromApplication(JaxRsApplicationProvider application, JaxRsApplicationContentProvider content) {
		if (application.isLegacy()) {
			return false;
		}
		if (content instanceof JaxRsResourceProvider) {
			return application.removeResource((JaxRsResourceProvider) content);
		}
		if (content instanceof JaxRsExtensionProvider) {
			return application.removeExtension((JaxRsExtensionProvider) content);
		}
		return false;
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

}
