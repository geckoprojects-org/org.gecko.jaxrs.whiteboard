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

import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

/**
 * 
 * @author ilenia
 * @since Jun 15, 2020
 */
public class TestWriterInterceptorException implements WriterInterceptor {
	
	private String toReplace;
	private String replaceWith;
	
	public TestWriterInterceptorException(String toReplace, String replaceWith) {
		this.toReplace = toReplace;
		this.replaceWith = replaceWith;
	}

	/* 
	 * (non-Javadoc)
	 * @see javax.ws.rs.ext.WriterInterceptor#aroundWriteTo(javax.ws.rs.ext.WriterInterceptorContext)
	 */
	@Override
	public void aroundWriteTo(WriterInterceptorContext ctx) throws IOException, WebApplicationException {
		Object entity = ctx.getEntity();
		if (entity != null) {
			ctx.setEntity(entity.toString().replace(toReplace, replaceWith));
		}
		ctx.proceed();
	}

}
