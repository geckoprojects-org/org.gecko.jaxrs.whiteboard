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
package org.gecko.rest.jersey.runtime.util;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsExtension;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsWhiteboardTarget;

/**
 * Handles responses that contain an Optional. It returns a 204 if the Optional. If not it will resolve the entity and set it as the response entity.
 * 
 * @author Juergen Albert
 * @since 21 Sep 2022
 */
//@Component
@JaxrsExtension
@JaxrsName("OptionalResponse Handler")
@ServiceRanking(Integer.MIN_VALUE)
@JaxrsWhiteboardTarget("(!(optional.handler = false))")
@JaxrsApplicationSelect("(!(optional.handler = false))")
public class OptionalResponseFilter implements ContainerResponseFilter {

	
    /* 
     * (non-Javadoc)
     * @see javax.ws.rs.container.ContainerResponseFilter#filter(javax.ws.rs.container.ContainerRequestContext, javax.ws.rs.container.ContainerResponseContext)
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext response) throws IOException {
        Object entity = response.getEntity();
        if(entity instanceof Optional<?> && ((Optional<?>) entity).isPresent()) {
        	response.setEntity(((Optional<?>) entity).get());
        	return;
        }
        if(entity == null || isNonPresentOptionalValue(entity)) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            response.setEntity(null);
        }
    }

    @SuppressWarnings("rawtypes")
	private boolean isNonPresentOptionalValue(Object entity) {
        return entity instanceof Optional<?> && !((Optional)entity).isPresent();
    }
}