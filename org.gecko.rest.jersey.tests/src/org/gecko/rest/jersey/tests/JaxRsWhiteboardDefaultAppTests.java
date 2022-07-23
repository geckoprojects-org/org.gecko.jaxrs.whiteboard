/**
 * Copyright (c) 2012 - 2022 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 *     Stefan Bishof - API and implementation
 *     Tim Ward - implementation
 */
package org.gecko.rest.jersey.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.gecko.rest.jersey.provider.JerseyConstants;
import org.gecko.rest.jersey.tests.applications.SimpleApplication;
import org.gecko.rest.jersey.tests.resources.HelloResource;
import org.gecko.rest.jersey.tests.resources.StringResource;
import org.gecko.util.test.common.service.ServiceChecker;
import org.gecko.util.test.common.test.AbstractOSGiTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.runtime.dto.RuntimeDTO;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

/**
 * Tests to check the implementation of of the specs regarding the .default app and the ways to substitute it
 * 
 * @author ilenia
 * @since Jun 9, 2020
 */
@SuppressWarnings("deprecation")
@RunWith(MockitoJUnitRunner.class)
public class JaxRsWhiteboardDefaultAppTests extends AbstractOSGiTest{

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
	public JaxRsWhiteboardDefaultAppTests() {
		super(FrameworkUtil.getBundle(JaxRsWhiteboardDefaultAppTests.class).getBundleContext());
	}
	
	/*
	 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
	 */	
	int port = 8185;
	String contextPath = "test";
	String url = "http://localhost:" + port + "/" + contextPath;
	
	
	
