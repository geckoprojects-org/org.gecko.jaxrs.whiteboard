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
package org.gecko.rest.jersey.runtime.application;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Application;

import org.gecko.rest.jersey.dto.DTOConverter;
import org.gecko.rest.jersey.helper.JaxRsHelper;
import org.gecko.rest.jersey.helper.JerseyHelper;
import org.gecko.rest.jersey.provider.application.AbstractJaxRsProvider;
import org.gecko.rest.jersey.provider.application.JaxRsApplicationContentProvider;
import org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider;
import org.gecko.rest.jersey.provider.application.JaxRsExtensionProvider;
import org.gecko.rest.jersey.provider.application.JaxRsResourceProvider;
import org.gecko.rest.jersey.runtime.common.DefaultApplication;
import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.service.jaxrs.runtime.dto.BaseApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.runtime.dto.FailedApplicationDTO;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

/**
 * Implementation of the Application Provider
 * @author Mark Hoffmann
 * @since 30.07.2017
 */
public class JerseyApplicationProvider extends AbstractJaxRsProvider<Application> implements JaxRsApplicationProvider {

	private static final Logger logger = Logger.getLogger("jersey.applicationProvider");
	private List<ServletContainer> applicationContainers = new LinkedList<>();
	private String applicationBase;
	private boolean changed = true;
	private JerseyApplication wrappedApplication = null;

