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
package org.gecko.rest.jersey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider;
import org.gecko.rest.jersey.provider.application.JaxRsExtensionProvider;
import org.gecko.rest.jersey.resources.TestApplication;
import org.gecko.rest.jersey.resources.TestExtension;
import org.gecko.rest.jersey.runtime.application.JerseyApplicationProvider;
import org.gecko.rest.jersey.runtime.application.JerseyExtensionProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceObjects;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.BaseApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.BaseExtensionDTO;
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
@RunWith(MockitoJUnitRunner.class)
public class JaxRsExtensionProviderTest {

	@Mock
	private ServiceObjects<Object> serviceObject;
	
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
		
		BaseApplicationDTO dto = provider.getApplicationDTO();
		assertFalse(dto instanceof FailedApplicationDTO);
		
		assertEquals("/test/*", provider.getPath());
		assertEquals("test", provider.getName());
		
		Map<String, Object> resourceProperties = new HashMap<>();
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		resourceProperties.put(Constants.OBJECTCLASS, new String[] {TestExtension.class.getName()});
		when(serviceObject.getService()).thenReturn(new TestExtension());
		JaxRsExtensionProvider resourceProvider = new JerseyExtensionProvider<Object>(serviceObject, resourceProperties);
		
		BaseExtensionDTO resourceDto = resourceProvider.getExtensionDTO();
		assertTrue(resourceDto instanceof FailedExtensionDTO);
		assertFalse(resourceProvider.isExtension());
		assertTrue(resourceProvider.isSingleton());
		
		resourceProperties.clear();
		resourceProperties.put(JaxRSWhiteboardConstants.JAX_RS_EXTENSION, "true");
		resourceProperties.put(Constants.OBJECTCLASS, new String[] {TestExtension.class.getName()});
		resourceProvider = new JerseyExtensionProvider<Object>(serviceObject, resourceProperties);
		
		resourceDto = resourceProvider.getExtensionDTO();
		assertFalse(resourceDto instanceof FailedExtensionDTO);
		assertTrue(resourceProvider.isExtension());
		assertTrue(resourceProvider.isSingleton());
		
	}
	
}
