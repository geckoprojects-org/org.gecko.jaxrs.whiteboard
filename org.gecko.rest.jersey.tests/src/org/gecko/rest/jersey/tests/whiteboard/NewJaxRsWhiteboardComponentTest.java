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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.gecko.rest.jersey.provider.JerseyConstants;
import org.gecko.rest.jersey.tests.whiteboard.resources.HelloResource;
import org.gecko.util.test.common.service.ServiceChecker;
import org.gecko.util.test.common.test.AbstractOSGiTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

/**
 * 
 * @author jalbert
 * @since 25 Sep 2018
 */
@RunWith(MockitoJUnitRunner.class)
public class NewJaxRsWhiteboardComponentTest extends AbstractOSGiTest {

	/**
	 * Creates a new instance.
	 * @param bundleContext
	 */
	public NewJaxRsWhiteboardComponentTest() {
		super(FrameworkUtil.getBundle(NewJaxRsWhiteboardComponentTest.class).getBundleContext());
	}
	
	@Test
	public void testJaxRsContext() throws IOException, InvalidSyntaxException, InterruptedException {
		
		Configuration httpConfig = null;
		Configuration jaxRsConfig = null;
		
		try {
			int port = 8185;
			String contextPath = "test";
			String url = "http://localhost:" + port + "/" + contextPath + "/contextpath/hello";
			
			/*
			 * Initial Setup of the HTTP Runtime
			 * 
			 */
			Dictionary<String, Object> props = new Hashtable<>();
			props.put("org.osgi.service.http.port", port);
			props.put("org.apache.felix.http.context_path", "/test");
			props.put("org.apache.felix.http.name", "Test");
			props.put("org.apache.felix.http.runtime.init." + "test.id", "endpoints");
	
			httpConfig = createConfigForCleanup("org.apache.felix.http", "?", props);
			
			/*
			 * Initial setup for the REST runtime by targeting the http whiteboard and the context
			 */
			Dictionary<String, Object> properties = new Hashtable<>();
			properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
			properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, "/contextpath/*");
			properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(test.id=endpoints)");
			
			ServiceChecker<JaxrsServiceRuntime> checker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
			checker.start();
			
			jaxRsConfig = createConfigForCleanup("JaxRsHttpWhiteboardRuntimeComponent", "?", properties);
			
			assertTrue(checker.waitCreate());
			/*
			 * Check that the REST runtime service become available 
			 */
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
			System.out.println("Register resource for uri /hello under application customer");
			
			checker.stop();
			checker.setModifyCount(1);
			checker.start();
			
			registerServiceForCleanup(HelloResource.class, new HelloResource(), helloProps);
			Filter f = FrameworkUtil.createFilter("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=Hello)");
			HelloResource service = getService(f, 3000l);
			assertNotNull(service);

			assertTrue(checker.waitModify());
			
			response = get.invoke();
			assertEquals(200, response.getStatus());
		} finally {
			if(jaxRsConfig != null) {
				deleteConfigurationAndRemoveFromCleanup(jaxRsConfig);
			}
			if(httpConfig != null) {
				deleteConfigurationAndRemoveFromCleanup(httpConfig);
			}
		}
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
