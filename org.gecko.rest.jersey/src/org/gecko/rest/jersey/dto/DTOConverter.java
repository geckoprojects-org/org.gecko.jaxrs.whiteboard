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
package org.gecko.rest.jersey.dto;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
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

import org.gecko.rest.jersey.provider.application.JaxRsApplicationContentProvider;
import org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider;
import org.gecko.rest.jersey.provider.application.JaxRsExtensionProvider;
import org.gecko.rest.jersey.provider.application.JaxRsResourceProvider;
import org.gecko.rest.jersey.runtime.application.JerseyExtensionProvider;
import org.gecko.rest.jersey.runtime.application.JerseyResourceProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.ExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceMethodInfoDTO;

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
		String basePath = applicationProvider.getPath();
		if(basePath!=null) {
			dto.base = basePath.replaceAll("/\\*", "/");	
		}
	
		Long sid = applicationProvider.getServiceId();
		dto.serviceId = sid != null ? sid.longValue() : -1;

		// Search for contentProvider and generate ResourceDTOs and ExtensionDTOs
		List<ResourceDTO> rdtos = new ArrayList<>();
		List<ResourceMethodInfoDTO> rmidtos = new ArrayList<>();
		List<ExtensionDTO> edtos = new ArrayList<>();

		// todo: add methods of static defined Ressources to ResourceMethodInfoDTO rmidtos.add()
		
		if (applicationProvider.getContentProviers() != null) {

			for (JaxRsApplicationContentProvider contentProvider : applicationProvider.getContentProviers()) {

				if (contentProvider instanceof JerseyResourceProvider) {
					rdtos.add(toResourceDTO((JaxRsResourceProvider) contentProvider));					
				} else if (contentProvider instanceof JerseyExtensionProvider) {
					edtos.add(toExtensionDTO((JerseyExtensionProvider<?>) contentProvider));
				}
			}
		}
		dto.resourceDTOs = rdtos.toArray(new ResourceDTO[rdtos.size()]);
		dto.extensionDTOs = edtos.toArray(new ExtensionDTO[edtos.size()]);
		dto.resourceMethods = rmidtos.toArray(new ResourceMethodInfoDTO[rmidtos.size()]);
		return dto;
	}
	
	/**
	 * This mapping sequence was taken from:
	 * @see https://github.com/njbartlett/osgi_jigsaw/blob/master/nbartlett-jigsaw-osgi/src/nbartlett.jigsaw_osgi/org/apache/felix/framework/DTOFactory.java
	 * @param svc the service reference
	 * @return the service reference dto
	 */
	public static FailedApplicationDTO toFailedApplicationDTO(JaxRsApplicationProvider applicationProvider, int reason) {
		if (applicationProvider == null) {
			throw new IllegalArgumentException("Expected an application provider to create a FailedApplicationDTO");
		}
		FailedApplicationDTO dto = new FailedApplicationDTO();
		dto.name = applicationProvider.getName();
		dto.base = applicationProvider.getPath();
		Long sid = applicationProvider.getServiceId();
		dto.serviceId = sid != null ? sid.longValue() : -1; 
		dto.failureReason = reason;
		return dto;
	}

	/**
	 * Maps a {@link JaxRsResourceProvider} into a {@link ResourceDTO}
	 * @param resourceProvider the resource provider instance, needed to be inspect
	 * @return a {@link ResourceDTO} or <code>null</code>, if the given object is no JaxRs resource
	 */
	public static <T> ResourceDTO toResourceDTO(JaxRsResourceProvider resourceProvider) {
		if (resourceProvider == null) {
			throw new IllegalArgumentException("Expected an resource provider to create an ResourceDTO");
		}
		ResourceDTO dto = new JerseyResourceDTO();
		dto.name = resourceProvider.getName();
		Long serviceId = resourceProvider.getServiceId();
		dto.serviceId = -1;
		if (serviceId != null) {
			dto.serviceId = serviceId.longValue();
		} 
		ResourceMethodInfoDTO[] rmiDTOs = getResourceMethodInfoDTOs(resourceProvider.getObjectClass());
		if (rmiDTOs != null) {
			dto.resourceMethods = rmiDTOs;
		}
		return dto;
	}
	
	/**
	 * Maps resource provider into a {@link FailedResourceDTO}
	 * @param resourceProvider the resource provider instance, needed to be inspect
	 * @param reason the error reason
	 * @return a {@link FailedResourceDTO} or <code>null</code>, if the given object is no JaxRs resource
	 */
	public static FailedResourceDTO toFailedResourceDTO(JaxRsResourceProvider resourceProvider, int reason) {
		if (resourceProvider == null) {
			throw new IllegalArgumentException("Expected an resource provider to create an FailedResourceDTO");
		}
		FailedResourceDTO dto = new FailedResourceDTO();
		dto.name = resourceProvider.getName();
		Long serviceId = resourceProvider.getServiceId();
		dto.serviceId = serviceId != null ? serviceId.longValue() : -1;
		dto.failureReason = reason;
		return dto;
	}
	
	/**
	 * Maps a {@link JaxRsExtensionProvider} into a {@link ExtensionDTO}
	 * @param provider the extension provider instance, needed to be inspect
	 * @return a {@link ExtensionDTO} or <code>null</code>, if the given object is no JaxRs extension
	 */
	public static <T> ExtensionDTO toExtensionDTO(JaxRsExtensionProvider provider) {
		if (provider == null) {
			throw new IllegalArgumentException("Expected an application content provider to create an ExtensionDTO");
		}
		ExtensionDTO dto = new JerseyExtensionDTO();
		Class<?> clazz = provider.getObjectClass();
		Produces produces = clazz.getAnnotation(Produces.class);
		if (produces != null) {
			dto.produces = produces.value();
		}
		Consumes consumes = clazz.getAnnotation(Consumes.class);
		if (consumes != null) {
			dto.consumes = consumes.value();
		}
		dto.name = provider.getName();
		Long serviceId = provider.getServiceId();
		dto.serviceId = -1;
		if (serviceId != null) {
			dto.serviceId = serviceId.longValue();
		} 
		return dto;
	}
	
	/**
	 * Maps resource provider into a {@link FailedExtensionDTO}
	 * @param provider the extension provider instance, needed to be inspect
	 * @param reason the error reason
	 * @return a {@link FailedExtensionDTO} or <code>null</code>, if the given object is no JaxRs extension
	 */
	public static FailedExtensionDTO toFailedExtensionDTO(JaxRsExtensionProvider provider, int reason) {
		if (provider == null) {
			throw new IllegalArgumentException("Expected an application content provider to create an FailedExtensionDTO");
		}
		FailedExtensionDTO dto = new FailedExtensionDTO();
		dto.name = provider.getName();
		Long serviceId = provider.getServiceId();
		dto.serviceId = serviceId != null ? serviceId.longValue() : -1;
		dto.failureReason = reason;
		return dto;
	}

	/**
	 * Creates an array of {@link ResourceMethodInfoDTO} from a given object. A object will only be created,
	 * if at least one of the fields is set.
	 * @param resource the object class to parse
	 * @return an array of method objects or <code>null</code>
	 */
	public static <T> ResourceMethodInfoDTO[] getResourceMethodInfoDTOs(Class<T> clazz) {
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
		ResourceMethodInfoDTO dto = new ResourceMethodInfoDTO();
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
		Path path = method.getAnnotation(Path.class);
		if (path != null) {
			dto.path = path.value();
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
