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
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Tests the whiteboard dispatcher
 * @author Mark Hoffmann
 * @since 12.10.2017
 */
@RunWith(MockitoJUnitRunner.class)
public class JaxRsWhiteboardApplicationLifecycleTests {

	private final BundleContext context = FrameworkUtil.getBundle(JaxRsWhiteboardApplicationLifecycleTests.class).getBundleContext();
	
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
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class, context);
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
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "/legacy");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "legacyApp");
		ServiceRegistration<Application> appRegistration = context.registerService(Application.class, new AnnotatedTestLegacyApplication(), appProps);
		Filter appFilter = FrameworkUtil.createFilter("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=legacyApp)");
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
		runtimeChecker.setModifyTimeout(5);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "legacyChanged");
		appRegistration.setProperties(appProps);
		
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

	private void tearDownTest(Configuration configuration, JerseyInvocation get) throws IOException, InterruptedException {
		/*
		 * Tear-down the system
		 */
		CountDownLatch deleteLatch = new CountDownLatch(1);
		TestServiceCustomizer<JaxrsServiceRuntime, JaxrsServiceRuntime> c = new TestServiceCustomizer<>(context, null, deleteLatch);
		configuration.delete();
		awaitRemovedService(JaxrsServiceRuntime.class, c);
		deleteLatch.await(10, TimeUnit.SECONDS);
		// wait for server shutdown
		Thread.sleep(2000L);
		if(get != null) {
			try {
				get.invoke();
				fail("Not expected to reach this line of code");
			} catch (ProcessingException e) {
				assertNotNull(e.getCause());
				assertTrue(e.getCause() instanceof ConnectException);
			}
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