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
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;

import org.gecko.rest.jersey.provider.JerseyConstants;
import org.gecko.rest.jersey.tests.applications.TestLegacySessionApplication;
import org.gecko.util.test.common.service.ServiceChecker;
import org.gecko.util.test.common.test.AbstractOSGiTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

/**
 * 
 * @author ilenia
 * @since Jun 11, 2020
 */
@SuppressWarnings("deprecation")
@RunWith(MockitoJUnitRunner.class)
public class ApplicationIsolationTests extends AbstractOSGiTest{
	
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
	public ApplicationIsolationTests() {
		super(FrameworkUtil.getBundle(ApplicationIsolationTests.class).getBundleContext());
	}
	
	@Test
	public void testApplicationIsolationContainer() throws IOException, InterruptedException {
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

//		Register a TestLegacySessionApplication
		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "app1");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "App1");
		
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
//		registerServiceForCleanup(Application.class,
//				new SimpleApplication(
//						Collections.singleton(SessionManipulator.class),
//						Collections.emptySet()), appProps);
		
		registerServiceForCleanup(Application.class,
				new TestLegacySessionApplication(), appProps);
		
		assertTrue(runtimeChecker.waitModify());
		
//		Register another TestLegacySessionApplication
		appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "app2");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "App2");
		
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
//		registerServiceForCleanup(Application.class,
//				new SimpleApplication(
//						Collections.singleton(SessionManipulator.class),
//						Collections.emptySet()), appProps);
		registerServiceForCleanup(Application.class,
				new TestLegacySessionApplication(), appProps);
		
		assertTrue(runtimeChecker.waitModify());
		
		
		String urlApp1 = url + "/app1/whiteboard/session/fizz";
		

		Invocation req = null;		
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget target1 = jerseyClient.target(urlApp1);
		req =  target1.request().buildPut(Entity.entity("fizzbuzz", "text/plain"));
		Response response = req.invoke();
		assertEquals(200, response.getStatus());
		final Cookie sessionId = response.getCookies().get("JSESSIONID");
		
		req =  target1.request().cookie(sessionId).buildGet();
		response = req.invoke();
		assertEquals(200, response.getStatus());
		assertNotNull(response.getEntity());
		String result = response.readEntity(String.class);
		assertNotNull(result);
		assertEquals("fizzbuzz", result);
		
		
//		String urlApp2 = url + "/app2/whiteboard/session/fizz";
//		
//		req = jerseyClient.target(urlApp2).request().buildPut(Entity.entity("buzz", "text/plain"));
//		response = req.invoke();
//		assertEquals(200, response.getStatus());		
//		
//		req = jerseyClient.target(urlApp2).request().buildGet();
//		response = req.invoke();
//		assertEquals(200, response.getStatus());
//		assertNotNull(response.getEntity());
//		result = response.readEntity(String.class);
//		assertNotNull(result);
//		assertEquals("buzz", result);
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
