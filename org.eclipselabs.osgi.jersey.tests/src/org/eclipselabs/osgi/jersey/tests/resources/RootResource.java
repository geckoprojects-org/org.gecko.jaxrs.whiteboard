/**
 * Copyright (c) 2012 - 2017 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.eclipselabs.osgi.jersey.tests.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * Very simple root resource that is used for the tests to be connected to the root application
 * @author Mark Hoffmann
 * @since 14.07.2017
 */
@Path("/")
@Component(name="RootResource", 
	service=Object.class, 
	property= {"osgi.jaxrs.whiteboard.target=test_wb", "osgi.jaxrs.resource=true"}, 
	scope=ServiceScope.PROTOTYPE)
public class RootResource {
	
	@GET
	public Response getTest() {
		return Response.ok("RootResource").build();
	}

}
