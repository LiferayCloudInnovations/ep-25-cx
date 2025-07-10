package com.liferay.sample;

import org.json.JSONObject;

/**
 * @author Marcel Tanuri
 */
public record TicketResponseDTO(String suggestions, String status) {

	public JSONObject toJsonObject() {
		JSONObject json = new JSONObject();

		json.put("suggestions", suggestions);

		JSONObject ticketStatusJson = new JSONObject();

		ticketStatusJson.put("key", status);

		json.put("ticketStatus", ticketStatusJson);

		return json;
	}

}
