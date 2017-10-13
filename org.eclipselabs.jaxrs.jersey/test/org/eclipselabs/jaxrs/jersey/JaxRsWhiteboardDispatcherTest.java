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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Application;

import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsWhiteboardDispatcher;
import org.eclipselabs.jaxrs.jersey.provider.whiteboard.JaxRsWhiteboardProvider;
import org.eclipselabs.jaxrs.jersey.resources.TestLegacyApplication;
import org.eclipselabs.jaxrs.jersey.resources.TestResource;
import org.eclipselabs.jaxrs.jersey.runtime.dispatcher.JerseyWhiteboardDispatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * Tests the whiteboard dispatcher
 * @author Mark Hoffmann
 * @since 12.10.2017
 */
@RunWith(MockitoJUnitRunner.class)
public class JaxRsWhiteboardDispatcherTest {

	@Mock
	private JaxRsWhiteboardProvider whiteboard;
	
	/**
	 * Tests the dispatcher in not ready state
	 */
	@Test(expected=IllegalStateException.class)
	public void testDispatcherNotReady() {
		JaxRsWhiteboardDispatcher dispatcher = new JerseyWhiteboardDispatcher();
		assertFalse(dispatcher.isDispatching());
		dispatcher.dispatch();
		assertFalse(dispatcher.isDispatching());
		
		Mockito.verify(whiteboard, never()).registerApplication(Mockito.any());
		Mockito.verify(whiteboard, never()).unregisterApplication(Mockito.any());
		Mockito.verify(whiteboard, never()).reloadApplication(Mockito.any());
	}
	
	/**
	 * Tests the dispatcher without any action
	 */
	@Test
	public void testDispatcherReady() {
		JaxRsWhiteboardDispatcher dispatcher = new JerseyWhiteboardDispatcher();
		assertFalse(dispatcher.isDispatching());
		assertNotNull(whiteboard);
		dispatcher.setWhiteboardProvider(whiteboard);
		
		dispatcher.dispatch();
		assertTrue(dispatcher.isDispatching());
		dispatcher.deactivate();
		assertFalse(dispatcher.isDispatching());
		
		// default application should be registered
		Mockito.verify(whiteboard, times(1)).registerApplication(Mockito.any());
		Mockito.verify(whiteboard, times(1)).unregisterApplication(Mockito.any());
		Mockito.verify(whiteboard, never()).reloadApplication(Mockito.any());
	}
	
	/**
	 * Tests the dispatcher with a legacy application
	 */
	@Test
	public void testDispatcherWithWhiteboardLegacyApplicationDefault() {
		JaxRsWhiteboardDispatcher dispatcher = new JerseyWhiteboardDispatcher();
		assertFalse(dispatcher.isDispatching());
		assertNotNull(whiteboard);
		dispatcher.setWhiteboardProvider(whiteboard);
		// whiteboard has no properties
		when(whiteboard.getProperties()).thenAnswer(new Answer<Map<String, Object>>() {
			@Override
			public Map<String, Object> answer(InvocationOnMock invocation) throws Throwable {
				return Collections.emptyMap();
			}
		});
		/* 
		 * isREgistered call
		 * 1. addApplication no name: false, 
		 * 2. addApplication test: add no name again: false,
		 * 2. addApplication test: add test: false,
		 * 4. Deactivate application no name: false
		 * 5. Deactivate test - application: true
		 */
		when(whiteboard.isRegistered(Mockito.any(JaxRsApplicationProvider.class))).thenReturn(false, false, false, false, true);
		
		dispatcher.dispatch();
		assertTrue(dispatcher.isDispatching());
		
		// register default application
		Mockito.verify(whiteboard, times(1)).registerApplication(Mockito.any());
		Mockito.verify(whiteboard, never()).unregisterApplication(Mockito.any());
		Mockito.verify(whiteboard, never()).reloadApplication(Mockito.any());
		
		Map<String, Object> appProperties = new HashMap<>();
		Application application = new TestLegacyApplication();
		
		assertEquals(0, dispatcher.getApplications().size());
		dispatcher.addApplication(application, appProperties);
		assertEquals(1, dispatcher.getApplications().size());
		
		// no further application registered because of missing properties
		Mockito.verify(whiteboard, times(1)).registerApplication(Mockito.any());
		Mockito.verify(whiteboard, never()).unregisterApplication(Mockito.any());
		Mockito.verify(whiteboard, never()).reloadApplication(Mockito.any());
		
		appProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "test");
		assertEquals(1, dispatcher.getApplications().size());
		dispatcher.addApplication(application, appProperties);
		assertEquals(2, dispatcher.getApplications().size());
		
		// no further application registered because it is legacy
		Mockito.verify(whiteboard, times(2)).registerApplication(Mockito.any());
		Mockito.verify(whiteboard, never()).unregisterApplication(Mockito.any());
		Mockito.verify(whiteboard, never()).reloadApplication(Mockito.any());
		
		dispatcher.deactivate();
		assertFalse(dispatcher.isDispatching());
		
