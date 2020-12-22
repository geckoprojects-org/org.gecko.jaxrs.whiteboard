/**
 * Copyright (c) 2012 - 2018 Data In Motion and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;

import org.gecko.rest.jersey.provider.JerseyConstants;
import org.gecko.rest.jersey.tests.applications.AnnotatedTestLegacyApplication;
import org.gecko.rest.jersey.tests.applications.TestLegacyApplication;
import org.gecko.rest.jersey.tests.resources.ContractedExtension;
import org.gecko.rest.jersey.tests.resources.HelloResource;
import org.gecko.rest.jersey.tests.resources.PrototypeExtension;
import org.gecko.rest.jersey.tests.resources.PrototypeResource;
import org.gecko.util.test.common.service.ServiceChecker;
import org.gecko.util.test.common.test.AbstractOSGiTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

/**
 * Tests the whiteboard dispatcher
 * @author Mark Hoffmann
 * @since 12.10.2017
 */
@SuppressWarnings("deprecation")
@RunWith(MockitoJUnitRunner.class)
public class JaxRsWhiteboardComponentTest extends AbstractOSGiTest{

	/**
	 * Creates a new instance.
	 * @param bundleContext
	 */
	public JaxRsWhiteboardComponentTest() {
		super(FrameworkUtil.getBundle(JaxRsWhiteboardComponentTest.class).getBundleContext());
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
		
		/*
		 * Initial setup for the REST runtime 
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		
		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);		
		
		assertTrue(runtimeChecker.waitCreate());
		
		/*
		 * Check if our RootResource is available under http://localhost:8185/test
		 */
		System.out.println("Checking URL is available: " + url);
		Invocation get = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(url);
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
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "customer");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "customerApp");
		
		Application application = new Application();
		registerServiceForCleanup(Application.class, application, appProps);

		assertTrue(runtimeChecker.waitModify());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(500);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		/*
		 * Mount the resource HelloResource that will become available under:
		 * http://localhost:8185/test/hello
		 */
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, true);
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");
		System.out.println("Register resource for uri /hello under application customer");
		HelloResource helloResource = new HelloResource();
		registerServiceForCleanup(HelloResource.class, helloResource, helloProps);
		
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
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		unregisterService(application);
		unregisterService(helloResource);
		
		assertTrue(runtimeChecker.waitModify());
		
		Thread.sleep(2000);
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
	}
	
	/**
	 * Tests 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testWhiteboardComponentApplicationAndResourceContextPathChange() throws IOException, InterruptedException, InvalidSyntaxException {
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
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		System.out.println("Register resource for uri /hello");
		HelloResource helloResource = new HelloResource();
		registerServiceForCleanup(HelloResource.class, helloResource , helloProps);
		
		/*
		 * Initial setup for the REST runtime 
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		
		Configuration configuration = createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);
		
		
		assertTrue(runtimeChecker.waitCreate());
		
		/*
		 * Check if http://localhost:8185/test/hello is available now. 
		 * Check as well, if http://localhost:8185/test is still available
		 */
		System.out.println("Checking URL is available " + url + "/hello");
		Invocation get = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(url + "/hello");
		get = webTarget.request().buildGet();
		Response response = get.invoke();
		assertEquals(200, response.getStatus());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath + "2");
		configuration.update(properties);
		
		assertTrue(runtimeChecker.waitModify());
		CountDownLatch latch = new CountDownLatch(1);
		latch.await(1, TimeUnit.SECONDS);
		/*
		 * Check if http://localhost:8185/test/hello is not available anymore. 
		 * Check as well, if http://localhost:8185/test is still available
		 */
		System.out.println("Checking URL is available anymore " + url + "2/hello");
		webTarget = jerseyClient.target(url + "2/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
	}
	
	/**
	 * Tests 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testWhiteboardComponentApplicationAndResourceContextPortChange() throws IOException, InterruptedException, InvalidSyntaxException {
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
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		System.out.println("Register resource for uri /hello");
		HelloResource helloResource = new HelloResource();
		registerServiceForCleanup(HelloResource.class, helloResource , helloProps);
		
		/*
		 * Initial setup for the REST runtime 
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		
		Configuration configuration = createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);
		
		
		assertTrue(runtimeChecker.waitCreate());
		
		/*
		 * Check if http://localhost:8185/test/hello is available now. 
		 * Check as well, if http://localhost:8185/test is still available
		 */
		System.out.println("Checking URL is available " + url + "/hello");
		Invocation get = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(url + "/hello");
		get = webTarget.request().buildGet();
		Response response = get.invoke();
		assertEquals(200, response.getStatus());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		port += 1;
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		configuration.update(properties);
		
		assertTrue(runtimeChecker.waitModify());
		CountDownLatch latch = new CountDownLatch(1);
		latch.await(1, TimeUnit.SECONDS);
		/*
		 * Check if http://localhost:8185/test/hello is not available anymore. 
		 * Check as well, if http://localhost:8185/test is still available
		 */
		url = "http://localhost:" + port + "/" + contextPath;
		System.out.println("Checking URL is available anymore " + url + "/hello");
		webTarget = jerseyClient.target(url + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
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
		
		/*
		 * Initial setup for the REST runtime 
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		
		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);
		
		
		assertTrue(runtimeChecker.waitCreate());
		
		/*
		 * Check if our RootResource is available under http://localhost:8185/test
		 */
		System.out.println("Checking URL is available: " + url);
		Invocation get = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(url);
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
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "customer");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "customerApp");
		Application customerApp = new Application();
		registerServiceForCleanup(Application.class, customerApp, appProps);
		
		/*
		 * Mount the resource HelloResource that will become available under:
		 * http://localhost:8185/test/hello
		 */
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=*)");
		
		System.out.println("Register resource for uri /hello under application customer");
		HelloResource helloResource = new HelloResource();
		registerServiceForCleanup(HelloResource.class, helloResource , helloProps);
		
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
		
		unregisterService(customerApp);
		
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
		
		unregisterService(helloResource);
			
		Thread.sleep(1000);
