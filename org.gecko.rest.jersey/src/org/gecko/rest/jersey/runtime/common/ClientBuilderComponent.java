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
package org.gecko.rest.jersey.runtime.common;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.ws.rs.client.ClientBuilder;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.jaxrs.client.SseEventSourceFactory;

/**
 * 
 * @author ilenia
 * @since Jun 11, 2020
 */
@Component
public class ClientBuilderComponent {

	private ServiceRegistration<ClientBuilder> registerClientBuilderService;
	private ServiceRegistration<SseEventSourceFactory> registerSseService;

	@Activate
	public void activate(BundleContext ctx) {

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(Constants.SERVICE_VENDOR, "Gecko.io");
		properties.put(Constants.SERVICE_RANKING, Integer.valueOf(1000));
		properties.put(Constants.SERVICE_DESCRIPTION, "A Jersey specific ClientBuilder");

		registerClientBuilderService = ctx.registerService(ClientBuilder.class, new PrototypeServiceFactory<ClientBuilder>() {
			/* 
			 * (non-Javadoc)
			 * @see org.osgi.framework.PrototypeServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
			 */
			@Override
			public ClientBuilder getService(Bundle bundle, ServiceRegistration<ClientBuilder> registration) {
				ClientBuilderService clientBuilder = new ClientBuilderService();
				return clientBuilder;
			}

			@Override
			public void ungetService(Bundle bundle, ServiceRegistration<ClientBuilder> registration,
					ClientBuilder service) {				
			}
		}, properties);


		properties = new Hashtable<>();
		properties.put(Constants.SERVICE_VENDOR, "Gecko.io");
		properties.put(Constants.SERVICE_DESCRIPTION, "An Implementation of the SseEventSourceFactory");

		registerSseService = ctx.registerService(SseEventSourceFactory.class, new PrototypeServiceFactory<SseEventSourceFactory>() {
			/* 
			 * (non-Javadoc)
			 * @see org.osgi.framework.PrototypeServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
			 */
			@Override
			public SseEventSourceFactory getService(Bundle bundle, ServiceRegistration<SseEventSourceFactory> registration) {
				return new SseEventSourceFactoryImpl();
			}

			@Override
			public void ungetService(Bundle bundle, ServiceRegistration<SseEventSourceFactory> registration,
					SseEventSourceFactory service) {				
			}
		}, properties);
		

	}

	@Deactivate
	public void deactivate() {
		registerClientBuilderService.unregister();
		registerSseService.unregister();
	}

}
