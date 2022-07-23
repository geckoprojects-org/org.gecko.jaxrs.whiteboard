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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Application;

import org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider;
import org.gecko.rest.jersey.provider.application.JaxRsResourceProvider;
import org.gecko.rest.jersey.resources.TestResource;
import org.gecko.rest.jersey.runtime.application.JerseyApplicationProvider;
import org.gecko.rest.jersey.runtime.application.JerseyResourceProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.ServiceObjects;
import org.osgi.service.jaxrs.runtime.dto.BaseApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.BaseDTO;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.runtime.dto.FailedApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedResourceDTO;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

/**
 * 
 * @author Mark Hoffmann
 * @since 21.09.2017
 */
@ExtendWith(MockitoExtension.class)
public class JaxRsResourceProviderTest {

	@Mock
	private ServiceObjects<Object> serviceObject;

	@Test
	public void testApplicationSelect() {
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("something", "else");
		applicationProperties.put(JaxrsWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(hallo=bla)");
		applicationProperties.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "test");
		applicationProperties.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "test");
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new Application(), applicationProperties);
		
		BaseApplicationDTO dto = provider.getApplicationDTO();
		assertFalse(dto instanceof FailedApplicationDTO);
		
		assertEquals("/test/*", provider.getPath());
		assertEquals("test", provider.getName());
		
		Map<String, Object> resourceProperties = new HashMap<>();
		resourceProperties.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		when(serviceObject.getService()).thenReturn(new TestResource());
		JaxRsResourceProvider resourceProvider = new JerseyResourceProvider<Object>(serviceObject, resourceProperties);
		
		BaseDTO resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		assertNotNull(resourceProvider.getName());
		assertEquals(TestResource.class, resourceProvider.getObjectClass());
		
//		In the current implementation the canHandleApplication is called before adding internally the resource,
//		but the addResource alone does not check the canHandleApplication thus it returns true also if the resource 
//		cannot handle the application
		assertFalse(resourceProvider.canHandleApplication(provider));
//		assertFalse(provider.addResource(resourceProvider));
		
		// invalid application filter
		resourceProperties.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "test");
		resourceProvider = new JerseyResourceProvider<Object>(serviceObject, resourceProperties);
		
		assertFalse(resourceProvider.canHandleApplication(provider));
//		assertFalse(provider.addResource(resourceProvider));
		
		resourceDto = resourceProvider.getResourceDTO();
		assertTrue(resourceDto instanceof FailedResourceDTO);
		FailedResourceDTO failedDto = (FailedResourceDTO) resourceDto;
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedDto.failureReason);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		// application filter does not match
		resourceProperties.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(name=xy)");
		resourceProvider = new JerseyResourceProvider<Object>(serviceObject, resourceProperties);
		
		assertFalse(resourceProvider.canHandleApplication(provider));
//		assertFalse(provider.addResource(resourceProvider));
		
		resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		// application filter matches
		resourceProperties.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=test)");
		resourceProvider = new JerseyResourceProvider<Object>(serviceObject, resourceProperties);
		
		assertTrue(resourceProvider.canHandleApplication(provider));
//		assertTrue(provider.addResource(resourceProvider));
		resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
	}
	
	@Test
	public void testResourceProviderPrototype() {
		
		when(serviceObject.getService()).thenReturn(new TestResource());
		JaxRsResourceProvider resourceProvider = new JerseyResourceProvider<Object>(serviceObject, Collections.emptyMap());
		BaseDTO resourceDto = resourceProvider.getResourceDTO();
		assertTrue(resourceDto instanceof FailedResourceDTO);
		FailedResourceDTO failedDto = (FailedResourceDTO) resourceDto;
		assertEquals(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE, failedDto.failureReason);
		assertFalse(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		Map<String, Object> resourceProperties = new HashMap<>();
		resourceProperties.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		
		resourceProvider = new JerseyResourceProvider<Object>(serviceObject, resourceProperties);
		
		resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		assertNotNull(resourceProvider.getName());
		assertEquals(TestResource.class, resourceProvider.getObjectClass());
		
		resourceProperties.put("service.scope", "prototype");
		
		resourceProvider = new JerseyResourceProvider<Object>(serviceObject, resourceProperties);
		
		resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertFalse(resourceProvider.isSingleton());
		
		assertNotNull(resourceProvider.getName());
		assertEquals(TestResource.class, resourceProvider.getObjectClass());
				
	}
	
	@Test
	public void testResourceProviderName() {

		when(serviceObject.getService()).thenReturn(new TestResource());
		JaxRsResourceProvider resourceProvider = new JerseyResourceProvider<Object>(serviceObject, Collections.emptyMap());
		BaseDTO resourceDto = resourceProvider.getResourceDTO();
		assertTrue(resourceDto instanceof FailedResourceDTO);
		FailedResourceDTO failedDto = (FailedResourceDTO) resourceDto;
		assertEquals(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE, failedDto.failureReason);
		assertFalse(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		Map<String, Object> resourceProperties = new HashMap<>();
		resourceProperties.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		
		resourceProvider = new JerseyResourceProvider<Object>(serviceObject, resourceProperties);
		
		resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		
		assertNotNull(resourceProvider.getName());
		assertNotEquals("test", resourceProvider.getName());
		assertEquals(TestResource.class, resourceProvider.getObjectClass());
		
		resourceProperties.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "test");
		
		resourceProvider = new JerseyResourceProvider<Object>(serviceObject, resourceProperties);
		
		resourceDto = resourceProvider.getResourceDTO();
		assertFalse(resourceDto instanceof FailedResourceDTO);
		assertTrue(resourceProvider.isResource());
		assertTrue(resourceProvider.isSingleton());
		assertEquals("test", resourceProvider.getName());
		
		assertNotNull(resourceProvider.getName());
		assertEquals(TestResource.class, resourceProvider.getObjectClass());
		
	}
	
}
