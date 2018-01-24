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
public class JaxRsWhiteboardComponentTest {

	private final BundleContext context = FrameworkUtil.getBundle(JaxRsWhiteboardComponentTest.class).getBundleContext();
	
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
	public void testWhiteboardComponentApplicationAndResourceTest() throws IOException, InterruptedException, InvalidSyntaxException {
		/*
		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
		 *  We mount the system with a resource RootResource under http://localhost:8185/test that will return a 
		 *  HTTP::200 using a GET request
		 */
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;
		
		ServiceChecker<JaxRSServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxRSServiceRuntime.class, context);
		runtimeChecker.start();
		
		/*
		 * Initial setup for the REST runtime 
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		assertNotNull(configAdmin);
		Configuration configuration = configAdmin.getConfiguration("JaxRsWhiteboardComponent", "?");
		assertNotNull(configuration);
		assertEquals(1, configuration.getChangeCount());
		Dictionary<String,Object> factoryProperties = configuration.getProperties();
		assertNull(factoryProperties);
		configuration.update(properties);
		
		assertTrue(runtimeChecker.waitCreate());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		runtimeChecker.waitModify();
		
		/*
		 * Check if our RootResource is available under http://localhost:8185/test
		 */
		System.out.println("Checking URL is available: " + url);
		JerseyInvocation get = null;
		JerseyClient jerseyClient = JerseyClientBuilder.createClient();
		JerseyWebTarget webTarget = jerseyClient.target(url);
		get = webTarget.request().buildGet();
		Response response = get.invoke();
		assertEquals(404, response.getStatus());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
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
		/*
		 * Check if http://localhost:8185/test/customer/hello is available now. 
		 * Check as well, if http://localhost:8185/test is /hello is not available
		 */
		System.out.println("Checking URL is available " + url + "/customer/hello");
		webTarget = jerseyClient.target(url + "/customer/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
		System.out.println("Checking URL is not available " + url + "/hello");
		webTarget = jerseyClient.target(url + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(2);
		runtimeChecker.start();
		
		helloRegistration.unregister();
		service = getService(f, 3000l);
		assertNull(service);
		
		appRegistration.unregister();
		service = getService(f, 3000l);
		assertNull(service);
		
		assertTrue(runtimeChecker.waitModify());
		/*
		 * Check if http://localhost:8185/test/customer/hello is not available anymore. 
		 * Check as well, if http://localhost:8185/test/hello is still not available
		 */
		System.out.println("Checking URL is not available anymore " + url + "/customer/hello");
		webTarget = jerseyClient.target(url + "/customer/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
		System.out.println("Checking URL is not available anymore " + url + "/hello");
		webTarget = jerseyClient.target(url + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
		tearDownTest(configuration, get);
	}
	
	/**
	 * Tests 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testWhiteboardComponentApplicationAndResourceWildcard() throws IOException, InterruptedException, InvalidSyntaxException {
		/*
		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
		 *  We mount the system with a resource RootResource under http://localhost:8185/test that will return a 
		 *  HTTP::200 using a GET request
		 */
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;
		
		ServiceChecker<JaxRSServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxRSServiceRuntime.class, context);
		runtimeChecker.start();
		
		/*
		 * Initial setup for the REST runtime 
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		assertNotNull(configAdmin);
		Configuration configuration = configAdmin.getConfiguration("JaxRsWhiteboardComponent", "?");
		assertNotNull(configuration);
		assertEquals(1, configuration.getChangeCount());
		Dictionary<String,Object> factoryProperties = configuration.getProperties();
		assertNull(factoryProperties);
		configuration.update(properties);
		
		assertTrue(runtimeChecker.waitCreate());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		runtimeChecker.waitModify();
		
		/*
		 * Check if our RootResource is available under http://localhost:8185/test
		 */
		System.out.println("Checking URL is available: " + url);
		JerseyInvocation get = null;
		JerseyClient jerseyClient = JerseyClientBuilder.createClient();
		JerseyWebTarget webTarget = jerseyClient.target(url);
		get = webTarget.request().buildGet();
		Response response = get.invoke();
		assertEquals(404, response.getStatus());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
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
		Filter appFilter = FrameworkUtil.createFilter("(" + JaxRSWhiteboardConstants.JAX_RS_NAME + "=customerApp)");
		Application application = getService(appFilter, 3000l);
		assertNotNull(application);
		
		/*
		 * Mount the resource HelloResource that will become available under:
		 * http://localhost:8185/test/hello
		 */
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "Hello");
		helloProps.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxRSWhiteboardConstants.JAX_RS_NAME + "=*)");
		System.out.println("Register resource for uri /hello under application customer");
		ServiceRegistration<Object> helloRegistration = context.registerService(Object.class, new HelloResource(), helloProps);
		Filter resourceFilter = FrameworkUtil.createFilter("(" + JaxRSWhiteboardConstants.JAX_RS_NAME + "=Hello)");
		Object service = getService(resourceFilter, 3000l);
		assertNotNull(service);
		
