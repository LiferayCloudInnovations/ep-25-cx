/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.trebuchet;

import com.liferay.petra.string.StringUtil;

/**
 * @author Raymond Augé
 */
public class PortalTrebuchetUtil {

	public static String routingKey(String name) {
		name = StringUtil.replace(name, "/", ".");
		name = StringUtil.replace(name, "_", ".");
		name = StringUtil.replace(name, "-", ".");

		return name;
	}

}