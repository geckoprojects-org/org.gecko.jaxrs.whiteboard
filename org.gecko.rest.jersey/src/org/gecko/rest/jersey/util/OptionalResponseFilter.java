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
package org.gecko.rest.jersey.util;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsApplicationSelect;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsExtension;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsWhiteboardTarget;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;

/**
 * Handles responses that contain an Optional. 
 * It returns a 204 if the Optional is not present, 
 * otherwise it will resolve the entity and set it as the response entity.
 * 
 * @author Juergen Albert
 * @since 21 Sep 2022
 */
@Component
@JakartarsExtension
@JakartarsName("OptionalResponse Filter")
@ServiceRanking(Integer.MIN_VALUE)
@JakartarsWhiteboardTarget("(!(optional.handler = false))")
@JakartarsApplicationSelect("(!(optional.handler = false))")
public class OptionalResponseFilter implements ContainerResponseFilter {

    /* 
     * (non-Javadoc)
     * @see javax.ws.rs.container.ContainerResponseFilter#filter(javax.ws.rs.container.ContainerRequestContext, javax.ws.rs.container.ContainerResponseContext)
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext response) throws IOException {
        Object entity = response.getEntity();
        if (entity != null && entity instanceof Optional<?>) {
        	Optional<?> optional = (Optional<?>) entity;
        	if(optional.isPresent()) {
        		response.setEntity(((Optional<?>) entity).get());
        	} else {
        		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        		response.setEntity(null);
        	}
        }
    }
}