	/**
	 * 151.6.1: The default application is implicitly created by the whiteboard and has the name .default. 
	 * The default application has a lower ranking than all registered services. 
	 * Therefore an application registered with a base of / will shadow a default application bound at /. 
	 * 
	 * We create an app with base path / and we check that this is registered while the .default one is not
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void testBasePathDefaultSubstitution() throws IOException, InterruptedException, InvalidSyntaxException {
		
//		Register the Whiteboard
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);		
		assertTrue(runtimeChecker.waitCreate());

		/*
		 * Mount the application newDefaultApp with base path / to substitute the .default one
		 */
		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "/");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "newDefaultApp");
		Application application = new Application(){};
		
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		registerServiceForCleanup(Application.class, application, appProps);		
		assertTrue(runtimeChecker.waitModify());
		
		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals(0, runtimeDTO.applicationDTOs.length);
		assertEquals("newDefaultApp", runtimeDTO.defaultApplication.name);	
		assertEquals(1, runtimeDTO.failedApplicationDTOs.length);
		assertEquals(".default", runtimeDTO.failedApplicationDTOs[0].name);
		assertEquals(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, runtimeDTO.failedApplicationDTOs[0].failureReason);
	}
	
	/**
	 * Creates a resource which will be added to the .default app
	 * Check that the resource is available
	 * Register an app with base / so to shadow the .default app
	 * Check that the resource is still available
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void testShadowDefaultApp() throws IOException, InterruptedException, InvalidSyntaxException {
		
//		Register the Whiteboard
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
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello Res");
				
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(15);
		runtimeChecker.start();		
		
		registerServiceForCleanup(HelloResource.class, new HelloResource(), resProps);		
		assertTrue(runtimeChecker.waitModify());	
		
		String checkUrl = url + "/hello";
		Invocation post = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildGet();
		Response response = post.invoke();
		assertEquals(200, response.getStatus());
		
		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "/");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "newDefaultApp");
		Application application = new Application(){};
		
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		registerServiceForCleanup(Application.class, application, appProps);		
		assertTrue(runtimeChecker.waitModify());
		
		post = webTarget.request().buildGet();
		response = post.invoke();
		assertEquals(200, response.getStatus());
		
	}
	
	@Test
	public void testComplianceShadowDefaultApp() throws IOException, InterruptedException, InvalidSyntaxException {
		
//		Register the Whiteboard
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
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "String Res");
				
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(15);
		runtimeChecker.start();		
		
		registerServiceForCleanup(StringResource.class, new StringResource("fizz"), resProps);		
		assertTrue(runtimeChecker.waitModify());	
		
		String checkUrl = url + "/whiteboard/string";
		Invocation post = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(checkUrl);
		Response response = null;
		String result = null;
		post = webTarget.request().buildGet();
		response = post.invoke();
		assertEquals(200, response.getStatus());
		result = response.readEntity(String.class);
		assertNotNull(result);
		assertEquals("fizz", result);
		
		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "/");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "newDefaultApp");
		
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		registerServiceForCleanup(Application.class, new SimpleApplication(Collections.emptySet(),
				Collections.singleton(new StringResource("buzz"))), appProps);		
		assertTrue(runtimeChecker.waitModify());
		
		checkUrl = url + "/whiteboard/string";
		post = webTarget.request().buildGet();
		response = post.invoke();
		assertEquals(200, response.getStatus());
		result = response.readEntity(String.class);
		assertNotNull(result);
//		assertEquals("buzz", result);
	}
	
	
	/**
	 * 151.6.1: A whiteboard application service may set an osgi.jaxrs.name of .default to replace the default application. 
	 * This technique may be used to rebind the default application to a base uri other than /. 
	 * 
	 * We create an app with name .default that this is registered while the "standard" .default one is not
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void testNameDefaultSubstitution() throws IOException, InterruptedException, InvalidSyntaxException {
		
//		Register the Whiteboard
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);		
		assertTrue(runtimeChecker.waitCreate());

		/*
		 * Mount the application .default with a new base path to substitute the .default one
		 */
		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "/newBasePath/");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, ".default");
		Application application = new Application(){};
		
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		registerServiceForCleanup(Application.class, application, appProps);		
		assertTrue(runtimeChecker.waitModify());
		
		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals("/newBasePath/", runtimeDTO.defaultApplication.base);	
		assertEquals(0, runtimeDTO.applicationDTOs.length);
		assertEquals(1, runtimeDTO.failedApplicationDTOs.length);
		assertEquals("/*", runtimeDTO.failedApplicationDTOs[0].base);
		assertEquals(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, runtimeDTO.failedApplicationDTOs[0].failureReason);
	}
	
	
	/**
	 * If two or more apps are registered with the same osgi.jaxrs.name property, then the higher rank one is 
	 * actually registered, and the others are stored as failed DTO
	 * 
	 * We create two apps with name .default and we check that only the highest rank one is registered
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void testMultipleDefault() throws IOException, InterruptedException, InvalidSyntaxException {
		
//		Register the Whiteboard
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);		
		assertTrue(runtimeChecker.waitCreate());

		/*
		 * Mount the application .default with a rank of 3
		 */
		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "/newBasePath/");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, ".default");
		appProps.put(Constants.SERVICE_RANKING, 3);
		Application application = new Application(){};
		
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		registerServiceForCleanup(Application.class, application, appProps);		
		assertTrue(runtimeChecker.waitModify());
		
		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals("/newBasePath/", runtimeDTO.defaultApplication.base);
		
		/*
		 * Mount the application .default with a rank of 4
		 */
		appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "/newBasePath2/");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, ".default");
		appProps.put(Constants.SERVICE_RANKING, 400);
		application = new Application(){};
		
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		registerServiceForCleanup(Application.class, application, appProps);		
		assertTrue(runtimeChecker.waitModify());
		
		Thread.sleep(2000);
		
		runtimeDTO = getRuntimeDTO();
		assertEquals("/newBasePath2/", runtimeDTO.defaultApplication.base);
		assertEquals(0, runtimeDTO.applicationDTOs.length);
		assertEquals(1, runtimeDTO.failedApplicationDTOs.length);
		assertEquals("/newBasePath/*", runtimeDTO.failedApplicationDTOs[0].base);
		assertEquals(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, runtimeDTO.failedApplicationDTOs[0].failureReason);
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
