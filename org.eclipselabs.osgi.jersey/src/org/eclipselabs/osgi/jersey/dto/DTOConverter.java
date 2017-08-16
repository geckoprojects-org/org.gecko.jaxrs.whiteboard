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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;

import org.eclipselabs.osgi.jaxrs.helper.JaxRsHelper;
import org.eclipselabs.osgi.jersey.JaxRsApplicationProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceMethodInfoDTO;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * Helper class to convert object into DTO's
 * @author Mark Hoffmann
 * @since 14.07.2017
 */
public class DTOConverter {
	
	/**
	 * This mapping sequence was taken from:
	 * @see https://github.com/njbartlett/osgi_jigsaw/blob/master/nbartlett-jigsaw-osgi/src/nbartlett.jigsaw_osgi/org/apache/felix/framework/DTOFactory.java
	 * @param svc the service reference
	 * @return the service reference dto
	 */
	public static ApplicationDTO toApplicationDTO(JaxRsApplicationProvider applicationProvider) {
		if (applicationProvider == null) {
			throw new IllegalArgumentException("Expected an application provider to create an ApplicationDTO");
		}
		ApplicationDTO dto = new JerseyApplicationDTO();
		dto.name = applicationProvider.getName();
		dto.base = applicationProvider.getPath();
		return dto;
	}

	/**
	 * This mapping sequence was taken from:
	 * @see https://github.com/njbartlett/osgi_jigsaw/blob/master/nbartlett-jigsaw-osgi/src/nbartlett.jigsaw_osgi/org/apache/felix/framework/DTOFactory.java
	 * @param svc the service reference
	 * @return the service reference dto
	 */
	public static ApplicationDTO toApplicationDTO(Application application, String name) {
		if (application == null || name == null) {
			throw new IllegalArgumentException("Expected an application and/or name parameter to create an ApplicationDTO");
		}
		ApplicationDTO dto = new JerseyApplicationDTO();
		dto.name = name;
		dto.base = JaxRsHelper.getServletPath(application);
		return dto;
	}

	/**
	 * Maps a object with properties and a base into a {@link ResourceDTO}
	 * @param resourceRef the service reference to be checked
	 * @param ctx the bundle context
	 * @return a {@link ResourceDTO} or <code>null</code>, if the given object is no JaxRs resource
	 */
	/**
	 * @return
	 */
	public static <T> ResourceDTO toResourceDTO(ServiceReference<T> resourceRef, BundleContext ctx) {
		if (resourceRef == null) {
			throw new IllegalArgumentException("Expected an resource service reference to create a ResourceDTO");
		}
		ServiceObjects<T> serviceObject = ctx.getServiceObjects(resourceRef);
		// service is not availalbe anymore
		if (serviceObject == null) {
			return null;
		}
		Dictionary<String, Object> properties = new Hashtable<>();
		for (String key : resourceRef.getPropertyKeys()) {
			properties.put(key, resourceRef.getProperty(key));
		}
		T service = serviceObject.getService();
		// service is not available anymore
		if (service == null) {
			return null;
		}
		ResourceDTO dto = toResourceDTO(service, properties);
		serviceObject.ungetService(service);
		return dto;
	}

	/**
	 * Maps a object with properties and a base into a {@link ResourceDTO}
	 * @param resource the resource instance, needed to be inspect
	 * @param properties, service properties
	 * @param base the application base
	 * @return a {@link ResourceDTO} or <code>null</code>, if the given object is no JaxRs resource
	 */
	public static <T> ResourceDTO toResourceDTO(T resource, Dictionary<String, Object> properties) {
		// to service available
		if (resource == null) {
			return null;
		}
		if (properties == null) {
			throw new IllegalArgumentException("Expected an resource properties to create a ResourceDTO");
		}
		ResourceDTO dto = new JerseyResourceDTO();
		boolean empty = true;
		Class<?> clazz = resource.getClass();
		Path path = clazz.getAnnotation(Path.class);
		if (path != null) {
			dto.base = path.value();
			empty = false;
		}
		String resourceName = (String) properties.get(JaxRSWhiteboardConstants.JAX_RS_NAME);
		if (resourceName != null) {
			dto.name = resourceName;
			empty = false;
		}
		Long serviceId = (Long) properties.get(Constants.SERVICE_ID);
		if (serviceId != null) {
			dto.serviceId = serviceId.longValue();
			empty = false;
		}
		
		ResourceMethodInfoDTO[] rmiDTOs = getResourceMethodInfoDTOs(resource);
		if (rmiDTOs != null) {
			dto.resourceMethods = rmiDTOs;
			empty = false;
		}
		return empty ? null : dto;
	}

