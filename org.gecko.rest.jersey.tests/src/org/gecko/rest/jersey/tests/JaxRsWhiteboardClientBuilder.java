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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import org.gecko.rest.jersey.tests.resources.HelloResource;
import org.gecko.util.test.common.test.AbstractOSGiTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

/**
 * Tests the whiteboard dispatcher
 * @author Mark Hoffmann
 * @since 12.10.2017
 */
@RunWith(MockitoJUnitRunner.class)
public class JaxRsWhiteboardClientBuilder extends AbstractOSGiTest{

	/**
	 * Creates a new instance.
	 * @param bundleContext
	 */
	public JaxRsWhiteboardClientBuilder() {
		super(FrameworkUtil.getBundle(JaxRsWhiteboardClientBuilder.class).getBundleContext());
	}

	/**
	 * Tests 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testWhiteboardComponentApplicationAndResourceTest() throws IOException, InterruptedException, InvalidSyntaxException {
		
		BundleContext context = getBundleContext();
		
		ClientBuilder clientBuilder = getService(ClientBuilder.class);
		
		/*
		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
		 *  We mount the system with a resource RootResource under http://localhost:8185/test that will return a 
		 *  HTTP::200 using a GET request
		 */
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;
		
		org.gecko.util.test.common.service.ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		/*
		 * Initial setup for the REST runtime 
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ConfigurationAdmin configAdmin = getConfigAdmin();
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
		Invocation get = null;
		Client jerseyClient = clientBuilder.build();
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
		ServiceRegistration<Application> appRegistration = context.registerService(Application.class, new Application(){}, appProps);
		Filter f = FrameworkUtil.createFilter("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");
		Application application = getService(f, 3000l);
		assertNotNull(application);

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
		ServiceRegistration<HelloResource> helloRegistration = context.registerService(HelloResource.class, new HelloResource(), helloProps);
		f = FrameworkUtil.createFilter("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=Hello)");
		Object service = getService(f, 3000l);
		
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
		
		helloRegistration.unregister();
		getServiceAssertNull(f);
		
		appRegistration.unregister();
		getServiceAssertNull(f);
		
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
	}
}
