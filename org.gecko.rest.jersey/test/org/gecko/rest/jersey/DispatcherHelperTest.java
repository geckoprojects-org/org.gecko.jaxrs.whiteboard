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
package org.gecko.rest.jersey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.Application;

import org.gecko.rest.jersey.helper.DispatcherHelper;
import org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider;
import org.gecko.rest.jersey.runtime.application.JerseyApplicationProvider;
import org.gecko.rest.jersey.runtime.common.DefaultApplication;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

/**
 * 
 * @author Mark Hoffmann
 * @since 21.10.2017
 */
public class DispatcherHelperTest {
	
	/**
	 * Test method for {@link org.gecko.rest.jersey.helper.JerseyHelper#isEmpty(javax.ws.rs.core.Application)}.
	 */
	@Test
	public void testGetDefaultApplicationIsEmpty() {
		assertNotNull(DispatcherHelper.getDefaultApplications(null));
		assertEquals(0, DispatcherHelper.getDefaultApplications(null).size());
		
		assertNotNull(DispatcherHelper.getDefaultApplication(null));
		assertFalse(DispatcherHelper.getDefaultApplication(null).isPresent());
	}
	
	/**
	 * Test method for {@link org.gecko.rest.jersey.helper.JerseyHelper#isEmpty(javax.ws.rs.core.Application)}.
	 */
	@Test
	public void testGetDefaultApplicationNoDefault() {
		assertNotNull(DispatcherHelper.getDefaultApplications(null));
		assertEquals(0, DispatcherHelper.getDefaultApplications(null).size());
		
		List<JaxRsApplicationProvider> providers = new LinkedList<JaxRsApplicationProvider>();
		providers.add(createApplicationProvider("test", Integer.valueOf(10), Long.valueOf(1)));
		providers.add(createApplicationProvider("test3", Integer.valueOf(20), Long.valueOf(2)));
		providers.add(createApplicationProvider("test54", Integer.valueOf(40), Long.valueOf(3)));
		
		assertEquals(0, DispatcherHelper.getDefaultApplications(providers).size());
		assertNotNull(DispatcherHelper.getDefaultApplication(providers));
		assertFalse(DispatcherHelper.getDefaultApplication(providers).isPresent());
	}
	
	/**
	 * Test method for {@link org.gecko.rest.jersey.helper.JerseyHelper#isEmpty(javax.ws.rs.core.Application)}.
	 */
	@Test
	public void testGetDefaultApplicationOne() {
		assertNotNull(DispatcherHelper.getDefaultApplications(null));
		assertEquals(0, DispatcherHelper.getDefaultApplications(null).size());
		
		List<JaxRsApplicationProvider> providers = new LinkedList<JaxRsApplicationProvider>();
		providers.add(createApplicationProvider("test", Integer.valueOf(10), Long.valueOf(1)));
		JaxRsApplicationProvider defaultProvider = createApplicationProvider(".default", Integer.valueOf(20), Long.valueOf(2));
		providers.add(defaultProvider);
		providers.add(createApplicationProvider("test54", Integer.valueOf(40), Long.valueOf(3)));
		
		Set<JaxRsApplicationProvider> result = DispatcherHelper.getDefaultApplications(providers);
		assertEquals(1, result.size());
		Optional<JaxRsApplicationProvider> first = result.stream().findFirst();
		assertTrue(first.isPresent());
		assertEquals(defaultProvider, first.get());
		
		first = DispatcherHelper.getDefaultApplication(providers);
		assertNotNull(first);
		assertTrue(first.isPresent());
		assertEquals(defaultProvider, first.get());
		
	}
	
	/**
	 * Test method for {@link org.gecko.rest.jersey.helper.JerseyHelper#isEmpty(javax.ws.rs.core.Application)}.
	 */
	@Test
	public void testGetDefaultApplicationMany() {
		assertNotNull(DispatcherHelper.getDefaultApplications(null));
		assertEquals(0, DispatcherHelper.getDefaultApplications(null).size());
		
		List<JaxRsApplicationProvider> providers = new LinkedList<JaxRsApplicationProvider>();
		providers.add(createApplicationProvider("test", Integer.valueOf(10), Long.valueOf(1)));
		JaxRsApplicationProvider defaultProvider01 = createApplicationProvider(".default", Integer.valueOf(20), Long.valueOf(2));
		providers.add(defaultProvider01);
		JaxRsApplicationProvider defaultProvider02 = createApplicationProvider(".default", Integer.valueOf(30), Long.valueOf(3));
		providers.add(defaultProvider02);
		providers.add(createApplicationProvider("test54", Integer.valueOf(40), Long.valueOf(4)));
		
		Set<JaxRsApplicationProvider> result = DispatcherHelper.getDefaultApplications(providers);
		assertEquals(2, result.size());
		int cnt = 0;
		for (JaxRsApplicationProvider p : result) {
			switch (cnt) {
			case 0:
				assertEquals(defaultProvider02, p);
				break;
			case 1:
				assertEquals(defaultProvider01, p);
				break;
			}
			cnt++;
		}
		
		Optional<JaxRsApplicationProvider> first = DispatcherHelper.getDefaultApplication(providers);
		assertNotNull(first);
		assertTrue(first.isPresent());
		assertEquals(defaultProvider02, first.get());
	}
	
