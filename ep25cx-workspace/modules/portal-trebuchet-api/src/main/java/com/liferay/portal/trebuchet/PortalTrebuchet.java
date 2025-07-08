/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

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
			long companyId, JSONObject payloadJSONObject, String queue,
			long userId)
		throws PortalException;

}