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

import javax.ws.rs.client.ClientBuilder;

import org.glassfish.jersey.client.JerseyClientBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceVendor;

/**
 * A simple class to enable DS to pickup on the Jersey Client Builder
 * @author Juergen Albert
 * @since 27 Jul 2018
 */
@Component(service = ClientBuilder.class, scope = ServiceScope.PROTOTYPE)
@ServiceVendor("Gecko.io")
@ServiceDescription("A Jersey specific ClientBuilder")
public class ClientBuilderService extends JerseyClientBuilder {

}
