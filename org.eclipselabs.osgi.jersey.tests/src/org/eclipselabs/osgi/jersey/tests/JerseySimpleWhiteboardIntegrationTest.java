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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;

import org.eclipselabs.osgi.jersey.JerseyConstants;
import org.eclipselabs.osgi.jersey.tests.customizer.TestServiceCustomizer;
import org.eclipselabs.osgi.jersey.tests.resources.HelloResource;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.JerseyInvocation;
import org.glassfish.jersey.client.JerseyWebTarget;
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
 * Tests the Jersey resource factory
 * @author Mark Hoffmann
 * @since 12.07.2017
 */
@RunWith(MockitoJUnitRunner.class)
public class JerseySimpleWhiteboardIntegrationTest {

	private final BundleContext context = FrameworkUtil.getBundle(JerseySimpleWhiteboardIntegrationTest.class).getBundleContext();
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
	 * Tests simple start and lazy start of REST resources
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testSimpleWhiteboard() throws IOException, InterruptedException, InvalidSyntaxException {
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
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		assertNotNull(configAdmin);
		Configuration configuration = configAdmin.getConfiguration("JaxRsRuntimeComponent", "?");
		assertNotNull(configuration);
		assertEquals(1, configuration.getChangeCount());
		Dictionary<String,Object> factoryProperties = configuration.getProperties();
		assertNull(factoryProperties);
		configuration.update(properties);
		
		/*
		 * Check that the REST runtime service become available 
		 */
		ServiceReference<JaxRSServiceRuntime> runtimeRef = getServiceReference(JaxRSServiceRuntime.class, 40000l);
		assertNotNull(runtimeRef);
		JaxRSServiceRuntime runtime = getService(JaxRSServiceRuntime.class, 30000l);
		assertNotNull(runtime);
		
		CountDownLatch cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		
		/*
		 * Check if our RootResource is available under http://localhost:8185/test
		 */
		System.out.println("Checking URL is available" + url);
		JerseyInvocation get = null;
		JerseyClient jerseyClient = JerseyClientBuilder.createClient();
		JerseyWebTarget webTarget = jerseyClient.target(url);
		get = webTarget.request().buildGet();
		Response response = get.invoke();
		assertEquals(200, response.getStatus());
		
		/*
		 * Check if http://localhost:8185/test/hello is not available yet. 
		 * We will mount this in a moment
		 */
		System.out.println("Checking URL is not available " + url + "/hello");
		webTarget = jerseyClient.target(url + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
		/*
		 * Mount the resource HelloResource that will become available under:
		 * http://localhost:8185/test/hello
		 */
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "Hello");
		System.out.println("Register resource for uri /hello");
		ServiceRegistration<Object> helloRegistration = context.registerService(Object.class, new HelloResource(), helloProps);
		Filter f = FrameworkUtil.createFilter("(" + JaxRSWhiteboardConstants.JAX_RS_NAME + "=Hello)");
		Object service = getService(f, 3000l);
		assertNotNull(service);

		/*
		 * Wait a short time to reload the configuration dynamically
		 */
		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		
		/*
		 * Check if http://localhost:8185/test/hello is available now. 
		 * Check as well, if http://localhost:8185/test is still available
		 */
		System.out.println("Checking URL is available " + url + "/hello");
		webTarget = jerseyClient.target(url + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		System.out.println("Checking URL is still available " + url);
		webTarget = jerseyClient.target(url);
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
		helloRegistration.unregister();
		service = getService(f, 3000l);
		assertNull(service);
		
		/*
		 * Wait a short time to reload the configuration dynamically
		 */
		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		
		/*
		 * Check if http://localhost:8185/test/hello is not available anymore. 
		 * Check as well, if http://localhost:8185/test is still available
		 */
		System.out.println("Checking URL is not available anymore " + url + "/hello");
		webTarget = jerseyClient.target(url + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		System.out.println("Checking URL is still available " + url);
		webTarget = jerseyClient.target(url);
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
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
		assertNotNull(get);
		try {
			get.invoke();
			fail("Not expected to reach this line of code");
		} catch (ProcessingException e) {
			assertNotNull(e.getCause());
			assertTrue(e.getCause() instanceof ConnectException);
		}
		
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