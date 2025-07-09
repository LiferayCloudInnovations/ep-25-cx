package com.liferay.sample;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import org.json.JSONArray;
import org.json.JSONObject;


/**
 * @author Marcel Tanuri
 */
@Component
public class RabbitMqMessageListener {

	@RabbitListener(queues = "C_TicketEvent.default")
	public void receive(String message) {
		System.out.println("Received message: " + message);


        JSONObject messageObject = new JSONObject(message);
        JSONObject objectEntryObject = messageObject.getJSONObject("objectEntry");
        JSONObject valuesObject = objectEntryObject.getJSONObject("values");
        String subject = valuesObject.getString("subject");

        System.out.println("Received subject: " + subject);
	}

    	private DocumentationReferral _documentationReferral =
		new DocumentationReferral();



	@Value("${com.liferay.lxc.dxp.mainDomain}")
	private String _lxcDXPMainDomain;

	@Value("${com.liferay.lxc.dxp.server.protocol}")
	private String _lxcDXPServerProtocol;

}