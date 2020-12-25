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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;

import org.gecko.rest.jersey.provider.JerseyConstants;
import org.gecko.rest.jersey.tests.applications.AnnotatedTestLegacyApplication;
import org.gecko.rest.jersey.tests.resources.HelloResource;
import org.gecko.rest.jersey.tests.resources.TestReadExtension;
import org.gecko.util.test.common.service.ServiceChecker;
import org.gecko.util.test.common.test.AbstractOSGiTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.runtime.dto.ExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.RuntimeDTO;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

/**
 * Tests the whiteboard dispatcher
 * @author Mark Hoffmann
 * @since 12.10.2017
 */
@SuppressWarnings("deprecation")
@RunWith(MockitoJUnitRunner.class)
public class JaxRsWhiteboardApplicationLifecycleTests extends AbstractOSGiTest{
	
	/**
	 * This is necessary for a {@link JaxRsWhiteboardExtensionTests#testWebSecurityExtension()} 
	 * and must be set before the first request is made. No other way was working...
	 */
	static {
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
	}
	
	/**
	 * Creates a new instance.
	 * @param bundleContext
	 */
	public JaxRsWhiteboardApplicationLifecycleTests() {
		super(FrameworkUtil.getBundle(JaxRsWhiteboardApplicationLifecycleTests.class).getBundleContext());
	}

	/**
	 * Tests ---- before 88s 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testWhiteboardComponentAnnotatedLegacyApplicationPathChange() throws IOException, InterruptedException, InvalidSyntaxException {
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
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
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
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "/legacy");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "legacyApp");
		AnnotatedTestLegacyApplication annotatedTestLegacyApplication = new AnnotatedTestLegacyApplication();
		registerServiceForCleanup(Application.class, annotatedTestLegacyApplication,  appProps);	
		
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
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "legacyChanged");
		updateServiceRegistration(annotatedTestLegacyApplication, appProps);
		
		assertTrue(runtimeChecker.waitModify());
		
		/*
		 * Check if http://localhost:8185/test/customer/hello is available now. 
		 * Check as well, if http://localhost:8185/test is /hello is not available
		 */
		System.out.println("Checking URL is available " + url + "/legacyChanged/annotated/hello/mark");
		webTarget = jerseyClient.target(url + "/legacyChanged/annotated/hello/mark");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());

		System.out.println("Checking URL is available " + url + "/legacy/annotated/hello/mark");
		webTarget = jerseyClient.target(url + "/legacy/annotated/hello/mark");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
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
	 * Tests ---- before 88s 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testWhiteboardResourceChange() throws IOException, InterruptedException, InvalidSyntaxException {
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
		 * http://localhost:8185/test/app1
		 */
		Dictionary<String, Object> app1Props = new Hashtable<>();
		app1Props.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "/app1");
		app1Props.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "App1");
		registerServiceForCleanup(Application.class, new Application(), app1Props);
		
		assertTrue(runtimeChecker.waitModify());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		/*
		 * Mount the application customer that will become available under: test/customer
		 * http://localhost:8185/test/app2
		 */
		Dictionary<String, Object> app2Props = new Hashtable<>();
		app2Props.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "/app2");
		app2Props.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "App2");
		registerServiceForCleanup(Application.class, new Application(){}, app2Props);
		
		assertTrue(runtimeChecker.waitModify());
		
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
		helloProps.put("test", "Hello");
//		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=*)");
		System.out.println("Register resource for uri /hello under application customer");
		HelloResource helloResource = new HelloResource();
		registerServiceForCleanup(HelloResource.class, helloResource, helloProps);
