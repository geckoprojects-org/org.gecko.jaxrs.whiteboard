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

import org.osgi.service.jaxrs.runtime.dto.BaseDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceDTO;

/**
 * Provider interface for JaxRs resources
 * @author Mark Hoffmann
 * @since 09.10.2017
 */
public interface JaxRsResourceProvider extends JaxRsApplicationContentProvider {
	
	/**
	 * Returns <code>true</code>, if the given resource is valid and contains the resource properties
	 * @return <code>true</code>, if the given resource is valid and contains the resource properties
	 */
	public boolean isResource();
	
	/**
	 * Returns the {@link ResourceDTO} or {@link FailedResourceDTO} as {@link BaseDTO} for this JaxRsResource.
	 * In case of an error a {@link FailedResourceDTO} instance will be returned
	 * @return the {@link ResourceDTO} or {@link FailedResourceDTO} for this JaxRsResource
	 */
	public BaseDTO getResourceDTO();
	
}
