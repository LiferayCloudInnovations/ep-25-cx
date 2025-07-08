/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.trebuchet.internal;

import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.trebuchet.configuration.MessageQueueConfiguration;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Raymond Augé
 */
@Component(
	configurationPid = "com.liferay.portal.trebuchet.configuration.MessageQueueConfiguration",
	configurationPolicy = ConfigurationPolicy.REQUIRE, service = {}
)
public class RabbitMQQueueFactory {

	@Activate
	protected void activate(Map<String, Object> properties) throws Exception {
		_messageQueueConfiguration = ConfigurableUtil.createConfigurable(
			MessageQueueConfiguration.class, properties);

		_rabbitMQQueueConfigurationManager.setMessageQueueConfiguration(
			_messageQueueConfiguration.name(), _messageQueueConfiguration);

		if (_log.isDebugEnabled()) {
			_log.debug("Activated " + properties);
		}
	}

	@Deactivate
	protected void deactivate() {
		_rabbitMQQueueConfigurationManager.unsetMessageQueueConfiguration(
			_messageQueueConfiguration);

		if (_log.isDebugEnabled()) {
			_log.debug("Deactivated " + _messageQueueConfiguration.name());
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		RabbitMQQueueFactory.class);

	@Reference
	private RabbitMQQueueConfigurationManager
		_rabbitMQQueueConfigurationManager;

	private MessageQueueConfiguration _messageQueueConfiguration;

}