		assertTrue(runtimeChecker.waitModify());
		
		/*
		 * Check if http://localhost:8185/test/customer/hello is available now. 
		 * Check as well, if http://localhost:8185/test is /hello is not available
		 */
		System.out.println("Checking URL is available " + url + "/customer/hello");
		webTarget = jerseyClient.target(url + "/customer/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
		System.out.println("Checking URL is available " + url + "/hello");
		webTarget = jerseyClient.target(url + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		appRegistration.unregister();
		
		assertTrue(runtimeChecker.waitModify());
		
		/*
		 * Check if http://localhost:8185/test/customer/hello is not available anymore. 
		 * Check as well, if http://localhost:8185/test/hello is still not available
		 */
		System.out.println("Checking URL is not available anymore " + url + "/customer/hello");
		webTarget = jerseyClient.target(url + "/customer/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
		System.out.println("Checking URL is still available " + url + "/hello");
		webTarget = jerseyClient.target(url + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		
		helloRegistration.unregister();
		
		assertTrue(runtimeChecker.waitModify());
		
		System.out.println("Checking URL is not available anymore " + url + "/hello");
		webTarget = jerseyClient.target(url + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
		tearDownTest(configuration, get);
	}
	
	/**
	 * Tests 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testWhiteboardComponentLegacyApplication() throws IOException, InterruptedException, InvalidSyntaxException {
		/*
		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
		 *  We mount the system with a resource RootResource under http://localhost:8185/test that will return a 
		 *  HTTP::200 using a GET request
		 */
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;
		
		ServiceChecker<JaxRSServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxRSServiceRuntime.class, context);
		runtimeChecker.start();
		
		/*
		 * Initial setup for the REST runtime 
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		assertNotNull(configAdmin);
		Configuration configuration = configAdmin.getConfiguration("JaxRsWhiteboardComponent", "?");
		assertNotNull(configuration);
		assertEquals(1, configuration.getChangeCount());
		Dictionary<String,Object> factoryProperties = configuration.getProperties();
		assertNull(factoryProperties);
		configuration.update(properties);
		
		assertTrue(runtimeChecker.waitCreate());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		runtimeChecker.waitModify();
		
		/*
		 * Check if our RootResource is not available under http://localhost:8185/test
		 */
		System.out.println("Checking URL is not available: " + url);
		JerseyInvocation get = null;
		JerseyClient jerseyClient = JerseyClientBuilder.createClient();
		JerseyWebTarget webTarget = jerseyClient.target(url);
		get = webTarget.request().buildGet();
		Response response = get.invoke();
		assertEquals(404, response.getStatus());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		
		/*
		 * Mount the application customer that will become available under: test/customer
		 * http://localhost:8185/test/customer
		 */
		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "legacy");
		appProps.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "legacyApp");
		ServiceRegistration<Application> appRegistration = context.registerService(Application.class, new TestLegacyApplication(), appProps);
		Filter appFilter = FrameworkUtil.createFilter("(" + JaxRSWhiteboardConstants.JAX_RS_NAME + "=legacyApp)");
		Application application = getService(appFilter, 3000l);
		assertNotNull(application);
		
		assertTrue(runtimeChecker.waitModify());
		
		/*
		 * Check if http://localhost:8185/test/customer/hello is available now. 
		 * Check as well, if http://localhost:8185/test is /hello is not available
		 */
		System.out.println("Checking URL is available " + url + "/legacy/hello/mark");
		webTarget = jerseyClient.target(url + "/legacy/hello/mark");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		appRegistration.unregister();
		
		assertTrue(runtimeChecker.waitModify());
		
		
		/*
		 * Check if http://localhost:8185/test/customer/hello is not available anymore. 
		 * Check as well, if http://localhost:8185/test/hello is still not available
		 */
		System.out.println("Checking URL is not available anymore " + url + "/legacy/hello/mark");
		webTarget = jerseyClient.target(url + "/legacy/hello/mark");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
		tearDownTest(configuration, get);
	}

	
	
