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
package org.eclipselabs.osgi.jersey.tests.services;

/**
 * Sample implementation
 * @author Mark Hoffmann
 * @since 12.07.2017
 */
//@Component(property="osgi.jaxrs.resource=org.eclipselabs.osgi.jersey.tests.services.TestImpl", scope=ServiceScope.PROTOTYPE, service=Object.class)
public class TestImpl implements ITest {
	
	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.tests.services.ITest#getString()
	 */
	@Override
	public String getString() {
		return toString();
	}

}
