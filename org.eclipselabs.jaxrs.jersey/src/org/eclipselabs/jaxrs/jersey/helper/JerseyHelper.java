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
package org.eclipselabs.jaxrs.jersey.helper;

import javax.ws.rs.core.Application;

import org.osgi.service.component.ComponentContext;

/**
 * Helper class for the Jersey whiteboard
 * @author Mark Hoffmann
 * @since 16.07.2017
 */
public class JerseyHelper {
	
	/**
	 * Returns the property. If it not available but a default value is set, the
	 * default value will be returned.
	 * @param context the component context
	 * @param key the properties key
	 * @param defaultValue the default value
	 * @return the value or defaultValue or <code>null</code>
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getPropertyWithDefault(ComponentContext context, String key, T defaultValue) {
		if (context == null) {
			throw new IllegalStateException("Cannot call getProperties in a state, where the component context is not available");
		}
		Object value = context.getProperties().get(key);
		return value == null ? defaultValue : (T)value;
	}
	
	/**
	 * Returns <code>true</code>, if the application does not contain any resources or extensions
	 * @param application the application to check
	 * @return s <code>true</code>, if the application does not contain any resources or extensions
	 */
	public static boolean isEmpty(Application application) {
		if (application == null) {
			return true;
		}
		return application.getClasses().isEmpty() && 
				application.getSingletons().isEmpty() && 
				application.getProperties().isEmpty();
		
	}

}