	/**
	 * Tests ---- before 88s 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testWhiteboardComponentAnnotatedLegacyApplication() throws IOException, InterruptedException, InvalidSyntaxException {
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
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		runtimeChecker.waitModify();
		
		/*
		 * Check if our RootResource is not available under http://localhost:8185/test
		 */
		System.out.println("Checking URL is not available: " + url);
		JerseyInvocation get = null;
		JerseyClient jerseyClient = JerseyClientBuilder.createClient();
		JerseyWebTarget webTarget = jerseyClient.target(url);
		get = webTarget.request().buildGet();
		Response response = get.invoke();
		assertEquals(404, response.getStatus());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		/*
		 * Mount the application customer that will become available under: test/customer
		 * http://localhost:8185/test/customer
		 */
		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, "legacy");
		appProps.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "legacyApp");
		ServiceRegistration<Application> appRegistration = context.registerService(Application.class, new AnnotatedTestLegacyApplication(), appProps);
		Filter appFilter = FrameworkUtil.createFilter("(" + JaxRSWhiteboardConstants.JAX_RS_NAME + "=legacyApp)");
		Application application = getService(appFilter, 3000l);
		assertNotNull(application);
		
		assertTrue(runtimeChecker.waitModify());
		
		/*
		 * Check if http://localhost:8185/test/customer/hello is available now. 
		 * Check as well, if http://localhost:8185/test is /hello is not available
		 */
		System.out.println("Checking URL is available " + url + "/legacy/annotated/hello/mark");
		webTarget = jerseyClient.target(url + "/legacy/annotated/hello/mark");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(60);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		appRegistration.unregister();
		application = getService(appFilter, 3000l);
		assertNull(application);
		
		assertTrue(runtimeChecker.waitModify());
		
		/*
		 * Check if http://localhost:8185/test/customer/hello is not available anymore. 
		 * Check as well, if http://localhost:8185/test/hello is still not available
		 */
		System.out.println("Checking URL is not available anymore " + url + "/legacy/annotated/hello/mark");
		webTarget = jerseyClient.target(url + "/legacy/annotated/hello/mark");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
		tearDownTest(configuration, get);
	}
	
	/**
	 * Tests 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testWhiteboardComponentDefaultResourceTest() throws IOException, InterruptedException, InvalidSyntaxException {
		/*
		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
		 *  We mount the system with a resource RootResource under http://localhost:8185/test that will return a 
		 *  HTTP::200 using a GET request
		 */
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;
		
		ServiceChecker<JaxRSServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxRSServiceRuntime.class, context);
		runtimeChecker.start();
		
		/*
		 * Initial setup for the REST runtime 
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		assertNotNull(configAdmin);
		Configuration configuration = configAdmin.getConfiguration("JaxRsWhiteboardComponent", "?");
		assertNotNull(configuration);
		assertEquals(1, configuration.getChangeCount());
		Dictionary<String,Object> factoryProperties = configuration.getProperties();
		assertNull(factoryProperties);
		configuration.update(properties);
		
		assertTrue(runtimeChecker.waitCreate());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		runtimeChecker.waitModify();
		
		/*
		 * Check if our RootResource is available under http://localhost:8185/test
		 */
		System.out.println("Checking URL is available: " + url);
		JerseyInvocation get = null;
		JerseyClient jerseyClient = JerseyClientBuilder.createClient();
		JerseyWebTarget webTarget = jerseyClient.target(url);
		get = webTarget.request().buildGet();
		Response response = get.invoke();
		assertEquals(404, response.getStatus());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		/*
		 * Mount the resource HelloResource that will become available under:
		 * http://localhost:8185/test/hello
		 */
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "Hello");
		System.out.println("Register resource for uri /hello");
		ServiceRegistration<Object> helloRegistration = context.registerService(Object.class, new HelloResource(), helloProps);
		
		assertTrue(runtimeChecker.waitModify());
		
		/*
		 * Check if http://localhost:8185/test/hello is available now. 
		 * Check as well, if http://localhost:8185/test is still available
		 */
		System.out.println("Checking URL is available " + url + "/hello");
		webTarget = jerseyClient.target(url + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		helloRegistration.unregister();

		assertTrue(runtimeChecker.waitModify());
				
		/*
		 * Check if http://localhost:8185/test/hello is not available anymore. 
		 * Check as well, if http://localhost:8185/test is still available
		 */
		System.out.println("Checking URL is not available anymore " + url + "/hello");
		webTarget = jerseyClient.target(url + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
		tearDownTest(configuration, get);
	}
	
	/**
	 * Tests 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testWhiteboardComponentDefaultResourceAvailableBeforeStart() throws IOException, InterruptedException, InvalidSyntaxException {
		/*
		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
		 *  We mount the system with a resource RootResource under http://localhost:8185/test that will return a 
		 *  HTTP::200 using a GET request
		 */
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;
		
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
		
		ServiceChecker<JaxRSServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxRSServiceRuntime.class, context);
		runtimeChecker.start();
		
		/*
		 * Initial setup for the REST runtime 
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		assertNotNull(configAdmin);
		Configuration configuration = configAdmin.getConfiguration("JaxRsWhiteboardComponent", "?");
		assertNotNull(configuration);
		assertEquals(1, configuration.getChangeCount());
		Dictionary<String,Object> factoryProperties = configuration.getProperties();
		assertNull(factoryProperties);
		configuration.update(properties);
		
		assertTrue(runtimeChecker.waitCreate());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		runtimeChecker.waitModify();
		
		/*
		 * Check if http://localhost:8185/test/hello is available now. 
		 * Check as well, if http://localhost:8185/test is still available
		 */
		System.out.println("Checking URL is available " + url + "/hello");
		JerseyInvocation get = null;
		JerseyClient jerseyClient = JerseyClientBuilder.createClient();
		JerseyWebTarget webTarget = jerseyClient.target(url + "/hello");
		get = webTarget.request().buildGet();
		Response response = get.invoke();
		assertEquals(200, response.getStatus());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		helloRegistration.unregister();
		service = getService(f, 3000l);
		
		assertTrue(runtimeChecker.waitModify());
		
		/*
		 * Check if http://localhost:8185/test/hello is not available anymore. 
		 * Check as well, if http://localhost:8185/test is still available
		 */
		System.out.println("Checking URL is not available anymore " + url + "/hello");
		webTarget = jerseyClient.target(url + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
		tearDownTest(configuration, get);
		System.out.println("==================================================");
	}
	
	/**
	 * Tests 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testWhiteboardComponentDefaultPrototype() throws IOException, InterruptedException, InvalidSyntaxException {
		/*
		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
		 *  We mount the system with a resource PrototypeResource under http://localhost:8185/test that will return a 
		 *  HTTP::200 using a GET request
		 */
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;
		
		ServiceChecker<JaxRSServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxRSServiceRuntime.class, context);
		runtimeChecker.start();
		
		/*
		 * Initial setup for the REST runtime 
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		assertNotNull(configAdmin);
		Configuration configuration = configAdmin.getConfiguration("JaxRsWhiteboardComponent", "?");
		assertNotNull(configuration);
		assertEquals(1, configuration.getChangeCount());
		Dictionary<String,Object> factoryProperties = configuration.getProperties();
		assertNull(factoryProperties);
		configuration.update(properties);
		
		assertTrue(runtimeChecker.waitCreate());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		runtimeChecker.waitModify();
		
		Filter resFilter = FrameworkUtil.createFilter("(osgi.jaxrs.name=ptr)");
		Object ptrResource = getService(resFilter, 3000l);
		assertNotNull(ptrResource);
		
		
		CountDownLatch cdl = new CountDownLatch(1);
		cdl.await(2, TimeUnit.SECONDS);
		
		/*
		 * Check if our RootResource is available under http://localhost:8185/test
		 */
		String checkUrl = url + "/test";
		System.out.println("Checking URL is available: " + checkUrl);
		JerseyInvocation get = null;
		JerseyClient jerseyClient = JerseyClientBuilder.createClient();
		JerseyWebTarget webTarget = jerseyClient.target(checkUrl);
		get = webTarget.request().buildGet();
		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		Response response = get.invoke();
		assertEquals(200, response.getStatus());
		assertNotNull(response.getEntity());
		String result01 = response.readEntity(String.class);
		assertNotNull(result01);
		
		assertTrue(result01.startsWith(PrototypeResource.PROTOTYPE_PREFIX));
		System.out.println(result01);
		assertTrue(result01.contains(PrototypeResource.PROTOTYPE_POSTFIX));
		assertTrue(result01.endsWith(PrototypeExtension.PROTOTYPE_POSTFIX));
		
		tearDownTest(configuration, get);
	}

	/**
	 * Tests 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testWhiteboardComponentExtensionContracts() throws IOException, InterruptedException, InvalidSyntaxException {
		/*
		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
		 *  We mount the system with a resource PrototypeResource under http://localhost:8185/test that will return a 
		 *  HTTP::200 using a GET request
		 */
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;
		
		ServiceChecker<JaxRSServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxRSServiceRuntime.class, context);
		runtimeChecker.start();
		
		/*
		 * Initial setup for the REST runtime 
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		assertNotNull(configAdmin);
		Configuration configuration = configAdmin.getConfiguration("JaxRsWhiteboardComponent", "?");
		assertNotNull(configuration);
		assertEquals(1, configuration.getChangeCount());
		Dictionary<String,Object> factoryProperties = configuration.getProperties();
		assertNull(factoryProperties);
		configuration.update(properties);
		
		assertTrue(runtimeChecker.waitCreate());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		runtimeChecker.waitModify();

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
		 * Mount the extension ContractedExtension that will become available under:
		 * http://localhost:8185/test/hello
		 */
		Dictionary<String, Object> extensionProps = new Hashtable<>();
		extensionProps.put(JaxRSWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "Contracted");
		extensionProps.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxRSWhiteboardConstants.JAX_RS_NAME + "=customerApp)");
		
		System.out.println("Register resource for uri /hello");
		ServiceRegistration<?> contractedRegistration = context.registerService(ContractedExtension.class, new ContractedExtension(), extensionProps);
		
		/*
		 * Mount the resource HelloResource that will become available under:
		 * http://localhost:8185/test/hello
		 */
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "Hello");
		helloProps.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxRSWhiteboardConstants.JAX_RS_NAME + "=customerApp)");
		
		System.out.println("Register resource for uri /hello");
		ServiceRegistration<HelloResource> helloRegistration = context.registerService(HelloResource.class, new HelloResource(), helloProps);

		f = FrameworkUtil.createFilter("(" + JaxRSWhiteboardConstants.JAX_RS_NAME + "=Hello)");
		Object service = getService(f, 3000l);
		assertNotNull(service);

