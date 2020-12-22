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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.SseEventSource;

import org.gecko.rest.jersey.provider.JerseyConstants;
import org.gecko.rest.jersey.tests.resources.AsyncTestResource;
import org.gecko.rest.jersey.tests.resources.HelloResource;
import org.gecko.rest.jersey.tests.resources.SseResource;
import org.gecko.util.test.common.service.ServiceChecker;
import org.gecko.util.test.common.test.AbstractOSGiTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jaxrs.client.PromiseRxInvoker;
import org.osgi.service.jaxrs.client.SseEventSourceFactory;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.util.promise.Promise;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tests the whiteboard dispatcher
 * @author Mark Hoffmann
 * @since 12.10.2017
 */
@SuppressWarnings("deprecation")
@RunWith(MockitoJUnitRunner.class)
public class JaxRsWhiteboardClientBuilderTests extends AbstractOSGiTest{

	/**
	 * Creates a new instance.
	 * @param bundleContext
	 */
	public JaxRsWhiteboardClientBuilderTests() {
		super(FrameworkUtil.getBundle(JaxRsWhiteboardClientBuilderTests.class).getBundleContext());
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

		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();

		/*
		 * Initial setup for the REST runtime 
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);		


		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);

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
		getService(f, 3000l);

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


	@Test
	public void testClientBuilderService() throws Exception {
		
		ServiceTracker<ClientBuilder,ClientBuilder> tracker = new ServiceTracker<>(
				getBundleContext(), ClientBuilder.class, null);
		tracker.open();

		assertNotNull(tracker.waitForService(2000));

		for (ServiceReference<ClientBuilder> ref : tracker.getTracked()
				.keySet()) {
			assertEquals(Constants.SCOPE_PROTOTYPE, ref.getProperty(Constants.SERVICE_SCOPE));
		}

		Client c = tracker.getService().build();
		
		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;

		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);	

		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);

		assertTrue(runtimeChecker.waitCreate());

		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		runtimeChecker.waitModify();
		
		WebTarget target = c.target(url + "/hello");

		assertEquals(Response.Status.NOT_FOUND.getStatusCode(),
				target.request().get().getStatusInfo().getStatusCode());
		
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
				
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();		
		
		HelloResource res =  new HelloResource();
		registerServiceForCleanup(HelloResource.class, res, helloProps);
		
		assertTrue(runtimeChecker.waitModify());
		
		try {
			// Do another get
			String responseString = target.request().get(String.class);
			assertTrue(responseString.startsWith("Hello_"));
		
		} finally {			
			tracker.close();
		}
	}
	
	@Test
	public void testPromiseRxInvoker() throws IOException, InterruptedException, InvalidSyntaxException, InvocationTargetException {

		ServiceTracker<ClientBuilder,ClientBuilder> tracker = new ServiceTracker<>(
				getBundleContext(), ClientBuilder.class, null);
		tracker.open();

		assertNotNull(tracker.waitForService(2000));

		Client c = tracker.getService().build();

		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;

		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();

		/*
		 * Initial setup for the REST runtime 
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);		


		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);

		assertTrue(runtimeChecker.waitCreate());

		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		runtimeChecker.waitModify();
		
//		Add async Resource
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Async Resource");
				
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();		
		
		registerServiceForCleanup(AsyncTestResource.class, new AsyncTestResource(() -> {}, () -> {}), helloProps);
		
		assertTrue(runtimeChecker.waitModify());
		
//		Make an asyn request
		WebTarget target = c.target(url + "/whiteboard/async/{name}");

		Promise<String> p = target.resolveTemplate("name", "Bob")
				.request()
				.rx(PromiseRxInvoker.class)
				.get(String.class);
		
		assertFalse(p.isDone());
		CountDownLatch cdl = new CountDownLatch(1);
		p.onResolve(cdl::countDown);
		assertTrue(cdl.await(5, TimeUnit.SECONDS));
		
		assertEquals("Bob", p.getValue());
	}

	
	@Test
	public void testSseEventSource() throws Exception {
		
		ServiceTracker<ClientBuilder,ClientBuilder> tracker = new ServiceTracker<>(
				getBundleContext(), ClientBuilder.class, null);
		tracker.open();

		assertNotNull(tracker.waitForService(2000));

		Client c = tracker.getService().build();

		int port = 8185;
		String contextPath = "test";
		String url = "http://localhost:" + port + "/" + contextPath;

		org.gecko.util.test.common.service.ServiceChecker<JaxrsServiceRuntime> runtimeChecker = 
				createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();

		/*
		 * Initial setup for the REST runtime 
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);		

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();		
		
		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);

		assertTrue(runtimeChecker.waitCreate());
		
		WebTarget target = c.target(url + "/whiteboard/stream");

		properties = new Hashtable<>();
		properties.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, Boolean.TRUE);
		
		registerServiceForCleanup(SseResource.class, new SseResource(MediaType.TEXT_PLAIN_TYPE), properties);

		assertTrue(runtimeChecker.waitModify());

		try {
			ServiceTracker<SseEventSourceFactory,SseEventSourceFactory> sseTracker = new ServiceTracker<>(
					getBundleContext(), SseEventSourceFactory.class, null);
			sseTracker.open();

			assertNotNull(sseTracker.waitForService(2000));

			AtomicReference<Throwable> ref = new AtomicReference<Throwable>();
			List<Integer> list = new CopyOnWriteArrayList<>();
			Semaphore s = new Semaphore(0);

			SseEventSource source = sseTracker.getService().newSource(target);

			source.register(e -> list.add(e.readData(Integer.class)),
					t -> ref.set(t), s::release);

			source.open();

			assertTrue(s.tryAcquire(10, TimeUnit.SECONDS));

			assertNull(ref.get());

			assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), list);
		
		} finally {
			tracker.close();
		}
		
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