	/**
	 * Test method for {@link org.gecko.rest.jersey.helper.JerseyHelper#isEmpty(javax.ws.rs.core.Application)}.
	 */
	@Test
	public void testGetDefaultApplicationManyWithRealDefault() {
		assertNotNull(DispatcherHelper.getDefaultApplications(null));
		assertEquals(0, DispatcherHelper.getDefaultApplications(null).size());
		
		List<JaxRsApplicationProvider> providers = new LinkedList<JaxRsApplicationProvider>();
		providers.add(createApplicationProvider("test", Integer.valueOf(10), Long.valueOf(1)));
		JaxRsApplicationProvider defaultProvider01 = createApplicationProvider(".default", Integer.valueOf(20), Long.valueOf(2));
		providers.add(defaultProvider01);
		JaxRsApplicationProvider defaultProvider02 = createApplicationProvider(".default", Integer.valueOf(30), Long.valueOf(3));
		providers.add(defaultProvider02);
		providers.add(createApplicationProvider("test54", Integer.valueOf(40), Long.valueOf(4)));
		providers.add(createApplicationProvider(".default", Integer.valueOf(50), Long.valueOf(5), true));
		
		Set<JaxRsApplicationProvider> result = DispatcherHelper.getDefaultApplications(providers);
		assertEquals(2, result.size());
		int cnt = 0;
		for (JaxRsApplicationProvider p : result) {
			switch (cnt) {
			case 0:
				assertEquals(defaultProvider02, p);
				break;
			case 1:
				assertEquals(defaultProvider01, p);
				break;
			}
			cnt++;
		}
		
		Optional<JaxRsApplicationProvider> first = DispatcherHelper.getDefaultApplication(providers);
		assertNotNull(first);
		assertTrue(first.isPresent());
		assertEquals(defaultProvider02, first.get());
	}
	
	@Test
	public void IntSortTest() {
		Set<Integer> sorted = Stream.of(12, 20, 19, 4).sorted(Comparator.reverseOrder()).collect(Collectors.toSet());
		System.out.println("----SORTED-------");
		for (Integer i : sorted) {
			System.out.println("i = " + i);
		}
		System.out.println("-----------------");
		Set<Integer> is = Stream.of(12, 20, 19, 4).sorted(Comparator.reverseOrder()).collect(Collectors.toCollection(LinkedHashSet::new));
		System.out.println("----IS:-----------");
		for (Integer i : is) {
			System.out.println("i = " + i);
		}
		System.out.println("-----------------");
	}
	
	/**
	 * Creates an application provider
	 * @param name provider name
	 * @param rank service rank
	 * @param serviceId the service id
	 * @return the JaxRsApplicationProvider instance 
	 */
	private JaxRsApplicationProvider createApplicationProvider(String name, Integer rank, Long serviceId) {
		return createApplicationProvider(name, rank, serviceId, false);
	}
	
	/**
	 * Creates an application provider
	 * @param name provider name
	 * @param rank service rank
	 * @param serviceId the service id
	 * @param defauktApp <code>true</code>, to create a real default application
	 * @return the JaxRsApplicationProvider instance 
	 */
	private JaxRsApplicationProvider createApplicationProvider(String name, Integer rank, Long serviceId, boolean defaultApp) {
		Map<String, Object> properties = new HashMap<String, Object>();
		if (name != null) {
			properties.put(JaxrsWhiteboardConstants.JAX_RS_NAME, name);
		}
		if (rank != null) {
			properties.put(Constants.SERVICE_RANKING, rank);
		}
		if (serviceId != null) {
			properties.put(Constants.SERVICE_ID, serviceId);
		}
		JaxRsApplicationProvider provider = new JerseyApplicationProvider(defaultApp ? new DefaultApplication() : new Application(), properties);
		return provider;
	}

}
