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
package org.gecko.rest.jersey.runtime.common;

import javax.ws.rs.client.RxInvokerProvider;

import org.glassfish.jersey.client.JerseyClientBuilder;
import org.osgi.service.jaxrs.client.PromiseRxInvoker;


/**
 * A simple class to enable DS to pickup on the Jersey Client Builder
 * @author Juergen Albert
 * @since 27 Jul 2018
 */
public class ClientBuilderService extends JerseyClientBuilder {
	
	/**
	 * Creates a new instance.
	 */
	public ClientBuilderService(RxInvokerProvider<PromiseRxInvoker> provider) {
		register(provider);
	}

}
