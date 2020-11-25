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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.WriterInterceptor;

import org.gecko.rest.jersey.provider.JerseyConstants;
import org.gecko.rest.jersey.tests.resources.BoundExtension;
import org.gecko.rest.jersey.tests.resources.BoundTestResource;
import org.gecko.rest.jersey.tests.resources.DtoTestExtension;
import org.gecko.rest.jersey.tests.resources.DtoTestResource;
import org.gecko.rest.jersey.tests.resources.HelloResServiceFactory;
import org.gecko.rest.jersey.tests.resources.HelloResource;
import org.gecko.rest.jersey.tests.resources.TestWriteExtension;
import org.gecko.rest.jersey.tests.resources.TestWriterInterceptorException;
import org.gecko.util.test.common.test.AbstractOSGiTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.runtime.dto.BaseDTO;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.runtime.dto.ExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceMethodInfoDTO;
import org.osgi.service.jaxrs.runtime.dto.RuntimeDTO;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

/**
 * Tests the whiteboard dispatcher
 * 
 * @author Mark Hoffmann
 * @since 12.10.2017
 */
@SuppressWarnings("deprecation")
@RunWith(MockitoJUnitRunner.class)
public class JaxRsWhiteboardDTOTests extends AbstractOSGiTest {

	BundleContext context = FrameworkUtil.getBundle(JaxRsWhiteboardDTOTests.class).getBundleContext();

	/**
	 * Creates a new instance.
	 * 
	 * @param bundleContext
	 */
	public JaxRsWhiteboardDTOTests() {
		super(FrameworkUtil.getBundle(JaxRsWhiteboardDTOTests.class).getBundleContext());
	}

	
//	@Test
	public void testDTOSet() throws IOException, InterruptedException, InvalidSyntaxException {
		/*
		 * The server runs on localhost port 8185 using context path test:
		 * http://localhost:8185/test We mount the system with a resource RootResource
		 * under http://localhost:8185/test that will return a HTTP::200 using a GET
		 * request
		 */
		int port = 8185;
		String contextPath = "test";
		/*
		 * Initial setup for the REST runtime
		 */
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);

		org.gecko.util.test.common.service.ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(
				JaxrsServiceRuntime.class);
		runtimeChecker.start();

		ConfigurationAdmin configAdmin = (ConfigurationAdmin) getConfigAdmin();
		assertNotNull(configAdmin);
		Configuration configuration = createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);
		assertNotNull(configuration);
		configuration.update(properties);

		assertTrue(runtimeChecker.waitCreate());

		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertNotNull(runtimeDTO);

		assertNotNull(runtimeDTO.serviceDTO);
		
		assertNotNull(runtimeDTO.defaultApplication);
		assertNotNull(runtimeDTO.defaultApplication.resourceDTOs);
		
		//TODO: The resource 'ptr' is registered because of its ComponentProperties. ? neccessary?
		assertEquals(1,runtimeDTO.defaultApplication.resourceDTOs.length);
		ResourceDTO resourceDtoPtr = getDTOof(runtimeDTO.defaultApplication.resourceDTOs, "ptr");
		assertNotNull(resourceDtoPtr);
		assertNotNull(resourceDtoPtr.resourceMethods);
		assertEquals(1, resourceDtoPtr.resourceMethods.length);

		ResourceMethodInfoDTO methodInfoDTO = getResMethIOof(resourceDtoPtr.resourceMethods, "test");
		assertNotNull(methodInfoDTO);
		assertEquals("GET", methodInfoDTO.method);

		assertNotNull(runtimeDTO.applicationDTOs);
		assertEquals(0, runtimeDTO.applicationDTOs.length);

		assertNotNull(runtimeDTO.failedApplicationDTOs);
		assertEquals(0, runtimeDTO.failedApplicationDTOs.length);

		assertNotNull(runtimeDTO.failedExtensionDTOs);
		assertEquals(0,runtimeDTO.failedExtensionDTOs.length);
		
		assertNotNull(runtimeDTO.failedResourceDTOs);
		assertEquals(0,runtimeDTO.failedResourceDTOs.length);	


		/*
		 * Mount the application customer that will become available under:
		 * test/customer http://localhost:8185/test/customer
		 */
		Dictionary<String, Object> appProps = new Hashtable<>();
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, "dtoApp");
		appProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "dtoApplication");
		
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
		registerServiceForCleanup(new Application(){}, appProps, Application.class);
		
		assertTrue(runtimeChecker.waitModify());
		
		runtimeDTO = getRuntimeDTO();

		assertNotNull(runtimeDTO);

		/*
		 * Mount the extension DtoTestExtension 
		 */
		Dictionary<String, Object> extensionProps = new Hashtable<>();
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "DtoTestExtension");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=dtoApplication)");

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();
		
