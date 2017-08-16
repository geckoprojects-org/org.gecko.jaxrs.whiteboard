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
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntime;
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntimeConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Tests the Jersey resource factory
 * @author Mark Hoffmann
 * @since 12.07.2017
 */
@RunWith(MockitoJUnitRunner.class)
public class WhiteboardIntegrationTest {

	private final BundleContext context = FrameworkUtil.getBundle(WhiteboardIntegrationTest.class).getBundleContext();
	private ServiceReference<ConfigurationAdmin> configAdminRef = null;
	private Configuration configuration;

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
	 * Tests start failing because of missing name parameter
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testWhiteboardFailMissingName() throws IOException, InterruptedException {
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;
		
		Dictionary<String, Object> properties = new Hashtable<>();
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		assertNotNull(configAdmin);
		Configuration configuration = configAdmin.getConfiguration("JerseyServiceRuntime", "?");
		assertNotNull(configuration);
		assertEquals(1, configuration.getChangeCount());
		Dictionary<String,Object> factoryProperties = configuration.getProperties();
		assertNull(factoryProperties);
		configuration = configAdmin.getConfiguration("JerseyServiceRuntime", "?");
		
		// put mandatory service properties 
		properties.put(JaxRSServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT, new String[] {url});
		properties.put("service.changecount", Long.valueOf(configuration.getChangeCount()));
//		properties.put(Constants.SERVICE_CHANGECOUNT, Long.valueOf(configuration.getChangeCount()));
		properties.put("rootResource.target", "(osgi.jaxrs.whiteboard.target=test_wb)");
		
		configuration.update(properties);
		ServiceReference<JaxRSServiceRuntime> runtimeRef = getServiceReference(JaxRSServiceRuntime.class, 5000l);
		assertNull(runtimeRef);
		configuration.delete();
		Thread.sleep(2000L);
	}
	
	/**
	 * Tests start failing because of missing change count parameter
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testWhiteboardFailMissingChangeCount() throws IOException, InterruptedException {
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;
		
		Dictionary<String, Object> properties = new Hashtable<>();
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		assertNotNull(configAdmin);
		Configuration configuration = configAdmin.getConfiguration("JerseyServiceRuntime", "?");
		assertNotNull(configuration);
		assertEquals(1, configuration.getChangeCount());
		Dictionary<String,Object> factoryProperties = configuration.getProperties();
		assertNull(factoryProperties);
		configuration = configAdmin.getConfiguration("JerseyServiceRuntime", "?");
		
		// put mandatory service properties 
		properties.put(JaxRSServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT, new String[] {url});
		properties.put("rootResource.target", "(osgi.jaxrs.whiteboard.target=test_wb)");
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		
		configuration.update(properties);
		ServiceReference<JaxRSServiceRuntime> runtimeRef = getServiceReference(JaxRSServiceRuntime.class, 5000l);
		assertNull(runtimeRef);
		configuration.delete();
		Thread.sleep(2000L);
	}
	
	/**
	 * Tests start failing because of missing change count parameter
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testWhiteboardFailMissingUrl() throws IOException, InterruptedException {
		
		Dictionary<String, Object> properties = new Hashtable<>();
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		assertNotNull(configAdmin);
		Configuration configuration = configAdmin.getConfiguration("JerseyServiceRuntime", "?");
		assertNotNull(configuration);
		assertEquals(1, configuration.getChangeCount());
		Dictionary<String,Object> factoryProperties = configuration.getProperties();
		assertNull(factoryProperties);
		configuration = configAdmin.getConfiguration("JerseyServiceRuntime", "?");
		
		// put mandatory service properties 
		properties.put("service.changecount", Long.valueOf(configuration.getChangeCount()));
		properties.put("rootResource.target", "(osgi.jaxrs.whiteboard.target=test_wb)");
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		
		configuration.update(properties);
		ServiceReference<JaxRSServiceRuntime> runtimeRef = getServiceReference(JaxRSServiceRuntime.class, 5000l);
		assertNull(runtimeRef);
		configuration.delete();
		Thread.sleep(2000L);
	}
	
	/**
	 * Tests simple start and stop
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testSimpleWhiteboard() throws IOException, InterruptedException {
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;
		
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		assertNotNull(configAdmin);
		Configuration configuration = configAdmin.getConfiguration("JerseyServiceRuntime", "?");
		assertNotNull(configuration);
		assertEquals(1, configuration.getChangeCount());
		Dictionary<String,Object> factoryProperties = configuration.getProperties();
		assertNull(factoryProperties);
		configuration = configAdmin.getConfiguration("JerseyServiceRuntime", "?");
		
		// put mandatory service properties 
		properties.put(JaxRSServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT, new String[] {url});
		properties.put("service.changecount", Long.valueOf(configuration.getChangeCount()));
//		properties.put(Constants.SERVICE_CHANGECOUNT, Long.valueOf(configuration.getChangeCount()));
		properties.put("rootResource.target", "(&(osgi.jaxrs.resource=true)(osgi.jaxrs.whiteboard.target=test_wb))");
		
		configuration.update(properties);
		ServiceReference<JaxRSServiceRuntime> runtimeRef = getServiceReference(JaxRSServiceRuntime.class, 5000l);
		assertNotNull(runtimeRef);
		
		Object urls = runtimeRef.getProperty("osgi.jaxrs.endpoint");
		assertNotNull(urls);
		assertTrue(urls instanceof String[]);
		
		Thread.sleep(2000l);
		
		JerseyInvocation get = null;
		JerseyClient jerseyClient = JerseyClientBuilder.createClient();
		JerseyWebTarget webTarget = jerseyClient.target(url);
		get = webTarget.request().buildGet();
		Response response = get.invoke();
		assertEquals(200, response.getStatus());
		
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
	
	/**
	 * Tests service modification while changing the port
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testRestartWhiteboard_Port() throws IOException, InterruptedException {
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;
		
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		assertNotNull(configAdmin);
		configuration = configAdmin.getConfiguration("JerseyServiceRuntime", "?");
		assertNotNull(configuration);
		Dictionary<String,Object> factoryProperties = configuration.getProperties();
		assertNull(factoryProperties);
		
		// put mandatory service properties 
		properties.put(JaxRSServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT, new String[] {url});
		properties.put("service.changecount", Long.valueOf(configuration.getChangeCount()));
//		properties.put(Constants.SERVICE_CHANGECOUNT, Long.valueOf(configuration.getChangeCount()));
		properties.put("rootResource.target", "(&(osgi.jaxrs.resource=true)(osgi.jaxrs.whiteboard.target=test_wb))");
		
		configuration.update(properties);
		ServiceReference<JaxRSServiceRuntime> runtimeRef = getServiceReference(JaxRSServiceRuntime.class, 5000l);
		assertNotNull(runtimeRef);
		
		Object urls = runtimeRef.getProperty("osgi.jaxrs.endpoint");
		assertNotNull(urls);
		assertTrue(urls instanceof String[]);
		
		Thread.sleep(2000l);
		
		JerseyInvocation get = null;
		JerseyClient jerseyClient = JerseyClientBuilder.createClient();
		JerseyWebTarget webTarget = jerseyClient.target(url);
		get = webTarget.request().buildGet();
		Response response = get.invoke();
		assertEquals(200, response.getStatus());
		
		/*
		 * Now play the same game, on the new port
		 */
		port++;
		url = "http://localhost:" + port + "/" + contextPath;
		
