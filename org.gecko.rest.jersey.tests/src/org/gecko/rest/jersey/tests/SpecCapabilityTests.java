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
package org.gecko.rest.jersey.tests;

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.ws.rs.client.ClientBuilder;

import org.gecko.rest.jersey.provider.JerseyConstants;
import org.gecko.util.test.common.service.ServiceChecker;
import org.gecko.util.test.common.test.AbstractOSGiTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.service.jaxrs.client.SseEventSourceFactory;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;

/**
 * Checks if the jaxrs bundles declare the capabilities in accordance with the OSGi Spec (151.11)
 * 
 * @author ilenia
 * @since Jun 12, 2020
 */
@SuppressWarnings("deprecation")
@RunWith(MockitoJUnitRunner.class)
public class SpecCapabilityTests extends AbstractOSGiTest{
	
	/**
	 * This is necessary for a {@link JaxRsWhiteboardExtensionTests#testWebSecurityExtension()} 
	 * and must be set before the first request is made. No other way was working...
	 */
	static {
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
	}
	
	/*
	 *  The server runs on localhost port 8185 using context path test: http://localhost:8185/test
	 */	
	int port = 8185;
	String contextPath = "test";
	String url = "http://localhost:" + port + "/" + contextPath;
	
	private static final String	SERVICE_NAMESPACE = "osgi.service";
	private static final String	CAPABILITY_OBJECTCLASS_ATTRIBUTE = "objectClass";
	private final static String	CAPABILITY_USES_DIRECTIVE = "uses";
	
	private static final String	IMPLEMENTATION_NAMESPACE		= "osgi.implementation";
	private static final String	CAPABILITY_VERSION_ATTRIBUTE	= "version";
	private static final String	JAX_RS_WHITEBOARD_SPECIFICATION_VERSION	= "1.0.0";
	
	private static final String	CONTRACT_NAMESPACE				= "osgi.contract";

	
	private static final List<String> JAX_RS_PACKAGES = Arrays.asList(
			"javax.ws.rs", "javax.ws.rs.core",
			"javax.ws.rs.ext", "javax.ws.rs.client",
			"javax.ws.rs.container", "javax.ws.rs.sse");

	
	
	/**
	 * Creates a new instance.
	 * @param bundleContext
	 */
	public SpecCapabilityTests() {
		super(FrameworkUtil.getBundle(SpecCapabilityTests.class).getBundleContext());
	}
	
	@Test
	public void testJaxRsServiceRuntimeServiceCapability() throws Exception {
		
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);		
		assertTrue(runtimeChecker.waitCreate());
		
		ServiceReference<JaxrsServiceRuntime> runtime = getJaxRsRuntimeService();
		assertNotNull(runtime);		
		
		List<BundleCapability> capabilities = runtime.getBundle()
				.adapt(BundleWiring.class)
				.getCapabilities(SERVICE_NAMESPACE);
		boolean hasCapability = false;
		boolean uses = false;

