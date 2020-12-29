/**
 * Copyright (c) 2012 - 2020 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.gecko.rest.jersey.factories;

import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.internal.inject.InjectionManager;

/**
 * @param <T>
 * @author ungei
 * @since 2 Dec 2020
 */
public interface InjectableFactory<T> extends Factory<T> {

	/**
	 * Sets the injectionManager.
	 * @param injectionManager the injectionManager to set
	 */
	void setInjectionManager(InjectionManager injectionManager);

}