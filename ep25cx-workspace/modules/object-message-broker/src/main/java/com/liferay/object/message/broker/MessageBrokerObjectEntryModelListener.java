package com.liferay.object.message.broker;

import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.portal.kernel.exception.ModelListenerException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.ModelListener;
import com.liferay.portal.kernel.module.service.Snapshot;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.trebuchet.PortalTrebuchet;
import com.liferay.portal.vulcan.dto.converter.DTOConverterRegistry;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Gregory Amerson
 */
@Component(immediate = true, service = ModelListener.class)
public class MessageBrokerObjectEntryModelListener
	extends BaseModelListener<ObjectEntry> {

	@Override
	public void onAfterAddAssociation(
			Object classPK, String associationClassName,
			Object associationClassPK)
		throws ModelListenerException {

		_log.error("onAfterAddAssociation NOT IMPLEMENTED!");
	}

	@Override
	public void onAfterCreate(ObjectEntry objectEntry)
		throws ModelListenerException {

		_enqueuePayload("onAfterCreate", null, objectEntry);
	}

	@Override
	public void onAfterRemove(ObjectEntry objectEntry)
		throws ModelListenerException {

		_enqueuePayload("onAfterRemove", null, objectEntry);
	}

	@Override
	public void onAfterRemoveAssociation(
			Object classPK, String associationClassName,
			Object associationClassPK)
		throws ModelListenerException {

		_log.error("onAfterRemoveAssociation NOT IMPLEMENTED!");
	}

	@Override
	public void onAfterUpdate(
			ObjectEntry originalObjectEntry, ObjectEntry objectEntry)
		throws ModelListenerException {

		_enqueuePayload("onAfterUpdate", originalObjectEntry, objectEntry);
	}

	@Activate
	protected void activate() {
		if (_log.isDebugEnabled()) {
			_log.debug("Activated");
		}
	}

	private void _enqueuePayload(
			String event, ObjectEntry originalObjectEntry,
			ObjectEntry objectEntry)
		throws ModelListenerException {

		PortalTrebuchet portalTrebuchet = _portalTrebuchet.get();

		if (portalTrebuchet == null) {
			_log.debug(event + ": message broker is not available");

			return;
		}

		long userId = PrincipalThreadLocal.getUserId();

		if (userId == 0) {
			if (objectEntry != null) {
				userId = objectEntry.getUserId();
			}
			else if (originalObjectEntry != null) {
				userId = originalObjectEntry.getUserId();
			}
		}

		try {
			ObjectDefinition objectDefinition =
				_objectDefinitionLocalService.getObjectDefinition(
					objectEntry.getObjectDefinitionId());

			JSONObject payloadJSONObject = ObjectEntryUtil.getPayloadJSONObject(
				_dtoConverterRegistry, _jsonFactory, event, objectDefinition,
				objectEntry, originalObjectEntry, null,
				_userLocalService.getUser(userId));

			if (_log.isDebugEnabled()) {
				_log.debug(
					"enqueuing jsonPayload: " +
						String.valueOf(payloadJSONObject));
			}

			portalTrebuchet.fire(
				objectEntry.getCompanyId(), payloadJSONObject,
				objectDefinition.getName() + "Event", userId);
		}
		catch (PortalException portalException) {
			throw new ModelListenerException(portalException);
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		MessageBrokerObjectEntryModelListener.class);

	private static final Snapshot<PortalTrebuchet> _portalTrebuchet =
		new Snapshot<>(
			MessageBrokerObjectEntryModelListener.class, PortalTrebuchet.class);

	@Reference
	private DTOConverterRegistry _dtoConverterRegistry;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Reference
	private UserLocalService _userLocalService;

}