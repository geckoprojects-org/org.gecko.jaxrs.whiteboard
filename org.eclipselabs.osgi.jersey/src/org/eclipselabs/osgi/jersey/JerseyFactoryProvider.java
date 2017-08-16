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

import org.glassfish.hk2.api.Factory;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Provider component the creates HK2 factories for prototype scoped resources
 * @author Mark Hoffmann
 * @since 12.07.2017
 */
@ProviderType
public interface JerseyFactoryProvider {
	
	/**
	 * Creates a new resource instance factory for the given class 
	 * @param clazz the class for what we need a resource instance factory for
	 * @return a resource instance factory or <code>null</code>, if the class is not of a registered prototype service
	 */
	public <T> Factory<T> createResourceFactory(Class<T> clazz);
	
	/**
	 * Returns <code>true</code>, i the given class is a registered prototype resource 
	 * @param clazz the class to check for
	 * @return <code>true</code>, if the class is registered, otherwise <code>false</code>
	 */
	public <T> boolean isPrototypeResource(Class<T> clazz);

}
