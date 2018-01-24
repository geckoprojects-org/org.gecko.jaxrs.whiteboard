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
package org.gecko.rest.jersey.tests;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.enterprise.inject.CreationException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;

import org.gecko.rest.jersey.provider.JerseyConstants;
import org.gecko.rest.jersey.tests.applications.AnnotatedTestLegacyApplication;
import org.gecko.rest.jersey.tests.applications.TestLegacyApplication;
import org.gecko.rest.jersey.tests.customizer.ServiceChecker;
import org.gecko.rest.jersey.tests.customizer.TestServiceCustomizer;
import org.gecko.rest.jersey.tests.resources.ContractedExtension;
import org.gecko.rest.jersey.tests.resources.HelloResource;
import org.gecko.rest.jersey.tests.resources.PrototypeExtension;
import org.gecko.rest.jersey.tests.resources.PrototypeResource;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.JerseyInvocation;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.glassfish.jersey.message.internal.EntityInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntime;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Tests the whiteboard dispatcher
 * @author Mark Hoffmann
 * @since 12.10.2017
 */
@RunWith(MockitoJUnitRunner.class)
public class JaxRsWhiteboardChangeCountTest {

	private final BundleContext context = FrameworkUtil.getBundle(JaxRsWhiteboardChangeCountTest.class).getBundleContext();
	
	private ServiceReference<ConfigurationAdmin> configAdminRef = null;

	@Before
	public void before() {
		configAdminRef = context.getServiceReference(ConfigurationAdmin.class);
		assertNotNull(configAdminRef);
	}

	@After
	public void after() {
		if (configAdminRef != null) {
			context.ungetService(configAdminRef);
		}
	}
	
	/**
	 * Tests 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testWhiteboardComponentChangeCountTestApplicationAdd() throws IOException, InterruptedException, InvalidSyntaxException {
		/*
		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
		 *  We mount the system with a resource RootResource under http://localhost:8185/test that will return a 
		 *  HTTP::200 using a GET request
		 */
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;
		
