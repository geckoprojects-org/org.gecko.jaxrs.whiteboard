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
package org.gecko.rest.jersey;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Application;

import org.gecko.rest.jersey.helper.JerseyHelper;
import org.junit.jupiter.api.Test;

/**
 * 
 * @author Mark Hoffmann
 * @since 21.10.2017
 */
public class JerseyHelperTests {
	
	private static class TestApplication extends Application {
		
		private Set<Class<?>> classes = new HashSet<>();
		private Set<Object> singletons = new HashSet<>();
		private Map<String, Object> properties = new HashMap<>();
		
		@Override
		public Set<Class<?>> getClasses() {
			return classes;
		}
		
		@Override
		public Set<Object> getSingletons() {
			return singletons;
		}
		
		@Override
		public Map<String, Object> getProperties() {
			return properties;
		}
		
	}
	
	/**
	 * Test method for {@link org.gecko.rest.jersey.helper.JerseyHelper#isEmpty(jakarta.ws.rs.core.Application)}.
	 */
	@Test
	public void testIsEmpty() {
		assertTrue(JerseyHelper.isEmpty(new TestApplication()));
		TestApplication app = new TestApplication();
	
		app.getProperties().put("test", "me");
		assertTrue(JerseyHelper.isEmpty(app));
		app.getProperties().clear();
		assertTrue(JerseyHelper.isEmpty(app));

		app.getClasses().add(String.class);
		assertFalse(JerseyHelper.isEmpty(app));
		app.getClasses().clear();
		assertTrue(JerseyHelper.isEmpty(app));
		
		app.getSingletons().add(new String("test"));
		assertFalse(JerseyHelper.isEmpty(app));
		app.getSingletons().clear();
		assertTrue(JerseyHelper.isEmpty(app));
	}

}
