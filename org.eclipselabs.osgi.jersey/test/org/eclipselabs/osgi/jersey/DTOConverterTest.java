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

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipselabs.osgi.jersey.dto.DTOConverter;
import org.eclipselabs.osgi.jersey.dto.JerseyResourceMethodInfoDTO;
import org.eclipselabs.osgi.jersey.resources.TestResource;
import org.junit.Test;
import org.osgi.service.jaxrs.runtime.dto.ResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceMethodInfoDTO;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * 
 * @author Mark Hoffmann
 * @since 14.07.2017
 */
public class DTOConverterTest {
	
	/**
	 *  Test method for {@link org.eclipselabs.osgi.jersey.dto.DTOConverter#toResourceDTO(java.lang.Objectm java.util.Dictionary)}.
	 */
	@Test
	public void testToResourceDTO() {
		TestResource resource = new TestResource();
		Dictionary<String, Object> properties = new Hashtable<>();
		
		ResourceDTO dto = DTOConverter.toResourceDTO(resource, properties);
		assertNotNull(dto);
		assertEquals("test", dto.base);
		assertNull(dto.name);
		assertEquals(0, dto.serviceId);
		ResourceMethodInfoDTO[] methodInfoDTOs = dto.resourceMethods;
		assertNotNull(methodInfoDTOs);
		assertEquals(2, methodInfoDTOs.length);
		
		properties.put("service.id", Long.valueOf(12));
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "Myresource");
		dto = DTOConverter.toResourceDTO(resource, properties);
		assertNotNull(dto);
		assertEquals("test", dto.base);
		assertEquals("Myresource", dto.name);
		assertEquals(12, dto.serviceId);
		methodInfoDTOs = dto.resourceMethods;
		assertNotNull(methodInfoDTOs);
		assertEquals(2, methodInfoDTOs.length);
		
		properties = new Hashtable<>();
		properties.put("service.id", Long.valueOf(13));
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "Myresource2");
		dto = DTOConverter.toResourceDTO(resource, properties);
		assertNotNull(dto);
		assertEquals("test", dto.base);
		assertEquals("Myresource2", dto.name);
		assertEquals(13, dto.serviceId);
		methodInfoDTOs = dto.resourceMethods;
		assertNotNull(methodInfoDTOs);
		assertEquals(2, methodInfoDTOs.length);
	}
	
	/**
	 *  Test method for {@link org.eclipselabs.osgi.jersey.dto.DTOConverter#toResourceDTO(java.lang.Objectm java.util.Dictionary)}.
	 */
	@Test
	public void testToResourceDTONull() {
		Dictionary<String, Object> properties = new Hashtable<>();
		
		ResourceDTO dto = DTOConverter.toResourceDTO(new String(), properties);
		assertNull(dto);
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
		JerseyResourceMethodInfoDTO dto = (JerseyResourceMethodInfoDTO) DTOConverter.toResourceMethodInfoDTO(method);
		assertNotNull(dto);
		assertEquals("pdf", dto.uri);
		assertEquals("POST", dto.method);
		assertEquals(1, dto.consumingMimeType.length);
		assertEquals("pdf", dto.consumingMimeType[0]);
		assertEquals(1, dto.producingMimeType.length);
		assertEquals("text", dto.producingMimeType[0]);
		
		method = TestResource.class.getDeclaredMethod("postAndOut", new Class[0]);
		dto = (JerseyResourceMethodInfoDTO) DTOConverter.toResourceMethodInfoDTO(method);
		assertNotNull(dto);
		assertNull(dto.uri);
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
		dto = (JerseyResourceMethodInfoDTO) DTOConverter.toResourceMethodInfoDTO(method);
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
