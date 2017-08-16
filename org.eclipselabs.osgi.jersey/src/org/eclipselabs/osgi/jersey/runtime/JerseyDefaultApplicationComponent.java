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
package org.eclipselabs.osgi.jersey.runtime;

import java.util.Map;

import org.eclipselabs.osgi.jersey.JaxRsApplicationProvider;
import org.eclipselabs.osgi.jersey.application.JerseyApplication;
import org.eclipselabs.osgi.jersey.application.JerseyApplicationProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * Implementation of the Application Provider
 * @author Mark Hoffmann
 * @since 30.07.2017
 */
@Component(name="defaultApplication", configurationPolicy=ConfigurationPolicy.REQUIRE, service=JaxRsApplicationProvider.class)
public class JerseyDefaultApplicationComponent extends JerseyApplicationProvider {
	
	public JerseyDefaultApplicationComponent() {
		super(".default", null);
	}
	
	/**
	 * Called on component activation
	 * @param properties the component properties
	 * @param context the bundle context
	 */
	@Activate
	public void activate(Map<String, Object> properties, BundleContext context) {
		setApplication(new JerseyApplication(".default", context));
	}

	/**
	 * Adds a service reference for a resource to the default application
	 * @param resourceRef the reference to register
	 */
	@Reference(name="jaxrsResource", policy=ReferencePolicy.DYNAMIC,cardinality=ReferenceCardinality.MULTIPLE, target="(&(" + JaxRSWhiteboardConstants.JAX_RS_RESOURCE + "=true)(!(" + JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET + "=*)))")
	public void addResourceReference(ServiceReference<?> resourceRef) {
		super.addResourceReference(resourceRef);
	}
	
	/**
	 * Removed a resource service reference from the default application
	 * @param resourceRef the service reference of the resource to be removed
	 */
	public void removeResourceReference(ServiceReference<?> resourceRef) {
		super.removeResourceReference(resourceRef);
	}
	
	/**
	 * Adds a service reference for a extension to the default application
	 * @param extensionRef the reference to register
	 */
	@Reference(name="jaxrsExtension", policy=ReferencePolicy.DYNAMIC,cardinality=ReferenceCardinality.MULTIPLE, target="(&(" + JaxRSWhiteboardConstants.JAX_RS_EXTENSION + "=true)(!(" + JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET + "=*)))")
	public void addExtensionReference(ServiceReference<?> extensionRef) {
		super.addExtensionReference(extensionRef);
	}
	
	/**
	 * Removes a extension service reference from the default application
	 * @param extensionRef the service reference of the extension to be removed
	 */
	public void removeExtensionReference(ServiceReference<?> extensionRef) {
		super.removeExtensionReference(extensionRef);
	}

}
