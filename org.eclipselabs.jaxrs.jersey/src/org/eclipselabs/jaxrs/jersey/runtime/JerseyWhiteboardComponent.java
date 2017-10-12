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

import javax.ws.rs.core.Application;

import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsWhiteboardDispatcher;
import org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider;
import org.eclipselabs.jaxrs.jersey.runtime.dispatcher.JerseyWhiteboardDispatcher;
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

	private final JaxRsWhiteboardDispatcher dispatcher = new JerseyWhiteboardDispatcher();
	private volatile JaxRsWhiteboardProvider whiteboard;

	/**
	 * Adds a new application
	 * @param application the application to add
	 * @param properties the service properties
	 */
	@Reference(name="application", cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC, unbind="removeApplication")
	public void addApplication(Application application, Map<String, Object> properties) {
		dispatcher.addApplication(application, properties);
	}

	/**
	 * Removes a application 
	 * @param application the application to remove
	 * @param properties the service properties
	 */
	public void removeApplication(Application application, Map<String, Object> properties) {
		dispatcher.removeApplication(application, properties);
	}

	/**
	 * Adds a new resource
	 * @param resource the resource to add
	 * @param properties the service properties
	 */
	@Reference(name="resource", cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC, unbind="removeResource", target="(" + JaxRSWhiteboardConstants.JAX_RS_RESOURCE + "='true')")
	public void addResource(Object resource, Map<String, Object> properties) {
		dispatcher.addResource(resource, properties);
	}

	/**
	 * Removes a resource 
	 * @param resource the resource to remove
	 * @param properties the service properties
	 */
	public void removeResource(Object resource, Map<String, Object> properties) {
		dispatcher.removeResource(resource, properties);
	}

	/**
	 * Adds a new extension
	 * @param extension the extension to add
	 * @param properties the service properties
	 */
	@Reference(name="extension", cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC, unbind="removeExtension", target="(" + JaxRSWhiteboardConstants.JAX_RS_EXTENSION + "='true')")
	public void addExtension(Object extension, Map<String, Object> properties) {
		dispatcher.addExtension(extension, properties);
	}

	/**
	 * Removes an extension 
	 * @param extension the extension to remove
	 * @param properties the service properties
	 */
	public void removeExtension(Object extension, Map<String, Object> properties) {
		dispatcher.removeExtension(extension, properties);
	}

}
