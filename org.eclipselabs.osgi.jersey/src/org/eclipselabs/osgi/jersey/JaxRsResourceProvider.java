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

import org.osgi.service.jaxrs.runtime.dto.FailedResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceDTO;

/**
 * Provider interface for JaxRs resources
 * @author Mark Hoffmann
 * @since 09.10.2017
 */
public interface JaxRsResourceProvider extends JaxRsConstants {
	
	/**
	 * Returns the application name which targets to the property osgi.jaxrs.name
	 * @return the application name
	 */
	public String getName();
	
	/**
	 * Returns <code>true</code>, if the given resource is valid and contains the resource properties
	 * @return <code>true</code>, if the given resource is valid and contains the resource properties
	 */
	public boolean isResource();
	
	/**
	 * Returns <code>true</code>, if this resource is a singleton service
	 * @return <code>true</code>, if this resource is a singleton service
	 */
	public boolean isSingleton();
	/**
	 * Returns the resource properties or an empty map
	 * @return the resource properties or an empty map
	 */
	public Map<String, Object> getResourceProperties();
	
	/**
	 * Returns the class of the resource
	 * @return the class of the resource
	 */
	public Class<?> getResourceClass();
	
	/**
	 * Returns the resource instance
	 * @return the resource instance
	 */
	public Object getResource();
	
	/**
	 * Returns <code>true</code>, if this resource can handle the given properties.
	 * If the resource contains a application select, than the properties are checked against
	 * the select filter and returns the result.
	 * If the resource has no application select filter, the method returns <code>true</code>, if it is the default application
	 * @param application the application provider
	 * @return <code>true</code>, if the resource can be handled by a whiteboard runtime with the given properties
	 */
	public boolean canHandleApplication(JaxRsApplicationProvider application);
	
	/**
	 * Returns the {@link ResourceDTO} for this JaxRsResource.
	 * In case of an error a {@link FailedResourceDTO} instance will be returned
	 * @return the {@link ResourceDTO} or {@link FailedResourceDTO} for this JaxRsResource
	 */
	public ResourceDTO getResourceDTO();
	
}