//		ServiceRegistration<HelloResourceLongPath> helloRegistration = context.registerService(HelloResourceLongPath.class, new HelloResourceLongPath(), helloProps);
		Filter resourceFilter = FrameworkUtil.createFilter("(test=Hello)");
		Object service = getService(resourceFilter, 3000l);
		assertNotNull(service);
		
		assertTrue(runtimeChecker.waitModify());
		
		/*
		 * Check if http://localhost:8185/test/customer/hello is available now. 
		 * Check as well, if http://localhost:8185/test is /hello is not available
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
		
		helloProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=App2)");
		updateServiceRegistration(helloResource, helloProps);
		
		assertTrue(runtimeChecker.waitModify());
		
		System.out.println("Checking URL is available " + url + "/app2/hello");
		webTarget = jerseyClient.target(url + "/app2/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
		System.out.println("Checking URL is not available yet: " + url + "/app1/hello");
		webTarget = jerseyClient.target(url + "/app1/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
		System.out.println("Checking URL is not available anymore: " + url + "/hello");
		webTarget = jerseyClient.target(url + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		helloProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=App1)");
		updateServiceRegistration(helloResource, helloProps);
		
		assertTrue(runtimeChecker.waitModify());
		
		System.out.println("Checking URL is available " + url + "/app1/hello");
		webTarget = jerseyClient.target(url + "/app1/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
		System.out.println("Checking URL is not available anymore: " + url + "/app2/hello");
		webTarget = jerseyClient.target(url + "/app2/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
		System.out.println("Checking URL is not available anymore: " + url + "/hello");
		webTarget = jerseyClient.target(url + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		helloProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=*)");
		updateServiceRegistration(helloResource, helloProps);
		
		assertTrue(runtimeChecker.waitModify());
		
		System.out.println("Checking URL is available " + url + "/app1/hello");
		webTarget = jerseyClient.target(url + "/app1/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
		System.out.println("Checking URL is available " + url + "/app2/hello");
		webTarget = jerseyClient.target(url + "/app2/hello");
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
		
		helloProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=App1)");
		updateServiceRegistration(helloResource, helloProps);
		
		assertTrue(runtimeChecker.waitModify());
		
		System.out.println("Checking URL is available " + url + "/app1/hello");
		webTarget = jerseyClient.target(url + "/app1/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
		System.out.println("Checking URL is not available anymore: " + url + "/app2/hello");
		webTarget = jerseyClient.target(url + "/app2/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
		System.out.println("Checking URL is not available anymore: " + url + "/hello");
		webTarget = jerseyClient.target(url + "/hello");
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
	public void testMoveDefaultApplication() throws IOException, InterruptedException, InvalidSyntaxException {
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
		Client jerseyClient = ClientBuilder.newClient();;
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
		helloProps.put("test", "Hello");
		System.out.println("Register resource for uri /hello under application customer");
		registerServiceForCleanup(HelloResource.class, new HelloResource(), helloProps);
	
		
		assertTrue(runtimeChecker.waitModify());
		
		/*
		 * Check if http://localhost:8185/test/customer/hello is available now. 
		 * Check as well, if http://localhost:8185/test is /hello is not available
		 */
		System.out.println("Checking URL is available " + url + "/hello");
		webTarget = jerseyClient.target(url + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
		runtimeChecker.stop();
		runtimeChecker.setCreateCount(1);
		runtimeChecker.setCreateTimeout(5);
		runtimeChecker.start();
		
		/*
		 * Mount the application customer that will become available under: test/customer
		 * http://localhost:8185/test/app1
		 */
		Dictionary<String, Object> app1Props = new Hashtable<>();
		app1Props.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "/app1");
		app1Props.put(JaxrsWhiteboardConstants.JAX_RS_NAME, ".default");
		registerServiceForCleanup(Application.class, new Application(), app1Props);
		
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
	 * Creates a resource which should be added to the .default app
	 * Verify that resource is available
	 * Creates an app which will substitute the .default one
	 * Verify that resource is available for new default app
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void testChangeDefaultApplicationRes() throws IOException, InterruptedException, InvalidSyntaxException {
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
		registerServiceForCleanup(HelloResource.class, new HelloResource(), helloProps);	
		
		assertTrue(runtimeChecker.waitModify());
		
		/*
		 * Check if http://localhost:8185/test/customer/hello is available now. 
		 * Check as well, if http://localhost:8185/test is /hello is not available
		 */
		Client jerseyClient = ClientBuilder.newClient();;
		WebTarget webTarget = jerseyClient.target(url + "/hello");
		Invocation get = webTarget.request().buildGet();
		Response response = get.invoke();
		assertEquals(200, response.getStatus());
		
		runtimeChecker.stop();
		runtimeChecker.setCreateCount(1);
		runtimeChecker.setCreateTimeout(5);
		runtimeChecker.start();
		
//		Add an app which will substitute the defualt one
		Dictionary<String, Object> app1Props = new Hashtable<>();
		app1Props.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "/app1");
		app1Props.put(JaxrsWhiteboardConstants.JAX_RS_NAME, ".default");
		registerServiceForCleanup(Application.class, new Application(), app1Props);
		
		assertTrue(runtimeChecker.waitModify());
		
//		Check that the resource is now available for the new default one
		webTarget = jerseyClient.target(url + "/app1/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
	}
	
	/**
	 * Creates an extension which should be added to the .default app
	 * Verify that resource is available
	 * Creates an app which will substitute the .default one
	 * Verify that extension is available for new default app
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void testChangeDefaultApplicationExt() throws IOException, InterruptedException, InvalidSyntaxException {
		/*
		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
		 *  We mount the system with a resource RootResource under http://localhost:8185/test that will return a 
		 *  HTTP::200 using a GET request
		 */
		int port = 8185;
		String contextPath = "test";
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
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		Dictionary<String, Object> extensionProps = new Hashtable<>();
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "TestReadExtension");
		
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		TestReadExtension extension = new TestReadExtension();
		
