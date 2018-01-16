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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Application;

import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsExtensionProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsResourceProvider;
import org.eclipselabs.jaxrs.jersey.resources.TestExtension;
import org.eclipselabs.jaxrs.jersey.resources.TestResource;
import org.eclipselabs.jaxrs.jersey.runtime.application.JerseyApplicationProvider;
import org.eclipselabs.jaxrs.jersey.runtime.application.JerseyExtensionProvider;
import org.eclipselabs.jaxrs.jersey.runtime.application.JerseyResourceProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.ServiceObjects;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.runtime.dto.ExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceDTO;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * Tests the basics of the {@link JaxRsProvider} interface
 * @author Mark Hoffmann
 * @since 13.10.2017
 */
@RunWith(MockitoJUnitRunner.class)
public class JaxRsProviderTest {

	@Mock
	private ServiceObjects<Object> serviceObject;
	
	/**
	 * Application has a custom name implementation
	 */
	@Test
	public void testNameApplication() {
		Map<String, Object> properties = new HashMap<>();
		JaxRsApplicationProvider appProvider = new JerseyApplicationProvider(new Application(), properties);
		// generated name
		assertTrue(appProvider.getName().startsWith("."));
		ApplicationDTO appDTO = appProvider.getApplicationDTO();
		assertNotNull(appDTO);
		assertTrue(appDTO instanceof FailedApplicationDTO);
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, ((FailedApplicationDTO)appDTO).failureReason);

