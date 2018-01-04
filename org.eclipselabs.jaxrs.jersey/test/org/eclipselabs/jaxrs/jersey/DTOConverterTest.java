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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

import javax.ws.rs.core.Application;

import org.eclipselabs.jaxrs.jersey.dto.DTOConverter;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsExtensionProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsResourceProvider;
import org.eclipselabs.jaxrs.jersey.resources.TestExtension;
import org.eclipselabs.jaxrs.jersey.resources.TestResource;
import org.eclipselabs.jaxrs.jersey.runtime.application.JerseyApplicationProvider;
import org.eclipselabs.jaxrs.jersey.runtime.application.JerseyExtensionProvider;
import org.eclipselabs.jaxrs.jersey.runtime.application.JerseyResourceProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.runtime.dto.ExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceMethodInfoDTO;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * Tests the DTO converter
 * @author Mark Hoffmann
 * @since 14.07.2017
 */
@RunWith(MockitoJUnitRunner.class)
public class DTOConverterTest {
	
	@Mock
	private ServiceReference<Application> appRef;
	
	/**
	 * Tests conversion of a failed application DTO
	 */
	@Test
	public void testToFailedApplicationDTO() {
		Map<String, Object> properties = new Hashtable<>();
		when(appRef.getPropertyKeys()).thenReturn(properties.keySet().toArray(new String[0]));
		when(appRef.getProperty(any())).then(new Answer<String>() {

			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				return (String) properties.get(invocation.getArgumentAt(0, String.class));
			}
		});
		
		JaxRsApplicationProvider resourceProvider = new JerseyApplicationProvider(appRef);
		
		FailedApplicationDTO dto = DTOConverter.toFailedApplicationDTO(resourceProvider, DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE);
		assertNotNull(dto);
		assertNotNull(dto.name);
		assertTrue(dto.name.startsWith("."));
		assertEquals(-1, dto.serviceId);
		assertEquals(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE, dto.failureReason);
		
		verify(appRef, times(1)).getPropertyKeys();
		
