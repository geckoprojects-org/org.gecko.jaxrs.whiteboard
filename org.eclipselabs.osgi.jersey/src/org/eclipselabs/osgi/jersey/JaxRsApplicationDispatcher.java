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

import java.util.Set;

import javax.ws.rs.core.Application;

import org.osgi.framework.ServiceReference;

/**
 * Interface for an application dispatcher
 * @author Mark Hoffmann
 * @since 28.08.2017
 */
public interface JaxRsApplicationDispatcher {
	
	/**
	 * Returns all cached applications of the dispatcher or an empty set
	 * @return all cached applications of the dispatcher or an empty set
	 */
	public Set<ServiceReference<Application>> getApplications();
	
	/**
	 * Returns all cached resources and extensions of the dispatcher or an empty set
	 * @return all cached resources and extensions of the dispatcher or an empty set
	 */
	public Set<ServiceReference<?>> getResources();
	/**
	 * Returns all cached runtimes of the dispatcher or an empty set
	 * @return all cached runtimes of the dispatcher or an empty set
	 */
	public Set<JaxRsJerseyRuntime> getRuntimes();

}