		System.out.println("Change the port to " + port);
		
		properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		// put mandatory service properties 
		properties.put(JaxRSServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT, new String[] {url});
		properties.put("service.changecount", Long.valueOf(configuration.getChangeCount()));
		properties.put("rootResource.target", "(&(osgi.jaxrs.resource=true)(osgi.jaxrs.whiteboard.target=test_wb))");
		
		configuration.update(properties);
		runtimeRef = getServiceReference(JaxRSServiceRuntime.class, 5000l);
		assertNotNull(runtimeRef);
		
		urls = runtimeRef.getProperty("osgi.jaxrs.endpoint");
		assertNotNull(urls);
		assertTrue(urls instanceof String[]);
		
		// wait for server shutdown
		Thread.sleep(2000L);
		// Check request on the old port
		try {
			get.invoke();
			fail("Not expected to reach this line of code");
		} catch (ProcessingException e) {
			assertNotNull(e.getCause());
			assertTrue(e.getCause() instanceof ConnectException);
		}
		
		// check client on the new port
		webTarget = jerseyClient.target(url);
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
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
	
	/**
	 * Tests service modification while changing the port and context
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testRestartWhiteboard_PortAndContext() throws IOException, InterruptedException {
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;
		
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		assertNotNull(configAdmin);
		configuration = configAdmin.getConfiguration("JerseyServiceRuntime", "?");
		assertNotNull(configuration);
		Dictionary<String,Object> factoryProperties = configuration.getProperties();
		assertNull(factoryProperties);
		
		// put mandatory service properties 
		properties.put(JaxRSServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT, new String[] {url});
		properties.put("service.changecount", Long.valueOf(configuration.getChangeCount()));
//		properties.put(Constants.SERVICE_CHANGECOUNT, Long.valueOf(configuration.getChangeCount()));
		properties.put("rootResource.target", "(&(osgi.jaxrs.resource=true)(osgi.jaxrs.whiteboard.target=test_wb))");
		
		configuration.update(properties);
		ServiceReference<JaxRSServiceRuntime> runtimeRef = getServiceReference(JaxRSServiceRuntime.class, 5000l);
		assertNotNull(runtimeRef);
		
		Object urls = runtimeRef.getProperty("osgi.jaxrs.endpoint");
		assertNotNull(urls);
		assertTrue(urls instanceof String[]);
		
		Thread.sleep(2000l);
		
		JerseyInvocation get = null;
		JerseyClient jerseyClient = JerseyClientBuilder.createClient();
		JerseyWebTarget webTarget = jerseyClient.target(url);
		get = webTarget.request().buildGet();
		Response response = get.invoke();
		assertEquals(200, response.getStatus());
		
		/*
		 * Now play the same game, on the new port and context
		 */
		port++;
		contextPath = "testme";
		url = "http://localhost:" + port + "/" + contextPath;
		
