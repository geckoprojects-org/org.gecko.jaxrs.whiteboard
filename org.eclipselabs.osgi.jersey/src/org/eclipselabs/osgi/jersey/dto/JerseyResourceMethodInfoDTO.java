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
package org.eclipselabs.osgi.jersey.dto;

import org.osgi.service.jaxrs.runtime.dto.ResourceMethodInfoDTO;

/**
 * An implementation {@link ResourceMethodInfoDTO}
 * 
 * https://osgi.org/bugzilla/show_bug.cgi?id=218:
 * All fields can be removed, as soon as the bug is resolved
 * 
 * @author Mark Hoffmann
 * @since 14.07.2017
 */
public class JerseyResourceMethodInfoDTO extends ResourceMethodInfoDTO {
	
	/**
	 * The HTTP verb being handled, for example GET, DELETE, PUT, POST, HEAD,
	 * OPTIONS
	 */
	public String	method;

	/**
	 * The mime-type(s) consumed by this resource method, null if not defined
	 */
	public String[]	consumingMimeType;

	/**
	 * The mime-type(s) produced by this resource method, null if not defined
	 */
	public String[]	producingMimeType;

	/**
	 * The URI of this sub-resource, null if this is not a sub-resource method
	 */
	public String	uri;

}
