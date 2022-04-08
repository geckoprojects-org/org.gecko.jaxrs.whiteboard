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
package org.gecko.rest.jersey.helper;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider;

/**
 * Helper class for the dispatcher
 * @author Mark Hoffmann
 * @since 20.03.2018
 */
public class DispatcherHelper {
	
	/**
	 * Returns a {@link Set} of applications with name default, sorted by their ranking
	 * @param applications the {@link Collection} of applications
	 * @return a {@link Set} of application or an empty {@link Set}
	 */
	public static Set<JaxRsApplicationProvider> getDefaultApplications(Collection<JaxRsApplicationProvider> applications) {
		if (applications == null) {
			return Collections.emptySet();
		}
		
		Set<JaxRsApplicationProvider> resultSet = applications.stream()
				.filter(app->(".default".equals(app.getName()) || "/*".equals(app.getPath())) && !app.isDefault())
				.sorted(Comparator.naturalOrder())
				.collect(Collectors.toUnmodifiableSet());
		return resultSet;
	}

	/**
	 * Returns the highest ranked default application with name default
	 * @param applications the {@link Collection} of applications
	 * @return a {@link Optional} of application 
	 */
	public static Optional<JaxRsApplicationProvider> getDefaultApplication(Collection<JaxRsApplicationProvider> applications) {
		if (applications == null) {
			return Optional.empty();
		}
		return getDefaultApplications(applications).stream().findFirst();
	}
	
}
