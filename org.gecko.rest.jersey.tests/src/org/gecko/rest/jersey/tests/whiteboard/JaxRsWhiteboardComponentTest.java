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
package org.gecko.rest.jersey.tests.whiteboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.gecko.rest.jersey.provider.JerseyConstants;
import org.gecko.rest.jersey.tests.whiteboard.applications.TestLegacyApplication;
import org.gecko.rest.jersey.tests.whiteboard.customizer.TestServiceCustomizer;
import org.gecko.rest.jersey.tests.whiteboard.resources.HelloResource;
import org.gecko.util.test.common.service.ServiceChecker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Tests the whiteboard dispatcher
 * @author Mark Hoffmann
 * @since 12.10.2017
 */
//@RunWith(MockitoJUnitRunner.class)
public class JaxRsWhiteboardComponentTest {

	private final BundleContext context = FrameworkUtil.getBundle(JaxRsWhiteboardComponentTest.class).getBundleContext();
	
	private ServiceReference<ConfigurationAdmin> configAdminRef = null;

//	@Before
	public void before() {
		configAdminRef = context.getServiceReference(ConfigurationAdmin.class);
		assertNotNull(configAdminRef);
	}

//	@After
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
//	@Test
	public void testWhiteboardComponentApplicationAndResource() throws IOException, InterruptedException, InvalidSyntaxException {
		/*
		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
		 *  We mount the system with a resource RootResource under http://localhost:8185/test that will return a 
		 *  HTTP::200 using a GET request
		 */
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		
		/*
		 * Initial Setup of the HTTP Runtime
		 * 
		 */
		Configuration runtimeConfig = configAdmin.createFactoryConfiguration("org.apache.felix.http", "?");
		
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("org.osgi.service.http.port", port);
		props.put("org.apache.felix.http.context_path", "/");
		props.put("org.apache.felix.http.name", "Test");
		props.put("org.apache.felix.http.runtime.init." + "test.id", "endpoints");
		
		runtimeConfig.update(props);
		
		//Register our Context
		ServletContextHelper newContext = new ServletContextHelper() {
		};
		
		Dictionary<String, String> ctxProps = new Hashtable<>();
		ctxProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/" + contextPath);
		ctxProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, contextPath);
		
		ServiceRegistration<ServletContextHelper> servletContextRegistration = context.registerService(ServletContextHelper.class, newContext, ctxProps);
		
		/*
		 * Initial setup for the REST runtime by targeting the http whiteboard and the context
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(test.id=endpoints)");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + contextPath + ")");
		
		ServiceReference<HttpServiceRuntime> httpRuntimeRef = getServiceReference(HttpServiceRuntime.class, 40000l);
		assertNotNull(httpRuntimeRef);
		
		assertNotNull(configAdmin);
		Configuration jasxRsWhiteBoardConfig = configAdmin.getConfiguration("JaxRsHttpWhiteboardRuntimeComponent", "?");
		assertNotNull(jasxRsWhiteBoardConfig);
		assertEquals(1, jasxRsWhiteBoardConfig.getChangeCount());
		Dictionary<String,Object> factoryProperties = jasxRsWhiteBoardConfig.getProperties();
		assertNull(factoryProperties);
		jasxRsWhiteBoardConfig.update(properties);
		
		/*
		 * Check that the REST runtime service become available 
		 */
		ServiceReference<JaxrsServiceRuntime> runtimeRef = getServiceReference(JaxrsServiceRuntime.class, 40000l);
		assertNotNull(runtimeRef);
		JaxrsServiceRuntime runtime = getService(JaxrsServiceRuntime.class, 30000l);
		assertNotNull(runtime);
		
		CountDownLatch cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		
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
		
