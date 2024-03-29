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
package {{basePackageName}};

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.http.whiteboard.annotations.RequireHttpWhiteboard;
import org.osgi.service.jaxrs.whiteboard.annotations.RequireJaxrsWhiteboard;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

/**
 * This is a Demo Resource for a Jaxrs Whiteboard 
 * 
 * @since 1.0
 */
@RequireHttpWhiteboard
@RequireJaxrsWhiteboard
@JaxrsResource
@JaxrsName("demo-http-whiteboard")
@Component(service = DemoResource.class, enabled = true, scope = ServiceScope.PROTOTYPE)
@Path("/")
public class DemoResource {

	/**
	 * Please check http://{{host}}:{{port}}{{httpWhiteboarContextPath}}{{jaxRsContextPath}}/hello-http-whiteboard
	 * @return
	 */
	@GET
	@Path("/hello-http-whiteboard")
	public String hello() {
		return "Hello World (via HTTP Whiteboard)!";
	}

}
