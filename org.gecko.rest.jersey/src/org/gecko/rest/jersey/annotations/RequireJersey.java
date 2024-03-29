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
package org.gecko.rest.jersey.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.osgi.annotation.bundle.Requirement;
import org.osgi.annotation.bundle.Requirements;
import org.osgi.framework.namespace.IdentityNamespace;

/**
 * 
 * @author Mark Hoffmann
 * @since 07.11.2022
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({
		ElementType.TYPE, ElementType.PACKAGE
})
@Requirements(value = {
		@Requirement(namespace = IdentityNamespace.IDENTITY_NAMESPACE, name = "org.glassfish.jersey.inject.jersey-hk2"),
		@Requirement(namespace = IdentityNamespace.IDENTITY_NAMESPACE, name = "org.glassfish.jersey.core.jersey-server"),
		@Requirement(namespace = IdentityNamespace.IDENTITY_NAMESPACE, name = "org.glassfish.jersey.containers.jersey-container-servlet"),
		@Requirement(namespace = IdentityNamespace.IDENTITY_NAMESPACE, name = "org.glassfish.jersey.media.jersey-media-sse"),
		@Requirement(namespace = IdentityNamespace.IDENTITY_NAMESPACE, name = "org.glassfish.jersey.media.jersey-media-jaxb"),
		@Requirement(namespace = IdentityNamespace.IDENTITY_NAMESPACE, name = "jakarta.validation.jakarta.validation-api", version = "2.0")
})
public @interface RequireJersey {

}
