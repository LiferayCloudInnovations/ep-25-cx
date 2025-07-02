package com.liferay.portal.trebuchet.configuration;

import aQute.bnd.annotation.metatype.Meta;
import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;

@ExtendedObjectClassDefinition(generateUI = false)
@Meta.OCD(
        factory = true,
        id = "com.liferay.portal.trebuchet.configuration.MessageBrokerConfiguration"
)
public interface MessageBrokerConfiguration {
    @Meta.AD(deflt = "localhost")
    String host();

    @Meta.AD(deflt = "5672")
    int port();

    @Meta.AD(deflt = "false")
    boolean automaticRecoveryEnabled();
}
