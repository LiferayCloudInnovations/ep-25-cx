/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.trebuchet.internal;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.trebuchet.PortalTrebuchetUtil;
import com.liferay.portal.trebuchet.configuration.MessageBrokerConfiguration;
import com.liferay.portal.trebuchet.configuration.MessageQueueConfiguration;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Raymond Augé
 */
@Component(
	configurationPid = "com.liferay.portal.trebuchet.configuration.MessageBrokerConfiguration",
	configurationPolicy = ConfigurationPolicy.REQUIRE,
	service = RabbitMQQueueConfigurationManager.class
)
public class RabbitMQQueueConfigurationManager {

	public Connection getConnection(long companyId) {
		Company company = _companyLocalService.fetchCompany(companyId);

		if (company == null) {
			return null;
		}

		String virtualHostId = company.getWebId();

		return _connections.computeIfAbsent(
			virtualHostId,
			theVirtualHostId -> {
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
	}

	public MessageQueueConfiguration getMessageQueueConfiguration(
			long companyId, String name)
		throws IOException {

		Map<String, MessageQueueConfiguration> messageQueueConfigurations =
			_messageQueueConfigurations.computeIfAbsent(
				companyId, theCompanyId -> new ConcurrentHashMap<>());

		MessageQueueConfiguration messageQueueConfiguration =
			messageQueueConfigurations.get(name);

		if (messageQueueConfiguration == null) {
			messageQueueConfiguration = new DefaultMessageQueueConfiguration(
				ConfigurableUtil.createConfigurable(
					MessageQueueConfiguration.class,
					HashMapBuilder.<String, Object>put(
						"companyId", companyId
					).put(
						"name", name
					).build()));

			setMessageQueueConfiguration(name, messageQueueConfiguration);
		}

		return messageQueueConfiguration;
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

		_connections.forEach(
			(k, v) -> {
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

	protected void setMessageQueueConfiguration(
			String name, MessageQueueConfiguration messageQueueConfiguration)
		throws IOException {

		Connection connection = getConnection(
			messageQueueConfiguration.companyId());

		if (connection == null) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					StringBundler.concat(
						"Could not establish a connection to the message broker ",
						"for companyId ",
						messageQueueConfiguration.companyId()));
			}

			return;
		}

		try (Channel channel = connection.createChannel()) {
			_configureQueue(channel, messageQueueConfiguration);
		}
		catch (TimeoutException timeoutException) {
			if (_log.isErrorEnabled()) {
				_log.error(timeoutException);
			}

			return;
		}

		Map<String, MessageQueueConfiguration> messageQueueConfigurations =
			_getMessageQueueConfigurations(
				messageQueueConfiguration.companyId());

		messageQueueConfigurations.put(name, messageQueueConfiguration);
	}

	protected void unsetMessageQueueConfiguration(
		MessageQueueConfiguration messageQueueConfiguration) {

		Map<String, MessageQueueConfiguration> messageQueueConfigurations =
			_getMessageQueueConfigurations(
				messageQueueConfiguration.companyId());

		messageQueueConfigurations.remove(messageQueueConfiguration.name());

		if (messageQueueConfigurations.isEmpty()) {
			_messageQueueConfigurations.remove(
				messageQueueConfiguration.companyId());
		}
	}

	private Object _coerce(String value) {
		if (Validator.isNumber(value)) {
			return GetterUtil.getInteger(value);
		}

		return value;
	}

	private void _configureQueue(
			Channel channel,
			MessageQueueConfiguration messageQueueConfiguration)
		throws IOException {

		Map<String, Object> arguments = new HashMap<>();

		if (messageQueueConfiguration.arguments() != null) {
			for (String argument : messageQueueConfiguration.arguments()) {
				if (StringUtil.contains(argument, "=")) {
					String[] parts = argument.split("=", 2);

					arguments.put(parts[0], _coerce(parts[1]));
				}
				else if (_log.isDebugEnabled()) {
					_log.debug(
						StringBundler.concat(
							"Failed to parse argument ", argument, " on ",
							messageQueueConfiguration.name(),
							" because it does not contain an equals sign"));
				}
			}
		}

		channel.queueDeclare(
			messageQueueConfiguration.name(),
			messageQueueConfiguration.durable(),
			messageQueueConfiguration.exclusive(),
			messageQueueConfiguration.autoDelete(), arguments);

		channel.queueBind(
			messageQueueConfiguration.name(),
			messageQueueConfiguration.exchange(),
			PortalTrebuchetUtil.routingKey(messageQueueConfiguration.name()));
	}

	private Map<String, MessageQueueConfiguration>
		_getMessageQueueConfigurations(long companyId) {

		return _messageQueueConfigurations.computeIfAbsent(
			companyId, theCompanyId -> new ConcurrentHashMap<>());
	}

	private static final Log _log = LogFactoryUtil.getLog(
		RabbitMQQueueConfigurationManager.class);

	@Reference
	private CompanyLocalService _companyLocalService;

	private Map<String, Connection> _connections = new ConcurrentHashMap<>();
	private MessageBrokerConfiguration _messageBrokerConfiguration;
	private final Map<Long, Map<String, MessageQueueConfiguration>>
		_messageQueueConfigurations = new ConcurrentHashMap<>();

	static class DefaultMessageQueueConfiguration
		implements MessageQueueConfiguration {

		DefaultMessageQueueConfiguration(
			MessageQueueConfiguration messageQueueConfiguration) {

			_messageQueueConfiguration = messageQueueConfiguration;
		}

		@Override
		public String[] arguments() {
			return _messageQueueConfiguration.arguments();
		}

		@Override
		public boolean autoDelete() {
			return _messageQueueConfiguration.autoDelete();
		}

		@Override
		public long companyId() {
			return _messageQueueConfiguration.companyId();
		}

		@Override
		public boolean durable() {
			return _messageQueueConfiguration.durable();
		}

		@Override
		public String exchange() {
			return _messageQueueConfiguration.exchange();
		}

		@Override
		public boolean exclusive() {
			return _messageQueueConfiguration.exclusive();
		}

		@Override
		public String name() {
			return _messageQueueConfiguration.name() + ".default";
		}

		private MessageQueueConfiguration _messageQueueConfiguration;

	}

}