//		The extension has to implements at least one of the allow interfaces, so it has to declares it
//		(in this case MessageBodyWriter)
		registerServiceForCleanup(new DtoTestExtension(), extensionProps, MessageBodyWriter.class);
		
		assertTrue(runtimeChecker.waitModify());
		
		runtimeDTO = getRuntimeDTO();

		assertNotNull(runtimeDTO);

		/*
		 * Mount the resource DtoTestResource
		 */
		Dictionary<String, Object> resProps = new Hashtable<>();
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "DtoTestResource");
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=dtoApplication)");

		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();		
		
		System.out.println("Register resource");
		registerServiceForCleanup(new DtoTestResource(), resProps, DtoTestResource.class);
		
		assertTrue(runtimeChecker.waitModify());
	
		runtimeDTO = getRuntimeDTO();
		assertNotNull(runtimeDTO);
	}
	
	
	@Test
	public void testResourceDTO() throws IOException, InterruptedException, InvalidSyntaxException {
		
		int port = 8185;
		String contextPath = "test";
		
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);

		org.gecko.util.test.common.service.ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(
				JaxrsServiceRuntime.class);
		runtimeChecker.start();

		ConfigurationAdmin configAdmin = (ConfigurationAdmin) getConfigAdmin();
		assertNotNull(configAdmin);
		Configuration configuration = createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);
		assertNotNull(configuration);
		configuration.update(properties);

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
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "DTO Test Resource");
		resProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=App)");	
				
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();		
		
		registerServiceForCleanup(DtoTestResource.class, new DtoTestResource(), resProps);		
		assertTrue(runtimeChecker.waitModify());
		
		Thread.sleep(2000);
		
		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals(1, runtimeDTO.applicationDTOs.length);
		assertEquals(1, runtimeDTO.applicationDTOs[0].resourceDTOs.length);
		ResourceDTO resDTO = runtimeDTO.applicationDTOs[0].resourceDTOs[0];
		
		assertEquals(2, resDTO.resourceMethods.length);
		for (ResourceMethodInfoDTO infoDTO : resDTO.resourceMethods) {
			String path = infoDTO.path.startsWith("/") ? infoDTO.path.substring(1)
					: infoDTO.path;
			switch(infoDTO.method) {
			case "GET":
				assertEquals("dtores/dtoget", path);
				assertNull(infoDTO.consumingMimeType);
				assertNotNull(infoDTO.producingMimeType);
				assertNull(infoDTO.nameBindings);
				assertEquals(MediaType.WILDCARD, infoDTO.producingMimeType[0]);
				break;
			case "POST":
				assertEquals("dtores/dtopost", path);
				assertNotNull(infoDTO.consumingMimeType);
				assertNull(infoDTO.producingMimeType);
				assertNull(infoDTO.nameBindings);
				assertEquals(MediaType.WILDCARD, infoDTO.consumingMimeType[0]);
				break;				
			}			
		}
		
	}

	
	@Test
	public void testExtensionDTO() throws IOException, InterruptedException, InvalidSyntaxException {
		
		int port = 8185;
		String contextPath = "test";
		
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);

		org.gecko.util.test.common.service.ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(
				JaxrsServiceRuntime.class);
		runtimeChecker.start();

		ConfigurationAdmin configAdmin = (ConfigurationAdmin) getConfigAdmin();
		assertNotNull(configAdmin);
		Configuration configuration = createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);
		assertNotNull(configuration);
		configuration.update(properties);

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
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Extension Test");
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
		
		Thread.sleep(2000);
		
		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals(1, runtimeDTO.applicationDTOs.length);
		assertEquals(1, runtimeDTO.applicationDTOs[0].extensionDTOs.length);
		ExtensionDTO extDTO = runtimeDTO.applicationDTOs[0].extensionDTOs[0];
		assertEquals("Extension Test", extDTO.name);
		assertEquals(1, extDTO.extensionTypes.length);
		assertEquals(WriterInterceptor.class.getName(), extDTO.extensionTypes[0]);		
	}
	
	
	@Test
	public void testNameBindingExtensionDTO() throws IOException, InterruptedException, InvalidSyntaxException {
		
		int port = 8185;
		String contextPath = "test";
		
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);

		org.gecko.util.test.common.service.ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(
				JaxrsServiceRuntime.class);
		runtimeChecker.start();

		ConfigurationAdmin configAdmin = (ConfigurationAdmin) getConfigAdmin();
		assertNotNull(configAdmin);
		Configuration configuration = createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);
		assertNotNull(configuration);
		configuration.update(properties);

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
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Bound Extension Test");
		extensionProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=App)");	
		
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(15);
		runtimeChecker.start();
		
		BoundExtension extension = new BoundExtension();
		
