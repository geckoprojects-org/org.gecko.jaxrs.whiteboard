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
package org.eclipselabs.jaxrs.jersey.provider.application;

import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;

/**
 * Dispatcher that handles the dynamic adding and removing of resources, extension and applications, that are
 * then delegated to the {@link JaxRsWhiteboardProvider}.
 * Only if the dispatch is active, the delegation to whiteboard provider is enabled
 * @author Mark Hoffmann
 * @since 12.10.2017
 */
public interface JaxRsWhiteboardDispatcher {
	
	/**
	 * #returns the batch mode
	 * @return the batch mode
	 */
	public boolean getBatchMode();
	
	/**
	 * Sets the batchMode
	 * @param batchMode <code>true</code> to handle dispatch manually
	 */
	public void setBatchMode(boolean batchMode);
	/**
	 * executes an manual dispatch, usually used if batchMode == true
	 */
	public void batchDispatch();
	
	/**
	 * Sets a whiteboard instance
	 * @param whiteboard the whiteboard to set
	 */
	public void setWhiteboardProvider(JaxRsWhiteboardProvider whiteboard);
	
	/**
	 * Returns the whiteboard provider instance
	 * @return the whiteboard provider instance
	 */
	public JaxRsWhiteboardProvider getWhiteboardProvider();
	
	/**
	 * Returns all applications or an empty set
	 * @return all applications or an empty set
	 */
	public Set<JaxRsApplicationProvider> getApplications();
	
	/**
	 * Returns all resources or an empty set
	 * @return all resources or an empty set
	 */
	public Set<JaxRsResourceProvider> getResources();
	
	/**
	 * Returns all extensions or an empty set
	 * @return all extensions or an empty set
	 */
	public Set<JaxRsExtensionProvider> getExtensions();
	
	/**
	 * Adds an application
	 * @param ref the {@link ServiceReference} of the {@link Application}
	 */
	public void addApplication(ServiceObjects<Application> appServiceObject, Map<String, Object> properties);
	
	/**
	 * Removes an application
	 * @param ref the {@link ServiceReference} of the {@link Application}
	 */
	public void removeApplication(Map<String, Object> properties);
	
	/**
	 * Adds a resource
	 * @param ref the {@link ServiceReference} of the Resource
	 */
	public void addResource(ServiceObjects<?> appServiceObject, Map<String, Object> properties);
	
	/**
	 * Removes a resource
	 * @param ref the {@link ServiceReference} of the Resource
	 */
	public void removeResource(Map<String, Object> properties);
	
	/**
	 * Adds an extension
	 * @param ref the {@link ServiceReference} of the Extension
	 */
	public void addExtension(ServiceObjects<?> appServiceObject, Map<String, Object> properties);

	/**
	 * Removes an extension
	 * @param ref the {@link ServiceReference} of the Resource
	 */
	public void removeExtension(Map<String, Object> properties);
	
	/**
	 * Activates dispatching
	 */
	public void dispatch();
	
	/**
	 * Deactivates dispatching
	 */
	public void deactivate();
	
	/**
	 * Returns <code>true</code>, if the dispatcher is currently working
	 * @return <code>true</code>, if the dispatcher is currently working
	 */
	public boolean isDispatching();

}
