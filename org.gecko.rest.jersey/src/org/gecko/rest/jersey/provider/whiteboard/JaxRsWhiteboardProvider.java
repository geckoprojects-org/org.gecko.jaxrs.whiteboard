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
package org.gecko.rest.jersey.provider.whiteboard;

import java.util.Map;

import org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

/**
 * Provider for a whiteboard component
 * @author Mark Hoffmann
 * @since 11.10.2017
 */
public interface JaxRsWhiteboardProvider {
	
	/**
	 * Initializes the whiteboard
	 * @param context the component context
	 * @throws ConfigurationException thrown on mis-configuration
	 */
	public void initialize(ComponentContext context) throws ConfigurationException;
	
	/**
	 * Called on whiteboard modification
	 * @param context the component context
	 * @throws ConfigurationException thrown on mis-configuration
	 */
	public void modified(ComponentContext context) throws ConfigurationException;
	
	/**
	 * Starts the whiteboard
	 */
	public void startup();
	
	/**
	 * Tears the whiteboard down
	 */
	public void teardown();
	
	/**
	 * Returns all urls that belong to the handler
	 * @param context the component context
	 * @return an array of URLs
	 */
	public String[] getURLs(ComponentContext context);
	
	/**
	 * Returns a map with whiteboard properties or an empty map
	 * @return a map with whiteboard properties or an empty map
	 */
	public Map<String, Object> getProperties();
	
	/**
	 * Returns the whiteboard name
	 * @return the whiteboard name
	 */
	public String getName();
	
	/**
	 * Registers a new application, that is contained in the application provider
	 * @param provider the application provider to register
	 */
	public void registerApplication(JaxRsApplicationProvider provider);
	
	/**
	 * Unregisters an application, contained in the application provider 
	 * @param provider the application provider to be removed
	 */
	public void unregisterApplication(JaxRsApplicationProvider provider);

	/**
	 * Reloads the application contained in the application provider
	 * @param provider the application to be reloaded
	 */
	public void reloadApplication(JaxRsApplicationProvider provider);
	
	/**
	 * Returns <code>true</code>, if the given application was already registered
	 * @param provider the application provider to check 
	 * @return <code>true</code>, if the given application was already registered
	 */
	public boolean isRegistered(JaxRsApplicationProvider provider);
	



}
