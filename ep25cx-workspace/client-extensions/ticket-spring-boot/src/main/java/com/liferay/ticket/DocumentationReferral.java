/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.ticket;

import com.liferay.portal.search.rest.client.dto.v1_0.Suggestion;
import com.liferay.portal.search.rest.client.dto.v1_0.SuggestionsContributorConfiguration;
import com.liferay.portal.search.rest.client.dto.v1_0.SuggestionsContributorResults;
import com.liferay.portal.search.rest.client.pagination.Page;
import com.liferay.portal.search.rest.client.resource.v1_0.SuggestionResource;

import java.time.Duration;

import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

import reactor.util.retry.Retry;

/**
 * @author Raymond Augé
 * @author Gregory Amerson
 * @author Allen Ziegenfus
 */
public class DocumentationReferral {

	public static final String SUGGESTION_HOST = "learn.liferay.com";

	public static final int SUGGESTION_PORT = 443;

	public static final String SUGGESTION_SCHEME = "https";

	public DocumentationReferral() {
		_initResourceBuilders();
	}

	public void addDocumentationReferralAndQueue(
		String lxcDXPServerProtocol, String lxcDXPMainDomain, String jwtToken,
		JSONObject jsonObject) {

		Objects.requireNonNull(jsonObject);

		JSONObject jsonTicketDTO = jsonObject.getJSONObject(
			"objectEntryDTOTicket");

		JSONObject jsonProperties = jsonTicketDTO.getJSONObject("properties");

		JSONObject jsonTicketStatus = jsonProperties.getJSONObject(
			"ticketStatus");

		String subject = jsonProperties.getString("subject");

		jsonTicketStatus.remove("name");
		jsonTicketStatus.put("key", "queued");
		jsonProperties.put("suggestions", _getSuggestionsJSON(subject));

		_log.info("JSON OUTPUT: \n\n" + jsonProperties.toString(4) + "\n");

		WebClient.Builder builder = WebClient.builder();

		WebClient webClient = builder.baseUrl(
			lxcDXPServerProtocol + "://" + lxcDXPMainDomain
		).defaultHeader(
			HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE
		).defaultHeader(
			HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE
		).build();

		webClient.patch(
		).uri(
			"/o/c/tickets/{ticketId}", jsonTicketDTO.getLong("id")
		).bodyValue(
			jsonProperties.toString()
		).header(
			HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken
		).exchangeToMono(
			clientResponse -> {
				HttpStatus httpStatus = clientResponse.statusCode();

				if (httpStatus.is2xxSuccessful()) {
					return clientResponse.bodyToMono(String.class);
				}
				else if (httpStatus.is4xxClientError()) {
					if (_log.isInfoEnabled()) {
						_log.info("Output: " + httpStatus.getReasonPhrase());
					}
				}

				Mono<WebClientResponseException> mono =
					clientResponse.createException();

				return mono.flatMap(Mono::error);
			}
		).retryWhen(
			Retry.backoff(
				3, Duration.ofSeconds(1)
			).doAfterRetry(
				retrySignal -> _log.info("Retrying request")
			)
		).doOnNext(
			output -> {
				if (_log.isInfoEnabled()) {
					_log.info("Output: " + output);
				}
			}
		).subscribe();
	}

	private String _getSuggestionsJSON(String subject) {

		WebClient.Builder builder = WebClient.builder();

		WebClient webClient = builder.baseUrl(
			 "http://learn-dot-liferay-com-api.localtest.me"
		).defaultHeader(
			HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE
		).defaultHeader(
			HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE
		).build();

		String responseBody = webClient.get(
		).uri(
			"/suggestions?search={search}", subject )
        .exchangeToMono(
			clientResponse -> {
				HttpStatus httpStatus = clientResponse.statusCode();

				if (httpStatus.is2xxSuccessful()) {
					return clientResponse.bodyToMono(String.class);
				}
				else if (httpStatus.is4xxClientError()) {
					if (_log.isInfoEnabled()) {
						_log.info("Output: " + httpStatus.getReasonPhrase());
					}
				}

				Mono<WebClientResponseException> mono =
					clientResponse.createException();

				return mono.flatMap(Mono::error);
			}
		).retryWhen(
			Retry.backoff(
				3, Duration.ofSeconds(1)
			).doAfterRetry(
				retrySignal -> _log.info("Retrying request")
			)
		).block();


        JSONObject rootObject = new JSONObject(responseBody);
		JSONArray suggestionsJSONArray = new JSONArray();

		JSONArray items = rootObject.optJSONArray("items");
		if (items == null) {
			_log.warn("No 'items' array found in response");
			return suggestionsJSONArray.toString();
		}

		for (int i = 0; i < items.length(); i++) {
			JSONObject contributorResults = items.getJSONObject(i);
			JSONArray suggestions = contributorResults.optJSONArray("suggestions");

			if (suggestions == null) {
				continue;
			}

			for (int j = 0; j < suggestions.length(); j++) {
				JSONObject suggestion = suggestions.getJSONObject(j);
				JSONObject attributes = suggestion.optJSONObject("attributes");

				String text = suggestion.optString("text", "No text");
				String assetURL = attributes != null ? attributes.optString("assetURL", "") : "";

				suggestionsJSONArray.put(
					new JSONObject()
						.put("text", text)
						.put("assetURL", "https://learn.liferay.com" + assetURL)
				);
			}
		}

        return suggestionsJSONArray.toString();
		
	}


	private void _initResourceBuilders() {
		SuggestionResource.Builder dataDefinitionResourceBuilder =
			SuggestionResource.builder();

		_suggestionResource = dataDefinitionResourceBuilder.header(
			HttpHeaders.USER_AGENT, TicketRestController.class.getName()
		).header(
			HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE
		).header(
			HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE
		).endpoint(
			SUGGESTION_HOST, SUGGESTION_PORT, SUGGESTION_SCHEME
		).build();
	}

	private static final Log _log = LogFactory.getLog(
		DocumentationReferral.class);

	private SuggestionResource _suggestionResource;

}