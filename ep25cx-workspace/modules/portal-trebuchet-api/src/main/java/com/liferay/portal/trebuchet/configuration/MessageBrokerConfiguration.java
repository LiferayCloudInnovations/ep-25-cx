package com.liferay.portal.trebuchet.configuration;

import aQute.bnd.annotation.metatype.Meta;

import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;

/**
 * @author Gregory Amerson
 */
@ExtendedObjectClassDefinition(generateUI = false)
@Meta.OCD(
	factory = true,
	id = "com.liferay.portal.trebuchet.configuration.MessageBrokerConfiguration"
)
public interface MessageBrokerConfiguration {

	@Meta.AD(deflt = "localhost", required = false)
	public String host();

	@Meta.AD(deflt = "5672", required = false)
	public int port();

	@Meta.AD(deflt = "false", required = false)
	public boolean automaticRecoveryEnabled();

	public String userName();

	public String password();

}