	public JerseyApplicationProvider(Application application, Map<String, Object> properties) {
		super(application, properties);
		// create name after validation, because some fields are needed eventually
		if(application != null) {
			wrappedApplication = new JerseyApplication(getProviderName(), application, properties);
		}
		validateProperties();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsApplicationProvider#setServletContainer(org.glassfish.jersey.servlet.ServletContainer)
	 */
	@Override
	public void addServletContainer(ServletContainer applicationContainer) {
		applicationContainers.add(applicationContainer);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsApplicationProvider#setServletContainer(org.glassfish.jersey.servlet.ServletContainer)
	 */
	@Override
	public void removeServletContainer(ServletContainer applicationContainer) {
		applicationContainers.remove(applicationContainer);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsApplicationPrsovider#getServletContainers()
	 */
	@Override
	public List<ServletContainer> getServletContainers() {
		return applicationContainers;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsApplicationProvider#getPath()
	 */
	@Override
	public String getPath() {
		if (wrappedApplication == null) {
			throw new IllegalStateException("This application provider does not contain an application, but should have one to create a context path");
		}
		return applicationBase == null ? null : JaxRsHelper.getServletPath(wrappedApplication.getSourceApplication() , applicationBase);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsApplicationProvider#getJaxRsApplication()
	 */
	@Override
	public Application getJaxRsApplication() {
		if (wrappedApplication == null) {
			throw new IllegalStateException("This application provider does not contain an application, but should have one to return an application");
		}
		return wrappedApplication;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsApplicationProvider#getApplicationProperties()
	 */
	@Override
	public Map<String, Object> getApplicationProperties() {
		return getProviderProperties();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsApplicationProvider#getApplicationDTO()
	 */
	@Override
	public BaseApplicationDTO getApplicationDTO() {
		int status = getProviderStatus();
		if (wrappedApplication == null) {
			throw new IllegalStateException("This application provider does not contain an application, but should have one to get a DTO");
		}
		if (status == NO_FAILURE) {
			return DTOConverter.toApplicationDTO(this);
		} else {
			return DTOConverter.toFailedApplicationDTO(this, status);
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsApplicationProvider#isDefault()
	 */
	public boolean isDefault() {
		return getProviderObject() instanceof DefaultApplication;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return JerseyHelper.isEmpty(wrappedApplication);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider#isChanged()
	 */
	@Override
	public boolean isChanged() {
		return changed;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider#markUnchanged()
	 */
	@Override
	public void markUnchanged() {
		changed = false;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider#addResource(org.gecko.rest.jersey.provider.application.JaxRsResourceProvider)
	 */
	@Override
	public boolean addResource(JaxRsResourceProvider provider) {
		if (!provider.isResource()) {
			logger.log(Level.WARNING, "The resource to add is not declared with the resource property: " + JaxrsWhiteboardConstants.JAX_RS_RESOURCE);
			return false;
		}
		return doAddContent(provider);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider#removeResource(org.gecko.rest.jersey.provider.application.JaxRsResourceProvider)
	 */
	@Override
	public boolean removeResource(JaxRsResourceProvider provider) {
		if (provider == null) {
			logger.log(Level.WARNING, "The resource provider is null. There is nothing to remove.");
			return false;
		}
		if (!provider.isResource()) {
			logger.log(Level.WARNING, "The resource to be removed is not declared with the resource property: " + JaxrsWhiteboardConstants.JAX_RS_RESOURCE);
			return false;
		}
		return doRemoveContent(provider);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider#addExtension(org.gecko.rest.jersey.provider.application.JaxRsExtensionProvider)
	 */
	@Override
	public boolean addExtension(JaxRsExtensionProvider provider) {
		if (!provider.isExtension()) {
			logger.log(Level.WARNING, "The extension to add is not declared with the extension property: " + JaxrsWhiteboardConstants.JAX_RS_EXTENSION);
			return false;
		}
		return doAddContent(provider);
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider#removeExtension(org.gecko.rest.jersey.provider.application.JaxRsExtensionProvider)
	 */
	@Override
	public boolean removeExtension(JaxRsExtensionProvider provider) {
		if (provider == null) {
			logger.log(Level.WARNING, "The extension provider is null. There is nothing to remove.");
			return false;
		}
		if (!provider.isExtension()) {
			logger.log(Level.WARNING, "The extension to be removed is not declared with the extension property: " + JaxrsWhiteboardConstants.JAX_RS_EXTENSION);
			return false;
		}
		return doRemoveContent(provider);
	}
	
	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		return new JerseyApplicationProvider(getProviderObject(), getProviderProperties());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.AbstractJaxRsProvider#getProviderName()
	 */
	@Override
	protected String getProviderName() {
		String name = null;
		Map<String, Object> providerProperties = getProviderProperties();
		if (providerProperties != null) {
			String baseProperty = (String) providerProperties.get(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE);
			if (wrappedApplication != null) {
				baseProperty = getPath();
			}
			name = (String) providerProperties.get(JaxrsWhiteboardConstants.JAX_RS_NAME);
			if (name == null && baseProperty != null) {
				name = "." + baseProperty;
			} else if (name != null && !name.equals(".default") && (name.startsWith(".") || name.startsWith("osgi"))) {
				updateStatus(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
			}
		}
		return name == null ? getProviderId() : name;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.AbstractJaxRsProvider#doValidateProperties(java.util.Map)
	 */
	@Override
	protected void doValidateProperties(Map<String, Object> properties) {
		String baseProperty = (String) properties.get(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE);
		if (applicationBase == null && (baseProperty == null || baseProperty.isEmpty())) {
			updateStatus(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
			return;
		}
		if (baseProperty != null && !baseProperty.isEmpty()) {
			applicationBase = baseProperty;
		} 
	}

	/**
	 * Adds content to the underlying {@link JerseyApplication}, if valid
	 * @param provider the content provider to be added
	 * @return <code>true</code>, if add was successful, otherwise <code>false</code>
	 */
	private boolean doAddContent(JaxRsApplicationContentProvider provider) {
		if(getApplicationDTO() instanceof FailedApplicationDTO) {
			return false;
		}
		boolean filterValid = true; 
		if (filterValid) {
			JerseyApplication ja = wrappedApplication;
			boolean added = ja.addContent(provider);
			if (!changed && added) {
				changed = added;
			}
		}
		return filterValid;
	}
	
	/**
	 * Removed content from the underlying {@link JerseyApplication}, if valid
	 * @param provider the content provider to be removed
	 * @return <code>true</code>, if removal was successful, otherwise <code>false</code>
	 */
	private boolean doRemoveContent(JaxRsApplicationContentProvider provider) {
		boolean removed = wrappedApplication.removeContent(provider);
		if (!changed && removed) {
			changed = removed;
		}
		return removed;
	}
	
	/* (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider#getContentProviers()
	 */
	@Override
	public Collection<JaxRsApplicationContentProvider> getContentProviers() {
		return wrappedApplication.getContentProviders();
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider#updateApplicationBase(java.lang.String)
	 */
	public void updateApplicationBase(String applicationBase) {
		doValidateProperties(Collections.singletonMap(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, applicationBase));
	}

	/**
	 * Returns <code>true</code>, if the application filter property handling is valid.
	 * @param properties the resource/extension properties
	 * @return <code>true</code>, if the handling is valid
	 */
//	private boolean isApplicationFilterValid(Map<String, Object> properties) {
//		if (isDefault()) {
//			logger.log(Level.WARNING, "There is no application select filter valid for the default application");
//			return false;
//		}
//		String applicationFilter = (String) properties.get(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT);
//		if (applicationFilter != null) {
//			try {
//				Filter filter = FrameworkUtil.createFilter(applicationFilter);
//				boolean applicationMatch = filter.matches(getApplicationProperties());
//				if (!applicationMatch) {
//					logger.log(Level.WARNING, "The given application select filter does not math to this application for this resource/extension: " + getId());
//					return false;
//				}
//			} catch (InvalidSyntaxException e) {
//				logger.log(Level.WARNING, "The given application select filter is invalid: " + applicationFilter, e);
//				return false;
//			}
//		} else {
//			logger.log(Level.WARNING, "There is no application select filter");
//			return false;
//		}
//		return true;
//	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.runtime.servlet.DestroyListener#servletContainerDestroyed(org.glassfish.jersey.servlet.ServletContainer)
	 */
	@Override
	public void servletContainerDestroyed(ServletContainer container) {
		applicationContainers.remove(container);
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.AbstractJaxRsProvider#updateStatus(int)
	 */
	@Override
	public void updateStatus(int newStatus) {
		super.updateStatus(newStatus);
	}


}
