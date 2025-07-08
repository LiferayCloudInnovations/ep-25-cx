package com.liferay.portal.trebuchet;

import com.liferay.petra.string.StringUtil;

public class PortalTrebuchetUtil {

	public static String routingKey(String name) {
		name = StringUtil.replace(name, "/", ".");
		name = StringUtil.replace(name, "_", ".");
		name = StringUtil.replace(name, "-", ".");

		return name;
	}

}