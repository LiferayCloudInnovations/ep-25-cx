package com.liferay.portal.trebuchet;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * @author Gregory Amerson
 */
@ProviderType
public interface PortalTrebuchet {

	public static final String DEFAULT_EXCHANGE = "";

	public void fire(
			long companyId, JSONObject payloadJSONObject, String queue,
			long userId)
		throws PortalException;

}