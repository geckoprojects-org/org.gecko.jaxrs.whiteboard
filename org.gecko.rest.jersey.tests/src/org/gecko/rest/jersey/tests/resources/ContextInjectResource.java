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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author mark
 *
 */
@Path("context/inject")
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.WILDCARD)
public class ContextInjectResource {
	
	@Context
	private ServletConfig servletConfig;
	
	@Context
	private ServletContext servletContext;
	
	@Context
	private HttpServletRequest httpServletRequest;
	
	@Context
	private HttpServletResponse httpServletResponse;

	@Context
	private Application application;
	
	@GET
	@Path("servletConfig")
	public Response getServletConfig() {
		if(servletConfig == null) {
			return Response.serverError().entity("nope").build();
		}
		return Response.ok("servletConfig").build();
	}

	@GET
	@Path("servletContext")
	public Response getServletContext() {
		if(servletContext == null) {
			return Response.serverError().entity("nope").build();
		}
		return Response.ok("servletContext").build();
	}
	
	@GET
	@Path("httpServletRequest")
	public Response getHttpServletRequest() {
		if(httpServletRequest == null) {
			return Response.serverError().entity("nope").build();
		}
		return Response.ok("httpServletRequest").build();
	}

	@GET
	@Path("httpServletResponse")
	public Response getHttpServletResponse() {
		if(httpServletResponse == null) {
			return Response.serverError().entity("nope").build();
		}
		return Response.ok("httpServletResponse").build();
	}

	@GET
	@Path("application")
	public Response getApplication() {
		if(application == null) {
			return Response.serverError().entity("nope").build();
		}
		return Response.ok("application").build();
	}

}
