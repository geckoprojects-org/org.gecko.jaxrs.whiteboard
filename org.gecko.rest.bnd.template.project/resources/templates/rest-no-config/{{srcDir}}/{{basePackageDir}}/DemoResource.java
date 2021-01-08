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
package {{basePackageName}};

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jaxrs.whiteboard.annotations.RequireJaxrsWhiteboard;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

/**
 * 
 * This is a Demo Resource for a Jaxrs Whiteboard 
 * 
 * @since 1.0
 */
@RequireJaxrsWhiteboard
@JaxrsResource
@JaxrsName("demo")
@Component(service = DemoResource.class, enabled = true, scope = ServiceScope.PROTOTYPE)
@Path("/")
public class DemoResource {

	@GET
	@Path("/hello")
	public String hello() {
		return "hello World";
	}

}
