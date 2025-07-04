package com.liferay.portal.trebuchet.internal;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.messaging.MessageListener;
import com.liferay.portal.kernel.messaging.MessageListenerException;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.trebuchet.PortalTrebuchet;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	immediate = true,
	property = {
		"destination.name=destination.workflow_definition_link",
		"destination.name=destination.workflow_timer",
		"destination.name=liferay/adaptive_media_processor",
		"destination.name=liferay/antivirus_batch",
		"destination.name=liferay/asset_auto_tagger",
		"destination.name=liferay/async_service",
		"destination.name=liferay/background_task_status",
		"destination.name=liferay/background_task",
		"destination.name=liferay/commerce_base_price_list",
		"destination.name=liferay/commerce_order_status",
		"destination.name=liferay/commerce_payment_status",
		"destination.name=liferay/commerce_shipment_status",
		"destination.name=liferay/commerce_subscription_status",
		"destination.name=liferay/convert_process",
		"destination.name=liferay/ct_collection_scheduled_publish",
		"destination.name=liferay/ct_score",
		"destination.name=liferay/ddm_structure_reindex",
		"destination.name=liferay/dispatch/executor",
		"destination.name=liferay/document_library_audio_processor",
		"destination.name=liferay/document_library_deletion",
		"destination.name=liferay/document_library_image_processor",
		"destination.name=liferay/document_library_pdf_processor",
		"destination.name=liferay/document_library_raw_metadata_processor",
		"destination.name=liferay/document_library_video_processor",
		"destination.name=liferay/export_import_lifecycle_event_async",
		"destination.name=liferay/export_import_lifecycle_event_sync",
		"destination.name=liferay/flags",
		"destination.name=liferay/layouts_local_publisher",
		"destination.name=liferay/layouts_remote_publisher",
		"destination.name=liferay/live_users",
		"destination.name=liferay/mail",
		"destination.name=liferay/message_boards_mailing_list",
		"destination.name=liferay/object_entry_attachment_download",
		"destination.name=liferay/scheduled_user_ldap_import",
		"destination.name=liferay/scheduler_dispatch",
		"destination.name=liferay/scheduler_scripting",
		"destination.name=liferay/scripting_executor",
		"destination.name=liferay/segments_entry_reindex",
		"destination.name=liferay/subscription_sender",
		"destination.name=liferay/tensorflow_model_download",
	},
	service = MessageListener.class
)
public class RabbitMQMessageListener implements MessageListener {

	@Override
	public void receive(Message message) throws MessageListenerException {
		long companyId = GetterUtil.getLong(
			message.get("companyId"), CompanyThreadLocal.getCompanyId());

		long userId = _getUserId(message);

		String destinationName = message.getDestinationName();

		Object payload = message.getPayload();

		if (!(payload instanceof JSONObject)) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					StringBundler.concat(
						"Message payload for ", destinationName,
						" is not a JSONObject: ", payload));
			}

			return;
		}

		try {
			_portalTrebuchet.fire(
				companyId, (JSONObject)payload, destinationName, userId);
		}
		catch (PortalException portalException) {
			throw new MessageListenerException(portalException);
		}
	}

	@Activate
	protected void activate() {
		if (_log.isDebugEnabled()) {
			_log.debug("Activated");
		}
	}

	private long _getUserId(Message message) {
		long userId = GetterUtil.getLong(message.get("principalName"));

		if (userId != 0L) {
			return userId;
		}

		Object object = message.get("permissionChecker");

		if (!(object instanceof PermissionChecker)) {
			return 0L;
		}

		PermissionChecker permissionChecker = (PermissionChecker)object;

		return permissionChecker.getUserId();
	}

	private static final Log _log = LogFactoryUtil.getLog(
		RabbitMQMessageListener.class);

	@Reference
	private PortalTrebuchet _portalTrebuchet;

}