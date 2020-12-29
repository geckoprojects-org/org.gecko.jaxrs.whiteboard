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
package org.gecko.rest.jersey.tests.whiteboard.applications;

import java.util.Collections;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.gecko.rest.jersey.tests.whiteboard.resources.LegacyResource;

/**
 * 
 * @author Mark Hoffmann
 * @since 13.10.2017
 */
public class TestLegacyApplication extends Application {
	
	@Override
	public Set<Class<?>> getClasses() {
		return Collections.singleton(LegacyResource.class);
	}

}
