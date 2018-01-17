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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Application;

import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsExtensionProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsResourceProvider;
import org.eclipselabs.jaxrs.jersey.resources.TestApplication;
import org.eclipselabs.jaxrs.jersey.resources.TestExtension;
import org.eclipselabs.jaxrs.jersey.resources.TestLegacyApplication;
import org.eclipselabs.jaxrs.jersey.resources.TestResource;
import org.eclipselabs.jaxrs.jersey.runtime.application.JerseyApplicationProvider;
import org.eclipselabs.jaxrs.jersey.runtime.application.JerseyExtensionProvider;
import org.eclipselabs.jaxrs.jersey.runtime.application.JerseyResourceProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceObjects;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.runtime.dto.FailedApplicationDTO;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * 
 * @author Mark Hoffmann
 * @since 21.09.2017
 */
@RunWith(MockitoJUnitRunner.class)
public class JaxRsApplicationProviderTest {

	@Mock
	private ServiceObjects<Object> serviceObject;
	
	@Test
	public void testApplicationProviderNoRequiredProperties() {
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("something", "else");
		// invalid filter schema
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(hallo=bla");
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new Application(), applicationProperties);
		
		ApplicationDTO dto = provider.getApplicationDTO();
		assertTrue(dto instanceof FailedApplicationDTO);
		FailedApplicationDTO failedDto = (FailedApplicationDTO) dto;
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedDto.failureReason);
		