//		We need to register the extension advertising which interfaces is implementing
		registerServiceForCleanup(extension, extensionProps, MessageBodyWriter.class.getName());
		assertTrue(runtimeChecker.waitModify());
		
		Thread.sleep(2000);
		
		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals(1, runtimeDTO.applicationDTOs.length);
		assertEquals(1, runtimeDTO.applicationDTOs[0].extensionDTOs.length);
		ExtensionDTO extDTO = runtimeDTO.applicationDTOs[0].extensionDTOs[0];
		assertEquals("Bound Extension Test", extDTO.name);
		assertEquals(1, extDTO.extensionTypes.length);
		assertEquals(MessageBodyWriter.class.getName(), extDTO.extensionTypes[0]);	
		assertEquals(1, extDTO.nameBindings.length);
		assertEquals(BoundExtension.NameBound.class.getName(), extDTO.nameBindings[0]);
	}
	
	@Test
	public void testNameBindingResourceDTO() throws IOException, InterruptedException, InvalidSyntaxException {
		
		int port = 8185;
		String contextPath = "test";
		
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);

		org.gecko.util.test.common.service.ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(
				JaxrsServiceRuntime.class);
		runtimeChecker.start();

		ConfigurationAdmin configAdmin = (ConfigurationAdmin) getConfigAdmin();
		assertNotNull(configAdmin);
		Configuration configuration = createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);
		assertNotNull(configuration);
		configuration.update(properties);

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
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Bound Extension Test");
		extensionProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=App)");	
		
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(15);
		runtimeChecker.start();
		
		BoundExtension extension = new BoundExtension();
		