//		f = FrameworkUtil.createFilter("(" + JaxRSWhiteboardConstants.JAX_RS_NAME + "=Contracted)");
//		service = getService(f, 3000l);
//		assertNotNull(service);
		
		
		CountDownLatch cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		
		/*
		 * Check if our RootResource is available under http://localhost:8185/test
		 */
		String checkUrl = url + "/customer/hello";
		System.out.println("Checking URL is available: " + checkUrl);
		JerseyInvocation post = null;
		JerseyClient jerseyClient = JerseyClientBuilder.createClient();
		JerseyWebTarget webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildPost(Entity.entity("test", "application/testThing"));
		Response response = post.invoke();
		assertEquals(200, response.getStatus());
		assertNotNull(response.getEntity());
		String result01 = response.readEntity(String.class);
		System.out.println(result01);
		assertNotNull(result01);
		
		assertTrue(result01.contains(ContractedExtension.READER_POSTFIX));
		assertTrue(result01.contains(ContractedExtension.WRITER_POSTFIX));

		contractedRegistration.unregister();
		contractedRegistration = context.registerService(MessageBodyReader.class, new ContractedExtension(), extensionProps);
		
		cdl.await(2, TimeUnit.SECONDS);
		
		response = post.invoke();
		assertEquals(200, response.getStatus());
		assertNotNull(response.getEntity());
		result01 = response.readEntity(String.class);
		System.out.println(result01);
		assertNotNull(result01);
		
		assertTrue(result01.contains(ContractedExtension.READER_POSTFIX));
		assertFalse(result01.contains(ContractedExtension.WRITER_POSTFIX));
		
		
		
		helloRegistration.unregister();
		contractedRegistration.unregister();
		appRegistration.unregister();
		
		tearDownTest(configuration, post);
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


}
