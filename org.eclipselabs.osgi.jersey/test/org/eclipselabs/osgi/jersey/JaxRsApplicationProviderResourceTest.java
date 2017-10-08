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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Application;

import org.eclipselabs.osgi.jersey.application.JerseyApplicationProvider;
import org.eclipselabs.osgi.jersey.resources.TestApplication;
import org.eclipselabs.osgi.jersey.resources.TestResource;
import org.junit.Test;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedApplicationDTO;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * 
 * @author Mark Hoffmann
 * @since 21.09.2017
 */
public class JaxRsApplicationProviderResourceTest {

	@Test
	public void testApplicationProviderWithoutResourceProperty() {
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("something", "else");
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(hallo=bla)");
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "test");
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new TestApplication(), applicationProperties);
		
		ApplicationDTO dto = provider.getApplicationDTO();
		assertFalse(dto instanceof FailedApplicationDTO);
		
		assertEquals("test/*", provider.getPath());
		assertEquals(".test", provider.getName());
		
		assertFalse(provider.addResource(new TestResource(), Collections.emptyMap()));
	}
	
	@Test
	public void testApplicationProviderApplicationSelect() {
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("something", "else");
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(hallo=bla)");
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "test");
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "test");
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new TestApplication(), applicationProperties);
		
		ApplicationDTO dto = provider.getApplicationDTO();
		assertFalse(dto instanceof FailedApplicationDTO);
		
		assertEquals("test/*", provider.getPath());
		assertEquals("test", provider.getName());
		
		Map<String, Object> resourceProperties = new HashMap<>();
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		
		assertFalse(provider.addResource(new TestResource(), resourceProperties));
		
		// invalid application filter
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "test");
		assertFalse(provider.addResource(new TestResource(), resourceProperties));
		
		// application filter does not match
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(name=xy)");
		assertFalse(provider.addResource(new TestResource(), resourceProperties));
		
		// application filter matches
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxRSWhiteboardConstants.JAX_RS_NAME + "=test)");
		assertTrue(provider.addResource(new TestResource(), resourceProperties));
	}
	
	@Test
	public void testApplicationProviderDefaultApplication() {
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(".default", new TestApplication(), "/");
		
		ApplicationDTO dto = provider.getApplicationDTO();
		assertFalse(dto instanceof FailedApplicationDTO);
		
		assertEquals("*", provider.getPath());
		assertEquals(".default", provider.getName());
		
		assertFalse(provider.addResource(new TestResource(), Collections.emptyMap()));
		
		Map<String, Object> resourceProperties = new HashMap<>();
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		
		assertFalse(provider.addResource(new TestResource(), resourceProperties));
	}
	
	@Test
	public void testApplicationProviderWithLegacyApplication() {
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("something", "else");
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(hallo=bla)");
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "test");
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new Application(), applicationProperties);
		
		ApplicationDTO dto = provider.getApplicationDTO();
		assertFalse(dto instanceof FailedApplicationDTO);
		
		assertEquals("test/*", provider.getPath());
		assertEquals(".test", provider.getName());
		
		assertFalse(provider.addResource(new TestResource(), Collections.emptyMap()));
		
		Map<String, Object> resourceProperties = new HashMap<>();
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		
		assertFalse(provider.addResource(new TestResource(), resourceProperties));
		
	}
	
}