		assertNull(provider.getPath());
		assertNotNull(provider.getName());
		assertTrue(provider.getName().startsWith("."));
	}
	
	@Test
	public void testApplicationProviderWithRequiredPropertiesInvalidTargetFilter() {
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("something", "else");
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(hallo=bla");
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "test");
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new Application(), applicationProperties);
		
		ApplicationDTO dto = provider.getApplicationDTO();
		assertTrue(dto instanceof FailedApplicationDTO);
		FailedApplicationDTO failedDto = (FailedApplicationDTO) dto;
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedDto.failureReason);
		
		assertEquals("test/*", provider.getPath());
		assertEquals(".test", provider.getName());
	}
	
	@Test
	public void testApplicationProviderWithRequiredPropertiesInvalidTargetFilterButName() {
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("something", "else");
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(hallo=bla");
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "test");
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "myTest");
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new Application(), applicationProperties);
		
		ApplicationDTO dto = provider.getApplicationDTO();
		assertTrue(dto instanceof FailedApplicationDTO);
		FailedApplicationDTO failedDto = (FailedApplicationDTO) dto;
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedDto.failureReason);
		
		assertEquals("test/*", provider.getPath());
		assertEquals("myTest", provider.getName());
	}
	
	@Test
	public void testApplicationProviderWithRequiredProperties() {
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("something", "else");
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(hallo=bla)");
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "test");
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new Application(), applicationProperties);
		
		ApplicationDTO dto = provider.getApplicationDTO();
		assertFalse(dto instanceof FailedApplicationDTO);
		
		assertEquals("test/*", provider.getPath());
		assertEquals(".test", provider.getName());
	}
	
	@Test
	public void testApplicationProviderWithRequiredPropertiesAndName() {
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("something", "else");
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(hallo=bla)");
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "test");
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "myTest");
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new Application(), applicationProperties);
		
		ApplicationDTO dto = provider.getApplicationDTO();
		assertFalse(dto instanceof FailedApplicationDTO);
		
		assertEquals("test/*", provider.getPath());
		assertEquals("myTest", provider.getName());
	}
	
	@Test
	public void testHandleApplicationTargetFilterWrong() {
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("something", "else");
		// invalid filter schema
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(hallo=bla");
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new Application(), applicationProperties);
		
		Map<String, Object> runtimeProperties = new HashMap<>();
		assertFalse(provider.canHandleWhiteboard(runtimeProperties));
		
		ApplicationDTO dto = provider.getApplicationDTO();
		assertTrue(dto instanceof FailedApplicationDTO);
		FailedApplicationDTO failedDto = (FailedApplicationDTO) dto;
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedDto.failureReason);
		
		runtimeProperties.put("role", "rest");
		runtimeProperties.put("mandant", "eTest");
		
		assertFalse(provider.canHandleWhiteboard(runtimeProperties));
		
		dto = provider.getApplicationDTO();
		assertTrue(dto instanceof FailedApplicationDTO);
		failedDto = (FailedApplicationDTO) dto;
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedDto.failureReason);
	}
	
	@Test
	public void testHandleApplicationTargetFilterNoMatch() {
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("something", "else");
		// invalid filter schema
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(hallo=bla)");
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new Application(), applicationProperties);
		
		Map<String, Object> runtimeProperties = new HashMap<>();
		assertFalse(provider.canHandleWhiteboard(runtimeProperties));
		
		ApplicationDTO dto = provider.getApplicationDTO();
		assertTrue(dto instanceof FailedApplicationDTO);
		FailedApplicationDTO failedDto = (FailedApplicationDTO) dto;
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedDto.failureReason);
		
		runtimeProperties.put("role", "rest");
		runtimeProperties.put("mandant", "eTest");
		assertFalse(provider.canHandleWhiteboard(runtimeProperties));
		
		dto = provider.getApplicationDTO();
		assertTrue(dto instanceof FailedApplicationDTO);
		failedDto = (FailedApplicationDTO) dto;
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedDto.failureReason);
	}
	
	@Test
	public void testHandleApplicationTargetFilterMatchButMissingBase() {
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("something", "else");
		// invalid filter schema
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(|(role=bla)(mandant=eTest))");
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new Application(), applicationProperties);
		
		Map<String, Object> runtimeProperties = new HashMap<>();
		assertFalse(provider.canHandleWhiteboard(runtimeProperties));
		
		ApplicationDTO dto = provider.getApplicationDTO();
		assertTrue(dto instanceof FailedApplicationDTO);
		FailedApplicationDTO failedDto = (FailedApplicationDTO) dto;
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedDto.failureReason);
		
		runtimeProperties.put("role", "rest");
		runtimeProperties.put("mandant", "eTest");
		
		assertFalse(provider.canHandleWhiteboard(runtimeProperties));
		
		dto = provider.getApplicationDTO();
		assertTrue(dto instanceof FailedApplicationDTO);
		failedDto = (FailedApplicationDTO) dto;
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedDto.failureReason);
		
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(&(role=rest)(mandant=eTest))");
		
		assertFalse(provider.canHandleWhiteboard(runtimeProperties));
		
		dto = provider.getApplicationDTO();
		assertTrue(dto instanceof FailedApplicationDTO);
		failedDto = (FailedApplicationDTO) dto;
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedDto.failureReason);
		
	}
	
	@Test
	public void testHandleApplicationTargetFilterMatch() {
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("something", "else");
		// invalid filter schema
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(|(role=bla)(mandant=eTest))");
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new Application(), applicationProperties);
		
		Map<String, Object> runtimeProperties = new HashMap<>();
		assertFalse(provider.canHandleWhiteboard(runtimeProperties));
		
		ApplicationDTO dto = provider.getApplicationDTO();
		assertTrue(dto instanceof FailedApplicationDTO);
		FailedApplicationDTO failedDto = (FailedApplicationDTO) dto;
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedDto.failureReason);
		
		runtimeProperties.put("role", "rest");
		runtimeProperties.put("mandant", "eTest");
		// still wrong, because of missing application.base property
		assertFalse(provider.canHandleWhiteboard(runtimeProperties));
		
		dto = provider.getApplicationDTO();
		assertTrue(dto instanceof FailedApplicationDTO);
		failedDto = (FailedApplicationDTO) dto;
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedDto.failureReason);
		
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(&(role=rest)(mandant=eTest))");
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "test");
		
		provider = new JerseyApplicationProvider(new Application(), applicationProperties);
		assertTrue(provider.canHandleWhiteboard(runtimeProperties));
		
		dto = provider.getApplicationDTO();
		assertFalse(dto instanceof FailedApplicationDTO);
	}
	
	@Test
	public void testHandleApplicationNullProperties() {
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("something", "else");
		// invalid filter schema
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(|(role=bla)(mandant=eTest))");
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new Application(), applicationProperties);
		
		assertFalse(provider.canHandleWhiteboard(null));
		
		ApplicationDTO dto = provider.getApplicationDTO();
		assertTrue(dto instanceof FailedApplicationDTO);
		FailedApplicationDTO failedDto = (FailedApplicationDTO) dto;
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedDto.failureReason);
		
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "test");
		provider = new JerseyApplicationProvider(new Application(), applicationProperties);
		assertFalse(provider.canHandleWhiteboard(null));
		
		dto = provider.getApplicationDTO();
		assertFalse(dto instanceof FailedApplicationDTO);
	}
	
	@Test
	public void testLegacyApplicationWithNullProperties() {
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("something", "else");
		// invalid filter schema
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(|(role=bla)(mandant=eTest))");
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new TestLegacyApplication(), applicationProperties);
		
		assertFalse(provider.canHandleWhiteboard(null));
		
		ApplicationDTO dto = provider.getApplicationDTO();
		assertTrue(dto instanceof FailedApplicationDTO);
		FailedApplicationDTO failedDto = (FailedApplicationDTO) dto;
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedDto.failureReason);
		
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "test");
		provider = new JerseyApplicationProvider(new Application(), applicationProperties);
		assertFalse(provider.canHandleWhiteboard(null));
		
		dto = provider.getApplicationDTO();
		
		// according to the current spec an application that does not match a whiteboard will not produce a failure dto 
		assertFalse(dto instanceof FailedApplicationDTO);
	}
	
	@Test
	public void testLegacyApplicationChangeInvalid() {
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("something", "else");
		// invalid filter schema
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(|(role=bla)(mandant=eTest))");
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new TestLegacyApplication(), applicationProperties);
		
		assertTrue(provider.isChanged());
		
		when(serviceObject.getService()).thenReturn(new TestResource());
		Map<String, Object> contentProperties = new HashMap<>();
		contentProperties.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		JaxRsResourceProvider resource = new JerseyResourceProvider<Object>(serviceObject, contentProperties);
		
		assertFalse(provider.addResource(resource));
		assertTrue(provider.isChanged());
		assertFalse(provider.removeResource(resource));
		assertTrue(provider.isChanged());
		
		when(serviceObject.getService()).thenReturn(new TestExtension());
		contentProperties.put(JaxRSWhiteboardConstants.JAX_RS_EXTENSION, "true");
		contentProperties.put(Constants.OBJECTCLASS, new String[] {TestExtension.class.getName()});
		JaxRsExtensionProvider extension = new JerseyExtensionProvider<Object>(serviceObject, contentProperties);
		
		assertFalse(provider.addExtension(extension));
		assertTrue(provider.isChanged());
		assertFalse(provider.removeExtension(extension));
		assertTrue(provider.isChanged());
	}
	
	@Test
	public void testLegacyApplicationChange() {
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("something", "else");
		// invalid filter schema
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "test");
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new TestApplication(), applicationProperties);
		
		assertTrue(provider.isEmpty());
		assertTrue(provider.isChanged());
		provider.markUnchanged();
		
		provider = new JerseyApplicationProvider(new TestLegacyApplication(), applicationProperties);
		
		assertFalse(provider.isEmpty());
		assertTrue(provider.isChanged());
		provider.markUnchanged();
		
		when(serviceObject.getService()).thenReturn(new TestResource());
		Map<String, Object> contentProperties = new HashMap<>();
		contentProperties.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		JaxRsResourceProvider resource = new JerseyResourceProvider<Object>(serviceObject, contentProperties);
		
		assertFalse(provider.addResource(resource));
		assertFalse(provider.isChanged());
		assertFalse(provider.removeResource(resource));
		assertFalse(provider.isChanged());
		
		when(serviceObject.getService()).thenReturn(new TestExtension());
		contentProperties.put(JaxRSWhiteboardConstants.JAX_RS_EXTENSION, "true");
		contentProperties.put(Constants.OBJECTCLASS, new String[] {TestExtension.class.getName()});
		JaxRsExtensionProvider extension = new JerseyExtensionProvider<Object>(serviceObject, contentProperties);
		
		assertFalse(provider.addExtension(extension));
		assertFalse(provider.isChanged());
		assertFalse(provider.removeExtension(extension));
		assertFalse(provider.isChanged());
	}
	
	@Test
	public void testApplicationChangeInvalid() {
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("something", "else");
		// invalid filter schema
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(|(role=bla)(mandant=eTest))");
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new Application(), applicationProperties);
		
		assertTrue(provider.isChanged());
		provider.markUnchanged();
		
		when(serviceObject.getService()).thenReturn(new TestResource());
		Map<String, Object> contentProperties = new HashMap<>();
		contentProperties.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		JaxRsResourceProvider resource = new JerseyResourceProvider<Object>(serviceObject, contentProperties);
		
		assertFalse(provider.addResource(resource));
		assertFalse(provider.isChanged());
		assertFalse(provider.removeResource(resource));
		assertFalse(provider.isChanged());
		
		when(serviceObject.getService()).thenReturn(new TestExtension());
		contentProperties.put(JaxRSWhiteboardConstants.JAX_RS_EXTENSION, "true");
		contentProperties.put(Constants.OBJECTCLASS, new String[] {TestExtension.class.getName()});
		JaxRsExtensionProvider extension = new JerseyExtensionProvider<Object>(serviceObject, contentProperties);
		
		assertFalse(provider.addExtension(extension));
		assertFalse(provider.isChanged());
		assertFalse(provider.removeExtension(extension));
		assertFalse(provider.isChanged());
	}
	
	@Test
	public void testApplicationNoChange() {
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("something", "else");
		// invalid filter schema
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "test");
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new Application(), applicationProperties);
		
		assertTrue(provider.isChanged());
		provider.markUnchanged();
		
		when(serviceObject.getService()).thenReturn(new TestResource());
		Map<String, Object> contentProperties = new HashMap<>();
		contentProperties.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		JaxRsResourceProvider resource = new JerseyResourceProvider<Object>(serviceObject, contentProperties);
		
		assertFalse(provider.addResource(resource));
		assertFalse(provider.isChanged());
		assertFalse(provider.removeResource(resource));
		assertFalse(provider.isChanged());
		
		when(serviceObject.getService()).thenReturn(new TestExtension());
		contentProperties.put(JaxRSWhiteboardConstants.JAX_RS_EXTENSION, "true");
		contentProperties.put(Constants.OBJECTCLASS, new String[] {TestExtension.class.getName()});
		JaxRsExtensionProvider extension = new JerseyExtensionProvider<Object>(serviceObject, contentProperties);
		
		assertFalse(provider.addExtension(extension));
		assertFalse(provider.isChanged());
		assertFalse(provider.removeExtension(extension));
		assertFalse(provider.isChanged());
	}
	
	@Test
	public void testApplicationChange() {
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("name", "me");
		// invalid filter schema
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "test");
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new Application(), applicationProperties);
		
		assertTrue(provider.isChanged());
		provider.markUnchanged();
		
		when(serviceObject.getService()).thenReturn(new TestResource());
		Map<String, Object> contentProperties = new HashMap<>();
		contentProperties.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		contentProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(name=me)");
		contentProperties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "res_one");
		JaxRsResourceProvider resource = new JerseyResourceProvider<Object>(serviceObject, contentProperties);
		
		Map<String, Object> contentProperties2 = new HashMap<>(contentProperties);
		contentProperties2.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "two");
		JaxRsResourceProvider resource2 = new JerseyResourceProvider<Object>(serviceObject, contentProperties2);
		
		assertTrue(provider.addResource(resource));
		assertTrue(provider.isChanged());
		
		provider.markUnchanged();
		
		assertFalse(provider.isChanged());
		
		assertFalse(provider.removeResource(resource2));
		assertFalse(provider.isChanged());
		assertTrue(provider.removeResource(resource));
		assertTrue(provider.isChanged());
		
		when(serviceObject.getService()).thenReturn(new TestExtension());
		contentProperties.put(JaxRSWhiteboardConstants.JAX_RS_EXTENSION, "true");
		contentProperties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "ext_one");
		contentProperties.put(Constants.OBJECTCLASS, new String[] {TestExtension.class.getName()});		JaxRsExtensionProvider extension = new JerseyExtensionProvider<Object>(serviceObject, contentProperties);
		
		assertTrue(provider.addExtension(extension));
		assertTrue(provider.isChanged());
		provider.markUnchanged();
		assertFalse(provider.isChanged());
		assertTrue(provider.removeExtension(extension));
		assertTrue(provider.isChanged());
	}
}
