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
package org.gecko.rest.jersey.provider.application;

import java.util.Map;

import org.glassfish.jersey.internal.inject.InjectionManager;
import org.osgi.service.jaxrs.runtime.dto.BaseExtensionDTO;

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
	public BaseExtensionDTO getExtensionDTO();
	
	/**
	 * Returns the contracts under which the Extensions have to be registered. Can be <code>null</code> if
	 * no specific contracts are provided. In this case the JAX-RS implementation has to scan the class for 
	 * Annotations and Interfaces
	 * @return the Array of Classes
	 */
	public Class<?>[] getContracts();
	
	/**
	 * Returns an Extension instance suitable for binding into an application. This extension must be
	 * disposed once it is finished with
	 * @param injectionManager 
	 * @return the extension
	 */
	public JaxRsExtension getExtension(InjectionManager injectionManager);
	
	public interface JaxRsExtension {
		
		/**
		 * Returns the contract priorities for this extension. The keys will match the values returned by
		 * {@link JaxRsExtensionProvider#getContracts()}
		 * @return the Contract priorities
		 */
		public Map<Class<?>, Integer> getContractPriorities();
		
		/**
		 * Get the extension object
		 */
		public Object getExtensionObject();
		
		/**
		 * Release the extension object
		 */
		public void dispose();
	}
}
