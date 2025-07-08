package com.liferay.portal.trebuchet.configuration;

import aQute.bnd.annotation.metatype.Meta;

import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;
import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition.Scope;

/**
 * @author Raymond Augé
 */
@ExtendedObjectClassDefinition(generateUI = false, scope = Scope.COMPANY)
@Meta.OCD(
	factory = true,
	id = "com.liferay.portal.trebuchet.configuration.MessageQueueConfiguration"
)
public interface MessageQueueConfiguration {

	@Meta.AD(required = false)
	public String[] arguments();

	@Meta.AD(deflt = "false", required = false)
	public boolean autoDelete();

	public long companyId();

	@Meta.AD(deflt = "true", required = false)
	public boolean durable();

	@Meta.AD(deflt = "amq.topic", required = false)
	public String exchange();

	@Meta.AD(deflt = "false", required = false)
	public boolean exclusive();

	public String name();

}