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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.ws.rs.core.Application;

import org.gecko.rest.jersey.provider.JerseyConstants;
import org.gecko.rest.jersey.tests.resources.DtoTestExtension;
import org.gecko.rest.jersey.tests.resources.DtoTestResource;
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

	/**
	 * Tests ---- before 88s
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvalidSyntaxException
	 */
	@Test
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
		
		
		registerServiceForCleanup(new DtoTestExtension(), extensionProps, DtoTestExtension.class);
		
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
