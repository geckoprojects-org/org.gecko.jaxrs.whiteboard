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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Application;

import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider;
import org.eclipselabs.jaxrs.jersey.runtime.application.JerseyApplicationProvider;
import org.junit.Test;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.runtime.dto.FailedApplicationDTO;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * 
 * @author Mark Hoffmann
 * @since 21.09.2017
 */
public class JaxRsApplicationProviderTest {

	@Test
	public void testApplicationProviderNoRequiredProperties() {
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("something", "else");
		// invalid filter schema
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(hallo=bla");
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new Application(), applicationProperties);
		
		assertTrue(provider.isLegacy());
		
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
		
		assertTrue(provider.isLegacy());
		
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
		
		assertTrue(provider.isLegacy());
		
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
		
		assertTrue(provider.isLegacy());
		
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
		assertTrue(provider.isLegacy());
	}
	
	@Test
	public void testHandleApplicationTargetFilterWrong() {
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("something", "else");
		// invalid filter schema
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(hallo=bla");
		
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(new Application(), applicationProperties);
		
		assertTrue(provider.isLegacy());
		
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
		
		assertTrue(provider.isLegacy());
		
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
		
		assertTrue(provider.isLegacy());
		
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
		
		assertTrue(provider.isLegacy());
		
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
		
		assertTrue(provider.isLegacy());
		
		assertFalse(provider.canHandleWhiteboard(null));
		
		ApplicationDTO dto = provider.getApplicationDTO();
		assertTrue(dto instanceof FailedApplicationDTO);
		FailedApplicationDTO failedDto = (FailedApplicationDTO) dto;
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedDto.failureReason);
		
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "test");
		provider = new JerseyApplicationProvider(new Application(), applicationProperties);
		assertFalse(provider.canHandleWhiteboard(null));
		
		dto = provider.getApplicationDTO();
		assertTrue(dto instanceof FailedApplicationDTO);
		failedDto = (FailedApplicationDTO) dto;
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedDto.failureReason);
	}
}
