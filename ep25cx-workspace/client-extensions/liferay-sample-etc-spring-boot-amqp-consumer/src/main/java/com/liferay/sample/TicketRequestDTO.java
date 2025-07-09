package com.liferay.sample;

import org.json.JSONObject;

/**
 * @author Marcel Tanuri
 */
public record TicketRequestDTO(Long ticketId, String subject, String ticketStatus) {

    public static TicketRequestDTO fromMessage(String message) {
        JSONObject messageObject = new JSONObject(message);
        JSONObject objectEntryObject = messageObject.getJSONObject("objectEntry");

        Long ticketId = objectEntryObject.getLong("id");

        JSONObject valuesObject = objectEntryObject.getJSONObject("values");
        String subject = valuesObject.getString("subject");

        String ticketStatus = valuesObject.getString("ticketStatus");

        return new TicketRequestDTO(ticketId, subject, ticketStatus);
    }
}