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
@Component
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