	/**
	 * Creates an array of {@link ResourceMethodInfoDTO} from a given object. A object will only be created,
	 * if at least one of the fields is set.
	 * @param resource the object class to parse
	 * @return an array of method objects or <code>null</code>
	 */
	public static <T> ResourceMethodInfoDTO[] getResourceMethodInfoDTOs(T resource) {
		if (resource == null) {
			throw new IllegalArgumentException("Expected an resource isntance to introspect resource methods an create a ResourceMethodInfoDTO");
		}
		Class<?> clazz = resource.getClass();
		Method[] methods = clazz.getDeclaredMethods();
		List<ResourceMethodInfoDTO> dtos = new ArrayList<>(methods.length);

		for (Method method : methods) {
			ResourceMethodInfoDTO dto = toResourceMethodInfoDTO(method);
			if (dto != null) {
				dtos.add(dto);
			}
		}
		return dtos.isEmpty() ? null : dtos.toArray(new ResourceMethodInfoDTO[dtos.size()]);
	}

	/**
	 * Creates a {@link ResourceMethodInfoDTO} from a given {@link Method}. An object will only be created,
	 * if at least one of the fields is set.
	 * @param method the {@link Method} to parse
	 * @return an DTO or <code>null</code>
	 */
	public static <T> ResourceMethodInfoDTO toResourceMethodInfoDTO(Method method) {
		if (method == null) {
			throw new IllegalArgumentException("Expected a method instance to introspect annpotations and create a ResourceMethodInfoDTO");
		}
		boolean empty = true;
		JerseyResourceMethodInfoDTO dto = new JerseyResourceMethodInfoDTO();
		Path path = method.getAnnotation(Path.class);
		if (path != null) {
			dto.uri = path.value();
			empty = false;
		}
		Consumes consumes = method.getAnnotation(Consumes.class);
		if (consumes != null) {
			dto.consumingMimeType = consumes.value();
			empty = false;
		}
		Produces produces = method.getAnnotation(Produces.class);
		if (produces != null) {
			dto.producingMimeType = produces.value();
			empty = false;
		}
		String methodString = getMethodStrings(method);
		if (methodString != null) {
			dto.method = methodString;
			empty = false;
		}
		return empty ? null : dto;
	}

	/**
	 * Parses the given method for a JaxRs method annotation. If the method is annotated with more than one
	 * method annotation, the values are separated by , 
	 * @param method the method instance of the class to be parsed.
	 * @return the HTTP method string or <code>null</code>
	 */
	public static String getMethodStrings(Method method) {
		List<String> methods = new LinkedList<>();
		checkMethodString(method, GET.class, methods);
		checkMethodString(method, POST.class, methods);
		checkMethodString(method, PUT.class, methods);
		checkMethodString(method, DELETE.class, methods);
		checkMethodString(method, HEAD.class, methods);
		checkMethodString(method, OPTIONS.class, methods);
		if (methods.isEmpty()) {
			return null;
		}
		return methods.stream().collect(Collectors.joining(","));
	}

	/**
	 * Checks a given annotation for presence on the method and add it to the result list
	 * @param method the method to be checked
	 * @param type the annotation type
	 * @param resultList the result list
	 */
	public static <T extends Annotation> void checkMethodString(Method method, Class<T> type, List<String> resultList) {
		T annotation = method.getAnnotation(type);
		if (annotation != null) {
			resultList.add(type.getSimpleName().toUpperCase());
		}
	}

	/**
	 * This mapping sequence was taken from:
	 * @see https://github.com/njbartlett/osgi_jigsaw/blob/master/nbartlett-jigsaw-osgi/src/nbartlett.jigsaw_osgi/org/apache/felix/framework/DTOFactory.java
	 * @param svc the service reference
	 * @return the service reference dto
	 */
	public static ServiceReferenceDTO toServiceReferenceDTO(ServiceReference<?> svc) {
		ServiceReferenceDTO dto = new ServiceReferenceDTO();
		dto.bundle = svc.getBundle().getBundleId();
		dto.id = (Long) svc.getProperty(Constants.SERVICE_ID);
		Map<String, Object> props = new HashMap<String, Object>();
		for (String key : svc.getPropertyKeys()) {
			props.put(key, svc.getProperty(key));
		}
		dto.properties = new HashMap<String, Object>(props);

		Bundle[] ubs = svc.getUsingBundles();
		if (ubs == null)
		{
			dto.usingBundles = new long[0];
		}
		else
		{
			dto.usingBundles = new long[ubs.length];
			for (int j=0; j < ubs.length; j++)
			{
				dto.usingBundles[j] = ubs[j].getBundleId();
			}
		}
		return dto;
	}

}
