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
package org.gecko.rest.jersey.tests.resources;

import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * Sample rest resource as prototype
 * @author Mark Hoffmann
 * @since 21.10.2017
 */
@Path("/")
@Component(service=Object.class, scope=ServiceScope.PROTOTYPE, property= {"osgi.jaxrs.name=ptr", "osgi.jaxrs.resource=true"})
public class PrototypeResource {
	
	public static final String PROTOTYPE_PREFIX = "test_";
	public static final String PROTOTYPE_POSTFIX = "_protoResource";
	private static AtomicInteger counter = new AtomicInteger();
	private String postFix = "";
	
	@Activate
	public void activate() {
		postFix = PROTOTYPE_POSTFIX;
	}
	
	@GET
	@Path("test")
	public Response getTest() {
		String value = PROTOTYPE_PREFIX + counter.incrementAndGet() + postFix;
		return Response.ok(value).build();
	}

}