		/*
		 * Mount the application customer that will become available under: test/customer
		 * http://localhost:8185/test/customer
		 */
		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "customer");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "customerApp");
		ServiceRegistration<Application> appRegistration = context.registerService(Application.class, new Application(), appProps);
		Filter f = FrameworkUtil.createFilter("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");
		Application application = getService(f, 3000l);
		assertNotNull(application);
		
		/*
		 * Mount the resource HelloResource that will become available under:
		 * http://localhost:8185/test/hello
		 */
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");
		System.out.println("Register resource for uri /hello under application customer");
		ServiceRegistration<Object> helloRegistration = context.registerService(Object.class, new HelloResource(), helloProps);
		f = FrameworkUtil.createFilter("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=Hello)");
		Object service = getService(f, 3000l);
		assertNotNull(service);
		
		/*
		 * Wait a short time to reload the configuration dynamically
		 */
		cdl = new CountDownLatch(1);
		cdl.await(3, TimeUnit.SECONDS);
		
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
		
		helloRegistration.unregister();
		service = getService(f, 3000l);
		assertNull(service);
		
		appRegistration.unregister();
		service = getService(f, 3000l);
		assertNull(service);
		
		/*
		 * Wait a short time to reload the configuration dynamically
		 */
		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		
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

		
		servletContextRegistration.unregister();
		tearDownTest(jasxRsWhiteBoardConfig);
		tearDownTest(HttpServiceRuntime.class, runtimeConfig, get);
	}

	/**
	 * Tests 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
//	@Test
//	public void testWhiteboardDemoCase() throws IOException, InterruptedException, InvalidSyntaxException {
//		/*
//		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
//		 *  We mount the system with a resource RootResource under http://localhost:8185/test that will return a 
//		 *  HTTP::200 using a GET request
//		 */
//		int port = 8185;
//		String contextPath = "fancyProductA";
//		String url = "http://localhost:" + port + "/" + contextPath;
//		String url2 = "http://localhost:" + (port +1) + "/" + contextPath;
//		
//		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
//		
//		/*
//		 * Initial Setup of the HTTP Runtime
//		 * 
//		 */
//Configuration runtimeConfig = configAdmin.createFactoryConfiguration("http.server.jetty", "?");
//		
//		Dictionary<String, Object> props = new Hashtable<>();
//		List<String> endpoints = new ArrayList<>();
//		endpoints.add("http://localhost:" + port);
//		props.put(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT, endpoints);
//		props.put("test.id", "endpoints");
//		runtimeConfig.update(props);
//		
//		//Register our Context
//		ServletContextHelper newContext = new ServletContextHelper() {
//		};
//		
//		Dictionary<String, String> ctxProps = new Hashtable<>();
//		ctxProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/" + contextPath);
//		ctxProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, contextPath);
//		
//		ServiceRegistration<ServletContextHelper> servletContextRegistration = context.registerService(ServletContextHelper.class, newContext, ctxProps);
//		
//		/*
//		 * Initial setup for the REST runtime by targeteing the http whiteboard and the context
//		 */
//		Dictionary<String, Object> properties = new Hashtable<>();
//		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
//		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(test.id=endpoints)");
//		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + contextPath + ")");
//		
//		ServiceReference<HttpServiceRuntime> httpRuntimeRef = getServiceReference(HttpServiceRuntime.class, 40000l);
//		assertNotNull(httpRuntimeRef);
//		
//		assertNotNull(configAdmin);
//		Configuration jasxRsWhiteBoardConfig = configAdmin.getConfiguration("JaxRsHttpWhiteboardRuntimeComponent", "?");
//		assertNotNull(jasxRsWhiteBoardConfig);
//		assertEquals(1, jasxRsWhiteBoardConfig.getChangeCount());
//		Dictionary<String,Object> factoryProperties = jasxRsWhiteBoardConfig.getProperties();
//		assertNull(factoryProperties);
//		jasxRsWhiteBoardConfig.update(properties);
//		
//		/*
//		 * Check that the REST runtime service become available 
//		 */
//		ServiceReference<JaxrsServiceRuntime> runtimeRef = getServiceReference(JaxrsServiceRuntime.class, 40000l);
//		assertNotNull(runtimeRef);
//		JaxrsServiceRuntime runtime = getService(JaxrsServiceRuntime.class, 30000l);
//		assertNotNull(runtime);
//		
//		CountDownLatch cdl = new CountDownLatch(1);
//		cdl.await(1, TimeUnit.SECONDS);
//		
//		/*
//		 * Check if our RootResource is available under http://localhost:8185/test
//		 */
//		System.out.println("Checking URL is available: " + url);
//		JerseyInvocation get = null;
//		JerseyClient jerseyClient = JerseyClientBuilder.createClient();
//		JerseyWebTarget webTarget = jerseyClient.target(url);
//		get = webTarget.request().buildGet();
//		Response response = get.invoke();
//		assertEquals(404, response.getStatus());
//		
//		/*
//		 * Mount the application customer that will become available under: test/customer
//		 * http://localhost:8185/test/customer
//		 */
//		Dictionary<String, Object> appProps = new Hashtable<>();
//		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "customer");
//		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "customerApp");
//		ServiceRegistration<Application> appRegistration = context.registerService(Application.class, new Application(), appProps);
//		Filter appFilter = FrameworkUtil.createFilter("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");
//		Application application = getService(appFilter, 3000l);
//		assertNotNull(application);
//		
//		/*
//		 * Mount the resource HelloResource that will become available under:
//		 * http://localhost:8185/test/hello
//		 */
//		Dictionary<String, Object> helloProps = new Hashtable<>();
//		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
//		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
//		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=*)");
//		System.out.println("Register resource for uri /hello under application customer");
//		ServiceRegistration<Object> helloRegistration = context.registerService(Object.class, new HelloResource(), helloProps);
//		Filter resourceFilter = FrameworkUtil.createFilter("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=Hello)");
//		Object service = getService(resourceFilter, 3000l);
//		assertNotNull(service);
//		
//		/*
//		 * Wait a short time to reload the configuration dynamically
//		 */
//		cdl = new CountDownLatch(1);
//		cdl.await(3, TimeUnit.SECONDS);
//		
//		/*
//		 * Check if http://localhost:8185/test/customer/hello is available now. 
//		 * Check as well, if http://localhost:8185/test is /hello is not available
//		 */
//		System.out.println("Checking URL is available " + url + "/customer/hello");
//		webTarget = jerseyClient.target(url + "/customer/hello");
//		get = webTarget.request().buildGet();
//		response = get.invoke();
//		assertEquals(200, response.getStatus());
//		
//		
//		Configuration runtimeConfig2 = configAdmin.createFactoryConfiguration("http.server.jetty", "?");
//		
//		Dictionary<String, Object> props2 = new Hashtable<>();
//		List<String> endpoints2 = new ArrayList<>();
//		endpoints2.add("http://localhost:" + (port+1));
//		props2.put(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT, endpoints2);
//		props2.put("tenant.name", "Customer 2");
//		props2.put("fancyProduct1", "true");
//		runtimeConfig2.update(props2);
//		
//		
//		/*
//		 * Wait a short time to reload the configuration dynamically
//		 */
//		cdl = new CountDownLatch(1);
//		cdl.await(5, TimeUnit.SECONDS);
//		
//		/*
//		 * Check if http://localhost:8185/test/customer/hello is available now. 
//		 * Check as well, if http://localhost:8185/test is /hello is not available
//		 */
//		System.out.println("Checking URL is available " + url2 + "/customer/hello");
//		webTarget = jerseyClient.target(url2 + "/customer/hello");
//		get = webTarget.request().buildGet();
//		response = get.invoke();
//		assertEquals(200, response.getStatus());
//		
//		
//		
//		System.out.println("Checking URL is not available " + url + "/hello");
//		webTarget = jerseyClient.target(url + "/hello");
//		get = webTarget.request().buildGet();
//		response = get.invoke();
//		assertEquals(404, response.getStatus());
//		
//		helloRegistration.unregister();
//		service = getService(f, 3000l);
//		assertNull(service);
//		
//		appRegistration.unregister();
//		service = getService(f, 3000l);
//		assertNull(service);
//		
//		/*
//		 * Wait a short time to reload the configuration dynamically
//		 */
//		cdl = new CountDownLatch(1);
//		cdl.await(1, TimeUnit.SECONDS);
//		
//		/*
//		 * Check if http://localhost:8185/test/customer/hello is not available anymore. 
//		 * Check as well, if http://localhost:8185/test/hello is still not available
//		 */
//		System.out.println("Checking URL is not available anymore " + url + "/customer/hello");
//		webTarget = jerseyClient.target(url + "/customer/hello");
//		get = webTarget.request().buildGet();
//		response = get.invoke();
//		assertEquals(404, response.getStatus());
//		
//		System.out.println("Checking URL is not available anymore " + url + "/hello");
//		webTarget = jerseyClient.target(url + "/hello");
//		get = webTarget.request().buildGet();
//		response = get.invoke();
//		assertEquals(404, response.getStatus());
//		
//		
//		servletContextRegistration.unregister();
//		tearDownTest(jasxRsWhiteBoardConfig);
//		tearDownTest(HttpServiceRuntime.class, runtimeConfig2, get);
//	}
	
	/**
	 * Tests 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
//	@Test
	public void testWhiteboardComponentApplicationAndResourceWildcard() throws IOException, InterruptedException, InvalidSyntaxException {
		/*
		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
		 *  We mount the system with a resource RootResource under http://localhost:8185/test that will return a 
		 *  HTTP::200 using a GET request
		 */
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		
		/*
		 * Initial Setup of the HTTP Runtime
		 * 
		 */
		Configuration runtimeConfig = configAdmin.createFactoryConfiguration("org.apache.felix.http", "?");
		
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("org.osgi.service.http.port", port);
		props.put("org.apache.felix.http.context_path", "/");
		props.put("org.apache.felix.http.name", "Test");
		props.put("org.apache.felix.http.runtime.init." + "test.id", "endpoints");
		
		runtimeConfig.update(props);
		
		//Register our Context
		ServletContextHelper newContext = new ServletContextHelper() {
		};
		
		Dictionary<String, String> ctxProps = new Hashtable<>();
		ctxProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/" + contextPath);
		ctxProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, contextPath);
		
		ServiceRegistration<ServletContextHelper> servletContextRegistration = context.registerService(ServletContextHelper.class, newContext, ctxProps);
		
		/*
		 * Initial setup for the REST runtime by targeteing the http whiteboard and the context
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(test.id=endpoints)");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + contextPath + ")");
		
		ServiceReference<HttpServiceRuntime> httpRuntimeRef = getServiceReference(HttpServiceRuntime.class, 40000l);
		assertNotNull(httpRuntimeRef);
		
		assertNotNull(configAdmin);
		Configuration jasxRsWhiteBoardConfig = configAdmin.getConfiguration("JaxRsHttpWhiteboardRuntimeComponent", "?");
		assertNotNull(jasxRsWhiteBoardConfig);
		assertEquals(1, jasxRsWhiteBoardConfig.getChangeCount());
		Dictionary<String,Object> factoryProperties = jasxRsWhiteBoardConfig.getProperties();
		assertNull(factoryProperties);
		jasxRsWhiteBoardConfig.update(properties);
		
		/*
		 * Check that the REST runtime service become available 
		 */
		ServiceReference<JaxrsServiceRuntime> runtimeRef = getServiceReference(JaxrsServiceRuntime.class, 40000l);
		assertNotNull(runtimeRef);
		JaxrsServiceRuntime runtime = getService(JaxrsServiceRuntime.class, 30000l);
		assertNotNull(runtime);
		
		CountDownLatch cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		
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
		
		/*
		 * Mount the application customer that will become available under: test/customer
		 * http://localhost:8185/test/customer
		 */
		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "customer");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "customerApp");
		ServiceRegistration<Application> appRegistration = context.registerService(Application.class, new Application(), appProps);
		Filter appFilter = FrameworkUtil.createFilter("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");
		Application application = getService(appFilter, 3000l);
		assertNotNull(application);
		
		/*
		 * Mount the resource HelloResource that will become available under:
		 * http://localhost:8185/test/hello
		 */
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=*)");
		System.out.println("Register resource for uri /hello under application customer");
		ServiceRegistration<Object> helloRegistration = context.registerService(Object.class, new HelloResource(), helloProps);
		Filter resourceFilter = FrameworkUtil.createFilter("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=Hello)");
		Object service = getService(resourceFilter, 3000l);
		assertNotNull(service);
		
		/*
		 * Wait a short time to reload the configuration dynamically
		 */
		cdl = new CountDownLatch(1);
		cdl.await(3, TimeUnit.SECONDS);
		
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
		
		appRegistration.unregister();
		application = getService(appFilter, 3000l);
		assertNull(application);
		
		/*
		 * Wait a short time to reload the configuration dynamically
		 */
		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		
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
		
		helloRegistration.unregister();
		service = getService(resourceFilter, 3000l);
		assertNull(service);
		
		System.out.println("Checking URL is not available anymore " + url + "/hello");
		webTarget = jerseyClient.target(url + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
		/*
		 * Wait a short time to reload the configuration dynamically
		 */
		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		
		servletContextRegistration.unregister();
		tearDownTest(jasxRsWhiteBoardConfig);
		tearDownTest(HttpServiceRuntime.class, runtimeConfig, get);
	}

	/**
	 * Tests 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
//	@Test
	public void testWhiteboardComponentApplicationAndResourceWildcardWithMultipleHttpWhiteboards() throws IOException, InterruptedException, InvalidSyntaxException {
		/*
		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
		 *  We mount the system with a resource RootResource under http://localhost:8185/test that will return a 
		 *  HTTP::200 using a GET request
		 */
		int port = 8185;
		String contextPath = "test";
		String urlWhiteboard1 = "http://localhost:" + port + "/" + contextPath;
		String urlWhiteboard2 = "http://localhost:" + (port + 1) + "/" + contextPath;
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		
		/*
		 * Initial Setup of the HTTP Runtime
		 * 
		 */
		Configuration runtimeConfig1 = configAdmin.createFactoryConfiguration("org.apache.felix.http", "?");
		
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("org.osgi.service.http.port", port);
		props.put("org.apache.felix.http.context_path", "/");
		props.put("org.apache.felix.http.name", "Test");
		props.put("org.apache.felix.http.runtime.init." + "test.id", "endpoints");
		
		runtimeConfig1.update(props);

		/*
		 * Initial Setup of the HTTP Runtime
		 * 
		 */
		Configuration runtimeConfig2 = configAdmin.createFactoryConfiguration("org.apache.felix.http", "?");
		
		Dictionary<String, Object> props2 = new Hashtable<>();
		props2.put("org.osgi.service.http.port", port + 1);
		props2.put("org.apache.felix.http.context_path", "/");
		props2.put("org.apache.felix.http.name", "Test");
		props2.put("org.apache.felix.http.runtime.init." + "test.id", "endpoints");
		
		runtimeConfig2.update(props2);
		
		//Register our Context
		ServletContextHelper newContext = new ServletContextHelper() {
		};
		
		Dictionary<String, String> ctxProps = new Hashtable<>();
		ctxProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/" + contextPath);
		ctxProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, contextPath);
		
		ServiceRegistration<ServletContextHelper> servletContextRegistration = context.registerService(ServletContextHelper.class, newContext, ctxProps);
		
		/*
		 * Initial setup for the REST runtime by targeteing the http whiteboard and the context
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(test.id=endpoints)");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + contextPath + ")");
		
		ServiceReference<HttpServiceRuntime> httpRuntimeRef = getServiceReference(HttpServiceRuntime.class, 40000l);
		assertNotNull(httpRuntimeRef);
		
		assertNotNull(configAdmin);
		Configuration jasxRsWhiteBoardConfig = configAdmin.getConfiguration("JaxRsHttpWhiteboardRuntimeComponent", "?");
		assertNotNull(jasxRsWhiteBoardConfig);
		assertEquals(1, jasxRsWhiteBoardConfig.getChangeCount());
		Dictionary<String,Object> factoryProperties = jasxRsWhiteBoardConfig.getProperties();
		assertNull(factoryProperties);
		jasxRsWhiteBoardConfig.update(properties);
		
		/*
		 * Check that the REST runtime service become available 
		 */
		ServiceReference<JaxrsServiceRuntime> runtimeRef = getServiceReference(JaxrsServiceRuntime.class, 40000l);
		assertNotNull(runtimeRef);
		JaxrsServiceRuntime runtime = getService(JaxrsServiceRuntime.class, 30000l);
		assertNotNull(runtime);
		
		CountDownLatch cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		
		/*
		 * Check if our RootResource is available under http://localhost:8185/test
		 */
		System.out.println("Checking URL is available: " + urlWhiteboard1);
		Invocation get = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(urlWhiteboard1);
		get = webTarget.request().buildGet();
		Response response = get.invoke();
		assertEquals(404, response.getStatus());
		
		/*
		 * Mount the application customer that will become available under: test/customer
		 * http://localhost:8185/test/customer
		 */
		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "customer");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "customerApp");
		ServiceRegistration<Application> appRegistration = context.registerService(Application.class, new Application(), appProps);
		Filter appFilter = FrameworkUtil.createFilter("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");
		Application application = getService(appFilter, 3000l);
		assertNotNull(application);
		
		/*
		 * Mount the resource HelloResource that will become available under:
		 * http://localhost:8185/test/hello
		 */
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=*)");
		System.out.println("Register resource for uri /hello under application customer");
		ServiceRegistration<Object> helloRegistration = context.registerService(Object.class, new HelloResource(), helloProps);
		Filter resourceFilter = FrameworkUtil.createFilter("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=Hello)");
		Object service = getService(resourceFilter, 3000l);
		assertNotNull(service);
		
		/*
		 * Wait a short time to reload the configuration dynamically
		 */
		cdl = new CountDownLatch(1);
		cdl.await(3, TimeUnit.SECONDS);
		
		/*
		 * Check if http://localhost:8185/test/customer/hello is available now. 
		 * Check as well, if http://localhost:8185/test is /hello is not available
		 */
		System.out.println("Checking URL is available " + urlWhiteboard1 + "/customer/hello");
		webTarget = jerseyClient.target(urlWhiteboard1 + "/customer/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
		System.out.println("Checking URL is available " + urlWhiteboard1 + "/hello");
		webTarget = jerseyClient.target(urlWhiteboard1 + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());

		/*
		 * Check if http://localhost:8186/test/customer/hello is available now. 
		 * Check as well, if http://localhost:8186/test is /hello is not available
		 */
		System.out.println("Checking URL is available " + urlWhiteboard2 + "/customer/hello");
		webTarget = jerseyClient.target(urlWhiteboard2 + "/customer/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
		System.out.println("Checking URL is available " + urlWhiteboard2 + "/hello");
		webTarget = jerseyClient.target(urlWhiteboard2 + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
		//change the properties of the second http runtime, so the jaxRS runtime should removed
		props2.put("org.apache.felix.http.runtime.init." + "test.id", "endpoints2");
		runtimeConfig2.update(props2);

		cdl.await(3, TimeUnit.SECONDS);
		
		/*
		 * Check if http://localhost:8185/test/customer/hello is available now. 
		 * Check as well, if http://localhost:8185/test is /hello is available
		 */
		System.out.println("Checking URL is available " + urlWhiteboard1 + "/customer/hello");
		webTarget = jerseyClient.target(urlWhiteboard1 + "/customer/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
		System.out.println("Checking URL is available " + urlWhiteboard1 + "/hello");
		webTarget = jerseyClient.target(urlWhiteboard1 + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());

		/*
		 * Check if http://localhost:8186/test/customer/hello is not available annymore. 
		 * Check as well, if http://localhost:8186/test is /hello is not available anymore
		 */
		System.out.println("Checking URL is available " + urlWhiteboard2 + "/customer/hello");
		webTarget = jerseyClient.target(urlWhiteboard2 + "/customer/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
		System.out.println("Checking URL is available " + urlWhiteboard2 + "/hello");
		webTarget = jerseyClient.target(urlWhiteboard2 + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
		
		appRegistration.unregister();
		application = getService(appFilter, 3000l);
		assertNull(application);
		
		/*
		 * Wait a short time to reload the configuration dynamically
		 */
		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		
		/*
		 * Check if http://localhost:8185/test/customer/hello is not available anymore. 
		 * Check as well, if http://localhost:8185/test/hello is still not available
		 */
		System.out.println("Checking URL is not available anymore " + urlWhiteboard1 + "/customer/hello");
		webTarget = jerseyClient.target(urlWhiteboard1 + "/customer/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
		System.out.println("Checking URL is still available " + urlWhiteboard1 + "/hello");
		webTarget = jerseyClient.target(urlWhiteboard1 + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(200, response.getStatus());
		
		helloRegistration.unregister();
		service = getService(resourceFilter, 3000l);
		assertNull(service);
		
		System.out.println("Checking URL is not available anymore " + urlWhiteboard1 + "/hello");
		webTarget = jerseyClient.target(urlWhiteboard1 + "/hello");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
		/*
		 * Wait a short time to reload the configuration dynamically
		 */
		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		
		servletContextRegistration.unregister();
		tearDownTest(jasxRsWhiteBoardConfig);
		tearDownTest(HttpServiceRuntime.class, runtimeConfig1, get);
		tearDownTest(HttpServiceRuntime.class, runtimeConfig2, get);
	}
	
	/**
	 * Tests 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
//	@Test
	public void testWhiteboardComponentLegacyApplication() throws IOException, InterruptedException, InvalidSyntaxException {
		/*
		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
		 *  We mount the system with a resource RootResource under http://localhost:8185/test that will return a 
		 *  HTTP::200 using a GET request
		 */
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		
		/*
		 * Initial Setup of the HTTP Runtime
		 * 
		 */
		Configuration runtimeConfig = configAdmin.createFactoryConfiguration("org.apache.felix.http", "?");
		
		ServiceChecker<HttpServiceRuntime> httpRuntimeChecker = createdCheckerTrackedForCleanUp("(test.id=endpoints)", context);
		httpRuntimeChecker.setCreateCount(1);
		httpRuntimeChecker.start();
		
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("org.osgi.service.http.port", port);
		props.put("org.apache.felix.http.context_path", "/");
		props.put("org.apache.felix.http.name", "Test");
		props.put("org.apache.felix.http.runtime.init." + "test.id", "endpoints");

		runtimeConfig.update(props);
		
		assertTrue(httpRuntimeChecker.waitCreate());
		
		//Register our Context
		ServletContextHelper newContext = new ServletContextHelper() {
		};
		
		Dictionary<String, String> ctxProps = new Hashtable<>();
		ctxProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/" + contextPath);
		ctxProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, contextPath);
		
		ServiceRegistration<ServletContextHelper> servletContextRegistration = context.registerService(ServletContextHelper.class, newContext, ctxProps);
		
		/*
		 * Initial setup for the REST runtime by targeteing the http whiteboard and the context
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(test.id=endpoints)");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + contextPath + ")");
		
		ServiceReference<HttpServiceRuntime> httpRuntimeRef = getServiceReference(HttpServiceRuntime.class, 40000l);
		assertNotNull(httpRuntimeRef);
		
		assertNotNull(configAdmin);
		Configuration jasxRsWhiteBoardConfig = configAdmin.getConfiguration("JaxRsHttpWhiteboardRuntimeComponent", "?");
		assertNotNull(jasxRsWhiteBoardConfig);
		assertEquals(1, jasxRsWhiteBoardConfig.getChangeCount());
		Dictionary<String,Object> factoryProperties = jasxRsWhiteBoardConfig.getProperties();
		assertNull(factoryProperties);
		jasxRsWhiteBoardConfig.update(properties);
		
		
		/*
		 * Check that the REST runtime service become available 
		 */
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class, context);
		runtimeChecker.start();
		

		assertTrue(runtimeChecker.waitCreate());
		
		ServiceReference<JaxrsServiceRuntime> serviceReference = getServiceReference(JaxrsServiceRuntime.class, 500);
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
		
		httpRuntimeChecker.stop();
		httpRuntimeChecker.setModifyTimeout(5);
		httpRuntimeChecker.setModifyCount(1);
		httpRuntimeChecker.start();
		
		/*
		 * Mount the application customer that will become available under: test/customer
		 * http://localhost:8185/test/customer
		 */
		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "/legacy");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "legacyApp");
		ServiceRegistration<Application> appRegistration = context.registerService(Application.class, new TestLegacyApplication(), appProps);
		Filter appFilter = FrameworkUtil.createFilter("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=legacyApp)");
		Application application = getService(appFilter, 3000l);
		assertNotNull(application);
		
		assertTrue(runtimeChecker.waitModify());	
		assertTrue(httpRuntimeChecker.waitModify());	
		
		/*
		 * Check if http://localhost:8185/test/customer/hello is available now. 
		 * Check as well, if http://localhost:8185/test is /hello is not available
		 */
		System.out.println("Checking URL is available " + url + "/legacy/hello/mark changecout " + serviceReference.getProperty("service.changecount"));
		webTarget = jerseyClient.target(url + "/legacy/hello/mark");
		get = webTarget.request().buildGet();
		response = get.invoke();
		
		assertEquals(200, response.getStatus());
	
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		appRegistration.unregister();
		application = getService(appFilter, 3000l);
		assertNull(application);
		
		servletContextRegistration.unregister();
		
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
		
		tearDownTest(jasxRsWhiteBoardConfig);
		tearDownTest(HttpServiceRuntime.class,runtimeConfig, get);
	}

	/**
	 * Tests 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
	public void testWhiteboardComponentMultipleEndpointsLegacyApplication() throws IOException, InterruptedException, InvalidSyntaxException {
		/*
		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
		 *  We mount the system with a resource RootResource under http://localhost:8185/test that will return a 
		 *  HTTP::200 using a GET request
		 */
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		
		/*
		 * Initial Setup of the HTTP Runtime
		 * 
		 */
		Configuration runtimeConfig = configAdmin.createFactoryConfiguration("http.server.jetty", "?");
		
		Dictionary<String, Object> props = new Hashtable<>();
		List<String> endpoints = new ArrayList<>();
		endpoints.add("http://localhost:" + port);
		endpoints.add("http://127.0.0.1:8093");
		endpoints.add("http://0.0.0.0:8094");
		props.put(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT, endpoints);
		props.put("test.id", "endpoints");
		runtimeConfig.update(props);
		
		//Register our Context
		ServletContextHelper newContext = new ServletContextHelper() {
		};
		
		Dictionary<String, String> ctxProps = new Hashtable<>();
		ctxProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/" + contextPath);
		ctxProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, contextPath);
		
		ServiceRegistration<ServletContextHelper> servletContextRegistration = context.registerService(ServletContextHelper.class, newContext, ctxProps);
		
		/*
		 * Initial setup for the REST runtime by targeteing the http whiteboard and the context
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(test.id=endpoints)");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + contextPath + ")");
		
		assertNotNull(configAdmin);
		Configuration configuration = configAdmin.getConfiguration("JaxRsHttpWhiteboardRuntimeComponent", "?");
		assertNotNull(configuration);
		assertEquals(1, configuration.getChangeCount());
		Dictionary<String,Object> factoryProperties = configuration.getProperties();
		assertNull(factoryProperties);
		configuration.update(properties);
		
		/*
		 * Check that the REST runtime service become available 
		 */
		ServiceReference<JaxrsServiceRuntime> runtimeRef = getServiceReference(JaxrsServiceRuntime.class, 40000l);
		assertNotNull(runtimeRef);
		JaxrsServiceRuntime runtime = getService(JaxrsServiceRuntime.class, 30000l);
		assertNotNull(runtime);
		
		CountDownLatch cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		
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
		
		/*
		 * Mount the application customer that will become available under: test/customer
		 * http://localhost:8185/test/customer
		 */
		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "legacy");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "legacyApp");
		ServiceRegistration<Application> appRegistration = context.registerService(Application.class, new TestLegacyApplication(), appProps);
		Filter appFilter = FrameworkUtil.createFilter("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=legacyApp)");
		Application application = getService(appFilter, 3000l);
		assertNotNull(application);
		
		/*
		 * Wait a short time to reload the configuration dynamically
		 */
		cdl = new CountDownLatch(1);
		cdl.await(2, TimeUnit.SECONDS);
		
		/*
		 * Check if http://localhost:8185/test/customer/hello is available now. 
		 * Check as well, if http://localhost:8185/test is /hello is not available
		 */
		for(String endpoint : endpoints) {
			String curUrl = endpoint + "/test";
			System.out.println("Checking URL is available " + curUrl + "/legacy/hello/mark");
			webTarget = jerseyClient.target(curUrl + "/legacy/hello/mark");
			get = webTarget.request().buildGet();
			response = get.invoke();
			assertEquals(200, response.getStatus());
		}
		
		servletContextRegistration.unregister();
		
		appRegistration.unregister();
		application = getService(appFilter, 3000l);
		assertNull(application);
		
		/*
		 * Wait a short time to reload the configuration dynamically
		 */
		cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		
		/*
		 * Check if http://localhost:8185/test/customer/hello is not available anymore. 
		 * Check as well, if http://localhost:8185/test/hello is still not available
		 */
		System.out.println("Checking URL is not available anymore " + url + "/legacy/hello/mark");
		webTarget = jerseyClient.target(url + "/legacy/hello/mark");
		get = webTarget.request().buildGet();
		response = get.invoke();
		assertEquals(404, response.getStatus());
		
		tearDownTest(configuration);
		tearDownTest(HttpServiceRuntime.class, runtimeConfig, get);
	}

	/**
	 * Tests 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
//	@Test
	public void testWhiteboardComponentDefaultResource() throws IOException, InterruptedException, InvalidSyntaxException {
		/*
		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
		 *  We mount the system with a resource RootResource under http://localhost:8185/test that will return a 
		 *  HTTP::200 using a GET request
		 */
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		
		/*
		 * Initial Setup of the HTTP Runtime
		 * 
		 */
		Configuration runtimeConfig = configAdmin.createFactoryConfiguration("org.apache.felix.http", "?");
		
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("org.osgi.service.http.port", port);
		props.put("org.apache.felix.http.context_path", "/");
		props.put("org.apache.felix.http.name", "Test");
		props.put("org.apache.felix.http.runtime.init." + "test.id", "endpoints");
		
		runtimeConfig.update(props);
		
		//Register our Context
		ServletContextHelper newContext = new ServletContextHelper() {
		};
		
		Dictionary<String, String> ctxProps = new Hashtable<>();
		ctxProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/" + contextPath);
		ctxProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, contextPath);
		
		ServiceRegistration<ServletContextHelper> servletContextRegistration = context.registerService(ServletContextHelper.class, newContext, ctxProps);
		
		/*
		 * Initial setup for the REST runtime by targeteing the http whiteboard and the context
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(test.id=endpoints)");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + contextPath + ")");
		
		ServiceReference<HttpServiceRuntime> httpRuntimeRef = getServiceReference(HttpServiceRuntime.class, 40000l);
		assertNotNull(httpRuntimeRef);
		
		assertNotNull(configAdmin);
		Configuration jasxRsWhiteBoardConfig = configAdmin.getConfiguration("JaxRsHttpWhiteboardRuntimeComponent", "?");
		assertNotNull(jasxRsWhiteBoardConfig);
		assertEquals(1, jasxRsWhiteBoardConfig.getChangeCount());
		Dictionary<String,Object> factoryProperties = jasxRsWhiteBoardConfig.getProperties();
		assertNull(factoryProperties);
		jasxRsWhiteBoardConfig.update(properties);
		
		/*
		 * Check that the REST runtime service become available 
		 */
		ServiceReference<JaxrsServiceRuntime> runtimeRef = getServiceReference(JaxrsServiceRuntime.class, 40000l);
		assertNotNull(runtimeRef);
		JaxrsServiceRuntime runtime = getService(JaxrsServiceRuntime.class, 30000l);
		assertNotNull(runtime);
		
		CountDownLatch cdl = new CountDownLatch(1);
		cdl.await(1, TimeUnit.SECONDS);
		
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
		
		/*
		 * Mount the resource HelloResource that will become available under:
		 * http://localhost:8185/test/hello
		 */
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		System.out.println("Register resource for uri /hello");
		ServiceRegistration<Object> helloRegistration = context.registerService(Object.class, new HelloResource(), helloProps);
		Filter f = FrameworkUtil.createFilter("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=Hello)");
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
		
		servletContextRegistration.unregister();
		tearDownTest(jasxRsWhiteBoardConfig);
		tearDownTest(HttpServiceRuntime.class, runtimeConfig, get);
	}
	
	/**
	 * Tests 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
	//@Test
	public void testWhiteboardComponentDefaultResourceAvailableBeforeStart() throws IOException, InterruptedException, InvalidSyntaxException {
		/*
		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
		 *  We mount the system with a resource RootResource under http://localhost:8185/test that will return a 
		 *  HTTP::200 using a GET request
		 */
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;
		
		ConfigurationAdmin configAdmin = context.getService(configAdminRef);
		
		/*
		 * Initial Setup of the HTTP Runtime
		 * 
		 */
		Configuration runtimeConfig = configAdmin.createFactoryConfiguration("org.apache.felix.http", "?");
		
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("org.osgi.service.http.port", port);
		props.put("org.apache.felix.http.context_path", "/");
		props.put("org.apache.felix.http.name", "Test");
		props.put("org.apache.felix.http.runtime.init." + "test.id", "endpoints");

		//Register our Context
		ServletContextHelper newContext = new ServletContextHelper() {
		};
		
		Dictionary<String, String> ctxProps = new Hashtable<>();
		ctxProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/" + contextPath);
		ctxProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, contextPath);
		
		ServiceRegistration<ServletContextHelper> servletContextRegistration = context.registerService(ServletContextHelper.class, newContext, ctxProps);
		
		/*
		 * Initial setup for the REST runtime by targeteing the http whiteboard and the context
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(test.id=endpoints)");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + contextPath + ")");
		
		ServiceReference<HttpServiceRuntime> httpRuntimeRef = getServiceReference(HttpServiceRuntime.class, 40000l);
		assertNotNull(httpRuntimeRef);
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class, context);
		runtimeChecker.start();
		
		assertNotNull(configAdmin);
		Configuration jasxRsWhiteBoardConfig = configAdmin.getConfiguration("JaxRsHttpWhiteboardRuntimeComponent", "?");
		assertNotNull(jasxRsWhiteBoardConfig);
		assertEquals(1, jasxRsWhiteBoardConfig.getChangeCount());
		Dictionary<String,Object> factoryProperties = jasxRsWhiteBoardConfig.getProperties();
		assertNull(factoryProperties);
		jasxRsWhiteBoardConfig.update(properties);

		/*
		 * Mount the resource HelloResource that will become available under:
		 * http://localhost:8185/test/hello
		 */
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		System.out.println("Register resource for uri /hello");
		ServiceRegistration<Object> helloRegistration = context.registerService(Object.class, new HelloResource(), helloProps);
		Filter f = FrameworkUtil.createFilter("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=Hello)");
		Object service = getService(f, 3000l);
		assertNotNull(service);
		
		/*
		 * Check that the REST runtime service become available 
		 */
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
		
		helloRegistration.unregister();
		service = getService(f, 3000l);
		assertNull(service);
		
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
		
		servletContextRegistration.unregister();
		tearDownTest(jasxRsWhiteBoardConfig);
		tearDownTest(HttpServiceRuntime.class, runtimeConfig, get);
		
	}
	
	private <T> void tearDownTest(Class<T> clazz, Configuration configuration, Invocation get) throws IOException, InterruptedException {
		/*
		 * Tear-down the system
		 */
		CountDownLatch deleteLatch = new CountDownLatch(1);
		TestServiceCustomizer<T, T> c = new TestServiceCustomizer<>(context, null, deleteLatch);
		configuration.delete();
		awaitRemovedService(clazz, c);
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

	private void tearDownTest(Configuration configuration) throws IOException, InterruptedException {
		/*
		 * Tear-down the system
		 */
		CountDownLatch deleteLatch = new CountDownLatch(1);
		TestServiceCustomizer<JaxrsServiceRuntime, JaxrsServiceRuntime> c = new TestServiceCustomizer<>(context, null, deleteLatch);
		awaitRemovedService(JaxrsServiceRuntime.class, c);
		configuration.delete();
		assertTrue(deleteLatch.await(10, TimeUnit.SECONDS));
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

	<T> ServiceReference<T> getServiceReference(Filter filter, long timeout) throws InterruptedException {
		ServiceTracker<T, T> tracker = new ServiceTracker<>(context, filter, null);
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

	@SuppressWarnings("unchecked")
	private <T extends Object> ServiceChecker<T>  createdCheckerTrackedForCleanUp(String filter, BundleContext context) throws InvalidSyntaxException {
		ServiceChecker<? extends Object> checker = new ServiceChecker<>(filter, context);
		
		checker.setCreateCount(1);
		checker.setDeleteCount(1);
		checker.setCreateTimeout(5);
		checker.setDeleteTimeout(5);
		return (ServiceChecker<T>) checker;
	}
	
}
