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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipselabs.osgi.jersey.application.JerseyApplicationProvider;
import org.eclipselabs.osgi.jersey.application.JerseyResource;
import org.eclipselabs.osgi.jersey.resources.TestApplication;
import org.eclipselabs.osgi.jersey.resources.TestResource;
import org.junit.Test;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.runtime.dto.FailedApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceDTO;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * 
 * @author Mark Hoffmann
 * @since 21.09.2017
 */
public class JaxRsResourceProviderTest {

	@Test
	public void testApplicationSelect() {
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
		
		JaxRsResourceProvider resourceProvider = new JerseyResource(new TestResource(), resourceProperties);
		
		ResourceDTO resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		assertNotNull(resourceProvider.getName());
		assertEquals(TestResource.class, resourceProvider.getResourceClass());
		
		assertFalse(resourceProvider.canHandleApplication(provider));
		
		// invalid application filter
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "test");
		resourceProvider = new JerseyResource(new TestResource(), resourceProperties);
		
		assertFalse(resourceProvider.canHandleApplication(provider));
		resourceDto = resourceProvider.getResourceDTO();
		assertTrue(resourceDto instanceof FailedResourceDTO);
		FailedResourceDTO failedDto = (FailedResourceDTO) resourceDto;
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedDto.failureReason);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		// application filter does not match
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(name=xy)");
		resourceProvider = new JerseyResource(new TestResource(), resourceProperties);
		
		assertFalse(resourceProvider.canHandleApplication(provider));
		resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		// application filter matches
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxRSWhiteboardConstants.JAX_RS_NAME + "=test)");
		resourceProvider = new JerseyResource(new TestResource(), resourceProperties);
		
		assertTrue(resourceProvider.canHandleApplication(provider));
		resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
	}
	
	@Test
	public void testApplicationProviderDefaultApplication() {
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(".default", new TestApplication(), "/");
		
		ApplicationDTO dto = provider.getApplicationDTO();
		assertFalse(dto instanceof FailedApplicationDTO);
		
		assertEquals("*", provider.getPath());
		assertEquals(".default", provider.getName());
		assertTrue(provider.isDefault());
		
		JaxRsResourceProvider resourceProvider = new JerseyResource(new TestResource(), Collections.emptyMap());
		ResourceDTO resourceDto = resourceProvider.getResourceDTO();
		assertTrue(resourceDto instanceof FailedResourceDTO);
		FailedResourceDTO failedDto = (FailedResourceDTO) resourceDto;
		assertEquals(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE, failedDto.failureReason);
		assertFalse(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		Map<String, Object> resourceProperties = new HashMap<>();
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		
		resourceProvider = new JerseyResource(new TestResource(), resourceProperties);
		
		resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		assertTrue(resourceProvider.canHandleApplication(provider));
		
		assertNotNull(resourceProvider.getName());
		assertEquals(TestResource.class, resourceProvider.getResourceClass());
	}
	
	@Test
	public void testResourceProviderPrototype() {
		
		JaxRsResourceProvider resourceProvider = new JerseyResource(new TestResource(), Collections.emptyMap());
		ResourceDTO resourceDto = resourceProvider.getResourceDTO();
		assertTrue(resourceDto instanceof FailedResourceDTO);
		FailedResourceDTO failedDto = (FailedResourceDTO) resourceDto;
		assertEquals(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE, failedDto.failureReason);
		assertFalse(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		Map<String, Object> resourceProperties = new HashMap<>();
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		
		resourceProvider = new JerseyResource(new TestResource(), resourceProperties);
		
		resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		assertNotNull(resourceProvider.getName());
		assertEquals(TestResource.class, resourceProvider.getResourceClass());
		
		resourceProperties.put("service.scope", "prototype");
		
		resourceProvider = new JerseyResource(new TestResource(), resourceProperties);
		
		resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertFalse(resourceProvider.isSingleton());
		
		assertNotNull(resourceProvider.getName());
		assertEquals(TestResource.class, resourceProvider.getResourceClass());
				
	}
	
	@Test
	public void testResourceProviderName() {

		JaxRsResourceProvider resourceProvider = new JerseyResource(new TestResource(), Collections.emptyMap());
		ResourceDTO resourceDto = resourceProvider.getResourceDTO();
		assertTrue(resourceDto instanceof FailedResourceDTO);
		FailedResourceDTO failedDto = (FailedResourceDTO) resourceDto;
		assertEquals(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE, failedDto.failureReason);
		assertFalse(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		Map<String, Object> resourceProperties = new HashMap<>();
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		
		resourceProvider = new JerseyResource(new TestResource(), resourceProperties);
		
		resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		assertNotNull(resourceProvider.getName());
		assertNotEquals("test", resourceProvider.getName());
		assertEquals(TestResource.class, resourceProvider.getResourceClass());
		
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "test");
		
		resourceProvider = new JerseyResource(new TestResource(), resourceProperties);
		
		resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		assertEquals("test", resourceProvider.getName());
		
		assertNotNull(resourceProvider.getName());
		assertEquals(TestResource.class, resourceProvider.getResourceClass());
		
	}
	
}
