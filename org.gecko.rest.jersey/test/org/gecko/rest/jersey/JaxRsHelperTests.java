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
package org.gecko.rest.jersey;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.gecko.rest.jersey.helper.JaxRsHelper;
import org.junit.jupiter.api.Test;

/**
 * 
 * @author Mark Hoffmann
 * @since 21.09.2017
 */
public class JaxRsHelperTests {

	@Test
	public void testToServletPath() {
		assertEquals("/*", JaxRsHelper.toServletPath(null));
		assertEquals("/*", JaxRsHelper.toServletPath(""));
		assertEquals("/*", JaxRsHelper.toServletPath("/"));
		assertEquals("/test/*", JaxRsHelper.toServletPath("/test"));
		assertEquals("/test/*", JaxRsHelper.toServletPath("test"));
		assertEquals("/test/*", JaxRsHelper.toServletPath("/test/*"));
		assertEquals("/test/*", JaxRsHelper.toServletPath("test/*"));
	}
	
	@Test
	public void testToApplicationPath() {
		assertEquals("*", JaxRsHelper.toApplicationPath(null));
		assertEquals("*", JaxRsHelper.toApplicationPath(""));
		assertEquals("*", JaxRsHelper.toApplicationPath("/"));
		assertEquals("test/*", JaxRsHelper.toApplicationPath("/test"));
		assertEquals("test/*", JaxRsHelper.toApplicationPath("test"));
		assertEquals("test/*", JaxRsHelper.toApplicationPath("/test/*"));
		assertEquals("test/*", JaxRsHelper.toApplicationPath("test/*"));
	}

}
