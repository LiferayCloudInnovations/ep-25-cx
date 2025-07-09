package com.liferay.sample;

import org.json.JSONObject;

/**
 * @author Marcel Tanuri
 */
public record TicketRequestDTO(Long ticketId, String subject) {

    public static TicketRequestDTO fromMessage(String message) {
        JSONObject messageObject = new JSONObject(message);
        JSONObject objectEntryObject = messageObject.getJSONObject("objectEntry");

        Long ticketId = objectEntryObject.getLong("id");

        JSONObject valuesObject = objectEntryObject.getJSONObject("values");
        String subject = valuesObject.getString("subject");

        return new TicketRequestDTO(ticketId, subject);
    }
}