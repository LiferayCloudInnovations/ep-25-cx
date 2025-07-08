package com.liferay.login.message.broker;

import com.liferay.portal.kernel.events.Action;
import com.liferay.portal.kernel.events.LifecycleAction;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.module.service.Snapshot;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.trebuchet.PortalTrebuchet;
import com.liferay.portal.vulcan.dto.converter.DTOConverterRegistry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Allen Ziegenfus
 */
@Component(
	property = "key=" + PropsKeys.LOGIN_EVENTS_POST,
	service = LifecycleAction.class
)
public class LoginPostAction extends Action {

	@Override
	public void run(
		HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse) {

		try {
			User user = _portal.getUser(httpServletRequest);

			_log.error(
				"Login Action for user: " + String.valueOf(user.getUserId()));

			_enqueuePayload(user);
		}
		catch (Exception exception) {
			if (_log.isDebugEnabled()) {
				_log.debug(exception);
			}
		}
	}

	private void _enqueuePayload(User user) throws PortalException {
		String event = PropsKeys.LOGIN_EVENTS_POST;
		PortalTrebuchet portalTrebuchet = _portalTrebuchet.get();

		if (portalTrebuchet == null) {
			_log.debug(event + ": message broker is not available");

			return;
		}

		JSONObject payloadJSONObject = UserUtil.getPayloadJSONObject(
			_dtoConverterRegistry, _jsonFactory, event, user);

		if (_log.isDebugEnabled()) {
			_log.debug(
				"enqueuing jsonPayload: " + String.valueOf(payloadJSONObject));
		}

		portalTrebuchet.fire(
			user.getCompanyId(), payloadJSONObject,
			PropsKeys.LOGIN_EVENTS_POST + "Event", user.getUserId());
	}

	private static final Log _log = LogFactoryUtil.getLog(
		LoginPostAction.class);

	private static final Snapshot<PortalTrebuchet> _portalTrebuchet =
		new Snapshot<>(LoginPostAction.class, PortalTrebuchet.class);

	@Reference
	private DTOConverterRegistry _dtoConverterRegistry;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private Portal _portal;

}