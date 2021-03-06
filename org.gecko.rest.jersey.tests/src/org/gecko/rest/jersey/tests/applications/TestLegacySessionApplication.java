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
package org.gecko.rest.jersey.tests.applications;

import java.util.Collections;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.gecko.rest.jersey.tests.resources.SessionManipulator;

/**
 * 
 * @author Mark Hoffmann
 * @since 13.10.2017
 */
public class TestLegacySessionApplication extends Application {
	
	SessionManipulator sessionManipulator = new SessionManipulator();
	
	@Override
	public Set<Object> getSingletons() {
//		return Collections.singleton(new SessionManipulator());
		return Collections.singleton(sessionManipulator);
	}
}
