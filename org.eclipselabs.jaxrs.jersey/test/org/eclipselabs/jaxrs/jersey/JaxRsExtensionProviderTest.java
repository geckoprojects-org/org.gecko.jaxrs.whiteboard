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
package org.eclipselabs.jaxrs.jersey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsExtensionProvider;
import org.eclipselabs.jaxrs.jersey.resources.TestApplication;
import org.eclipselabs.jaxrs.jersey.resources.TestResource;
import org.eclipselabs.jaxrs.jersey.runtime.application.JerseyApplicationProvider;
import org.eclipselabs.jaxrs.jersey.runtime.application.JerseyExtensionProvider;
import org.junit.Test;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.ExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedExtensionDTO;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * Test the extension provider. Because it is extended from the resource provider, only the specific behavior is tested.
 * For all other Tests look at {@link JaxRsResourceProviderTest}
 * @author Mark Hoffmann
 * @since 21.09.2017
 */
public class JaxRsExtensionProviderTest {

	/**
	 * Test extension specific behavior 
	 */
	@Test
	public void testExtension() {
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
		
		JaxRsExtensionProvider resourceProvider = new JerseyExtensionProvider<TestResource>(new TestResource(), resourceProperties);
		
		ExtensionDTO resourceDto = resourceProvider.getExtensionDTO();
		assertTrue(resourceDto instanceof FailedExtensionDTO);
		assertFalse(resourceProvider.isExtension());
		assertTrue(resourceProvider.isSingleton());
		
		resourceProperties.clear();
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_EXTENSION, "true");
		resourceProvider = new JerseyExtensionProvider<TestResource>(new TestResource(), resourceProperties);
		
		resourceDto = resourceProvider.getExtensionDTO();
		assertFalse(resourceDto instanceof FailedExtensionDTO);
		assertTrue(resourceProvider.isExtension());
		assertTrue(resourceProvider.isSingleton());
		
	}
	
}