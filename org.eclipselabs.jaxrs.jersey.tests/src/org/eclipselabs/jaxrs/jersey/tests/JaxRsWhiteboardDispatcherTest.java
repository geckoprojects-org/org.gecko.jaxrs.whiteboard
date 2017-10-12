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
package org.eclipselabs.jaxrs.jersey.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsWhiteboardDispatcher;
import org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider;
import org.eclipselabs.jaxrs.jersey.runtime.dispatcher.JerseyWhiteboardDispatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * Tests the whiteboard dispatcher
 * @author Mark Hoffmann
 * @since 12.10.2017
 */
@RunWith(MockitoJUnitRunner.class)
public class JaxRsWhiteboardDispatcherTest {

	@Mock
	private JaxRsWhiteboardProvider whiteboard;
	private final BundleContext context = FrameworkUtil.getBundle(JaxRsWhiteboardDispatcherTest.class).getBundleContext();
	
	/**
	 * Tests 
	 */
	@Test
	public void testDispatcherNotReady() {
		JaxRsWhiteboardDispatcher dispatcher = new JerseyWhiteboardDispatcher();
		assertFalse(dispatcher.isDispatching());
	}

}
