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

/**
 * Base interface that provides basic provider information
 * 
 * @author Mark Hoffmann
 * @since 11.10.2017
 */
public interface JaxRsProvider {
	
	/**
	 * Returns the application name which targets to the property osgi.jaxrs.name
	 * @return the application name
	 */
	public String getName();
	
	/**
	 * Returns the service id, component id or <code>null</code>
	 * @return the service id, component id or <code>null</code>
	 */
	public Long getServiceId();
	
	/**
	 * Returns the providers properties, which are usually the service properties or an empty map
	 * @return the providers properties, which are usually the service properties or an empty map
	 */
	public Map<String, Object> getProviderProperties();
	
	/**
	 * Returns <code>true</code>, if this application can handle the given properties.
	 * If the application contains a whiteboard target select, than the properties are checked against
	 * the select filter and returns the result.
	 * If the application has no whiteboard select filter, the method returns <code>true</code>
	 * @param runtimeProperties the properties of the whiteboard runtime
	 * @return <code>true</code>, if the application can be handled by a whiteboard runtime with the given properties
	 */
	public boolean canHandleWhiteboard(Map<String, Object> runtimeProperties);

}