		properties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "test");
		appProvider = new JerseyApplicationProvider(new Application(), properties);
		// generated name
		assertTrue(appProvider.getName().startsWith("."));
		appDTO = appProvider.getApplicationDTO();
		assertNotNull(appDTO);
		assertTrue(appDTO instanceof ApplicationDTO);

		properties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "test");
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, ".test");
		appProvider = new JerseyApplicationProvider(new Application(), properties);

		// generated name
		assertEquals(".test", appProvider.getName());
		appDTO = appProvider.getApplicationDTO();
		assertNotNull(appDTO);
		assertTrue(appDTO instanceof FailedApplicationDTO);
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, ((FailedApplicationDTO)appDTO).failureReason);
		
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "osgitest");
		appProvider = new JerseyApplicationProvider(new Application(), properties);
		
		// generated name
		assertEquals("osgitest", appProvider.getName());
		appDTO = appProvider.getApplicationDTO();
		assertNotNull(appDTO);
		assertTrue(appDTO instanceof FailedApplicationDTO);
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, ((FailedApplicationDTO)appDTO).failureReason);
		
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "mytest");
		appProvider = new JerseyApplicationProvider(new Application(), properties);
		
		// generated name
		assertEquals("mytest", appProvider.getName());
		appDTO = appProvider.getApplicationDTO();
		assertNotNull(appDTO);
		assertTrue(appDTO instanceof ApplicationDTO);
	}
	
	/**
	 * Resources and extensions share their name implementation
	 */
	@Test
	public void testNameExtension() {
		Map<String, Object> properties = new HashMap<>();
		when(serviceObject.getService()).thenReturn(new TestExtension());
		JaxRsExtensionProvider extProvider = new JerseyExtensionProvider<Object>(serviceObject, properties);
		// generated name
		assertTrue(extProvider.getName().startsWith("."));
		ExtensionDTO extDTO = extProvider.getExtensionDTO();
		assertNotNull(extDTO);
		assertTrue(extDTO instanceof FailedExtensionDTO);
		assertEquals(DTOConstants.FAILURE_REASON_NOT_AN_EXTENSION_TYPE, ((FailedExtensionDTO)extDTO).failureReason);
		
		properties.put(JaxRSWhiteboardConstants.JAX_RS_EXTENSION, "false");
		extProvider = new JerseyExtensionProvider<Object>(serviceObject, properties);
		// generated name
		assertNotNull(extProvider.getName());
		extDTO = extProvider.getExtensionDTO();
		assertNotNull(extDTO);
		assertTrue(extDTO instanceof FailedExtensionDTO);
		assertEquals(DTOConstants.FAILURE_REASON_NOT_AN_EXTENSION_TYPE, ((FailedExtensionDTO)extDTO).failureReason);
		
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, ".test");
		extProvider = new JerseyExtensionProvider<Object>(serviceObject, properties);
		
		// generated name
		assertEquals(".test", extProvider.getName());
		extDTO = extProvider.getExtensionDTO();
		assertNotNull(extDTO);
		assertTrue(extDTO instanceof FailedExtensionDTO);
		assertEquals(DTOConstants.FAILURE_REASON_NOT_AN_EXTENSION_TYPE, ((FailedExtensionDTO)extDTO).failureReason);
		
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "test");
		extProvider = new JerseyExtensionProvider<Object>(serviceObject, properties);
		
		// generated name
		assertEquals("test", extProvider.getName());
		extDTO = extProvider.getExtensionDTO();
		assertNotNull(extDTO);
		assertTrue(extDTO instanceof FailedExtensionDTO);
		assertEquals(DTOConstants.FAILURE_REASON_NOT_AN_EXTENSION_TYPE, ((FailedExtensionDTO)extDTO).failureReason);
		
		properties.put(JaxRSWhiteboardConstants.JAX_RS_EXTENSION, "true");
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, ".test");
		extProvider = new JerseyExtensionProvider<Object>(serviceObject, properties);
		
		// generated name
		assertEquals(".test", extProvider.getName());
		extDTO = extProvider.getExtensionDTO();
		assertNotNull(extDTO);
		assertTrue(extDTO instanceof FailedExtensionDTO);
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, ((FailedExtensionDTO)extDTO).failureReason);
		
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "osgitest");
		extProvider = new JerseyExtensionProvider<Object>(serviceObject, properties);
		
		// generated name
		assertEquals("osgitest", extProvider.getName());
		extDTO = extProvider.getExtensionDTO();
		assertNotNull(extDTO);
		assertTrue(extDTO instanceof FailedExtensionDTO);
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, ((FailedExtensionDTO)extDTO).failureReason);
		
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "mytest");
		extProvider = new JerseyExtensionProvider<Object>(serviceObject, properties);
		
		// generated name
		assertEquals("mytest", extProvider.getName());
		extDTO = extProvider.getExtensionDTO();
		assertNotNull(extDTO);
		assertTrue(extDTO instanceof ExtensionDTO);
	}
	
	/**
	 * Resources and extensions share their name implementation
	 */
	@Test
	public void testNameResource() {
		Map<String, Object> properties = new HashMap<>();
		when(serviceObject.getService()).thenReturn(new TestResource());
		JaxRsResourceProvider resProvider = new JerseyResourceProvider<Object>(serviceObject, properties);
		// generated name
		assertTrue(resProvider.getName().startsWith("."));
		ResourceDTO resDTO = resProvider.getResourceDTO();
		assertNotNull(resDTO);
		assertTrue(resDTO instanceof FailedResourceDTO);
		assertEquals(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE, ((FailedResourceDTO)resDTO).failureReason);
		
		properties.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "false");
		resProvider = new JerseyResourceProvider<Object>(serviceObject, properties);
		// generated name
		assertNotNull(resProvider.getName());
		resDTO = resProvider.getResourceDTO();
		assertNotNull(resDTO);
		assertTrue(resDTO instanceof FailedResourceDTO);
		assertEquals(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE, ((FailedResourceDTO)resDTO).failureReason);
		
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, ".test");
		resProvider = new JerseyResourceProvider<Object>(serviceObject, properties);
		
		// generated name
		assertEquals(".test", resProvider.getName());
		resDTO = resProvider.getResourceDTO();
		assertNotNull(resDTO);
		assertTrue(resDTO instanceof FailedResourceDTO);
		assertEquals(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE, ((FailedResourceDTO)resDTO).failureReason);
		
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "test");
		resProvider = new JerseyResourceProvider<Object>(serviceObject, properties);
		
		// generated name
		assertEquals("test", resProvider.getName());
		resDTO = resProvider.getResourceDTO();
		assertNotNull(resDTO);
		assertTrue(resDTO instanceof FailedResourceDTO);
		assertEquals(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE, ((FailedResourceDTO)resDTO).failureReason);
		
		properties.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, ".test");
		resProvider = new JerseyResourceProvider<Object>(serviceObject, properties);
		
		// generated name
		assertEquals(".test", resProvider.getName());
		resDTO = resProvider.getResourceDTO();
		assertNotNull(resDTO);
		assertTrue(resDTO instanceof FailedResourceDTO);
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, ((FailedResourceDTO)resDTO).failureReason);
		
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "osgitest");
		resProvider = new JerseyResourceProvider<Object>(serviceObject, properties);
		
		// generated name
		assertEquals("osgitest", resProvider.getName());
		resDTO = resProvider.getResourceDTO();
		assertNotNull(resDTO);
		assertTrue(resDTO instanceof FailedResourceDTO);
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, ((FailedResourceDTO)resDTO).failureReason);
		
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "mytest");
		resProvider = new JerseyResourceProvider<Object>(serviceObject, properties);
		
		// generated name
		assertEquals("mytest", resProvider.getName());
		resDTO = resProvider.getResourceDTO();
		assertNotNull(resDTO);
		assertTrue(resDTO instanceof ResourceDTO);
	}

}