		properties.put(Constants.SERVICE_ID, Long.valueOf(12));
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "MyApp");
		properties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "test");
		
		resourceProvider = new JerseyApplicationProvider(appRef);
		dto = DTOConverter.toFailedApplicationDTO(resourceProvider, DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
		
		assertNotNull(dto);
		assertEquals("test/*", dto.base);
		assertEquals("MyApp", dto.name);
		assertEquals(12, dto.serviceId);
		assertEquals(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, dto.failureReason);
		
	}
	
	/**
	 * Tests conversion of an application DTO
	 */
	@Test
	public void testToApplicationDTO() {
		Map<String, Object> properties = new Hashtable<>();
		
		JaxRsApplicationProvider resourceProvider = new JerseyApplicationProvider(new Application(), properties);
		
		ApplicationDTO dto = DTOConverter.toApplicationDTO(resourceProvider);
		assertNotNull(dto);
		assertNotNull(dto.name);
		assertTrue(dto.name.startsWith("."));
		assertEquals(-1, dto.serviceId);
		
		properties.put(Constants.SERVICE_ID, Long.valueOf(12));
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "MyApp");
		properties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "test");
		
		resourceProvider = new JerseyApplicationProvider(new Application(), properties);
		dto = DTOConverter.toApplicationDTO(resourceProvider);
		
		assertNotNull(dto);
		assertEquals("test/*", dto.base);
		assertEquals("MyApp", dto.name);
		assertEquals(12, dto.serviceId);
	}
	
	/**
	 * Tests conversion of a failed resource DTO
	 */
	@Test
	public void testToFailedResourceDTO() {
		TestResource resource = new TestResource();
		Map<String, Object> properties = new Hashtable<>();
		
		JaxRsResourceProvider resourceProvider = new JerseyResourceProvider<Object>(resource, properties);
		
		FailedResourceDTO dto = DTOConverter.toFailedResourceDTO(resourceProvider, DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE);
		assertNotNull(dto);
		assertEquals(Collections.emptyMap().toString(), dto.name);
		assertEquals(-1, dto.serviceId);
		assertEquals(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE, dto.failureReason);
		
		properties.put(Constants.SERVICE_ID, Long.valueOf(12));
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "Myresource");
		
		resourceProvider = new JerseyResourceProvider<Object>(resource, properties);
		dto = DTOConverter.toFailedResourceDTO(resourceProvider, DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
		
		assertNotNull(dto);
		assertEquals("Myresource", dto.name);
		assertEquals(12, dto.serviceId);
		assertEquals(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, dto.failureReason);
		
	}
	
	/**
	 *  Test method for {@link org.eclipselabs.osgi.jersey.dto.DTOConverter#toResourceDTO(java.lang.Objectm java.util.Dictionary)}.
	 */
	@Test
	public void testToResourceDTO() {
		TestResource resource = new TestResource();
		Map<String, Object> properties = new Hashtable<>();
		
		JaxRsResourceProvider resourceProvider = new JerseyResourceProvider<Object>(resource, properties);
		
		ResourceDTO dto = DTOConverter.toResourceDTO(resourceProvider);
		assertNotNull(dto);
		assertEquals(Collections.emptyMap().toString(), dto.name);
		assertEquals(-1, dto.serviceId);
		ResourceMethodInfoDTO[] methodInfoDTOs = dto.resourceMethods;
		assertNotNull(methodInfoDTOs);
		assertEquals(2, methodInfoDTOs.length);
		
		properties.put(Constants.SERVICE_ID, Long.valueOf(12));
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "Myresource");
		
		resourceProvider = new JerseyResourceProvider<Object>(resource, properties);
		dto = DTOConverter.toResourceDTO(resourceProvider);
		
		assertNotNull(dto);
		assertEquals("Myresource", dto.name);
		assertEquals(12, dto.serviceId);
		methodInfoDTOs = dto.resourceMethods;
		assertNotNull(methodInfoDTOs);
		assertEquals(2, methodInfoDTOs.length);
		
		properties = new Hashtable<>();
		properties.put(ComponentConstants.COMPONENT_ID, Long.valueOf(13));
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "Myresource2");

		resourceProvider = new JerseyResourceProvider<Object>(resource, properties);
		dto = DTOConverter.toResourceDTO(resourceProvider);
		
		assertNotNull(dto);
		assertEquals("Myresource2", dto.name);
		assertEquals(13, dto.serviceId);
		methodInfoDTOs = dto.resourceMethods;
		assertNotNull(methodInfoDTOs);
		assertEquals(2, methodInfoDTOs.length);
	}
	
	/**
	 * Tests conversion of a failed extension DTO
	 */
	@Test
	public void testToFailedExtensionDTO() {
		TestExtension extension = new TestExtension();
		Map<String, Object> properties = new Hashtable<>();
		
		JaxRsExtensionProvider extensionProvider = new JerseyExtensionProvider<Object>(extension, properties);
		
		FailedExtensionDTO dto = DTOConverter.toFailedExtensionDTO(extensionProvider, DTOConstants.FAILURE_REASON_NOT_AN_EXTENSION_TYPE);
		assertNotNull(dto);
		assertEquals(Collections.emptyMap().toString(), dto.name);
		assertEquals(-1, dto.serviceId);
		assertEquals(DTOConstants.FAILURE_REASON_NOT_AN_EXTENSION_TYPE, dto.failureReason);
		
		properties.put(Constants.SERVICE_ID, Long.valueOf(12));
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "Myresource");
		
		extensionProvider = new JerseyExtensionProvider<Object>(extension, properties);
		dto = DTOConverter.toFailedExtensionDTO(extensionProvider, DTOConstants.FAILURE_REASON_DUPLICATE_NAME);
		
		assertNotNull(dto);
		assertEquals("Myresource", dto.name);
		assertEquals(12, dto.serviceId);
		assertEquals(DTOConstants.FAILURE_REASON_DUPLICATE_NAME, dto.failureReason);
		
	}
	
	/**
	 *  Test method for {@link org.eclipselabs.osgi.jersey.dto.DTOConverter#toResourceDTO(java.lang.Objectm java.util.Dictionary)}.
	 */
	@Test
	public void testToExtensionDTO() {
		TestExtension extension = new TestExtension();
		Map<String, Object> properties = new Hashtable<>();
		
		JaxRsExtensionProvider extensionProvider = new JerseyExtensionProvider<Object>(extension, properties);
		
		ExtensionDTO dto = DTOConverter.toExtensionDTO(extensionProvider);
		assertNotNull(dto);
		assertEquals(Collections.emptyMap().toString(), dto.name);
		assertEquals(-1, dto.serviceId);
		
		properties.put(Constants.SERVICE_ID, Long.valueOf(12));
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "Myresource");
		
		extensionProvider = new JerseyExtensionProvider<Object>(extension, properties);
		dto = DTOConverter.toExtensionDTO(extensionProvider);
		
		assertNotNull(dto);
		assertEquals("Myresource", dto.name);
		assertEquals(12, dto.serviceId);
		assertEquals(2, dto.produces.length);
		assertEquals(1, dto.consumes.length);
		
		properties = new Hashtable<>();
		properties.put(ComponentConstants.COMPONENT_ID, Long.valueOf(13));
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "Myresource2");
		
		extensionProvider = new JerseyExtensionProvider<Object>(extension, properties);
		dto = DTOConverter.toExtensionDTO(extensionProvider);
		
		assertNotNull(dto);
		assertEquals("Myresource2", dto.name);
		assertEquals(13, dto.serviceId);
		assertEquals(2, dto.produces.length);
		assertEquals("xml", dto.produces[0]);
		assertEquals("json", dto.produces[1]);
		assertEquals(1, dto.consumes.length);
		assertEquals("test", dto.consumes[0]);
	}

	/**
	 * Test method for {@link org.eclipselabs.osgi.jersey.dto.DTOConverter#toResourceMethodInfoDTO(java.lang.reflect.Method)}.
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 */
	@Test
	public void testToResourceMethodInfoDTOs() throws NoSuchMethodException, SecurityException {
		TestResource resource = new TestResource();
		ResourceMethodInfoDTO[] methodInfoDTOsParsed = DTOConverter.getResourceMethodInfoDTOs(resource);
		assertNotNull(methodInfoDTOsParsed);
		assertEquals(2, methodInfoDTOsParsed.length);
	}
	
	/**
	 * Test method for {@link org.eclipselabs.osgi.jersey.dto.DTOConverter#toResourceMethodInfoDTO(java.lang.reflect.Method)}.
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 */
	@Test
	public void testToResourceMethodInfoDTO() throws NoSuchMethodException, SecurityException {
		Method method = TestResource.class.getDeclaredMethod("postMe", new Class[] {String.class});
		ResourceMethodInfoDTO dto = DTOConverter.toResourceMethodInfoDTO(method);
		assertNotNull(dto);
		assertEquals("POST", dto.method);
		assertEquals(1, dto.consumingMimeType.length);
		assertEquals("pdf", dto.consumingMimeType[0]);
		assertEquals(1, dto.producingMimeType.length);
		assertEquals("text", dto.producingMimeType[0]);
		
		method = TestResource.class.getDeclaredMethod("postAndOut", new Class[0]);
		dto = DTOConverter.toResourceMethodInfoDTO(method);
		assertNotNull(dto);
		assertTrue(dto.method.contains("POST"));
		assertTrue(dto.method.contains("PUT"));
		assertFalse(dto.method.contains("DELETE"));
		assertFalse(dto.method.contains("HEAD"));
		assertFalse(dto.method.contains("GET"));
		assertFalse(dto.method.contains("OPTION"));
		assertNull(dto.consumingMimeType);
		assertEquals(1, dto.producingMimeType.length);
		assertEquals("text", dto.producingMimeType[0]);
		
		method = TestResource.class.getDeclaredMethod("helloWorld", new Class[0]);
		dto = DTOConverter.toResourceMethodInfoDTO(method);
		assertNull(dto);
		
	}

	/**
	 * Test method for {@link org.eclipselabs.osgi.jersey.dto.DTOConverter#checkMethodString(java.lang.reflect.Method, java.lang.Class, java.util.List)}.
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 */
	@Test
	public void testCheckMethodString() throws NoSuchMethodException, SecurityException {
		Class<TestResource> clazz = TestResource.class;
		assertEquals(3, clazz.getDeclaredMethods().length);
		int cnt = 0;
		for (Method m : clazz.getDeclaredMethods()) {
			String result = DTOConverter.getMethodStrings(m);
			switch (m.getName()) {
			case "helloWorld":
				assertNull(result);
				cnt++;
				break;
			case "postAndOut":
				assertTrue(result.contains("POST"));
				assertTrue(result.contains("PUT"));
				assertFalse(result.contains("DELETE"));
				assertFalse(result.contains("HEAD"));
				assertFalse(result.contains("GET"));
				assertFalse(result.contains("OPTION"));
				cnt++;
				break;
			case "postMe":
				assertEquals("POST", result);
				cnt++;
				break;
			default:
				fail("Not tested operation found in test stub TestResource");
				break;
			}
		}
		assertEquals(3, cnt);
	}

}