		Mockito.verify(whiteboard, times(2)).registerApplication(Mockito.any());
		Mockito.verify(whiteboard, times(2)).unregisterApplication(Mockito.any());
		Mockito.verify(whiteboard, never()).reloadApplication(Mockito.any());
	}
	
	/**
	 * Tests the dispatcher with a legacy application
	 */
	@Test
	public void testDispatcherReloadDefaultApplication() {
		JaxRsWhiteboardDispatcher dispatcher = new JerseyWhiteboardDispatcher();
		assertFalse(dispatcher.isDispatching());
		assertNotNull(whiteboard);
		dispatcher.setWhiteboardProvider(whiteboard);
		// whiteboard has no properties
		when(whiteboard.getProperties()).thenAnswer(new Answer<Map<String, Object>>() {
			@Override
			public Map<String, Object> answer(InvocationOnMock invocation) throws Throwable {
				return Collections.emptyMap();
			}
		});
		/* 
		 * 1. addApplication test: false, 
		 * 2. addResource: Application test: true,
		 * 3. Deactivate test - application: true
		 */
		when(whiteboard.isRegistered(Mockito.any(JaxRsApplicationProvider.class))).thenReturn(false);
		
		dispatcher.dispatch();
		assertTrue(dispatcher.isDispatching());
		
		// register default application
		Mockito.verify(whiteboard, times(1)).registerApplication(Mockito.any());
		Mockito.verify(whiteboard, never()).unregisterApplication(Mockito.any());
		Mockito.verify(whiteboard, never()).reloadApplication(Mockito.any());
		
		Map<String, Object> appProperties = new HashMap<>();
		appProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "test");
		Application application = new Application();
		
		assertEquals(0, dispatcher.getApplications().size());
		dispatcher.addApplication(application, appProperties);
		assertEquals(1, dispatcher.getApplications().size());
		
		// .default is registered, .test is empty
		Mockito.verify(whiteboard, times(1)).registerApplication(Mockito.any());
		Mockito.verify(whiteboard, never()).unregisterApplication(Mockito.any());
		Mockito.verify(whiteboard, never()).reloadApplication(Mockito.any());
		
		TestResource resource = new TestResource();
		Map<String, Object> resProperties = new HashMap<>();
		resProperties.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		assertEquals(0, dispatcher.getResources().size());
		dispatcher.addResource(resource, resProperties);
		assertEquals(1, dispatcher.getResources().size());
		
		// .default is registered and resource was added to default
		Mockito.verify(whiteboard, times(1)).registerApplication(Mockito.any());
		Mockito.verify(whiteboard, never()).unregisterApplication(Mockito.any());
		Mockito.verify(whiteboard, times(1)).reloadApplication(Mockito.any());
		
		dispatcher.deactivate();
		assertFalse(dispatcher.isDispatching());
		
		Mockito.verify(whiteboard, times(1)).registerApplication(Mockito.any());
		Mockito.verify(whiteboard, times(1)).unregisterApplication(Mockito.any());
		Mockito.verify(whiteboard, times(1)).reloadApplication(Mockito.any());
	}
	
	/**
	 * Tests the dispatcher with a legacy application
	 */
	@Test
	public void testDispatcherWithWhiteboardLegacyApplicationResource() {
		JaxRsWhiteboardDispatcher dispatcher = new JerseyWhiteboardDispatcher();
		assertFalse(dispatcher.isDispatching());
		assertNotNull(whiteboard);
		dispatcher.setWhiteboardProvider(whiteboard);
		// whiteboard has no properties
		when(whiteboard.getProperties()).thenAnswer(new Answer<Map<String, Object>>() {
			@Override
			public Map<String, Object> answer(InvocationOnMock invocation) throws Throwable {
				return Collections.emptyMap();
			}
		});
		/* 
		 * 1. addApplication test: false, 
		 * 2. addResource: Application test: true,
		 * 3. Deactivate test - application: true
		 */
		when(whiteboard.isRegistered(Mockito.any(JaxRsApplicationProvider.class))).thenReturn(false, true, true);
		
		dispatcher.dispatch();
		assertTrue(dispatcher.isDispatching());
		
		// register default application
		Mockito.verify(whiteboard, times(1)).registerApplication(Mockito.any());
		Mockito.verify(whiteboard, never()).unregisterApplication(Mockito.any());
		Mockito.verify(whiteboard, never()).reloadApplication(Mockito.any());
		
		Map<String, Object> appProperties = new HashMap<>();
		appProperties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "test");
		Application application = new TestLegacyApplication();
		
		assertEquals(0, dispatcher.getApplications().size());
		dispatcher.addApplication(application, appProperties);
		assertEquals(1, dispatcher.getApplications().size());
		
		// .default is registered, .test is empty
		Mockito.verify(whiteboard, times(2)).registerApplication(Mockito.any());
		Mockito.verify(whiteboard, never()).unregisterApplication(Mockito.any());
		Mockito.verify(whiteboard, never()).reloadApplication(Mockito.any());
		
		TestResource resource = new TestResource();
		Map<String, Object> resProperties = new HashMap<>();
		resProperties.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		assertEquals(0, dispatcher.getResources().size());
		dispatcher.addResource(resource, resProperties);
		assertEquals(1, dispatcher.getResources().size());
		
		// .default is registered and resource was added to default
		Mockito.verify(whiteboard, times(2)).registerApplication(Mockito.any());
		Mockito.verify(whiteboard, never()).unregisterApplication(Mockito.any());
		Mockito.verify(whiteboard, times(1)).reloadApplication(Mockito.any());
		
		dispatcher.removeResource(resource, resProperties);
		
		// .default is registered and resource was added to default
		Mockito.verify(whiteboard, times(2)).registerApplication(Mockito.any());
		Mockito.verify(whiteboard, never()).unregisterApplication(Mockito.any());
		Mockito.verify(whiteboard, times(2)).reloadApplication(Mockito.any());
		
		dispatcher.deactivate();
		assertFalse(dispatcher.isDispatching());
		
		Mockito.verify(whiteboard, times(2)).registerApplication(Mockito.any());
		Mockito.verify(whiteboard, times(2)).unregisterApplication(Mockito.any());
		Mockito.verify(whiteboard, times(2)).reloadApplication(Mockito.any());
	}

}
