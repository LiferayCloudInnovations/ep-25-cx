package com.liferay.login.message.broker;

import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.vulcan.dto.converter.DTOConverterRegistry;

/**
 * @author Allen Ziegenfus
 */
public class UserUtil {

	public static JSONObject getPayloadJSONObject(
		DTOConverterRegistry dtoConverterRegistry, JSONFactory jsonFactory,
		String loginActionTriggerKey, User user) {

		return JSONUtil.put(
			"loginActionTriggerKey", loginActionTriggerKey
		).put(
			"loginIP", user.getLoginIP()
		).put(
			"userFirstName", user.getFirstName()
		).put(
			"userId", user.getUserId()
		).put(
			"userLastName", user.getLastName()
		);
	}

}