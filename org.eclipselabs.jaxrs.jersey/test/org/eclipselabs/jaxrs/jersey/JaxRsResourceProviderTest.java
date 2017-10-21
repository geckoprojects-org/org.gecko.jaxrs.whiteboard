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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Application;

import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsResourceProvider;
import org.eclipselabs.jaxrs.jersey.resources.TestApplication;
import org.eclipselabs.jaxrs.jersey.resources.TestResource;
import org.eclipselabs.jaxrs.jersey.runtime.application.JerseyApplicationProvider;
import org.eclipselabs.jaxrs.jersey.runtime.application.JerseyResourceProvider;
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
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new Application(), applicationProperties);
		
		ApplicationDTO dto = provider.getApplicationDTO();
		assertFalse(dto instanceof FailedApplicationDTO);
		
		assertEquals("test/*", provider.getPath());
		assertEquals("test", provider.getName());
		
		Map<String, Object> resourceProperties = new HashMap<>();
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		
		JaxRsResourceProvider resourceProvider = new JerseyResourceProvider<TestResource>(new TestResource(), resourceProperties);
		
		ResourceDTO resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		assertNotNull(resourceProvider.getName());
		assertEquals(TestResource.class, resourceProvider.getObjectClass());
		
		assertFalse(resourceProvider.canHandleApplication(provider));
		assertFalse(provider.addResource(resourceProvider));
		
		// invalid application filter
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "test");
		resourceProvider = new JerseyResourceProvider<TestResource>(new TestResource(), resourceProperties);
		
		assertFalse(resourceProvider.canHandleApplication(provider));
		assertFalse(provider.addResource(resourceProvider));
		
		resourceDto = resourceProvider.getResourceDTO();
		assertTrue(resourceDto instanceof FailedResourceDTO);
		FailedResourceDTO failedDto = (FailedResourceDTO) resourceDto;
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedDto.failureReason);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		// application filter does not match
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(name=xy)");
		resourceProvider = new JerseyResourceProvider<TestResource>(new TestResource(), resourceProperties);
		
		assertFalse(resourceProvider.canHandleApplication(provider));
		assertFalse(provider.addResource(resourceProvider));
		
		resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		// application filter matches
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxRSWhiteboardConstants.JAX_RS_NAME + "=test)");
		resourceProvider = new JerseyResourceProvider<TestResource>(new TestResource(), resourceProperties);
		
		assertTrue(resourceProvider.canHandleApplication(provider));
		assertTrue(provider.addResource(resourceProvider));
		resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
	}
	
	@Test
	public void testApplicationProviderDefaultApplication() {
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(".default", new Application(), "/");
		
		ApplicationDTO dto = provider.getApplicationDTO();
		assertFalse(dto instanceof FailedApplicationDTO);
		
		assertEquals("*", provider.getPath());
		assertEquals(".default", provider.getName());
		assertTrue(provider.isDefault());
		assertFalse(provider.isLegacy());
		
		JaxRsResourceProvider resourceProvider = new JerseyResourceProvider<TestResource>(new TestResource(), Collections.emptyMap());
		ResourceDTO resourceDto = resourceProvider.getResourceDTO();
		assertTrue(resourceDto instanceof FailedResourceDTO);
		FailedResourceDTO failedDto = (FailedResourceDTO) resourceDto;
		assertEquals(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE, failedDto.failureReason);
		assertFalse(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		Map<String, Object> resourceProperties = new HashMap<>();
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		
		resourceProvider = new JerseyResourceProvider<TestResource>(new TestResource(), resourceProperties);
		
		resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		assertTrue(resourceProvider.canHandleApplication(provider));
		assertTrue(provider.addResource(resourceProvider));
		
		assertNotNull(resourceProvider.getName());
		assertEquals(TestResource.class, resourceProvider.getObjectClass());
	}
	
	@Test
	public void testApplicationProviderDefaultApplicationLegacy() {
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(".default", new TestApplication(), "/");
		
		ApplicationDTO dto = provider.getApplicationDTO();
		assertFalse(dto instanceof FailedApplicationDTO);
		
		assertEquals("*", provider.getPath());
		assertEquals(".default", provider.getName());
		assertTrue(provider.isDefault());
		assertFalse(provider.isLegacy());
		
		JaxRsResourceProvider resourceProvider = new JerseyResourceProvider<TestResource>(new TestResource(), Collections.emptyMap());
		ResourceDTO resourceDto = resourceProvider.getResourceDTO();
		assertTrue(resourceDto instanceof FailedResourceDTO);
		FailedResourceDTO failedDto = (FailedResourceDTO) resourceDto;
		assertEquals(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE, failedDto.failureReason);
		assertFalse(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		Map<String, Object> resourceProperties = new HashMap<>();
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		
		resourceProvider = new JerseyResourceProvider<TestResource>(new TestResource(), resourceProperties);
		
		resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		assertTrue(resourceProvider.canHandleApplication(provider));
		assertTrue(provider.addResource(resourceProvider));
		
		assertNotNull(resourceProvider.getName());
		assertEquals(TestResource.class, resourceProvider.getObjectClass());
	}
	
	@Test
	public void testResourceProviderPrototype() {
		
		JaxRsResourceProvider resourceProvider = new JerseyResourceProvider<TestResource>(new TestResource(), Collections.emptyMap());
		ResourceDTO resourceDto = resourceProvider.getResourceDTO();
		assertTrue(resourceDto instanceof FailedResourceDTO);
		FailedResourceDTO failedDto = (FailedResourceDTO) resourceDto;
		assertEquals(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE, failedDto.failureReason);
		assertFalse(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		Map<String, Object> resourceProperties = new HashMap<>();
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		
		resourceProvider = new JerseyResourceProvider<TestResource>(new TestResource(), resourceProperties);
		
		resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		assertNotNull(resourceProvider.getName());
		assertEquals(TestResource.class, resourceProvider.getObjectClass());
		
		resourceProperties.put("service.scope", "prototype");
		
		resourceProvider = new JerseyResourceProvider<TestResource>(new TestResource(), resourceProperties);
		
		resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertFalse(resourceProvider.isSingleton());
		
		assertNotNull(resourceProvider.getName());
		assertEquals(TestResource.class, resourceProvider.getObjectClass());
				
	}
	
	@Test
	public void testResourceProviderName() {

		JaxRsResourceProvider resourceProvider = new JerseyResourceProvider<TestResource>(new TestResource(), Collections.emptyMap());
		ResourceDTO resourceDto = resourceProvider.getResourceDTO();
		assertTrue(resourceDto instanceof FailedResourceDTO);
		FailedResourceDTO failedDto = (FailedResourceDTO) resourceDto;
		assertEquals(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE, failedDto.failureReason);
		assertFalse(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		Map<String, Object> resourceProperties = new HashMap<>();
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		
		resourceProvider = new JerseyResourceProvider<TestResource>(new TestResource(), resourceProperties);
		
		resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		assertNotNull(resourceProvider.getName());
		assertNotEquals("test", resourceProvider.getName());
		assertEquals(TestResource.class, resourceProvider.getObjectClass());
		
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "test");
		
		resourceProvider = new JerseyResourceProvider<TestResource>(new TestResource(), resourceProperties);
		
		resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		assertEquals("test", resourceProvider.getName());
		
		assertNotNull(resourceProvider.getName());
		assertEquals(TestResource.class, resourceProvider.getObjectClass());
		
	}
	
}