		/*
		 * Initial setup for the REST runtime 
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ServiceChecker<JaxRSServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxRSServiceRuntime.class, context);
		runtimeChecker.start();
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		assertNotNull(configAdmin);
		Configuration configuration = configAdmin.getConfiguration("JaxRsWhiteboardComponent", "?");
		assertNotNull(configuration);
		assertEquals(1, configuration.getChangeCount());
		Dictionary<String,Object> factoryProperties = configuration.getProperties();
		assertNull(factoryProperties);
		configuration.update(properties);

		assertTrue(runtimeChecker.waitCreate());
		/*
		 * Check that the REST runtime service become available 
		 */
		ServiceReference<JaxRSServiceRuntime> runtimeRef = getServiceReference(JaxRSServiceRuntime.class, 40000l);
		assertNotNull(runtimeRef);
		Long firstChangeCount = (Long) runtimeRef.getProperty("service.changecount");
		
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(60);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		/*
		 * Mount the application customer that will become available under: test/customer
		 * http://localhost:8185/test/customer
		 */
		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "customer");
		appProps.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "customerApp");
		ServiceRegistration<Application> appRegistration = context.registerService(Application.class, new Application(), appProps);
		Filter f = FrameworkUtil.createFilter("(" + JaxRSWhiteboardConstants.JAX_RS_NAME + "=customerApp)");
		Application application = getService(f, 3000l);
		assertNotNull(application);
	
		assertTrue(runtimeChecker.waitModify());
		assertTrue(firstChangeCount < (Long) runtimeRef.getProperty("service.changecount"));
		
		appRegistration.unregister();
		
		tearDownTest(configuration, null);
	}

	/**
	 * Tests 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testWhiteboardComponentChangeCountTestApplicationAndResource() throws IOException, InterruptedException, InvalidSyntaxException {
		/*
		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
		 *  We mount the system with a resource RootResource under http://localhost:8185/test that will return a 
		 *  HTTP::200 using a GET request
		 */
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;
		
		/*
		 * Initial setup for the REST runtime 
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ServiceChecker<JaxRSServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxRSServiceRuntime.class, context);
		runtimeChecker.start();
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		assertNotNull(configAdmin);
		Configuration configuration = configAdmin.getConfiguration("JaxRsWhiteboardComponent", "?");
		assertNotNull(configuration);
		assertEquals(1, configuration.getChangeCount());
		Dictionary<String,Object> factoryProperties = configuration.getProperties();
		assertNull(factoryProperties);
		configuration.update(properties);
	
		assertTrue(runtimeChecker.waitCreate());
		
		/*
		 * Check that the REST runtime service become available 
		 */
		ServiceReference<JaxRSServiceRuntime> runtimeRef = getServiceReference(JaxRSServiceRuntime.class, 40000l);
		assertNotNull(runtimeRef);
		JaxRSServiceRuntime runtime = getService(JaxRSServiceRuntime.class, 30000l);
		assertNotNull(runtime);
		
		Long lastChangeCount = (Long) runtimeRef.getProperty("service.changecount");
		System.out.println("----------------------------");
		System.out.println("start service.changecount: " + lastChangeCount);
		System.out.println("----------------------------");
		
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(60);
		runtimeChecker.setModifyCount(2);
		runtimeChecker.start();
		
		/*
		 * Mount the application customer that will become available under: test/customer
		 * http://localhost:8185/test/customer
		 */
		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "customer");
		appProps.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "customerApp");
		ServiceRegistration<Application> appRegistration = context.registerService(Application.class, new Application(), appProps);
		Filter f = FrameworkUtil.createFilter("(" + JaxRSWhiteboardConstants.JAX_RS_NAME + "=customerApp)");
		Application application = getService(f, 3000l);
		assertNotNull(application);
		
		/*
		 * Mount the resource HelloResource that will become available under:
		 * http://localhost:8185/test/hello
		 */
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "Hello");
		helloProps.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxRSWhiteboardConstants.JAX_RS_NAME + "=customerApp)");
		System.out.println("Register resource for uri /hello under application customer");
		ServiceRegistration<HelloResource> helloRegistration = context.registerService(HelloResource.class, new HelloResource(), helloProps);
		f = FrameworkUtil.createFilter("(" + JaxRSWhiteboardConstants.JAX_RS_NAME + "=Hello)");
		Object service = getService(f, 3000l);
		assertNotNull(service);
		
		assertTrue(runtimeChecker.waitModify());
		assertTrue(lastChangeCount < (Long) runtimeRef.getProperty("service.changecount"));
		lastChangeCount = (Long) runtimeRef.getProperty("service.changecount");

		System.out.println("----------------------------");
		System.out.println("Application and Resource Added service.changecount: " + lastChangeCount);
		System.out.println("----------------------------");
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(60);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		helloRegistration.unregister();
		
		assertTrue(runtimeChecker.waitModify());
		assertTrue(lastChangeCount < (Long) runtimeRef.getProperty("service.changecount"));
		lastChangeCount = (Long) runtimeRef.getProperty("service.changecount");
	
		System.out.println("----------------------------");
		System.out.println("Resource removed service.changecount: " + lastChangeCount);
		System.out.println("----------------------------");
		
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(60);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		System.out.println("unregistering =============================");
		appRegistration.unregister();
		System.out.println("unregistered =============================");
		
		
		assertTrue(runtimeChecker.waitModify());
		assertTrue(lastChangeCount < (Long) runtimeRef.getProperty("service.changecount"));
		lastChangeCount = (Long) runtimeRef.getProperty("service.changecount");

		System.out.println("----------------------------");
		System.out.println("Application removed service.changecount: " + lastChangeCount);
		System.out.println("----------------------------");
		
		tearDownTest(configuration, null);
	}
	
	private void tearDownTest(Configuration configuration, JerseyInvocation get) throws IOException, InterruptedException {
		/*
		 * Tear-down the system
		 */
		CountDownLatch deleteLatch = new CountDownLatch(1);
		TestServiceCustomizer<JaxRSServiceRuntime, JaxRSServiceRuntime> c = new TestServiceCustomizer<>(context, null, deleteLatch);
		configuration.delete();
		awaitRemovedService(JaxRSServiceRuntime.class, c);
		deleteLatch.await(10, TimeUnit.SECONDS);
		// wait for server shutdown
		Thread.sleep(2000L);
		if(get != null) {
			try {
				get.invoke();
				fail("Not expected to reach this line of code");
			} catch (ProcessingException e) {
				assertNotNull(e.getCause());
				assertTrue(e.getCause() instanceof ConnectException);
			}
		}
	}

	private <T extends Object> ServiceChecker<T> createdCheckerTrackedForCleanUp(Class<T> serviceClass, BundleContext context) {
		ServiceChecker<T> checker = new ServiceChecker<>(serviceClass, context);

		checker.setCreateCount(1);
		checker.setDeleteCount(1);
		checker.setCreateTimeout(5000);
		checker.setDeleteTimeout(5000);
		return (ServiceChecker<T>) checker;
	}

	private <T extends Object> ServiceChecker<T>  createdCheckerTrackedForCleanUp(String filter, BundleContext context) throws InvalidSyntaxException {
		ServiceChecker<? extends Object> checker = new ServiceChecker<>(filter, context);
		
		checker.setCreateCount(1);
		checker.setDeleteCount(1);
		checker.setCreateTimeout(5);
		checker.setDeleteTimeout(5);
		return (ServiceChecker<T>) checker;
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
