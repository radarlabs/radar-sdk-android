package io.radar.ci

import org.json.JSONObject

/**
 * GitHub Rest API.
 */
class GitHubClient {

    private static final String OWNER = 'radarlabs'
    private static final String REPO = 'radar-sdk-android'

    static void repositoryDispatch(String eventType,
                                   JSONObject clientPayload = null,
                                   String username = null,
                                   String password = null,
                                   ApiCallback callback) {
        JSONObject body = new JSONObject()
        body.put('event_type', eventType)
        if (clientPayload != null) {
            body.put('client_payload', clientPayload)
        }
        request("https://api.github.com/repos/$OWNER/$REPO/dispatches", 'POST', body, username, password, callback)
    }

    private static void request(String url, String method, JSONObject body, String username, String password, ApiCallback callback) {
        HttpURLConnection connection = new URL(url).openConnection()
        connection.setRequestProperty('accept', 'application/vnd.github.v3+json')
        if (username != null && password != null) {
            connection.setRequestProperty('Authorization', "Basic ${Base64.encoder.encode("$username:$password".bytes)}")
        }
        connection.requestMethod = method
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        if (body != null) {
            connection.doOutput = true

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.outputStream)
            outputStreamWriter.write(body.toString())
            outputStreamWriter.close()
        }
        String data
        boolean isSuccessful
        if ((200..400).contains(connection.responseCode)) {
            data = new String(connection.inputStream.readAllBytes())
            isSuccessful = true
        } else {
            data = new String(connection.errorStream.readAllBytes())
            isSuccessful = false
        }
        callback.onResult(new ApiResponse() {

            @Override
            int getStatus() {
                connection.responseCode
            }

            @Override
            String getMessage() {
                connection.responseMessage
            }

            @Override
            JSONObject getData() {
                data ? new JSONObject(data) : null
            }

            @Override
            boolean isSuccess() {
                isSuccessful
            }

        })

        connection.disconnect()
    }

    interface ApiCallback {

        void onResult(ApiResponse response)

    }

    interface ApiResponse {

        int getStatus()

        String getMessage()

        JSONObject getData()

        boolean isSuccess()

    }

}
