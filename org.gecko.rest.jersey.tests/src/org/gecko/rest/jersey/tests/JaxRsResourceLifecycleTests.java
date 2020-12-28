/**
 * Copyright (c) 2012 - 2020 Data In Motion and others.
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

import org.gecko.rest.jersey.provider.JerseyConstants;
import org.gecko.rest.jersey.tests.resources.ContextFieldInjectTestResource;
import org.gecko.rest.jersey.tests.resources.ContextMethodInjectTestResource;
import org.gecko.util.test.common.service.ServiceChecker;
import org.gecko.util.test.common.test.AbstractOSGiTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

/**
 * 
 * @author ilenia
 * @since Jun 15, 2020
 */
@SuppressWarnings("deprecation")
@RunWith(MockitoJUnitRunner.class)
public class JaxRsResourceLifecycleTests extends AbstractOSGiTest{
	
	/**
	 * This is necessary for a {@link JaxRsWhiteboardExtensionTests#testWebSecurityExtension()} 
	 * and must be set before the first request is made. No other way was working...
	 */
	static {
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
	}
	
	/*
	 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
	 */	
	int port = 8185;
	String contextPath = "test";
	String url = "http://localhost:" + port + "/" + contextPath;
	
	/**
	 * Creates a new instance.
	 * @param bundleContext
	 */
	public JaxRsResourceLifecycleTests() {
		super(FrameworkUtil.getBundle(JaxRsResourceLifecycleTests.class).getBundleContext());
	}
	
	
	
	@Test
	public void testMethodContextInject() throws IOException, InterruptedException, InvalidSyntaxException {
	
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);	
		assertTrue(runtimeChecker.waitCreate());


		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "app");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "App");
		Application application = new Application(){};
		
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		registerServiceForCleanup(Application.class, application, appProps);		
		assertTrue(runtimeChecker.waitModify());
		
		Dictionary<String, Object> resProps = new Hashtable<>();
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Context Inject Res");
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=App)");
				
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(15);
		runtimeChecker.start();	
		
		
		
		registerServiceForCleanup(ContextMethodInjectTestResource.class, new PrototypeServiceFactory() {

			@Override
			public Object getService(Bundle bundle, ServiceRegistration registration) {
				return new ContextMethodInjectTestResource();
			}

			@Override
			public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
				// TODO Auto-generated method stub
				
			}
		}, resProps);		
		assertTrue(runtimeChecker.waitModify());	
		
		String checkUrl = url + "/app/whiteboard/context";
		Invocation post = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().header(ContextMethodInjectTestResource.CUSTOM_HEADER, "test").buildGet();
		Response response = post.invoke();
		assertEquals(200, response.getStatus());
		assertNotNull(response.getEntity());
		String result = response.readEntity(String.class);
		assertNotNull(result);
		assertEquals("test", result);	
	}
	
	@Test
	public void testFieldContextInject() throws IOException, InterruptedException, InvalidSyntaxException {
	
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);	
		assertTrue(runtimeChecker.waitCreate());


		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "app");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "App");
		Application application = new Application(){};
		
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		registerServiceForCleanup(Application.class, application, appProps);		
		assertTrue(runtimeChecker.waitModify());
		
		Dictionary<String, Object> resProps = new Hashtable<>();
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Context Inject Res");
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=App)");
				
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(15);
		runtimeChecker.start();	
		
		
		
		registerServiceForCleanup(ContextFieldInjectTestResource.class, new PrototypeServiceFactory() {

			@Override
			public Object getService(Bundle bundle, ServiceRegistration registration) {
				return new ContextFieldInjectTestResource();
			}

			@Override
			public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
				// TODO Auto-generated method stub
				
			}
		}, resProps);		
		assertTrue(runtimeChecker.waitModify());	
		
		String checkUrl = url + "/app/whiteboard/context";
		Invocation post = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().header(ContextFieldInjectTestResource.CUSTOM_HEADER, "test").buildGet();
		Response response = post.invoke();
		assertEquals(200, response.getStatus());
		assertNotNull(response.getEntity());
		String result = response.readEntity(String.class);
		assertNotNull(result);
		assertEquals("test", result);	
	}
	
	@Test
	public void testDefaultAppContextMethodInject() throws IOException, InterruptedException, InvalidSyntaxException {
	
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);	
		assertTrue(runtimeChecker.waitCreate());
		
		Dictionary<String, Object> resProps = new Hashtable<>();
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Context Inject Res");
				
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(15);
		runtimeChecker.start();		
		
		registerServiceForCleanup(ContextMethodInjectTestResource.class, new ContextMethodInjectTestResource(), resProps);		
		assertTrue(runtimeChecker.waitModify());	

		String checkUrl = url + "/whiteboard/context";
		Invocation post = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().header(ContextMethodInjectTestResource.CUSTOM_HEADER, "test").buildGet();
		Response response = post.invoke();
		assertEquals(200, response.getStatus());
		assertNotNull(response.getEntity());
		String result = response.readEntity(String.class);
		assertNotNull(result);
		assertEquals("test", result);	
	}
	
	@Test
	public void testDefaultAppContextFieldInject() throws IOException, InterruptedException, InvalidSyntaxException {
	
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);	
		assertTrue(runtimeChecker.waitCreate());
		
		Dictionary<String, Object> resProps = new Hashtable<>();
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Context Inject Res");
				
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(15);
		runtimeChecker.start();		
		
		registerServiceForCleanup(ContextFieldInjectTestResource.class, new ContextFieldInjectTestResource(), resProps);		
		
		assertTrue(runtimeChecker.waitModify());	
		
		String checkUrl = url + "/whiteboard/context";
		Invocation post = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().header(ContextFieldInjectTestResource.CUSTOM_HEADER, "test").buildGet();
		Response response = post.invoke();
		assertEquals(200, response.getStatus());
		assertNotNull(response.getEntity());
		String result = response.readEntity(String.class);
		assertNotNull(result);
		assertEquals("test", result);	
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
