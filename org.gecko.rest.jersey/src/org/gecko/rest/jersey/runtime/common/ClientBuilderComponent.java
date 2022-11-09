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
package org.gecko.rest.jersey.runtime.common;

import static org.osgi.namespace.service.ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;
import static org.osgi.resource.Namespace.EFFECTIVE_ACTIVE;

import java.util.Dictionary;
import java.util.Hashtable;

import org.gecko.rest.jersey.provider.JerseyConstants;
import org.osgi.annotation.bundle.Capability;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.condition.Condition;
import org.osgi.service.jakartars.client.PromiseRxInvoker;
import org.osgi.service.jakartars.client.SseEventSourceFactory;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.RxInvokerProvider;

/**
 * 
 * @author ilenia
 * @since Jun 11, 2020
 */
@Component(immediate = true, 
	reference = @Reference(name = "runtimeCondition", 
		service = Condition.class , 
		target = JerseyConstants.JERSEY_RUNTIME_CONDITION)
)
@Capability(
		namespace = SERVICE_NAMESPACE,
		uses = ClientBuilder.class,
		effective = EFFECTIVE_ACTIVE,
		attribute = {
				CAPABILITY_OBJECTCLASS_ATTRIBUTE + "=jakarta.ws.rs.client.ClientBuilder",
				"service.scope=prototype"
		}
)
@Capability(
		namespace = SERVICE_NAMESPACE,
		uses = SseEventSourceFactory.class,
		effective = EFFECTIVE_ACTIVE,
		attribute = {
				CAPABILITY_OBJECTCLASS_ATTRIBUTE + "=org.osgi.service.jakartars.client.SseEventSourceFactory",
				"service.scope=bundle"
		}
)
public class ClientBuilderComponent {

	private ServiceRegistration<ClientBuilder> registerClientBuilderService;
	private ServiceRegistration<SseEventSourceFactory> registerSseService;
	@Reference
	private RxInvokerProvider<PromiseRxInvoker> rxInvokerProvider;

	@Activate
	public void activate(BundleContext ctx) {

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(Constants.SERVICE_VENDOR, "Gecko.io");
		properties.put(Constants.SERVICE_SCOPE, Constants.SCOPE_PROTOTYPE);
		properties.put(Constants.SERVICE_DESCRIPTION, "A Jersey specific ClientBuilder");

		registerClientBuilderService = ctx.registerService(ClientBuilder.class, new PrototypeServiceFactory<ClientBuilder>() {
			/* 
			 * (non-Javadoc)
			 * @see org.osgi.framework.PrototypeServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
			 */
			@Override
			public ClientBuilder getService(Bundle bundle, ServiceRegistration<ClientBuilder> registration) {
				ClientBuilderService clientBuilder = new ClientBuilderService(rxInvokerProvider);
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

		registerSseService = ctx.registerService(SseEventSourceFactory.class, new ServiceFactory<SseEventSourceFactory>() {
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