		for (Capability cap : capabilities) {
			Object o = cap.getAttributes()
					.get(CAPABILITY_OBJECTCLASS_ATTRIBUTE);
			@SuppressWarnings("unchecked")
			List<String> objectClass = o instanceof List ? (List<String>) o
					: asList(valueOf(o));

			if (objectClass.contains(JaxrsServiceRuntime.class.getName())) {
				hasCapability = true;

				String usesDirective = cap.getDirectives()
						.get(CAPABILITY_USES_DIRECTIVE);
				if (usesDirective != null) {
					Set<String> packages = new HashSet<String>(Arrays
							.asList(usesDirective.trim().split("\\s*,\\s*")));
					uses = packages.contains("org.osgi.service.jaxrs.runtime")
							&& packages.contains(
									"org.osgi.service.jaxrs.runtime.dto");
				}

				break;
			}
		}
		assertTrue(
				"No osgi.service capability for the JaxrsServiceRuntime service",
				hasCapability);
		assertTrue(
				"No suitable uses constraint on the runtime package for the JaxrsServiceRuntime service",
				uses);
	}
	
	
	
	@Test
	public void testClientBuilderServiceCapability() throws Exception {
		
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);		
		assertTrue(runtimeChecker.waitCreate());
		
		ServiceReference<JaxrsServiceRuntime> runtime = getJaxRsRuntimeService();
		assertNotNull(runtime);		
		
		List<BundleCapability> capabilities = runtime.getBundle()
				.adapt(BundleWiring.class)
				.getCapabilities(SERVICE_NAMESPACE);

		boolean hasCapability = false;
		boolean uses = false;

		for (Capability cap : capabilities) {
			Object o = cap.getAttributes()
					.get(CAPABILITY_OBJECTCLASS_ATTRIBUTE);
			@SuppressWarnings("unchecked")
			List<String> objectClass = o instanceof List ? (List<String>) o
					: asList(valueOf(o));

			if (objectClass.contains(ClientBuilder.class.getName())) {
				hasCapability = true;

				String usesDirective = cap.getDirectives()
						.get(CAPABILITY_USES_DIRECTIVE);
				if (usesDirective != null) {
					Set<String> packages = new HashSet<String>(Arrays
							.asList(usesDirective.trim().split("\\s*,\\s*")));
					uses = packages.contains("javax.ws.rs.client") &&
							packages.contains("org.osgi.service.jaxrs.client");
				}

				break;
			}
		}
		assertTrue(
				"No osgi.service capability for the ClientBuilder service",
				hasCapability);
		assertTrue(
				"No suitable uses constraint on the runtime package for the ClientBuilder service",
				uses);
	}

	
	@Test
	public void testSseEventSourceFactoryCapability() throws Exception {
		
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);		
		assertTrue(runtimeChecker.waitCreate());
		
		ServiceReference<JaxrsServiceRuntime> runtime = getJaxRsRuntimeService();
		assertNotNull(runtime);		
		
		List<BundleCapability> capabilities = runtime.getBundle()
				.adapt(BundleWiring.class)
				.getCapabilities(SERVICE_NAMESPACE);

		boolean hasCapability = false;
		boolean uses = false;
		
		for (Capability cap : capabilities) {
			Object o = cap.getAttributes()
					.get(CAPABILITY_OBJECTCLASS_ATTRIBUTE);
			@SuppressWarnings("unchecked")
			List<String> objectClass = o instanceof List ? (List<String>) o
					: asList(valueOf(o));

			if (objectClass.contains(SseEventSourceFactory.class.getName())) {
				hasCapability = true;

				String usesDirective = cap.getDirectives()
						.get(CAPABILITY_USES_DIRECTIVE);
				if (usesDirective != null) {
					Set<String> packages = new HashSet<String>(Arrays
							.asList(usesDirective.trim().split("\\s*,\\s*")));
					uses = packages.contains("org.osgi.service.jaxrs.client");
				}

				break;
			}
		}
		assertTrue(
				"No osgi.service capability for the SseEventSourceFactory service",
				hasCapability);
		assertTrue(
				"No suitable uses constraint on the runtime package for the SseEventSourceFactory service",
				uses);
	}
	
	@Test
	public void testJaxRsWhiteboardImplementationCapability() throws Exception {
		
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(JerseyConstants.JERSEY_WHITEBOARD_NAME, "test_wb");
		properties.put(JerseyConstants.JERSEY_PORT, Integer.valueOf(port));
		properties.put(JerseyConstants.JERSEY_CONTEXT_PATH, contextPath);
		
		ServiceChecker<JaxrsServiceRuntime> runtimeChecker = createdCheckerTrackedForCleanUp(JaxrsServiceRuntime.class);
		runtimeChecker.start();
		
		createConfigForCleanup("JaxRsWhiteboardComponent", "?", properties);		
		assertTrue(runtimeChecker.waitCreate());
		
		ServiceReference<JaxrsServiceRuntime> runtime = getJaxRsRuntimeService();
		assertNotNull(runtime);		
		
		boolean hasCapability = false;
		boolean uses = false;
		boolean version = false;

		bundles: for (Bundle bundle : getBundleContext().getBundles()) {
			List<BundleCapability> capabilities = bundle
					.adapt(BundleWiring.class)
					.getCapabilities(IMPLEMENTATION_NAMESPACE);

			for (Capability cap : capabilities) {
				hasCapability = "osgi.jaxrs".equals(
						cap.getAttributes().get(IMPLEMENTATION_NAMESPACE));
				if (hasCapability) {
					Version required = Version
							.valueOf(JAX_RS_WHITEBOARD_SPECIFICATION_VERSION);
					Version toCheck = (Version) cap.getAttributes()
							.get(CAPABILITY_VERSION_ATTRIBUTE);

					version = required.equals(toCheck);

					String usesDirective = cap.getDirectives()
							.get(CAPABILITY_USES_DIRECTIVE);
					if (usesDirective != null) {
						Collection<String> requiredPackages = new ArrayList<>(
								JAX_RS_PACKAGES);
						requiredPackages
								.add("org.osgi.service.jaxrs.whiteboard");

						Set<String> packages = new HashSet<String>(
								Arrays.asList(usesDirective.trim()
										.split("\\s*,\\s*")));

						uses = packages.containsAll(requiredPackages);
					}

					break bundles;
				}
			}
		}

		assertTrue(
				"No osgi.implementation capability for the JAX-RS whiteboard implementation",
				hasCapability);

		assertTrue(
				"No osgi.implementation capability for the JAX-RS Whiteboard at version "
						+ JAX_RS_WHITEBOARD_SPECIFICATION_VERSION,
				version);
		assertTrue(
				"The osgi.implementation capability for the JAX-RS API does not have the correct uses constraint",
				uses);
	}
	
	@Test
	public void testJaxRsContractCapability()
			throws Exception {
		
		boolean hasCapability = false;
		boolean uses = false;
		boolean version = false;
		
		bundles: for (Bundle bundle : getBundleContext().getBundles()) {
			List<BundleCapability> capabilities = bundle
					.adapt(BundleWiring.class)
					.getCapabilities(CONTRACT_NAMESPACE);
			
			for (Capability cap : capabilities) {
				hasCapability = "JavaJAXRS".equals(
						cap.getAttributes().get(CONTRACT_NAMESPACE));
				if (hasCapability) {
					Version required = Version.valueOf("2.1");
					List<Version> toCheck = Collections.emptyList();
					
					Object rawVersion = cap.getAttributes().get(CAPABILITY_VERSION_ATTRIBUTE);
					if(rawVersion instanceof Version) {
						toCheck = Collections.singletonList((Version)rawVersion);
					} else if (rawVersion instanceof Version[]) {
						toCheck = Arrays.asList((Version[])rawVersion);
					} else if (rawVersion instanceof List) {
						@SuppressWarnings("unchecked")
						List<Version> tmp = (List<Version>) rawVersion;
						toCheck = tmp;
					}
					
					version = toCheck.contains(required);

					String usesDirective = cap.getDirectives()
							.get(CAPABILITY_USES_DIRECTIVE);
					if (usesDirective != null) {
						Collection<String> requiredPackages = JAX_RS_PACKAGES;

						Set<String> packages = new HashSet<String>(Arrays
								.asList(usesDirective.trim().split("\\s*,\\s*")));

						uses = packages.containsAll(requiredPackages);
					}

					break bundles;
				}
			}
		}
		
		assertTrue(
				"No osgi.contract capability for the JAX-RS API",
				hasCapability);
		assertTrue(
				"No osgi.contract capability for the JAX-RS API at version 2.1",
				version);
		assertTrue(
				"The osgi.contract capability for the JAX-RS API does not have the correct uses constraint",
				uses);
	}
	
	private ServiceReference<JaxrsServiceRuntime> getJaxRsRuntimeService() {
		ServiceReference<JaxrsServiceRuntime> jaxRSRuntime = getServiceReference(JaxrsServiceRuntime.class);
		return jaxRSRuntime;
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