		System.out.println("Change the port to " + port);
		
		properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		// put mandatory service properties 
		properties.put(JaxRSServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT, new String[] {url});
		properties.put("service.changecount", Long.valueOf(configuration.getChangeCount()));
		properties.put("rootResource.target", "(&(osgi.jaxrs.resource=true)(osgi.jaxrs.whiteboard.target=test_wb))");
		
		configuration.update(properties);
		runtimeRef = getServiceReference(JaxRSServiceRuntime.class, 5000l);
		assertNotNull(runtimeRef);
		
		urls = runtimeRef.getProperty("osgi.jaxrs.endpoint");
		assertNotNull(urls);
		assertTrue(urls instanceof String[]);
		
		// wait for server shutdown
		Thread.sleep(2000L);
		// Check request on the old port
		try {
			get.invoke();
			fail("Not expected to reach this line of code");
		} catch (ProcessingException e) {
			assertNotNull(e.getCause());
			assertTrue(e.getCause() instanceof ConnectException);
		}
		
		// check client on the new port
		webTarget = jerseyClient.target(url);
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
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
	
	/**
	 * Tests service modification while changing the port and context
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testRestartWhiteboard_Context() throws IOException, InterruptedException {
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;
		
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		assertNotNull(configAdmin);
		configuration = configAdmin.getConfiguration("JerseyServiceRuntime", "?");
		assertNotNull(configuration);
		Dictionary<String,Object> factoryProperties = configuration.getProperties();
		assertNull(factoryProperties);
		
		// put mandatory service properties 
		properties.put(JaxRSServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT, new String[] {url});
		properties.put("service.changecount", Long.valueOf(configuration.getChangeCount()));
//		properties.put(Constants.SERVICE_CHANGECOUNT, Long.valueOf(configuration.getChangeCount()));
		properties.put("rootResource.target", "(&(osgi.jaxrs.resource=true)(osgi.jaxrs.whiteboard.target=test_wb))");
		
		configuration.update(properties);
		ServiceReference<JaxRSServiceRuntime> runtimeRef = getServiceReference(JaxRSServiceRuntime.class, 5000l);
		assertNotNull(runtimeRef);
		
		Object urls = runtimeRef.getProperty("osgi.jaxrs.endpoint");
		assertNotNull(urls);
		assertTrue(urls instanceof String[]);
		
		Thread.sleep(2000l);
		
		JerseyInvocation get = null;
		JerseyClient jerseyClient = JerseyClientBuilder.createClient();
		String targetUrl = url; 
		JerseyWebTarget webTarget = jerseyClient.target(targetUrl);
		get = webTarget.request().buildGet();
		Response response = get.invoke();
		assertEquals(200, response.getStatus());
		
		/*
		 * Now play the same game, on the new context
		 */
		contextPath = "testme";
		url = "http://localhost:" + port + "/" + contextPath;
		
		System.out.println("Change the port to " + port);
		
		properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		// put mandatory service properties 
		properties.put(JaxRSServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT, new String[] {url});
		properties.put("service.changecount", Long.valueOf(configuration.getChangeCount()));
		properties.put("rootResource.target", "(&(osgi.jaxrs.resource=true)(osgi.jaxrs.whiteboard.target=test_wb))");
		
		configuration.update(properties);
		runtimeRef = getServiceReference(JaxRSServiceRuntime.class, 5000l);
		assertNotNull(runtimeRef);
		
		urls = runtimeRef.getProperty("osgi.jaxrs.endpoint");
		assertNotNull(urls);
		assertTrue(urls instanceof String[]);
		
		// wait for server shutdown
		Thread.sleep(2000L);
		
		// We should get an 404 on the old context
		get.invoke();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
		// check client on the new port
		webTarget = jerseyClient.target(url);
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
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