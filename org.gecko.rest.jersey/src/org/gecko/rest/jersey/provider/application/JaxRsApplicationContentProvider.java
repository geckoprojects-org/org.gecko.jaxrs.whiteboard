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
package org.gecko.rest.jersey.provider.application;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import org.gecko.rest.jersey.provider.JaxRsConstants;

/**
 * Provider interface for JaxRs resources and extensions. 
 * This interface contains common methods for both of them
 * 
 * @author Mark Hoffmann
 * @since 09.10.2017
 */
public interface JaxRsApplicationContentProvider extends JaxRsProvider, JaxRsConstants {
	
	static class ContentProviderComparator implements Comparator<JaxRsApplicationContentProvider> {

		/* 
		 * (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(JaxRsApplicationContentProvider o1, JaxRsApplicationContentProvider o2) {
			if (o1 == null || o2 == null) {
				return -1;
			}
			return o1.getId().compareTo(o2.getId());
		}
		
	}
	
	/**
	 * Creates the comparator instance
	 * @return the comparator instance
	 */
	public static ContentProviderComparator getComparator() {
		return new ContentProviderComparator();
	}
	
	/**
	 * Returns <code>true</code>, if this resource is a singleton service
	 * @return <code>true</code>, if this resource is a singleton service
	 */
	public boolean isSingleton();
	/**
	 * Returns the properties or an empty map
	 * @return the properties or an empty map
	 */
	public Map<String, Object> getProperties();
	
	/**
	 * Returns the class of the resource
	 * @return the class of the resource
	 */
	public Class<?> getObjectClass();
	
	/**
	 * Returns <code>true</code>, if this resource can handle the given properties.
	 * If the resource contains a application select, than the properties are checked against
	 * the select filter and returns the result.
	 * If the resource has no application select filter, the method returns <code>true</code>, if it is the default application
	 * @param application the application provider
	 * @return <code>true</code>, if the resource can be handled by a whiteboard runtime with the given properties
	 */
	public boolean canHandleApplication(JaxRsApplicationProvider application);
	
	/**
	 * Returns <code>true</code>, if this resource can be handled by the default application. If no application select is given.
	 * If the application select is given an the application name matches .default, this call returns <code>true</code>, as well 
	 * @return <code>true</code>, if the resource can be handled by the default application. 
	 */
	public boolean canHandleDefaultApplication();
	
	/**
	 * Returns <code>true</code>, if this resource can handle one of theses applications or the default application.
	 * If neither the default application nor any other application can handle this content, this results in an 
	 * VALIDATION FAILED status and so in a failed DTO.
	 * @param applications the application providers
	 * @return <code>true</code>, if the content can be handled by by one of theses applications or the default application
	 */
	public boolean validateApplications(Collection<JaxRsApplicationProvider> applications);
	
	/**
	 * @return a clone of the Object
	 * @throws CloneNotSupportedException
	 */
	Object clone() throws CloneNotSupportedException;
	
}
