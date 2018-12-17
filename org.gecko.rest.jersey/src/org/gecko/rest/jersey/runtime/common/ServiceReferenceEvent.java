package org.gecko.rest.jersey.runtime.common;

import org.osgi.framework.ServiceReference;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

@SuppressWarnings("rawtypes")
public class ServiceReferenceEvent {

	private ServiceReference reference;
	private Type type;

	public enum Type {
		ADD,
		MODIFY,
		REMOVE
	}
	
	public ServiceReferenceEvent(ServiceReference reference, Type type) {
		this.reference = reference;
		this.type = type;
	}
	
	public ServiceReference getReference() {
		return reference;
	}
	
	public Type getType() {
		return type;
	}
	
	public boolean isExtension() {
		Object isExtension = reference.getProperty(JaxrsWhiteboardConstants.JAX_RS_EXTENSION);
		if(isExtension == null) {
			return false;
		}
		return isExtension instanceof Boolean ? (boolean) isExtension : Boolean.parseBoolean(isExtension.toString());
	}

	public boolean isResource() {
		Object isResource = reference.getProperty(JaxrsWhiteboardConstants.JAX_RS_RESOURCE);
		if(isResource == null) {
			return false;
		}
		return isResource instanceof Boolean ? (boolean) isResource : Boolean.parseBoolean(isResource.toString());
	}
}
