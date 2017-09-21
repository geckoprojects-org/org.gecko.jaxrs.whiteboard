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
package org.eclipselabs.osgi.jersey.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Application;

import org.eclipselabs.osgi.jersey.JaxRsApplicationDispatcher;
import org.eclipselabs.osgi.jersey.JaxRsJerseyRuntime;
import org.eclipselabs.osgi.jersey.JerseyConstants;
import org.eclipselabs.osgi.jersey.tests.resources.HelloResource;
import org.eclipselabs.osgi.jersey.tests.resources.RootResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import static org.mockito.Mockito.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Tests the Jersey resource factory
 * @author Mark Hoffmann
 * @since 12.07.2017
 */
@RunWith(MockitoJUnitRunner.class)
public class JerseyDispatcherIntegrationTest {

	private final BundleContext context = FrameworkUtil.getBundle(JerseyDispatcherIntegrationTest.class).getBundleContext();
	@Mock
	private JaxRsJerseyRuntime jerseyRuntime;
	private final List<ServiceRegistration<?>> registrations = new LinkedList<>();

	@Before
	public void before() {
	}

	@After
	public void after() {
		registrations.forEach((r)->{
			try {
				r.unregister();
				System.out.println("JerseyDispatcherIntegrationTest - Registration cleanup on teardown");
			} catch (Exception e) {
				System.out.println("JerseyDispatcherIntegrationTest - Registration cleanup on teardown failed, maybe service was already unregistered");
			}
		});
		registrations.clear();
	}