//		We need to register the extension advertising which interfaces is implementing
		registerServiceForCleanup(extension, extensionProps, MessageBodyReader.class.getName());
		
		assertTrue(runtimeChecker.waitModify());
		
//		Add an app which will substitute the defualt one
		Dictionary<String, Object> app1Props = new Hashtable<>();
		app1Props.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "/app1");
		app1Props.put(JaxrsWhiteboardConstants.JAX_RS_NAME, ".default");
		registerServiceForCleanup(Application.class, new Application(), app1Props);
		
		assertTrue(runtimeChecker.waitModify());
		
		Thread.sleep(2000);
		
		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals("/app1/", runtimeDTO.defaultApplication.base);
		assertEquals(2, runtimeDTO.defaultApplication.extensionDTOs.length); //there is also an extension always registered in the tests
		ExtensionDTO dto = null;
		for(ExtensionDTO d : runtimeDTO.defaultApplication.extensionDTOs) {
			if("TestReadExtension".equals(d.name)) {
				dto = d;
			}
		}
		assertNotNull(dto);
	}
	
	
	/**
	 * Tests that if two apps are registered for the same whiteboard with the same base path property, then
	 * only the higher rank one is actually registered
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void testAppSameBasePath() throws IOException, InterruptedException, InvalidSyntaxException {
		
		int port = 8185;
		String contextPath = "test";
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
		appProps.put(Constants.SERVICE_RANKING, 2);
		Application application = new Application(){};
		
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		registerServiceForCleanup(Application.class, application, appProps);		
		assertTrue(runtimeChecker.waitModify());
		
		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals(1, runtimeDTO.applicationDTOs.length);
		assertEquals("customerApp", runtimeDTO.applicationDTOs[0].name);
		
		/*
		 * Mount a second application with the same base path
		 */
		appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "customer");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "anotherCustomerApp");
		appProps.put(Constants.SERVICE_RANKING, 400);
		
		application = new Application(){};
		
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		registerServiceForCleanup(Application.class, application, appProps);		
		assertTrue(runtimeChecker.waitModify());
		
		Thread.sleep(2000);
		
		runtimeDTO = getRuntimeDTO();
		assertEquals(1, runtimeDTO.applicationDTOs.length);
		assertEquals("anotherCustomerApp", runtimeDTO.applicationDTOs[0].name);
		assertEquals(1, runtimeDTO.failedApplicationDTOs.length);
		assertEquals("customerApp", runtimeDTO.failedApplicationDTOs[0].name);
		assertEquals(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, runtimeDTO.failedApplicationDTOs[0].failureReason);
	}
	
	
	/**
	 * Tests that if two apps are registered for the same whiteboard with the same name property, then
	 * only the higher rank one is actually registered
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void testAppSameName() throws IOException, InterruptedException, InvalidSyntaxException {
		
		int port = 8185;
		String contextPath = "test";
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
		appProps.put(Constants.SERVICE_RANKING, 2);
		Application application = new Application(){};
		
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		registerServiceForCleanup(Application.class, application, appProps);		
		assertTrue(runtimeChecker.waitModify());
	
		/*
		 * Mount a second application with the same base path
		 */
		appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "anotherCustomer");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "customerApp");
		appProps.put(Constants.SERVICE_RANKING, 400);
		
		application = new Application(){};
		
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(15);
		runtimeChecker.start();
		
		registerServiceForCleanup(Application.class, application, appProps);		
		assertTrue(runtimeChecker.waitModify());
		
		Thread.sleep(3000);
		
		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals(1, runtimeDTO.applicationDTOs.length);
		assertEquals("/anotherCustomer/", runtimeDTO.applicationDTOs[0].base);
		assertEquals(1, runtimeDTO.failedApplicationDTOs.length);
		assertEquals("/customer/*", runtimeDTO.failedApplicationDTOs[0].base);	
		assertEquals(DTOConstants.FAILURE_REASON_DUPLICATE_NAME, runtimeDTO.failedApplicationDTOs[0].failureReason);
	}
	
	private RuntimeDTO getRuntimeDTO() {
		JaxrsServiceRuntime jaxRSRuntime = getJaxRsRuntimeService();
		return jaxRSRuntime.getRuntimeDTO();
	}

	private JaxrsServiceRuntime getJaxRsRuntimeService() {
		JaxrsServiceRuntime jaxRSRuntime = getService(JaxrsServiceRuntime.class);
		return jaxRSRuntime;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.util.test.common.test.AbstractOSGiTest#doBefore()
	 */
	@Override
	public void doBefore() {		
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.util.test.common.test.AbstractOSGiTest#doAfter()
	 */
	@Override
	public void doAfter() {		
	}
}
