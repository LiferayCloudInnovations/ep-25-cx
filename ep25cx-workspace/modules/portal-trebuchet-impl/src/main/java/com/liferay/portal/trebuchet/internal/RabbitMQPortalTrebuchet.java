/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.trebuchet.internal;

import com.liferay.petra.executor.PortalExecutorManager;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.trebuchet.PortalTrebuchet;
import com.liferay.portal.trebuchet.configuration.MessageBrokerConfiguration;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Gregory Amerson
 */
@Component(
	configurationPid = "com.liferay.portal.trebuchet.configuration.MessageBrokerConfiguration",
	configurationPolicy = ConfigurationPolicy.REQUIRE,
	service = PortalTrebuchet.class
)
public class RabbitMQPortalTrebuchet implements PortalTrebuchet {

	@Override
	public void fire(
			long companyId, JSONObject payloadJSONObject, String queue,
			long userId)
		throws PortalException {

		ExecutorService executorService =
			_portalExecutorManager.getPortalExecutor(
				RabbitMQPortalTrebuchet.class.getName());

		executorService.submit(
			() -> {
				ConnectionFactory connectionFactory = new ConnectionFactory();

				connectionFactory.setAutomaticRecoveryEnabled(
					_messageBrokerConfiguration.automaticRecoveryEnabled());
				connectionFactory.setHost(_messageBrokerConfiguration.host());
				connectionFactory.setPort(_messageBrokerConfiguration.port());
				connectionFactory.setUsername(
					_messageBrokerConfiguration.userName());
				connectionFactory.setPassword(
					_messageBrokerConfiguration.password());

				try (Connection connection = connectionFactory.newConnection();
					Channel channel = connection.createChannel()) {

					channel.queueDeclare(queue, true, false, false, null);

					String payload = payloadJSONObject.toString();

					channel.basicPublish(
						DEFAULT_EXCHANGE, queue, null,
						payload.getBytes(StandardCharsets.UTF_8));

					if (_log.isDebugEnabled()) {
						_log.debug("Sent jsonBody => " + queue);
					}
				}
				catch (Exception exception) {
					_log.error(exception.getMessage(), exception);
				}
			});
	}

	@Activate
	protected void activate(Map<String, Object> properties) throws Exception {
		_messageBrokerConfiguration = ConfigurableUtil.createConfigurable(
			MessageBrokerConfiguration.class, properties);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		RabbitMQPortalTrebuchet.class);

	private MessageBrokerConfiguration _messageBrokerConfiguration;

	@Reference
	private PortalExecutorManager _portalExecutorManager;

}