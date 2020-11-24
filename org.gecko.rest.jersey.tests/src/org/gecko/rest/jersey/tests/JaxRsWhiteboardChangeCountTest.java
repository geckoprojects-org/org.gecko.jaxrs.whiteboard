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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.ws.rs.core.Application;

import org.gecko.rest.jersey.provider.JerseyConstants;
import org.gecko.rest.jersey.tests.resources.HelloResource;
import org.gecko.util.test.common.service.ServiceChecker;
import org.gecko.util.test.common.test.AbstractOSGiTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

/**
 * Tests the whiteboard dispatcher
 * @author Mark Hoffmann
 * @since 12.10.2017
 */
@SuppressWarnings("deprecation")
@RunWith(MockitoJUnitRunner.class)
public class JaxRsWhiteboardChangeCountTest extends AbstractOSGiTest{

	/**
	 * Creates a new instance.
	 */
	public JaxRsWhiteboardChangeCountTest() {
		super(FrameworkUtil.getBundle(JaxRsWhiteboardChangeCountTest.class).getBundleContext());
	}
	
	/**
	 * Tests 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testWhiteboardComponentChangeCountTestApplicationAdd() throws IOException, InterruptedException, InvalidSyntaxException {
		/*
		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
		 *  We mount the system with a resource RootResource under http://localhost:8185/test that will return a 
		 *  HTTP::200 using a GET request
		 */
		int port = 8185;
		String contextPath = "test";
//		String url = "http://localhost:" + port + "/" + contextPath;
		
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
		 * Check that the REST runtime service become available 
		 */
		ServiceReference<JaxrsServiceRuntime> runtimeRef = getServiceReference(JaxrsServiceRuntime.class);
		assertNotNull(runtimeRef);
		Long firstChangeCount = (Long) runtimeRef.getProperty("service.changecount");
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(60);
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
		assertTrue(firstChangeCount < (Long) runtimeRef.getProperty("service.changecount"));
		
	}

	/**
	 * Tests 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws InvalidSyntaxException 
	 */
	@Test
	public void testWhiteboardComponentChangeCountTestApplicationAndResource() throws IOException, InterruptedException, InvalidSyntaxException {
		/*
		 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
		 *  We mount the system with a resource RootResource under http://localhost:8185/test that will return a 
		 *  HTTP::200 using a GET request
		 */
		int port = 8185;
		String contextPath = "test";
//		String url = "http://localhost:" + port + "/" + contextPath;
		
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
		 * Check that the REST runtime service become available 
		 */
		ServiceReference<JaxrsServiceRuntime> runtimeRef = getServiceReference(JaxrsServiceRuntime.class);
		assertNotNull(runtimeRef);
		JaxrsServiceRuntime runtime = getService(JaxrsServiceRuntime.class);
		assertNotNull(runtime);
		
		Long lastChangeCount = (Long) runtimeRef.getProperty("service.changecount");
		System.out.println("----------------------------");
		System.out.println("start service.changecount: " + lastChangeCount);
		System.out.println("----------------------------");
		
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(60);
		runtimeChecker.setModifyCount(2);
		runtimeChecker.start();
		
		/*
		 * Mount the application customer that will become available under: test/customer
		 * http://localhost:8185/test/customer
		 */
		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "customer");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "customerApp");
		Application application = new Application();
		registerServiceForCleanup(Application.class, application , appProps);
		
		/*
		 * Mount the resource HelloResource that will become available under:
		 * http://localhost:8185/test/hello
		 */
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");
		System.out.println("Register resource for uri /hello under application customer");
		
		HelloResource resource = new HelloResource();
		registerServiceForCleanup(HelloResource.class, resource , helloProps);
		
		assertTrue(runtimeChecker.waitModify());
		assertTrue(lastChangeCount < (Long) runtimeRef.getProperty("service.changecount"));
		lastChangeCount = (Long) runtimeRef.getProperty("service.changecount");

		System.out.println("----------------------------");
		System.out.println("Application and Resource Added service.changecount: " + lastChangeCount);
		System.out.println("----------------------------");
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(60);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		unregisterService(resource);
		
		assertTrue(runtimeChecker.waitModify());
		assertTrue(lastChangeCount < (Long) runtimeRef.getProperty("service.changecount"));
		lastChangeCount = (Long) runtimeRef.getProperty("service.changecount");
	
		System.out.println("----------------------------");
		System.out.println("Resource removed service.changecount: " + lastChangeCount);
		System.out.println("----------------------------");
		
		
		runtimeChecker.stop();
		runtimeChecker.setModifyTimeout(60);
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		System.out.println("unregistering =============================");
		unregisterService(application);
		System.out.println("unregistered =============================");
		
		
		assertTrue(runtimeChecker.waitModify());
		assertTrue(lastChangeCount < (Long) runtimeRef.getProperty("service.changecount"));
		lastChangeCount = (Long) runtimeRef.getProperty("service.changecount");

		System.out.println("----------------------------");
		System.out.println("Application removed service.changecount: " + lastChangeCount);
		System.out.println("----------------------------");
		
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
