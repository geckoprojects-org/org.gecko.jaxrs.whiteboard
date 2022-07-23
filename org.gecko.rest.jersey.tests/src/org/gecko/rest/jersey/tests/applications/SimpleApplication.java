/**
 * Copyright (c) 2012 - 2022 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 *     Stefan Bishof - API and implementation
 *     Tim Ward - implementation
 */
package org.gecko.rest.jersey.tests.applications;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

/**
 * 
 * @author ilenia
 * @since Jun 11, 2020
 */
public class SimpleApplication extends Application {

	private final Set<Class< ? >>	classes		= new HashSet<>();

	private final Set<Object>		singletons	= new HashSet<>();

	public SimpleApplication(Set<Class< ? >> classes, Set<Object> singletons) {
		this.classes.addAll(classes);
		this.singletons.addAll(singletons);
	}

	@Override
	public Set<Class< ? >> getClasses() {
		return new HashSet<>(classes);
	}

	@Override
	public Set<Object> getSingletons() {
		return new HashSet<>(singletons);
	}

}
