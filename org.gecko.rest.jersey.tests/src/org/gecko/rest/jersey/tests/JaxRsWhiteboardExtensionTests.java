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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.WriterInterceptor;

import org.gecko.rest.jersey.provider.JerseyConstants;
import org.gecko.rest.jersey.tests.resources.ContractedExtension;
import org.gecko.rest.jersey.tests.resources.EchoResource;
import org.gecko.rest.jersey.tests.resources.HelloResource;
import org.gecko.rest.jersey.tests.resources.OSGiTextMimeTypeCodec;
import org.gecko.rest.jersey.tests.resources.TestReadExtension;
import org.gecko.rest.jersey.tests.resources.TestWriteExtension;
import org.gecko.rest.jersey.tests.resources.TestWriterInterceptorException;
import org.gecko.rest.jersey.tests.resources.TestWriterInterceptorException2;
import org.gecko.util.test.common.service.ServiceChecker;
import org.gecko.util.test.common.test.AbstractOSGiTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.runtime.dto.RuntimeDTO;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

/**
 * Tests for checking the implementation of osgi.jaxrs.extension.select options
 * and the other filters that can be applied to an extension
 * 
 * @author ilenia
 * @since Jun 9, 2020
 */
@SuppressWarnings("deprecation")
@RunWith(MockitoJUnitRunner.class)
public class JaxRsWhiteboardExtensionTests extends AbstractOSGiTest{

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
	public JaxRsWhiteboardExtensionTests() {
		super(FrameworkUtil.getBundle(JaxRsWhiteboardExtensionTests.class).getBundleContext());
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

	/**
	 *  Checks that if an extension is not advertising any of the allowed interfaces
	 *  it should not be registered and a RuntimeDTO should be created 
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void testExtensionNoContracts() throws IOException, InterruptedException, InvalidSyntaxException {
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
		Application application = new Application(){};

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		registerServiceForCleanup(Application.class, application, appProps);

		assertTrue(runtimeChecker.waitModify());

		/*
		 * Mount the extension ContractedExtension that will become available under:
		 * http://localhost:8185/test/hello
		 */
		Dictionary<String, Object> extensionProps = new Hashtable<>();
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Contracted");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + 
				JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");

		System.out.println("Register resource for uri /hello");

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		ContractedExtension extension = new ContractedExtension();

		//		We need to register the extension NOT advertising that it is implementing MessageBodyWriter
		registerServiceForCleanup(extension, extensionProps, ContractedExtension.class.getName());

		assertTrue(runtimeChecker.waitModify());

		Thread.sleep(1000);

		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals(1, runtimeDTO.failedExtensionDTOs.length);
		assertEquals("Contracted", runtimeDTO.failedExtensionDTOs[0].name);
	}

	/**
	 * Register an extension advertising one of the allowed interface and check that everything is 
	 * registering fine 
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void testExtensionContracts() throws IOException, InterruptedException, InvalidSyntaxException {

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
		Application application = new Application(){};

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		registerServiceForCleanup(Application.class, application, appProps);

		assertTrue(runtimeChecker.waitModify());

		/*
		 * Mount the extension ContractedExtension that will become available under:
		 * http://localhost:8185/test/hello
		 */
		Dictionary<String, Object> extensionProps = new Hashtable<>();
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Contracted");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + 
				JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");

		System.out.println("Register resource for uri /hello");

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		ContractedExtension extension = new ContractedExtension();

		//		We need to register the extension advertising which interfaces is implementing
		registerServiceForCleanup(extension, extensionProps, MessageBodyReader.class.getName(), 
				MessageBodyWriter.class.getName());

		assertTrue(runtimeChecker.waitModify());

