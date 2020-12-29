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
package org.gecko.rest.jersey.runtime.common;

import java.util.LinkedList;
import java.util.List;

import org.gecko.rest.jersey.factories.InjectableFactory;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * Wrapps the Resource Config and allows a hacky mechanism to delegate InjectionManager from the 
 * {@link ServletContainer} {@link ApplicationHandler} to the {@link InjectableFactory}
 * 
 * @author Mark Hoffmann
 * @author Juergen Albert
 * @since 2 Dec 2020
 */
public class ResourceConfigWrapper {

	public ResourceConfig config;
	
	public List<InjectableFactory<?>> factories = new LinkedList<>();
	
	public void setInjectionManager(InjectionManager manager) {
		factories.forEach(f -> f.setInjectionManager(manager));
	}
	
}
