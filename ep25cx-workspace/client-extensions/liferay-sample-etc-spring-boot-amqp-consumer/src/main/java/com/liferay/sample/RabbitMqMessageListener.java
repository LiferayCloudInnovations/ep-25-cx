package com.liferay.sample;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.liferay.client.extension.util.spring.boot3.client.LiferayOAuth2AccessTokenManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.JSONArray;
import org.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.time.Duration;
import reactor.util.retry.Retry;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;

import org.springframework.http.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONObject;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.Arrays;
import java.nio.charset.Charset;

import org.springframework.beans.factory.annotation.Autowired;


/**
 * @author Marcel Tanuri
 */
@Component
public class RabbitMqMessageListener {

	@RabbitListener(queues = "C_TicketEvent")
	public void receive(String message) throws Exception {
		_log.info("Received message: " + message);

        TicketRequestDTO ticketRequest = TicketRequestDTO.fromMessage(message);

        _log.info("Received subject: " + ticketRequest.subject());
        _log.info("Received ticketId: " + ticketRequest.ticketId());

        String ticketStatus = ticketRequest.ticketStatus();
        if (ticketStatus.equalsIgnoreCase("answered")) {
            return;
        }

		TicketResponseDTO ticketResponse = new TicketResponseDTO(_getSuggestionsJSON(ticketRequest.subject()), "answered");

		WebClient.Builder builder = WebClient.builder();

		WebClient webClient = builder.baseUrl(
			_lxcDXPServerProtocol + "://" + _lxcDXPMainDomain
		).defaultHeader(
			HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE
		).defaultHeader(
			HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE
		).build();

		webClient.patch(
		).uri(
			"/o/c/tickets/{ticketId}", ticketRequest.ticketId()
		).bodyValue(
			ticketResponse.toJsonObject().toString()
		).header(
			HttpHeaders.AUTHORIZATION,  _getAuthorization()
		).exchangeToMono(
			clientResponse -> {
				HttpStatusCode httpStatusCode = clientResponse.statusCode();

				if (httpStatusCode.is2xxSuccessful()) {
					return clientResponse.bodyToMono(String.class);
				}
				else if (httpStatusCode.is4xxClientError()) {
					if (_log.isInfoEnabled()) {
						_log.info("Output: " + httpStatusCode.value());
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
				HttpStatusCode httpStatusCode = clientResponse.statusCode();

				if (httpStatusCode.is2xxSuccessful()) {
					return clientResponse.bodyToMono(String.class);
				}
				else if (httpStatusCode.is4xxClientError()) {
					if (_log.isInfoEnabled()) {
						_log.info("Output: " + httpStatusCode.value());
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

	private String _getAuthorization() throws Exception{
        return _getOAuthAuthorization();
		//return _liferayOAuth2AccessTokenManager.getAuthorization(
			//"liferay-sample-etc-spring-boot-ampq");
	}


    private String _getOAuthAuthorization() throws Exception {
		HttpPost httpPost = new HttpPost(_lxcDXPServerProtocol + "://" + _lxcDXPMainDomain + "/o/oauth2/token");




    Path idPath = Paths.get("/etc/liferay/lxc/ext-init-metadata/liferay-sample-etc-spring-boot-ampq-consumer-oauth-application-headless-server.oauth2.headless.server.client.id");
   
    Path secretPath = Paths.get("/etc/liferay/lxc/ext-init-metadata/liferay-sample-etc-spring-boot-ampq-consumer-oauth-application-headless-server.oauth2.headless.server.client.secret");

    String _liferayOAuthClientId = Files.readAllLines(idPath).get(0);
    String _liferayOAuthClientSecret = Files.readAllLines(secretPath).get(0);





		httpPost.setEntity(
			new UrlEncodedFormEntity(
				Arrays.asList(
					new BasicNameValuePair("client_id", _liferayOAuthClientId),
					new BasicNameValuePair(
						"client_secret", _liferayOAuthClientSecret),
					new BasicNameValuePair(
						"grant_type", "client_credentials"))));
		httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

		HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

		try (CloseableHttpClient closeableHttpClient =
				httpClientBuilder.build();
			CloseableHttpResponse closeableHttpResponse =
				closeableHttpClient.execute(httpPost)) {

			StatusLine statusLine = closeableHttpResponse.getStatusLine();

			if (statusLine.getStatusCode() == org.apache.http.HttpStatus.SC_OK) {
				JSONObject jsonObject = new JSONObject(
					EntityUtils.toString(
						closeableHttpResponse.getEntity(),
						Charset.defaultCharset()));

				long _oauthExpirationMillis =
					jsonObject.getLong("expires_in") * 1000;

				return jsonObject.getString("token_type") + " " +
					jsonObject.getString("access_token");
			}

			throw new Exception("Unable to get OAuth authorization");
		}
	}

	@Value("${com.liferay.lxc.dxp.mainDomain}")
	private String _lxcDXPMainDomain;

	@Value("${com.liferay.lxc.dxp.server.protocol}")
	private String _lxcDXPServerProtocol;

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

	private static final Log _log = LogFactory.getLog(
		RabbitMqMessageListener.class);

}