		Thread.sleep(1000);

		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals(1, runtimeDTO.applicationDTOs.length);
		assertEquals("customerApp", runtimeDTO.applicationDTOs[0].name);
		assertEquals(1, runtimeDTO.applicationDTOs[0].extensionDTOs.length);
		assertEquals("Contracted", runtimeDTO.applicationDTOs[0].extensionDTOs[0].name);
	}


	/**
	 * Creates an app customerApp 
	 * Creates 2 extensions (one implementing MBR and one MBW) to be added to the app
	 * The MBW requires the MBR through the property osgi.jaxrs.extension.select
	 * Creates a Hello Resource to be added to the app
	 * Check that a post request is correctly called and executed
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void testExtensionSelectOK() throws IOException, InterruptedException, InvalidSyntaxException {

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
		Application application = new Application(){};

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		registerServiceForCleanup(Application.class, application, appProps);

		assertTrue(runtimeChecker.waitModify());

		/*
		 * Mount the extension TestReadExtension for application customerApp
		 */
		Dictionary<String, Object> extensionProps = new Hashtable<>();
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "TestReadExtension");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		TestReadExtension extension = new TestReadExtension();

		//		We need to register the extension advertising which interfaces is implementing
		registerServiceForCleanup(extension, extensionProps, MessageBodyReader.class.getName());

		assertTrue(runtimeChecker.waitModify());		

		/*
		 * Mount the extension TestWriteExtension for application customerApp
		 */
		extensionProps = new Hashtable<>();
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "TestWriteExtension");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION_SELECT, "("+JaxrsWhiteboardConstants.JAX_RS_NAME + "=TestReadExtension)");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		TestWriteExtension extension2 = new TestWriteExtension();

		//		We need to register the extension advertising which interfaces is implementing
		registerServiceForCleanup(extension2, extensionProps, MessageBodyWriter.class.getName());

		assertTrue(runtimeChecker.waitModify());

		/*
		 * Mount the resource HelloResource that will become available under:
		 * http://localhost:8185/test/customer/hello
		 */
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");

		System.out.println("Register resource for uri /hello");

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(15);
		runtimeChecker.start();


		registerServiceForCleanup(HelloResource.class, new HelloResource(), helloProps);

		assertTrue(runtimeChecker.waitModify());	

		Thread.sleep(2000);

		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals(0,runtimeDTO.failedExtensionDTOs.length);
		assertEquals(0, runtimeDTO.failedResourceDTOs.length);
		assertEquals(0, runtimeDTO.failedApplicationDTOs.length);
		assertEquals(1, runtimeDTO.applicationDTOs.length);
		assertEquals("customerApp", runtimeDTO.applicationDTOs[0].name);
		assertEquals(2, runtimeDTO.applicationDTOs[0].extensionDTOs.length);
		assertEquals(1, runtimeDTO.applicationDTOs[0].resourceDTOs.length);

		/*
		 * Check if our RootResource is available under http://localhost:8185/test/customer/hello
		 */
		String checkUrl = url + "/customer/hello";
		System.out.println("Checking URL is available: " + checkUrl);
		Invocation post = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildPost(Entity.entity("test", "text/plain"));
		Response response = post.invoke();
		assertEquals(200, response.getStatus());
		assertNotNull(response.getEntity());
		String result01 = response.readEntity(String.class);
		System.out.println(result01);
		assertNotNull(result01);

		assertTrue(result01.contains(TestReadExtension.READER_POSTFIX));
		assertTrue(result01.contains(TestWriteExtension.WRITER_POSTFIX));
	}


	/**
	 * Creates an app customerApp 
	 * Creates 1 extension TestReadExtension which depends on a TestWriteExtension
	 * We do NOT create the TestWriteExtension, so the TestReadExtension should not be added
	 * Creates a Hello resource
	 * Check that the response does not make use of the extension
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void testWrongExtensionSelect() throws IOException, InterruptedException, InvalidSyntaxException {

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
		Application application = new Application(){};

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		registerServiceForCleanup(Application.class, application, appProps);

		assertTrue(runtimeChecker.waitModify());

		/*
		 * Mount the extension TestReadExtension for application customerApp
		 */
		Dictionary<String, Object> extensionProps = new Hashtable<>();
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "TestReadExtension");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION_SELECT, "("+JaxrsWhiteboardConstants.JAX_RS_NAME + "=TestWriteExtension)");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		TestReadExtension extension = new TestReadExtension();

		//		We need to register the extension advertising which interfaces is implementing
		registerServiceForCleanup(extension, extensionProps, MessageBodyReader.class.getName());

		assertTrue(runtimeChecker.waitModify());

		/*
		 * Mount the resource HelloResource that will become available under:
		 * http://localhost:8185/test/customer/hello
		 */
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");

		System.out.println("Register resource for uri /hello");

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();		

		registerServiceForCleanup(HelloResource.class, new HelloResource(), helloProps);

		assertTrue(runtimeChecker.waitModify());

		Thread.sleep(2000);

		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals(1,runtimeDTO.failedExtensionDTOs.length);
		assertEquals("TestReadExtension", runtimeDTO.failedExtensionDTOs[0].name);
		assertEquals(DTOConstants.FAILURE_REASON_REQUIRED_EXTENSIONS_UNAVAILABLE, runtimeDTO.failedExtensionDTOs[0].failureReason);
		assertEquals(0, runtimeDTO.failedResourceDTOs.length);
		assertEquals(0, runtimeDTO.failedApplicationDTOs.length);
		assertEquals(1, runtimeDTO.applicationDTOs.length);
		assertEquals("customerApp", runtimeDTO.applicationDTOs[0].name);
		assertEquals(0, runtimeDTO.applicationDTOs[0].extensionDTOs.length);
		assertEquals(1, runtimeDTO.applicationDTOs[0].resourceDTOs.length);

		/*
		 * Check if our RootResource is available under http://localhost:8185/test/customer/hello
		 */
		String checkUrl = url + "/customer/hello";
		System.out.println("Checking URL is available: " + checkUrl);
		Invocation post = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildPost(Entity.entity("test", "text/plain"));
		Response response = post.invoke();
		assertEquals(200, response.getStatus());
		assertNotNull(response.getEntity());
		String result01 = response.readEntity(String.class);
		assertNotNull(result01);

		assertFalse(result01.contains(TestReadExtension.READER_POSTFIX));
		assertFalse(result01.contains(TestWriteExtension.WRITER_POSTFIX));		
	}

	/**
	 * Creates an app customerApp 
	 * Creates a Hello resource which depends on a TestWriteExtension
	 * We do NOT create the TestWriteExtension, so the TestReadExtension should not be added
	 * Check that NO Resource is actually available
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void testWrongResourceExtensionSelect() throws IOException, InterruptedException, InvalidSyntaxException {		
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
		Application application = new Application(){};

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		registerServiceForCleanup(Application.class, application, appProps);

		assertTrue(runtimeChecker.waitModify());

		/*
		 * Mount the resource HelloResource which requires a 
		 * TestWriteExtension that we are NOT going to register
		 */
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION_SELECT, "("+JaxrsWhiteboardConstants.JAX_RS_NAME + "=TestWriteExtension)");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");

		System.out.println("Register resource for uri /hello");

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();		

		registerServiceForCleanup(HelloResource.class, new HelloResource(), helloProps);

		assertTrue(runtimeChecker.waitModify());

		/*
		 * Check if our RootResource is available under http://localhost:8185/test/customer/hello
		 */
		String checkUrl = url + "/customer/hello";
		System.out.println("Checking URL is available: " + checkUrl);
		Invocation post = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildPost(Entity.entity("test", "text/plain"));
		Response response = post.invoke();
		assertEquals(404, response.getStatus());

		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals(1, runtimeDTO.applicationDTOs.length);
		assertEquals("customerApp", runtimeDTO.applicationDTOs[0].name);
		assertEquals(1, runtimeDTO.failedResourceDTOs.length);
		assertEquals("Hello", runtimeDTO.failedResourceDTOs[0].name);
		assertEquals(0, runtimeDTO.failedExtensionDTOs.length);
	}


	/**
	 * Creates an app customerApp 
	 * The app will depend on TestReadExtension
	 * Creates 1 extension TestReadExtension which depends on a TestWriteExtension
	 * We do NOT create the TestWriteExtension, so the TestReadExtension should not be added
	 * Creates a Hello resource
	 * Check that the resource is NOT available because the app itself should NOT be registered
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void testAppWrongExtensionSelect() throws IOException, InterruptedException, InvalidSyntaxException {

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
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION_SELECT, "("+
				JaxrsWhiteboardConstants.JAX_RS_NAME + "=TestReadExtension)");
		Application application = new Application(){};

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		registerServiceForCleanup(Application.class, application, appProps);

		assertTrue(runtimeChecker.waitModify());

		/*
		 * Mount the extension TestReadExtension for application customerApp
		 */
		Dictionary<String, Object> extensionProps = new Hashtable<>();
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "TestReadExtension");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION_SELECT, "("+
				JaxrsWhiteboardConstants.JAX_RS_NAME + "=TestWriteExtension)");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + 
				JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		TestReadExtension extension = new TestReadExtension();

		//		We need to register the extension advertising which interfaces is implementing
		registerServiceForCleanup(extension, extensionProps, MessageBodyReader.class.getName());

		assertTrue(runtimeChecker.waitModify());

		/*
		 * Mount the resource HelloResource that should not be registered because
		 * the TestReadExtension depends on the TestWriteExtension which is not there, 
		 * and the app itself depends on the TestReadExtension that is not registered 
		 * due to its missing dependency
		 */
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + 
				JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");

		System.out.println("Register resource for uri /hello");

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();		

		registerServiceForCleanup(HelloResource.class, new HelloResource(), helloProps);

		assertTrue(runtimeChecker.waitModify());

		/*
		 * Check if our RootResource is available under http://localhost:8185/test/customer/hello
		 */
		String checkUrl = url + "/customer/hello";
		System.out.println("Checking URL is available: " + checkUrl);
		Invocation post = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildPost(Entity.entity("test", "text/plain"));
		Response response = post.invoke();
		assertEquals(404, response.getStatus());

		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals(1, runtimeDTO.failedApplicationDTOs.length);
		assertEquals("customerApp", runtimeDTO.failedApplicationDTOs[0].name);
		assertEquals(1, runtimeDTO.failedResourceDTOs.length);
		assertEquals("Hello", runtimeDTO.failedResourceDTOs[0].name);
		assertEquals(1, runtimeDTO.failedExtensionDTOs.length);
		assertEquals("TestReadExtension", runtimeDTO.failedExtensionDTOs[0].name);
		assertEquals(0, runtimeDTO.applicationDTOs.length);
	}


	/**
	 * Creates an app customerApp 
	 * Creates 1 extension TestReadExtension which has the extension.select property sets to one of the app properties 
	 * (according to spec this is allowed)
	 * Creates a Hello resource
	 * Check that the resource is available and the result is as expected
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void testAppExtensionSelect() throws IOException, InterruptedException, InvalidSyntaxException {

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
		Application application = new Application(){};

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		registerServiceForCleanup(Application.class, application, appProps);

		assertTrue(runtimeChecker.waitModify());

		/*
		 * Mount the extension TestReadExtension for application customerApp
		 */
		Dictionary<String, Object> extensionProps = new Hashtable<>();
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "TestReadExtension");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION_SELECT, "("+JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE + "=customer)");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		TestReadExtension extension = new TestReadExtension();

		//		We need to register the extension advertising which interfaces is implementing
		registerServiceForCleanup(extension, extensionProps, MessageBodyReader.class.getName());

		assertTrue(runtimeChecker.waitModify());

		/*
		 * Mount the resource HelloResource 
		 */
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");

		System.out.println("Register resource for uri /hello");

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();		

		registerServiceForCleanup(HelloResource.class, new HelloResource(), helloProps);

		assertTrue(runtimeChecker.waitModify());

		/*
		 * Check if our RootResource is available under http://localhost:8185/test/customer/hello
		 */
		String checkUrl = url + "/customer/hello";
		System.out.println("Checking URL is available: " + checkUrl);
		Invocation post = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildPost(Entity.entity("test", "text/plain"));
		Response response = post.invoke();
		assertEquals(200, response.getStatus());
		assertNotNull(response.getEntity());
		String result01 = response.readEntity(String.class);
		System.out.println(result01);
		assertNotNull(result01);

		assertTrue(result01.contains(TestReadExtension.READER_POSTFIX));

		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals(1, runtimeDTO.applicationDTOs.length);
		assertEquals("customerApp", runtimeDTO.applicationDTOs[0].name);
		assertEquals(1, runtimeDTO.applicationDTOs[0].extensionDTOs.length);
		assertEquals("TestReadExtension", runtimeDTO.applicationDTOs[0].extensionDTOs[0].name);
		assertEquals(1, runtimeDTO.applicationDTOs[0].resourceDTOs.length);
		assertEquals("Hello", runtimeDTO.applicationDTOs[0].resourceDTOs[0].name);
		assertEquals(0, runtimeDTO.failedApplicationDTOs.length);
		assertEquals(0, runtimeDTO.failedExtensionDTOs.length);
		assertEquals(0, runtimeDTO.failedResourceDTOs.length);
	}


	/**
	 * Creates an app customerApp 
	 * Creates 1 extension TestReadExtension
	 * Creates a Hello resource which has the extension.select property sets to one of the whiteboard properties 
	 * (according to spec this is allowed)
	 * Check that the resource is available and the result is as expected
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void testWBExtensionSelect() throws IOException, InterruptedException, InvalidSyntaxException {

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
		Application application = new Application(){};

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		registerServiceForCleanup(Application.class, application, appProps);

		assertTrue(runtimeChecker.waitModify());

		/*
		 * Mount the extension TestReadExtension for application customerApp
		 */
		Dictionary<String, Object> extensionProps = new Hashtable<>();
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "TestReadExtension");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		TestReadExtension extension = new TestReadExtension();

		//		We need to register the extension advertising which interfaces is implementing
		registerServiceForCleanup(extension, extensionProps, MessageBodyReader.class.getName());

		assertTrue(runtimeChecker.waitModify());

		/*
		 * Mount the resource HelloResource 
		 */
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION_SELECT, "("+JerseyConstants.JERSEY_CONTEXT_PATH + "="+contextPath+")");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");

		System.out.println("Register resource for uri /hello");

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();		

		registerServiceForCleanup(HelloResource.class, new HelloResource(), helloProps);

		assertTrue(runtimeChecker.waitModify());

		/*
		 * Check if our RootResource is available under http://localhost:8185/test/customer/hello
		 */
		String checkUrl = url + "/customer/hello";
		System.out.println("Checking URL is available: " + checkUrl);
		Invocation post = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildPost(Entity.entity("test", "text/plain"));
		Response response = post.invoke();
		assertEquals(200, response.getStatus());
		assertNotNull(response.getEntity());
		String result01 = response.readEntity(String.class);
		System.out.println(result01);
		assertNotNull(result01);

		assertTrue(result01.contains(TestReadExtension.READER_POSTFIX));

		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals(1, runtimeDTO.applicationDTOs.length);
		assertEquals("customerApp", runtimeDTO.applicationDTOs[0].name);
		assertEquals(1, runtimeDTO.applicationDTOs[0].extensionDTOs.length);
		assertEquals("TestReadExtension", runtimeDTO.applicationDTOs[0].extensionDTOs[0].name);
		assertEquals(1, runtimeDTO.applicationDTOs[0].resourceDTOs.length);
		assertEquals("Hello", runtimeDTO.applicationDTOs[0].resourceDTOs[0].name);
		assertEquals(0, runtimeDTO.failedApplicationDTOs.length);
		assertEquals(0, runtimeDTO.failedExtensionDTOs.length);
		assertEquals(0, runtimeDTO.failedResourceDTOs.length);
	}

	/**
	 * Creates an app customerApp 
	 * Creates 1 extension TestReadExtension 
	 * Creates a Hello resource which has the extension.select property sets to a non existing wb property
	 * Check that the resource is not available
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void testWrongWBExtensionSelect() throws IOException, InterruptedException, InvalidSyntaxException {

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
		Application application = new Application(){};

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		registerServiceForCleanup(Application.class, application, appProps);

		assertTrue(runtimeChecker.waitModify());

		/*
		 * Mount the extension TestReadExtension for application customerApp
		 */
		Dictionary<String, Object> extensionProps = new Hashtable<>();
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "TestReadExtension");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		TestReadExtension extension = new TestReadExtension();

		//		We need to register the extension advertising which interfaces is implementing
		registerServiceForCleanup(extension, extensionProps, MessageBodyReader.class.getName());

		assertTrue(runtimeChecker.waitModify());

		/*
		 * Mount the resource HelloResource 
		 */
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION_SELECT, "("+JerseyConstants.JERSEY_CONTEXT_PATH + "=wrongPath)");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");

		System.out.println("Register resource for uri /hello");

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();		

		registerServiceForCleanup(HelloResource.class, new HelloResource(), helloProps);

		assertTrue(runtimeChecker.waitModify());

		/*
		 * Check if our RootResource is available under http://localhost:8185/test/customer/hello
		 */
		String checkUrl = url + "/customer/hello";
		System.out.println("Checking URL is available: " + checkUrl);
		Invocation post = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildPost(Entity.entity("test", "text/plain"));
		Response response = post.invoke();
		assertEquals(404, response.getStatus());

		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals(1, runtimeDTO.applicationDTOs.length);
		assertEquals("customerApp", runtimeDTO.applicationDTOs[0].name);
		assertEquals(1, runtimeDTO.applicationDTOs[0].extensionDTOs.length);
		assertEquals("TestReadExtension", runtimeDTO.applicationDTOs[0].extensionDTOs[0].name);
		assertEquals(0, runtimeDTO.applicationDTOs[0].resourceDTOs.length);		
		assertEquals(0, runtimeDTO.failedApplicationDTOs.length);
		assertEquals(0, runtimeDTO.failedExtensionDTOs.length);
		assertEquals(1, runtimeDTO.failedResourceDTOs.length);
		assertEquals("Hello", runtimeDTO.failedResourceDTOs[0].name);
	}


	/**
	 * Register an app for the whiteboard
	 * Register 2 extensions with the same name and different rank
	 * Verify that only the highest ranked one is registered, and the other one is resulting in a failure DTO
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void testExtensionSameName() throws IOException, InterruptedException, InvalidSyntaxException {

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
		Application application = new Application(){};

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		registerServiceForCleanup(Application.class, application, appProps);

		assertTrue(runtimeChecker.waitModify());

		/*
		 * Mount the extension TestReadExtension for application customerApp
		 */
		Dictionary<String, Object> extensionProps = new Hashtable<>();
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "TestReadExtension");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");
		extensionProps.put(Constants.SERVICE_RANKING, 2);

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(15);
		runtimeChecker.start();

		TestReadExtension extension = new TestReadExtension();

		//		We need to register the extension advertising which interfaces is implementing
		registerServiceForCleanup(extension, extensionProps, MessageBodyReader.class.getName());

		assertTrue(runtimeChecker.waitModify());

		//		Register another extension with the same name
		extensionProps = new Hashtable<>();
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "TestReadExtension");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT, "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=customerApp)");
		extensionProps.put(Constants.SERVICE_RANKING, 200);

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(20);
		runtimeChecker.start();

		extension = new TestReadExtension();

		//		We need to register the extension advertising which interfaces is implementing
		registerServiceForCleanup(extension, extensionProps, MessageBodyReader.class.getName());

		assertTrue(runtimeChecker.waitModify());

		Thread.sleep(2000);

		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals(1, runtimeDTO.failedExtensionDTOs.length);
		assertEquals(DTOConstants.FAILURE_REASON_DUPLICATE_NAME, runtimeDTO.failedExtensionDTOs[0].failureReason);

	}

	/**
	 * Creates an app which depends on an extension
	 * Creates a resource for both our app and the .default
	 * Checks that the resource is only available for the .default app
	 * Adds the required extension
	 * Check that the resource is now available for both our app and the .default one
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void testExtensionDependency() throws IOException, InterruptedException, InvalidSyntaxException {

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

		//		Register an app which requires an extension
		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "customer");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "customerApp");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION_SELECT,
				"(replacer-config=*)");
		Application application = new Application(){};

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		registerServiceForCleanup(Application.class, application, appProps);		
		assertTrue(runtimeChecker.waitModify());

		//		Register a resource for both customerApp and .default
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		helloProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=*)");		

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();		

		registerServiceForCleanup(HelloResource.class, new HelloResource(), helloProps);

		assertTrue(runtimeChecker.waitModify());

		/*
		 * Check if our resource is available for the .default app but not for customerApp
		 * so, it is only available on http://localhost:8185/test/hello
		 * but not on http://localhost:8185/test/customer/hello
		 */
		String checkUrl = url + "/hello";
		Invocation post = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildPost(Entity.entity("test", "text/plain"));
		Response response = post.invoke();
		assertEquals(200, response.getStatus());

		checkUrl = url + "/customer/hello";
		post = null;
		jerseyClient = ClientBuilder.newClient();
		webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildPost(Entity.entity("test", "text/plain"));
		response = post.invoke();
		assertEquals(404, response.getStatus());

		//		Add the required extension
		Dictionary<String, Object> extensionProps = new Hashtable<>();
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "TestReadExtension");
		extensionProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=*)");				
		extensionProps.put("replacer-config", "fizz-buzz");

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(15);
		runtimeChecker.start();

		TestReadExtension extension = new TestReadExtension();

		//		We need to register the extension advertising which interfaces is implementing
		registerServiceForCleanup(extension, extensionProps, MessageBodyReader.class.getName());		
		assertTrue(runtimeChecker.waitModify());

		//		Check that now the resource is available for both the apps		
		checkUrl = url + "/hello";
		post = null;
		jerseyClient = ClientBuilder.newClient();
		webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildPost(Entity.entity("test", "text/plain"));
		response = post.invoke();
		assertEquals(200, response.getStatus());

		checkUrl = url + "/customer/hello";
		post = null;
		jerseyClient = ClientBuilder.newClient();
		webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildPost(Entity.entity("test", "text/plain"));
		response = post.invoke();
		assertEquals(200, response.getStatus());
	}

	/**
	 * Creates 2 apps different from .default
	 * Creates 1 resource which is assignable to both apps plus the .default one
	 * Verify that resource is available for all 3 apps
	 * Add an extension which requires one of the app property as extension.select
	 * Verify that the resource is still available for the 3 apps but the response is different for the app with the extension
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void testAppExtensionDependency() throws IOException, InterruptedException, InvalidSyntaxException {

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

		//		Register an app which requires an extension
		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "app1");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "App1");
		appProps.put("replacer-config", "fizz-buzz");
		Application application = new Application(){};

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		registerServiceForCleanup(Application.class, application, appProps);		
		assertTrue(runtimeChecker.waitModify());

		appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "app2");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "App2");
		application = new Application(){};

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		registerServiceForCleanup(Application.class, application, appProps);		
		assertTrue(runtimeChecker.waitModify());


		//		Register a resource for both apps and .default
		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		helloProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=*)");		

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();		

		registerServiceForCleanup(HelloResource.class, new HelloResource(), helloProps);		
		assertTrue(runtimeChecker.waitModify());

		//		Verify that the resource is available for all the 3 apps (App1, App2 and .default)
		String checkUrl = url + "/hello";
		Invocation post = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildPost(Entity.entity("test", "text/plain"));
		Response response = post.invoke();
		assertEquals(200, response.getStatus());

		checkUrl = url + "/app1/hello";
		post = null;
		jerseyClient = ClientBuilder.newClient();
		webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildPost(Entity.entity("test", "text/plain"));
		response = post.invoke();
		assertEquals(200, response.getStatus());

		checkUrl = url + "/app2/hello";
		post = null;
		jerseyClient = ClientBuilder.newClient();
		webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildPost(Entity.entity("test", "text/plain"));
		response = post.invoke();
		assertEquals(200, response.getStatus());

		//		Add an extension which requires a property satisfied only by App1
		Dictionary<String, Object> extensionProps = new Hashtable<>();
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "TestWriteExtension");
		extensionProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=*)");				
		extensionProps.put(
				JaxrsWhiteboardConstants.JAX_RS_EXTENSION_SELECT,
				"(replacer-config=*)");

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(15);
		runtimeChecker.start();

		TestWriteExtension extension = new TestWriteExtension();

		//		We need to register the extension advertising which interfaces is implementing
		registerServiceForCleanup(extension, extensionProps, MessageBodyWriter.class.getName());		
		assertTrue(runtimeChecker.waitModify());

		//		Verify that the resource is still available for all 3 apps, but the response is different for the app with the extension
		checkUrl = url + "/app2/hello";
		post = null;
		jerseyClient = ClientBuilder.newClient();
		webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildPost(Entity.entity("test", "text/plain"));
		response = post.invoke();
		assertEquals(200, response.getStatus());
		assertFalse(response.readEntity(String.class).contains(TestWriteExtension.WRITER_POSTFIX));

		checkUrl = url + "/app1/hello";
		post = null;
		jerseyClient = ClientBuilder.newClient();
		webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildPost(Entity.entity("test", "text/plain"));
		response = post.invoke();
		assertEquals(200, response.getStatus());
		assertTrue(response.readEntity(String.class).contains(TestWriteExtension.WRITER_POSTFIX));

		checkUrl = url + "/hello";
		post = null;
		jerseyClient = ClientBuilder.newClient();
		webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildPost(Entity.entity("test", "text/plain"));
		response = post.invoke();
		assertEquals(200, response.getStatus());
		assertFalse(response.readEntity(String.class).contains(TestWriteExtension.WRITER_POSTFIX));		
	}

	@Test
	public void testExtensionOrdering() throws IOException, InterruptedException, InvalidSyntaxException {

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

		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		helloProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=App)");		

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();		

		registerServiceForCleanup(HelloResource.class, new HelloResource(), helloProps);		
		assertTrue(runtimeChecker.waitModify());

		Dictionary<String, Object> extensionProps = new Hashtable<>();
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Extension 1");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(Constants.SERVICE_RANKING, 100);
		extensionProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=App)");	

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(15);
		runtimeChecker.start();

		TestWriterInterceptorException extension = new TestWriterInterceptorException("fizz", "fizzbuzz");

		//		We need to register the extension advertising which interfaces is implementing
		registerServiceForCleanup(extension, extensionProps, WriterInterceptor.class.getName());		
		assertTrue(runtimeChecker.waitModify());	

		String checkUrl = url + "/app/replace";
		Invocation post = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildPost(Entity.entity("fizz", "text/plain"));
		Response response = post.invoke();
		assertEquals(200, response.getStatus());
		String result = response.readEntity(String.class);
		assertTrue(result.contains("fizzbuzz"));

		extensionProps = new Hashtable<>();
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Extension 2");
		extensionProps.put(Constants.SERVICE_RANKING, 1);
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");


		extensionProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=App)");	

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(15);
		runtimeChecker.start();

		TestWriterInterceptorException2 extension2 = new TestWriterInterceptorException2("buzz", "buzzfizz");

		//		We need to register the extension advertising which interfaces is implementing
		registerServiceForCleanup(extension2, extensionProps, WriterInterceptor.class.getName());		
		assertTrue(runtimeChecker.waitModify());

		Thread.sleep(2000);

		post = webTarget.request().buildPost(Entity.entity("fizz buzz", "text/plain"));
		response = post.invoke();
		assertEquals(200, response.getStatus());
		result = response.readEntity(String.class);
		System.out.println(result);
		assertTrue(result.contains("fizzbuzz"));
		assertTrue(result.contains("buzzfizz"));

	}

	/**
	 * Creates an extension which targets the whitebord
	 * Verify that the extension is used
	 * Update the extension for not targeting the whiteboard
	 * Verify that the extension is NOT used
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void testWhiteboardTarget() throws IOException, InterruptedException, InvalidSyntaxException {

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);

		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();

		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);		
		assertTrue(runtimeChecker.waitCreate());

		Long runtimeId = (Long) getJaxRsRuntimeServiceRef().getProperty(Constants.SERVICE_ID);

		String selectFilter = "(service.id=" + runtimeId + ")";
		String rejectFilter = "(!" + selectFilter + ")";

		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "app");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "App");
		Application application = new Application(){};

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();

		registerServiceForCleanup(Application.class, application, appProps);		
		assertTrue(runtimeChecker.waitModify());

		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");
		helloProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=App)");	

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();		

		registerServiceForCleanup(HelloResource.class, new HelloResource(), helloProps);		
		assertTrue(runtimeChecker.waitModify());

		String checkUrl = url + "/app/replace";
		Invocation post = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildPost(Entity.entity("fizz", "text/plain"));
		Response response = post.invoke();
		assertEquals(200, response.getStatus());
		String result = response.readEntity(String.class);
		assertEquals("fizz", result);

		Dictionary<String, Object> extensionProps = new Hashtable<>();
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=App)");	
		extensionProps.put(
				JaxrsWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET,
				selectFilter);	

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(15);
		runtimeChecker.start();

		TestWriterInterceptorException extension = new TestWriterInterceptorException("fizz", "fizzbuzz");

		//		We need to register the extension advertising which interfaces is implementing
		registerServiceForCleanup(extension, extensionProps, WriterInterceptor.class.getName());
		assertTrue(runtimeChecker.waitModify());

		post = webTarget.request().buildPost(Entity.entity("fizz", "text/plain"));
		response = post.invoke();
		assertEquals(200, response.getStatus());
		result = response.readEntity(String.class);
		assertEquals("fizzbuzz", result);

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(15);
		runtimeChecker.start();

		extensionProps.put(
				JaxrsWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET,
				rejectFilter);			
		updateServiceRegistration(extension, extensionProps);
		assertTrue(runtimeChecker.waitModify());

		Thread.sleep(2000);

		post = webTarget.request().buildPost(Entity.entity("fizz", "text/plain"));
		response = post.invoke();
		assertEquals(200, response.getStatus());
		result = response.readEntity(String.class);
		assertEquals("fizz", result);		
	}

	/**
	 * Creates an extension which targets the whitebord
	 * Verify that the extension is used
	 * Update the extension for not targeting the whiteboard
	 * Verify that the extension is NOT used
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void testDefaultAppWhiteboardTarget() throws IOException, InterruptedException, InvalidSyntaxException {

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);

		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();

		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);		
		assertTrue(runtimeChecker.waitCreate());

		Long runtimeId = (Long) getJaxRsRuntimeServiceRef().getProperty(Constants.SERVICE_ID);

		String selectFilter = "(service.id=" + runtimeId + ")";
		String rejectFilter = "(!" + selectFilter + ")";

		Dictionary<String, Object> helloProps = new Hashtable<>();
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		helloProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello");

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();		

		registerServiceForCleanup(HelloResource.class, new HelloResource(), helloProps);		
		assertTrue(runtimeChecker.waitModify());

		String checkUrl = url + "/replace";
		Invocation post = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(checkUrl);
		post = webTarget.request().buildPost(Entity.entity("fizz", "text/plain"));
		Response response = post.invoke();
		assertEquals(200, response.getStatus());
		String result = response.readEntity(String.class);
		assertTrue(result.contains("fizz"));
		assertFalse(result.contains("fizzbuzz"));

		Dictionary<String, Object> extensionProps = new Hashtable<>();
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(
				JaxrsWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET,
				selectFilter);	

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(15);
		runtimeChecker.start();

		TestWriterInterceptorException extension = new TestWriterInterceptorException("fizz", "fizzbuzz");

		//		We need to register the extension advertising which interfaces is implementing
		registerServiceForCleanup(extension, extensionProps, WriterInterceptor.class.getName());
		assertTrue(runtimeChecker.waitModify());

		post = webTarget.request().buildPost(Entity.entity("fizz", "text/plain"));
		response = post.invoke();
		assertEquals(200, response.getStatus());
		result = response.readEntity(String.class);
		assertTrue(result.contains("fizzbuzz"));

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(15);
		runtimeChecker.start();

		extensionProps.put(
				JaxrsWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET,
				rejectFilter);			
		updateServiceRegistration(extension, extensionProps);
		assertTrue(runtimeChecker.waitModify());

		Thread.sleep(2000);

		post = webTarget.request().buildPost(Entity.entity("fizz", "text/plain"));
		response = post.invoke();
		assertEquals(200, response.getStatus());
		result = response.readEntity(String.class);
		assertTrue(result.contains("fizz"));
		assertFalse(result.contains("fizzbuzz"));
	}

	@Test
	public void testRemoveSingletonExt() throws Exception {

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

		Dictionary<String, Object> extensionProps = new Hashtable<>();
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Extension 1");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=App)");	

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(15);
		runtimeChecker.start();

		TestWriterInterceptorException extension = new TestWriterInterceptorException("fizz", "fizzbuzz");

		//		We need to register the extension advertising which interfaces is implementing
		registerServiceForCleanup(extension, extensionProps, WriterInterceptor.class.getName());		
		assertTrue(runtimeChecker.waitModify());	

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(15);
		runtimeChecker.start();

		unregisterService(application);

		assertTrue(runtimeChecker.waitModify());	

		Thread.sleep(2000);

		// extension must be still there
		ServiceChecker<WriterInterceptor> extChecker = createdCheckerTrackedForCleanUp(WriterInterceptor.class);
		extChecker.start();
		assertEquals(1, extChecker.getCurrentCreateCount(true));	

	}

	/**
	 * Section 151.5 Register a JAX-RS MessageBodyReader and show that it
	 * gets applied to the request
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMessageBodyReaderExtension() throws Exception {
		
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);

		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();

		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);		
		assertTrue(runtimeChecker.waitCreate());
		
		properties = new Hashtable<>();
		properties.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, Boolean.TRUE);

		registerServiceForCleanup(new EchoResource(), properties, EchoResource.class.getName());	
		assertTrue(runtimeChecker.waitModify());
		String checkURL = url + "/echo/body";
		Invocation post = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(checkURL);
		MediaType mt = new MediaType("text", "UTF-8");
		post = webTarget.request().buildPost(Entity.text("fizz"));
		Response response = post.invoke();
		assertEquals(200, response.getStatus());
		assertTrue(response.hasEntity());
		assertEquals("fizz", response.readEntity(String.class));



		properties = new Hashtable<>();
		properties.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION,
				Boolean.TRUE);
		properties.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "pte 2");
		properties.put(Constants.SERVICE_RANKING, 100);
		registerServiceForCleanup(new OSGiTextMimeTypeCodec(), properties, MessageBodyReader.class.getName());	

		Thread.sleep(500);
			
		mt = new MediaType("osgi", "text", "UTF-8");
		post = webTarget.request().buildPost(Entity.entity("fizz", mt.toString()));
		response = post.invoke();
		assertEquals(200, response.getStatus());
		// we have a pte MessageBodyWriter that adds  '_1' to or provided content
		String responseString = response.readEntity(String.class);
		assertTrue(responseString.startsWith("OSGi Read: fizz_"));
	}

	/**
	 * Section 151.5 Register a JAX-RS MessageBodyReader and show that it
	 * gets applied to the request
	 * 
	 * @throws Exception
	 */
	@Test
	public void testWebSecurityExtension() throws Exception {
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		Configuration whiteboardConfig = createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);		
		assertTrue(runtimeChecker.waitCreate());
		
		Dictionary<String, Object> resourceProperties = new Hashtable<>();
		resourceProperties.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, Boolean.TRUE);
		
		registerServiceForCleanup(new EchoResource(), resourceProperties, EchoResource.class.getName());	
		assertTrue(runtimeChecker.waitModify());
		String checkURL = url + "/echo/body";
		Invocation post = null;
		Client jerseyClient = ClientBuilder.newClient();
		WebTarget webTarget = jerseyClient.target(checkURL);
		post = webTarget.request().buildPost(Entity.text("fizz"));
		Response response = post.invoke();
		assertEquals(200, response.getStatus());
		assertTrue(response.hasEntity());
		assertEquals("fizz", response.readEntity(String.class));
		assertNull(response.getHeaderString("Access-Control-Allow-Credentials"));
		
		
		runtimeChecker.stop();
		runtimeChecker.start();
		
		properties.put("websecurity", "false");
		whiteboardConfig.update(properties);
		
		assertTrue(runtimeChecker.waitModify());
		Thread.sleep(500);
		
		post = webTarget.request().header("Origin", "http://localhost").buildPost(Entity.text("fizz"));
		response = post.invoke();
		assertEquals(200, response.getStatus());
		assertTrue(response.hasEntity());
		assertEquals("fizz", response.readEntity(String.class));
		assertEquals("true", response.getHeaderString("Access-Control-Allow-Credentials"));	
		System.setProperty("sun.net.http.allowRestrictedHeaders", "false");
	}


	private RuntimeDTO getRuntimeDTO() {
		JaxrsServiceRuntime jaxRSRuntime = getJaxRsRuntimeService();
		return jaxRSRuntime.getRuntimeDTO();
	}

	private JaxrsServiceRuntime getJaxRsRuntimeService() {
		JaxrsServiceRuntime jaxRSRuntime = getService(JaxrsServiceRuntime.class);
		return jaxRSRuntime;
	}

	private ServiceReference<JaxrsServiceRuntime> getJaxRsRuntimeServiceRef() {
		ServiceReference<JaxrsServiceRuntime> jaxRSRuntime = getServiceReference(JaxrsServiceRuntime.class);
		return jaxRSRuntime;
	}


}
