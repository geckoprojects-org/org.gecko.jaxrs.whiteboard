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
package org.gecko.rest.jersey.tests.resources;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * Sample rest resource as prototype
 * @author Mark Hoffmann
 * @since 21.10.2017
 */
@Provider
@Component(service=PrototypeExtension.class, scope=ServiceScope.PROTOTYPE, property= {"osgi.jaxrs.name=pte", "osgi.jaxrs.extension=true"})
public class PrototypeExtension implements MessageBodyWriter<String>{

	public static final String PROTOTYPE_POSTFIX = "_protoExtension";
	private static AtomicInteger counter = new AtomicInteger();
	private String postFix = "";

	@Activate
	public void activate() {
		postFix = PROTOTYPE_POSTFIX;
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return true;
	}

	@Override
	public void writeTo(String t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
					throws IOException, WebApplicationException {
		String result = t + "_" + counter.incrementAndGet() + postFix;
		entityStream.write(result.getBytes());
		entityStream.flush();
	}

}
