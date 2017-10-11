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

import org.osgi.service.jaxrs.runtime.dto.ExtensionDTO;

/**
 * Provider interface for JaxRs extensions
 * @author Mark Hoffmann
 * @since 11.10.2017
 */
public interface JaxRsExtensionProvider extends JaxRsApplicationContentProvider {

	/**
	 * Returns <code>true</code>, if the provider contains a valid extension, otherwise <code>false</code>
	 * @return <code>true</code>, if the provider contains a valid extension, otherwise <code>false</code>
	 */
	public boolean isExtension();
	
	/**
	 * Returns the extension DTO or failed extension DTO
	 * @return the extension DTO or failed extension DTO
	 */
	public ExtensionDTO getExtensionDTO();
}
