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
package org.eclipselabs.osgi.jaxrs.provider;

import java.util.Set;

/**
 * Provider interface that provides only classes for prototype scoped resources that are from the whiteboard
 * @author Mark Hoffmann
 * @since 12.07.2017
 */
public interface PrototypeResourceProvider {
	
	/**
	 * Returns all classes of prototype scoped resources registered as whiteboard or en empty set
	 * @return all classes of prototype scoped resources registered as whiteboard or en empty set
	 */
	public Set<Class<?>> getPrototypeResourceClasses();

}
