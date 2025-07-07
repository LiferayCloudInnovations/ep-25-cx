/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.trebuchet.internal;

import com.liferay.petra.executor.PortalExecutorManager;
import com.liferay.petra.function.UnsafeFunction;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.trebuchet.PortalTrebuchet;
import com.liferay.portal.trebuchet.configuration.MessageBrokerConfiguration;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
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

		Company company = _companyLocalService.fetchCompany(companyId);

		if (company == null) {
			return;
		}

		String virtualHostId = company.getWebId();

		Connection connection = _connections.computeIfAbsent(
			virtualHostId, theVirtualHostId -> {
				ConnectionFactory connectionFactory = new ConnectionFactory();

				connectionFactory.setAutomaticRecoveryEnabled(
					_messageBrokerConfiguration.automaticRecoveryEnabled());
				connectionFactory.setHost(_messageBrokerConfiguration.host());
				connectionFactory.setPort(_messageBrokerConfiguration.port());
				connectionFactory.setUsername(
					_messageBrokerConfiguration.userName());
				connectionFactory.setPassword(
					_messageBrokerConfiguration.password());
				connectionFactory.setVirtualHost(theVirtualHostId);

				try {
					return connectionFactory.newConnection();
				}
				catch (IOException ioException) {
					if (_log.isErrorEnabled()) {
						_log.error(ioException);
					}
				}
				catch (TimeoutException timeoutException) {
					if (_log.isErrorEnabled()) {
						_log.error(timeoutException);
					}
				}

				return null;
			});

		executorService.submit(
			() -> {
				try (Channel channel = connection.createChannel()) {
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
					_log.error(exception);
				}
			});
	}

	@Activate
	protected void activate(Map<String, Object> properties) throws Exception {
		_messageBrokerConfiguration = ConfigurableUtil.createConfigurable(
			MessageBrokerConfiguration.class, properties);

		if (_log.isDebugEnabled()) {
			_log.debug("Activated");
		}
	}

	@Deactivate
	protected void deactivate() {
		if (_log.isDebugEnabled()) {
			_log.debug("Deactivated");
		}

		_connections.forEach((k, v) -> {
			try {
				v.close();
			}
			catch (IOException ioException) {
				if (_log.isErrorEnabled()) {
					_log.error(ioException);
				}
			}
		});

		_connections.clear();
	}

	private static final Log _log = LogFactoryUtil.getLog(
		RabbitMQPortalTrebuchet.class);

	@Reference
	private CompanyLocalService _companyLocalService;

	private Map<String, Connection> _connections = new ConcurrentHashMap<>();

	private MessageBrokerConfiguration _messageBrokerConfiguration;

	@Reference
	private PortalExecutorManager _portalExecutorManager;

}