//		assertTrue(runtimeChecker.waitModify());
		
		System.out.println("Checking URL is not available anymore " + url + "/hello");
		webTarget = jerseyClient.target(url + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
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
		
		/*
		 * Initial setup for the REST runtime 
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);
		
		assertTrue(runtimeChecker.waitCreate());
		
		/*
		 * Check if our RootResource is not available under http://localhost:8185/test
		 */
		System.out.println("Checking URL is not available: " + url);
		Invocation get = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(url);
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
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "legacy");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "legacyApp");
	
		TestLegacyApplication testLegacyApplication = new TestLegacyApplication();
		
		registerServiceForCleanup(Application.class, testLegacyApplication, appProps);
		
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

		/*
		 * Check if http://localhost:8185/test/customer/hello is available now. 
		 * Check as well, if http://localhost:8185/test is /hello is not available
		 */
		System.out.println("Checking URL is available " + url + "/legacy/singleton/hello/mark");
		webTarget = jerseyClient.target(url + "/legacy/singleton/hello/mark");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		unregisterService(testLegacyApplication);
		
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
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		
		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);
		
		
		assertTrue(runtimeChecker.waitCreate());
		
		/*
		 * Check if our RootResource is not available under http://localhost:8185/test
		 */
		System.out.println("Checking URL is not available: " + url);
		Invocation get = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(url);
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
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "legacy");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "legacyApp");
		AnnotatedTestLegacyApplication annotatedTestLegacyApplication = new AnnotatedTestLegacyApplication();
		registerServiceForCleanup(Application.class, annotatedTestLegacyApplication, appProps);
		
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
		
		unregisterService(annotatedTestLegacyApplication);
		
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
		
		/*
		 * Initial setup for the REST runtime 
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		
		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);
		
		
		assertTrue(runtimeChecker.waitCreate());
		
		/*
		 * Check if our RootResource is available under http://localhost:8185/test
		 */
		System.out.println("Checking URL is available: " + url);
		Invocation get = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(url);
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
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		System.out.println("Register resource for uri /hello");
		HelloResource helloResource = new HelloResource();
		registerServiceForCleanup(Object.class, helloResource, helloProps);
		
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
		
		unregisterService(helloResource);

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
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		System.out.println("Register resource for uri /hello");
		HelloResource helloResource = new HelloResource();
		registerServiceForCleanup(Object.class, helloResource , helloProps);
		
		/*
		 * Initial setup for the REST runtime 
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		
		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);
		
		
		assertTrue(runtimeChecker.waitCreate());
		
		/*
		 * Check if http://localhost:8185/test/hello is available now. 
		 * Check as well, if http://localhost:8185/test is still available
		 */
		System.out.println("Checking URL is available " + url + "/hello");
		Invocation get = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(url + "/hello");
		get = webTarget.request().buildGet();
		Response response = get.invoke();
		assertEquals(200, response.getStatus());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		unregisterService(helloResource);
		
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
		
		System.out.println("==================================================");
	}
	
	/**
	 * Tests 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
//	@Test
	public void testWhiteboardComponentDefaultPrototype() throws IOException, InterruptedException, InvalidSyntaxException {
		/*
		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
		 *  We mount the system with a resource PrototypeResource under http://localhost:8185/test that will return a 
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
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		
		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);
		
		assertTrue(runtimeChecker.waitCreate());
		
		Filter resFilter = FrameworkUtil.createFilter("(osgi.jaxrs.name=ptr)");
		Object ptrResource = getService(resFilter, 1000l);
		assertNotNull(ptrResource);
		
		
		CountDownLatch cdl = new CountDownLatch(1);
		cdl.await(2, TimeUnit.SECONDS);
		
		/*
		 * Check if our RootResource is available under http://localhost:8185/test
		 */
		String checkUrl = url + "/test";
		System.out.println("Checking URL is available: " + checkUrl);
		Invocation get = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(checkUrl);
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
	}

	/**
	 * Tests 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
//	@Test
	public void testWhiteboardComponentExtensionContracts() throws IOException, InterruptedException, InvalidSyntaxException {
		/*
		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
		 *  We mount the system with a resource PrototypeResource under http://localhost:8185/test that will return a 
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
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);
		
		
		assertTrue(runtimeChecker.waitCreate());

		/*
		 * Mount the application customer that will become available under: test/customer
		 * http://localhost:8185/test/customer
		 */
		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "customer");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "customerApp");
		Application application = new Application(){};
		
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		registerServiceForCleanup(Application.class, application, appProps);
		
		assertTrue(runtimeChecker.waitModify());
		
		/*
		 * Mount the extension ContractedExtension that will become available under:
		 * http://localhost:8185/test/hello
		 */
		Dictionary<String, Object> extensionProps = new Hashtable<>();
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Contracted");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");
		
		System.out.println("Register resource for uri /hello");
		
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		ContractedExtension extension = new ContractedExtension();
		
		registerServiceForCleanup(ContractedExtension.class, extension, extensionProps);
		
		assertTrue(runtimeChecker.waitModify());
		
		/*
		 * Mount the resource HelloResource that will become available under:
		 * http://localhost:8185/test/hello
		 */
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");
		
		System.out.println("Register resource for uri /hello");
		
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		
		registerServiceForCleanup(HelloResource.class, new HelloResource(), helloProps);
		
		assertTrue(runtimeChecker.waitModify());
		
		/*
		 * Check if our RootResource is available under http://localhost:8185/test
		 */
		String checkUrl = url + "/customer/hello";
		System.out.println("Checking URL is available: " + checkUrl);
		Invocation post = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildPost(Entity.entity("test", "application/testThing"));
		Response response = post.invoke();
		assertEquals(200, response.getStatus());
		assertNotNull(response.getEntity());
		String result01 = response.readEntity(String.class);
		System.out.println(result01);
		assertNotNull(result01);
		
		assertTrue(result01.contains(ContractedExtension.READER_POSTFIX));
		assertTrue(result01.contains(ContractedExtension.WRITER_POSTFIX));

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(2);
		runtimeChecker.start();
		
		assertFalse(runtimeChecker.waitModify());
		
		unregisterService(extension);
		registerServiceForCleanup(MessageBodyReader.class, extension, extensionProps);
		
		assertTrue(runtimeChecker.waitModify());
		
		
		response = post.invoke();
		assertEquals(200, response.getStatus());
		assertNotNull(response.getEntity());
		result01 = response.readEntity(String.class);
		System.out.println(result01);
		assertNotNull(result01);
		
		assertTrue(result01.contains(ContractedExtension.READER_POSTFIX));
		assertFalse(result01.contains(ContractedExtension.WRITER_POSTFIX));
	}

	
	
	

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.util.test.common.test.AbstractOSGiTest#doBefore()
	 */
	@Override
	public void doBefore() {
		// TODO Auto-generated method stub
		
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.util.test.common.test.AbstractOSGiTest#doAfter()
	 */
	@Override
	public void doAfter() {
		// TODO Auto-generated method stub
		
	}
}