//		We need to register the extension advertising which interfaces is implementing
		registerServiceForCleanup(extension, extensionProps, MessageBodyWriter.class.getName());
		assertTrue(runtimeChecker.waitModify());
		
		Dictionary<String, Object> resProps = new Hashtable<>();
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "DTO Test Resource");
		resProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=App)");	
				
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();		
		
		registerServiceForCleanup(BoundTestResource.class, new BoundTestResource(), resProps);		
		assertTrue(runtimeChecker.waitModify());
		
		Thread.sleep(2000);
		
		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals(1, runtimeDTO.applicationDTOs.length);
		assertEquals(1, runtimeDTO.applicationDTOs[0].extensionDTOs.length);
		assertEquals(1, runtimeDTO.applicationDTOs[0].resourceDTOs.length);
		ResourceDTO resDTO = runtimeDTO.applicationDTOs[0].resourceDTOs[0];
		assertEquals(2, resDTO.resourceMethods.length);
		for (ResourceMethodInfoDTO infoDTO : resDTO.resourceMethods) {
			String path = infoDTO.path.startsWith("/") ? infoDTO.path.substring(1)
					: infoDTO.path;
			switch(infoDTO.method) {
			case "GET":
				assertEquals("dtobound/bound", path);
				assertNull(infoDTO.consumingMimeType);
				assertNotNull(infoDTO.producingMimeType);
				assertNotNull(infoDTO.nameBindings);
				assertEquals(1, infoDTO.nameBindings.length);
				assertEquals(BoundExtension.NameBound.class.getName(), infoDTO.nameBindings[0]);
				assertEquals(MediaType.WILDCARD, infoDTO.producingMimeType[0]);
				break;
			case "POST":
				assertEquals("dtobound/unbound", path);
				assertNotNull(infoDTO.consumingMimeType);
				assertNull(infoDTO.producingMimeType);
				assertNull(infoDTO.nameBindings);
				assertEquals(MediaType.WILDCARD, infoDTO.consumingMimeType[0]);
				break;				
			}			
		}
		
		
		ExtensionDTO extDTO = runtimeDTO.applicationDTOs[0].extensionDTOs[0];
		assertEquals("Bound Extension Test", extDTO.name);
		assertEquals(1, extDTO.nameBindings.length);
		assertEquals(BoundExtension.NameBound.class.getName(), extDTO.nameBindings[0]);
		assertNotNull(extDTO.filteredByName);
		assertEquals(1, extDTO.filteredByName.length);
		assertEquals("DTO Test Resource", extDTO.filteredByName[0].name);		
	}
	
	
	@Test
	public void testApplicationDTO() throws IOException, InterruptedException, InvalidSyntaxException {
		
		int port = 8185;
		String contextPath = "test";
		
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);

		org.gecko.util.test.common.service.ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(
				JaxrsServiceRuntime.class);
		runtimeChecker.start();

		ConfigurationAdmin configAdmin = (ConfigurationAdmin) getConfigAdmin();
		assertNotNull(configAdmin);
		Configuration configuration = createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);
		assertNotNull(configuration);
		configuration.update(properties);

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
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "test");
		resProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=App)");	
				
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();		
		
		registerServiceForCleanup(HelloResource.class, new HelloResource(), resProps);		
		assertTrue(runtimeChecker.waitModify());
				
		
		Dictionary<String, Object> extensionProps = new Hashtable<>();
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, "true");
		extensionProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "test");
		extensionProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=App)");	
		
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.setModifyTimeout(15);
		runtimeChecker.start();
		
		TestWriteExtension extension = new TestWriteExtension();
		
