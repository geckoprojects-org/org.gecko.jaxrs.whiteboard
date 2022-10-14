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
package org.gecko.rest.jersey.runtime.common;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gecko.rest.jersey.provider.JerseyConstants;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.annotation.bundle.Requirements;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.condition.Condition;

import aQute.bnd.annotation.spi.ServiceProvider;

/**
 * 
 * @author mark
 * @since 12.07.2022
 */
@Component(immediate = true)
@ServiceProvider(value = Condition.class)
@Requirements({ 
	@Requirement(namespace = IdentityNamespace.IDENTITY_NAMESPACE, filter = "(" + IdentityNamespace.IDENTITY_NAMESPACE + "=org.glassfish.hk2.osgi-resource-locator)", effective = "resolve"), 
	@Requirement(namespace = IdentityNamespace.IDENTITY_NAMESPACE, filter = "(" + IdentityNamespace.IDENTITY_NAMESPACE + "=org.glassfish.jersey.inject.jersey-hk2)", effective = "resolve"), 
	@Requirement(namespace = IdentityNamespace.IDENTITY_NAMESPACE, filter = "(" + IdentityNamespace.IDENTITY_NAMESPACE + "=org.glassfish.jersey.core.jersey-common)", effective = "resolve"), 
	@Requirement(namespace = IdentityNamespace.IDENTITY_NAMESPACE, filter = "(" + IdentityNamespace.IDENTITY_NAMESPACE + "=org.glassfish.jersey.core.jersey-client)", effective = "resolve"), 
	@Requirement(namespace = IdentityNamespace.IDENTITY_NAMESPACE, filter = "(" + IdentityNamespace.IDENTITY_NAMESPACE + "=org.glassfish.jersey.core.jersey-server)", effective = "resolve") 
	})
public class JerseyRuntimeCheck implements BundleListener {
	
	private static final Logger logger = Logger.getLogger("runtime.check");
	private BundleContext ctx;
	private List<String> bsns = new ArrayList<String>(5);
	private int count = 0;
	private ServiceRegistration<Condition> jerseyRuntimeCondition;
	
	/**
	 * Creates a new instance.
	 */
	public JerseyRuntimeCheck() {
		bsns.add("org.glassfish.hk2.osgi-resource-locator");
		bsns.add("org.glassfish.jersey.inject.jersey-hk2");
		bsns.add("org.glassfish.jersey.core.jersey-common");
		bsns.add("org.glassfish.jersey.core.jersey-client");
		bsns.add("org.glassfish.jersey.core.jersey-server");
	}

	@Activate
	public void activate(BundleContext ctx) {
		this.ctx = ctx;
		startBundles();
		updateCondition();
		ctx.addBundleListener(this);
	}
	
	@Deactivate
	public void deactivate() {
		ctx.removeBundleListener(this);
		if (jerseyRuntimeCondition != null) {
			jerseyRuntimeCondition.unregister();
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
	 */
	@Override
	public void bundleChanged(BundleEvent event) {
		String bsn = event.getBundle().getSymbolicName();
		if (bsns.contains(bsn)) {
			switch (event.getType()) {
			case BundleEvent.STARTED:
				count++;
				updateCondition();
				break;
			default:
				count--;
				updateCondition();
				break;
			}
		}
	}

	/**
	 * Starts all defined bundles that are neccessary to get Jersey running properly
	 */
	private void startBundles() {
		for (Bundle b : ctx.getBundles()) {
			String bsn = b.getSymbolicName();
			if (bsns.contains(bsn)) {
				try {
					b.start();
					count++;
				} catch (BundleException e) {
					logger.log(Level.WARNING, e, ()->"Cannot start bundle: " + bsn);
				}
			}
		}
		
	}

	/**
	 * Updates the Jersey Runtime Condition. It will be removed, if not all needed bundles are started.
	 * It will be registered, if all needed bundles are started 
	 */
	private void updateCondition() {
		if (count == bsns.size()) {
			Dictionary<String, Object> properties = new Hashtable<String, Object>();
			properties.put(Condition.CONDITION_ID, JerseyConstants.JERSEY_RUNTIME);
			jerseyRuntimeCondition = ctx.registerService(Condition.class, Condition.INSTANCE, properties);
		} else {
			if (jerseyRuntimeCondition != null) {
				jerseyRuntimeCondition.unregister();
				jerseyRuntimeCondition = null;
			}
		}
	}

}
