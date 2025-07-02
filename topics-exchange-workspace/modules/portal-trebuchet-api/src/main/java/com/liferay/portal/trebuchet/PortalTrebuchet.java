package com.liferay.portal.trebuchet;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONObject;

import org.osgi.annotation.versioning.ProviderType;

/**
 * @author Gregory Amerson
 */
@ProviderType
public interface PortalTrebuchet {

	public void fire(
			long companyId, String exchangeKey, JSONObject payloadJSONObject,
			String routingKey, long userId)
		throws PortalException;

}