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

import java.util.Map;

/**
 * Interface for the Jersey runtime
 * @author Mark Hoffmann
 * @since 28.08.2017
 */
public interface JaxRsJerseyRuntime {
	
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

}
