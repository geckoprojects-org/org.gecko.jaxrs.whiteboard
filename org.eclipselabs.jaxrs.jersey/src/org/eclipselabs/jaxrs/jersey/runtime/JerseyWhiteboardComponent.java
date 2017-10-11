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
package org.eclipselabs.jaxrs.jersey.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.Application;

import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsExtensionProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsResourceProvider;
import org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider;
import org.eclipselabs.jaxrs.jersey.runtime.application.JerseyApplicationProvider;
import org.eclipselabs.jaxrs.jersey.runtime.application.JerseyExtensionProvider;
import org.eclipselabs.jaxrs.jersey.runtime.application.JerseyResourceProvider;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * A configurable component, that establishes a whiteboard
 * @author Mark Hoffmann
 * @since 11.10.2017
 */
public class JerseyWhiteboardComponent {

	private volatile Map<String, JaxRsApplicationProvider> applicationProviderCache = new ConcurrentHashMap<>();
	private volatile Map<String, JaxRsResourceProvider> resourceProviderCache = new ConcurrentHashMap<>();
	private volatile Map<String, JaxRsExtensionProvider> extensionProviderCache = new ConcurrentHashMap<>();
	private volatile JaxRsWhiteboardProvider whiteboard;

	/**
	 * Adds a new application
	 * @param application the application to add
	 * @param properties the service properties
	 */
	@Reference(name="application", cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC, unbind="removeApplication")
	public void addApplication(Application application, Map<String, Object> properties) {
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(application, properties);
		String name = provider.getName();
		if (!applicationProviderCache.containsKey(name)) {
			applicationProviderCache.put(name, provider);
			if (provider.canHandleWhiteboard(whiteboard.getProperties())) {
				whiteboard.registerApplication(provider);
			}
		}
	}

	/**
	 * Removes a application 
	 * @param application the application to remove
	 * @param properties the service properties
	 */
	public void removeApplication(Application application, Map<String, Object> properties) {
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(application, properties);
		String name = provider.getName();
		JaxRsApplicationProvider removed = applicationProviderCache.remove(name);
		if (removed != null) {
			whiteboard.unregisterApplication(provider);
		} 
	}

	/**
	 * Adds a new resource
	 * @param resource the resource to add
	 * @param properties the service properties
	 */
	@Reference(name="resource", cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC, unbind="removeResource", target="(" + JaxRSWhiteboardConstants.JAX_RS_RESOURCE + "='true')")
	public void addResource(Object resource, Map<String, Object> properties) {
		JaxRsResourceProvider provider = new JerseyResourceProvider<Object>(resource, properties);
		String name = provider.getName();
		if (!resourceProviderCache.containsKey(name)) {
			resourceProviderCache.put(name, provider);
			applicationProviderCache.values().forEach((ap)-> {
				if (provider.canHandleApplication(ap)) {
					ap.addResource(resource, properties);
				}
			});
		}
	}

	/**
	 * Removes a resource 
	 * @param resource the resource to remove
	 * @param properties the service properties
	 */
	public void removeResource(Object resource, Map<String, Object> properties) {
		JaxRsResourceProvider provider = new JerseyResourceProvider<Object>(resource, properties);
		String name = provider.getName();
		JaxRsResourceProvider removed = resourceProviderCache.remove(name);
		if (removed != null) {
			applicationProviderCache.values().forEach((ap)-> ap.removeResource(resource, properties));
		}
	}

	/**
	 * Adds a new extension
	 * @param extension the extension to add
	 * @param properties the service properties
	 */
	@Reference(name="extension", cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC, unbind="removeExtension", target="(" + JaxRSWhiteboardConstants.JAX_RS_EXTENSION + "='true')")
	public void addExtension(Object extension, Map<String, Object> properties) {
		JaxRsExtensionProvider provider = new JerseyExtensionProvider<Object>(extension, properties);
		String name = provider.getName();
		if (!extensionProviderCache.containsKey(name)) {
			extensionProviderCache.put(name, provider);
			applicationProviderCache.values().forEach((ap)-> {
				if (provider.canHandleApplication(ap)) {
					ap.addExtension(extension, properties);
				}
			});
		}
	}

	/**
	 * Removes an extension 
	 * @param extension the extension to remove
	 * @param properties the service properties
	 */
	public void removeExtension(Object extension, Map<String, Object> properties) {
		JaxRsExtensionProvider provider = new JerseyExtensionProvider<Object>(extension, properties);
		String name = provider.getName();
		JaxRsExtensionProvider removed = extensionProviderCache.remove(name);
		if (removed != null) {
			applicationProviderCache.values().forEach((ap)-> ap.removeExtension(extension, properties));
		}
	}

}