//		We need to register the extension advertising which interfaces is implementing
		registerServiceForCleanup(extension, extensionProps, MessageBodyWriter.class.getName());
		assertTrue(runtimeChecker.waitModify());
		
		Thread.sleep(2000);
		
		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals(1, runtimeDTO.applicationDTOs.length);
		assertEquals(1, runtimeDTO.failedExtensionDTOs.length);
		assertEquals(0, runtimeDTO.failedResourceDTOs.length);
		
		resProps = new Hashtable<>();
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_RESOURCE, "true");
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "test");
		resProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=App)");	
				
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();		
		
		registerServiceForCleanup(HelloResource.class, new HelloResource(), resProps);		
		assertTrue(runtimeChecker.waitModify());

		Thread.sleep(2000);
		
		runtimeDTO = getRuntimeDTO();
		assertEquals(1, runtimeDTO.applicationDTOs.length);
		assertEquals(1, runtimeDTO.failedExtensionDTOs.length);
		assertEquals(1, runtimeDTO.failedResourceDTOs.length);
	}
	
	@Test
	public void testNameFilter() throws IOException, InterruptedException, InvalidSyntaxException {
		
		int port = 8185;
		String contextPath = "test";
		
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);

		org.gecko.util.test.common.service.ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(
				JaxrsServiceRuntime.class);
		runtimeChecker.start();

		ConfigurationAdmin configAdmin = (ConfigurationAdmin) getConfigAdmin();
		assertNotNull(configAdmin);
		Configuration configuration = createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);
		assertNotNull(configuration);
		configuration.update(properties);

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
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello Resource");
		resProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=App)");	
				
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();		
		
		registerServiceForCleanup(HelloResource.class, new HelloResource(), resProps);		
		assertTrue(runtimeChecker.waitModify());
		
		Thread.sleep(2000);
				
		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals(1, runtimeDTO.applicationDTOs.length);
		assertEquals(1, runtimeDTO.applicationDTOs[0].resourceDTOs.length);
		assertEquals(3, runtimeDTO.applicationDTOs[0].resourceMethods.length);
	}
	
	@Test
	public void testInvalidProperty() throws IOException, InterruptedException, InvalidSyntaxException {
		
		int port = 8185;
		String contextPath = "test";
		
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);

		org.gecko.util.test.common.service.ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(
				JaxrsServiceRuntime.class);
		runtimeChecker.start();

		ConfigurationAdmin configAdmin = (ConfigurationAdmin) getConfigAdmin();
		assertNotNull(configAdmin);
		Configuration configuration = createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);
		assertNotNull(configuration);
		configuration.update(properties);

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
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello Resource");
		resProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=App)");	
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION_SELECT, "...foo=bar...");
				
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();		
		
		registerServiceForCleanup(HelloResource.class, new HelloResource(), resProps);		
		assertTrue(runtimeChecker.waitModify());
			
		Thread.sleep(2000);
		
		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals(1, runtimeDTO.applicationDTOs.length);
		assertEquals(1, runtimeDTO.failedResourceDTOs.length);
		assertEquals("Hello Resource", runtimeDTO.failedResourceDTOs[0].name);
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, runtimeDTO.failedResourceDTOs[0].failureReason);
	}
	
	@Test
	public void testUngettableService() throws IOException, InterruptedException, InvalidSyntaxException {
		
		int port = 8185;
		String contextPath = "test";
		
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);

		org.gecko.util.test.common.service.ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(
				JaxrsServiceRuntime.class);
		runtimeChecker.start();

		ConfigurationAdmin configAdmin = (ConfigurationAdmin) getConfigAdmin();
		assertNotNull(configAdmin);
		Configuration configuration = createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);
		assertNotNull(configuration);
		configuration.update(properties);

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
		resProps.put(JaxrsWhiteboardConstants.JAX_RS_NAME, "Hello Resource");
		resProps.put(
				JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
				"(osgi.jaxrs.name=App)");	
				
		runtimeChecker.stop();
		runtimeChecker.setModifyCount(1);
		runtimeChecker.start();		
	
		registerServiceForCleanup(HelloResource.class, new HelloResServiceFactory(()->null, (a,b)-> {}), resProps);		
		assertTrue(runtimeChecker.waitModify());
		
		Thread.sleep(2000);
				
		RuntimeDTO runtimeDTO = getRuntimeDTO();
		assertEquals(1, runtimeDTO.applicationDTOs.length);
		assertEquals(1, runtimeDTO.failedResourceDTOs.length);
		assertEquals("Hello Resource", runtimeDTO.failedResourceDTOs[0].name);
		assertEquals(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE, runtimeDTO.failedResourceDTOs[0].failureReason);
	}
	

	private ResourceMethodInfoDTO getResMethIOof(ResourceMethodInfoDTO[] resourceMethodInfoDTOs,String path) {
		for (ResourceMethodInfoDTO resourceMethodInfoDTO : resourceMethodInfoDTOs) {
			if (path.equals(resourceMethodInfoDTO.path)) {
				return resourceMethodInfoDTO;
			}
		}
		return null;
	}


	/**
	 * @param runtimeDTO
	 * @return
	 */
	private <T extends BaseDTO> T getDTOof(T[] baseDTOs,String appName) {
		for (T baseDTO : baseDTOs) {
			if (baseDTO.name.equals(appName)) {
				return baseDTO;
			}
		}
		return null;
	}

	/**
	 * @return
	 */
	private RuntimeDTO getRuntimeDTO() {
		JaxrsServiceRuntime jaxRSRuntime = getJaxRsRuntimeService();
		return jaxRSRuntime.getRuntimeDTO();
	}

	/**
	 * @return
	 */
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