	/**
	 * Tests simple add and remove of a runtime
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testAddRemoveRuntime() throws IOException, InterruptedException, InvalidSyntaxException {

		ServiceRegistration<JaxRsJerseyRuntime> runtimeRegistration = context.registerService(JaxRsJerseyRuntime.class, jerseyRuntime, null);
		registrations.add(runtimeRegistration);
		
		JaxRsApplicationDispatcher dispatcher = getService(JaxRsApplicationDispatcher.class, 3000l);
		assertNotNull(dispatcher);
		assertNotNull(dispatcher.getRuntime());


		CountDownLatch cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertNotNull(dispatcher.getRuntime());

		runtimeRegistration.unregister();
		registrations.remove(runtimeRegistration);

		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertNull(dispatcher.getRuntime());
	}

	/**
	 * Tests simple add and remove of a resource as DS Component (RootResource) and programmatic resource 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testAddRemoveResource() throws IOException, InterruptedException, InvalidSyntaxException {

		Filter filter = FrameworkUtil.createFilter("(component.name=RootResource)");
		RootResource rootResource = getService(filter, 3000l);
		assertNotNull(rootResource);
		ServiceRegistration<JaxRsJerseyRuntime> runtimeRegistration = context.registerService(JaxRsJerseyRuntime.class, jerseyRuntime, null);
		registrations.add(runtimeRegistration);
		
		JaxRsApplicationDispatcher dispatcher = getService(JaxRsApplicationDispatcher.class, 3000l);
		assertNotNull(dispatcher);
		assertEquals(1, dispatcher.getResources().size());// resource/RootResource.class

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		ServiceRegistration<Object> resourceRegistration = context.registerService(Object.class, new HelloResource(), properties);
		registrations.add(resourceRegistration);

		CountDownLatch cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertEquals(2, dispatcher.getResources().size());

		resourceRegistration.unregister();
		registrations.remove(resourceRegistration);

		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertEquals(1, dispatcher.getResources().size());
	}

	/**
	 * Tests simple add and remove of an application 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testAddRemoveApplication() throws IOException, InterruptedException, InvalidSyntaxException {

		ServiceRegistration<JaxRsJerseyRuntime> runtimeRegistration = context.registerService(JaxRsJerseyRuntime.class, jerseyRuntime, null);
		registrations.add(runtimeRegistration);
		
		JaxRsApplicationDispatcher dispatcher = getService(JaxRsApplicationDispatcher.class, 3000l);
		assertNotNull(dispatcher);
		assertEquals(0, dispatcher.getApplications().size());

		ServiceRegistration<Application> applicationRegistration = context.registerService(Application.class, new Application(), null);
		registrations.add(applicationRegistration);

		CountDownLatch cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertEquals(1, dispatcher.getApplications().size());

		applicationRegistration.unregister();
		registrations.remove(applicationRegistration);
		
		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertEquals(0, dispatcher.getApplications().size());
	}

	/**
	 * Tests registration of an application in case, that the application is registered after the runtime
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testRegisterApplicationAfterRuntime() throws IOException, InterruptedException, InvalidSyntaxException {

		when(jerseyRuntime.getProperties()).thenAnswer(new Answer<Map<String, Object>>() {
			@Override
			public Map<String, Object> answer(InvocationOnMock invocation) throws Throwable {
				return Collections.emptyMap();
			}
		});
		ServiceRegistration<JaxRsJerseyRuntime> runtimeRegistration = context.registerService(JaxRsJerseyRuntime.class, jerseyRuntime, null);
		registrations.add(runtimeRegistration);
		
		JaxRsApplicationDispatcher dispatcher = getService(JaxRsApplicationDispatcher.class, 3000l);
		assertNotNull(dispatcher);
		assertEquals(0, dispatcher.getApplications().size());

		CountDownLatch cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertNotNull(dispatcher.getRuntime());

		ServiceRegistration<Application> applicationRegistration = context.registerService(Application.class, new Application(), null);
		registrations.add(applicationRegistration);

		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertEquals(1, dispatcher.getApplications().size());

		Mockito.verify(jerseyRuntime).registerApplication(Mockito.any());

		applicationRegistration.unregister();
		registrations.remove(applicationRegistration);

		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertEquals(0, dispatcher.getApplications().size());

		runtimeRegistration.unregister();
		registrations.remove(runtimeRegistration);

		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertNull(dispatcher.getRuntime());
	}
	
	/**
	 * Tests registration of an application in case, that the application is registered after the runtime
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testRegisterApplicationWhiteBoardProperties() throws IOException, InterruptedException, InvalidSyntaxException {
		
		Dictionary<String, Object> runtimeProperties = new Hashtable<>();
		Map<String, Object> returnProperties = new HashMap<>();
		runtimeProperties.put("role", "rest");
		returnProperties.put("role", "rest");
		runtimeProperties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "myTest");
		returnProperties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "myTest");
		when(jerseyRuntime.getProperties()).thenAnswer(new Answer<Map<String, Object>>() {
			@Override
			public Map<String, Object> answer(InvocationOnMock invocation) throws Throwable {
				return returnProperties;
			}
		});
		ServiceRegistration<JaxRsJerseyRuntime> runtimeRegistration = context.registerService(JaxRsJerseyRuntime.class, jerseyRuntime, runtimeProperties);
		registrations.add(runtimeRegistration);
		
		JaxRsApplicationDispatcher dispatcher = getService(JaxRsApplicationDispatcher.class, 3000l);
		assertNotNull(dispatcher);
		assertEquals(0, dispatcher.getApplications().size());
		
		CountDownLatch cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertNotNull(dispatcher.getRuntime());
		
		ServiceRegistration<Application> applicationRegistration = context.registerService(Application.class, new Application(), null);
		registrations.add(applicationRegistration);
		
		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertEquals(1, dispatcher.getApplications().size());
		
		Mockito.verify(jerseyRuntime).registerApplication(Mockito.any());
		
		applicationRegistration.unregister();
		registrations.remove(applicationRegistration);
		
		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertEquals(0, dispatcher.getApplications().size());
		
		runtimeRegistration.unregister();
		registrations.remove(runtimeRegistration);
		
		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertNull(dispatcher.getRuntime());
	}
	
	/**
	 * Tests registration of an application in case, that the application is registered after the runtime
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testRegisterApplicationWhiteBoardPropertiesAndTarget() throws IOException, InterruptedException, InvalidSyntaxException {
		
		Dictionary<String, Object> runtimeProperties = new Hashtable<>();
		Map<String, Object> returnProperties = new HashMap<>();
		runtimeProperties.put("role", "rest");
		returnProperties.put("role", "rest");
		runtimeProperties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "myTest");
		returnProperties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "myTest");
		when(jerseyRuntime.getProperties()).thenAnswer(new Answer<Map<String, Object>>() {
			@Override
			public Map<String, Object> answer(InvocationOnMock invocation) throws Throwable {
				return returnProperties;
			}
		});
		ServiceRegistration<JaxRsJerseyRuntime> runtimeRegistration = context.registerService(JaxRsJerseyRuntime.class, jerseyRuntime, runtimeProperties);
		registrations.add(runtimeRegistration);
		
		JaxRsApplicationDispatcher dispatcher = getService(JaxRsApplicationDispatcher.class, 3000l);
		assertNotNull(dispatcher);
		assertEquals(0, dispatcher.getApplications().size());
		
		CountDownLatch cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertNotNull(dispatcher.getRuntime());
		
		Dictionary<String, Object> applicationProperties = new Hashtable<>();
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(&(role=rest)(" + JerseyConstants.JERSEY_WHITEBOARD_NAME + "=myTest))");
		ServiceRegistration<Application> applicationRegistration = context.registerService(Application.class, new Application(), applicationProperties);
		registrations.add(applicationRegistration);
		
		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertEquals(1, dispatcher.getApplications().size());
		
		Mockito.verify(jerseyRuntime).registerApplication(Mockito.any());
		
		applicationRegistration.unregister();
		registrations.remove(applicationRegistration);
		
		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertEquals(0, dispatcher.getApplications().size());
		
		runtimeRegistration.unregister();
		registrations.remove(runtimeRegistration);
		
		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertNull(dispatcher.getRuntime());
	}
	
	/**
	 * Tests registration of an application in case, that the application is registered after the runtime
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testRegisterApplicationWhiteBoardPropertiesInvalidTarget() throws IOException, InterruptedException, InvalidSyntaxException {
		
		Dictionary<String, Object> runtimeProperties = new Hashtable<>();
		Map<String, Object> returnProperties = new HashMap<>();
		runtimeProperties.put("role", "rest");
		returnProperties.put("role", "rest");
		runtimeProperties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "myTest");
		returnProperties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "myTest");
		when(jerseyRuntime.getProperties()).thenAnswer(new Answer<Map<String, Object>>() {
			@Override
			public Map<String, Object> answer(InvocationOnMock invocation) throws Throwable {
				return returnProperties;
			}
		});
		ServiceRegistration<JaxRsJerseyRuntime> runtimeRegistration = context.registerService(JaxRsJerseyRuntime.class, jerseyRuntime, runtimeProperties);
		registrations.add(runtimeRegistration);
		
		JaxRsApplicationDispatcher dispatcher = getService(JaxRsApplicationDispatcher.class, 3000l);
		assertNotNull(dispatcher);
		assertEquals(0, dispatcher.getApplications().size());
		
		CountDownLatch cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertNotNull(dispatcher.getRuntime());
		
		Dictionary<String, Object> applicationProperties = new Hashtable<>();
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(role=rest");
		ServiceRegistration<Application> applicationRegistration = context.registerService(Application.class, new Application(), applicationProperties);
		registrations.add(applicationRegistration);
		
		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertEquals(1, dispatcher.getApplications().size());
		
		Mockito.verify(jerseyRuntime, never()).registerApplication(Mockito.any());
		
		applicationRegistration.unregister();
		registrations.remove(applicationRegistration);
		
		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertEquals(0, dispatcher.getApplications().size());
		
		runtimeRegistration.unregister();
		registrations.remove(runtimeRegistration);
		
		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertNull(dispatcher.getRuntime());
	}
	
	/**
	 * Tests registration of an application in case, that the application is registered after the runtime
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testRegisterApplicationWhiteBoardPropertiesDontMatch() throws IOException, InterruptedException, InvalidSyntaxException {
		
		Dictionary<String, Object> runtimeProperties = new Hashtable<>();
		Map<String, Object> returnProperties = new HashMap<>();
		runtimeProperties.put("role", "rest");
		returnProperties.put("role", "rest");
		runtimeProperties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "myTest");
		returnProperties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "myTest");
		when(jerseyRuntime.getProperties()).thenAnswer(new Answer<Map<String, Object>>() {
			@Override
			public Map<String, Object> answer(InvocationOnMock invocation) throws Throwable {
				return returnProperties;
			}
		});
		ServiceRegistration<JaxRsJerseyRuntime> runtimeRegistration = context.registerService(JaxRsJerseyRuntime.class, jerseyRuntime, runtimeProperties);
		registrations.add(runtimeRegistration);
		
		JaxRsApplicationDispatcher dispatcher = getService(JaxRsApplicationDispatcher.class, 3000l);
		assertNotNull(dispatcher);
		assertEquals(0, dispatcher.getApplications().size());
		
		CountDownLatch cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertNotNull(dispatcher.getRuntime());
		
		Dictionary<String, Object> applicationProperties = new Hashtable<>();
		applicationProperties.put(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET, "(&(role=rest)(" + JerseyConstants.JERSEY_WHITEBOARD_NAME + "=myTestApp))");
		ServiceRegistration<Application> applicationRegistration = context.registerService(Application.class, new Application(), applicationProperties);
		registrations.add(applicationRegistration);
		
		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertEquals(1, dispatcher.getApplications().size());
		
		Mockito.verify(jerseyRuntime, never()).registerApplication(Mockito.any());
		
		applicationRegistration.unregister();
		registrations.remove(applicationRegistration);
		
		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertEquals(0, dispatcher.getApplications().size());
		
		runtimeRegistration.unregister();
		registrations.remove(runtimeRegistration);
		
		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertNull(dispatcher.getRuntime());
	}


	/**
	 * Tests simple add and remove of a resource as DS Component (RootResource) and profgrammatic resource 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testAddRemoveResourceWrongProperties() throws IOException, InterruptedException, InvalidSyntaxException {

		Filter filter = FrameworkUtil.createFilter("(component.name=RootResource)");
		RootResource rootResource = getService(filter, 3000l);
		assertNotNull(rootResource);
		
		ServiceRegistration<JaxRsJerseyRuntime> runtimeRegistration = context.registerService(JaxRsJerseyRuntime.class, jerseyRuntime, null);
		registrations.add(runtimeRegistration);
		
		JaxRsApplicationDispatcher dispatcher = getService(JaxRsApplicationDispatcher.class, 3000l);
		assertNotNull(dispatcher);
		assertEquals(1, dispatcher.getResources().size());// resource/RootResource.class

		ServiceRegistration<Object> resourceRegistration = context.registerService(Object.class, new HelloResource(), null);
		registrations.add(resourceRegistration);

		CountDownLatch cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertEquals(1, dispatcher.getResources().size());

		resourceRegistration.unregister();
		registrations.remove(resourceRegistration);

		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		assertEquals(1, dispatcher.getResources().size());
	}

	<T> T getService(Class<T> clazz, long timeout) throws InterruptedException {
		ServiceTracker<T, T> tracker = new ServiceTracker<>(context, clazz, null);
		tracker.open();
		return tracker.waitForService(timeout);
	}

	<T> void awaitRemovedService(Class<T> clazz, ServiceTrackerCustomizer<T, T> customizer) throws InterruptedException {
		ServiceTracker<T, T> tracker = new ServiceTracker<>(context, clazz, customizer);
		tracker.open(true);
	}


	<T> ServiceReference<T> getServiceReference(Class<T> clazz, long timeout) throws InterruptedException {
		ServiceTracker<T, T> tracker = new ServiceTracker<>(context, clazz, null);
		tracker.open();
		tracker.waitForService(timeout);
		return tracker.getServiceReference();
	}

	<T> T getService(Filter filter, long timeout) throws InterruptedException {
		ServiceTracker<T, T> tracker = new ServiceTracker<>(context, filter, null);
		tracker.open();
		return tracker.waitForService(timeout);
	}

}