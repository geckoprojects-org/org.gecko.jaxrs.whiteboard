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
package org.eclipselabs.osgi.jersey;

import org.glassfish.jersey.server.ResourceConfig;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntime;

/**
 * 
 * @author Mark Hoffmann
 * @since 30.07.2017
 */
public interface JaxRsJerseyHandler {
	
	/**
	 * Initializes the {@link JaxRsJerseyHandler}
	 * @param context the component context
	 * @throws ConfigurationException thrown on mis-configuration
	 */
	public void initialize(ComponentContext context) throws ConfigurationException;
	
	/**
	 * Called on handler modification
	 * @param context the component context
	 * @throws ConfigurationException thrown on mis-configuration
	 */
	public void modified(ComponentContext context) throws ConfigurationException;
	
	/**
	 * Starts the handler
	 */
	public void startup();
	
	/**
	 * Tears the handler down
	 */
	public void teardown();
	
	/**
	 * Returns all urls that belong to the handler
	 * @param context the component context
	 * @return an array of URLs
	 */
	public String[] getURLs(ComponentContext context);
	
	/**
	 * Registers a JaxRs {@link JaxRsApplicationProvider} to the Jersey servlet
	 * @param applicationProvider the JaxRs application provider
	 */
	public void registerApplication(JaxRsApplicationProvider applicationProvider);
	
	/**
	 * Un-registers a JaxRs {@link JaxRsApplicationProvider} from the Jersey servlet
	 * @param applicationProvider the JaxRs application provider
	 */
	public void unregisterApplication(JaxRsApplicationProvider applicationProvider);
	
	/**
	 * This method forces a Jersey servlet to be reloaded. For that a new {@link ResourceConfig} will be
	 * created from the given application provider
	 * @param applicationProvider the application  rpoviderto be reloaded
	 */
	public void reloadApplication(JaxRsApplicationProvider applicationProvider);
	
	/**
	 * Update the runtime dto
	 * @param serviceRef the service reference of the {@link JaxRSServiceRuntime}
	 */
	public void updateRuntimeDTO(ServiceReference<?> serviceRef);

}
