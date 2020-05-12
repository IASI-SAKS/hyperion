package it.cnr.saks.sisma.testing.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

public class SimpleRestClient {
    private final RestTemplate restTemplate;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public SimpleRestClient() {
        restTemplate = new RestTemplate();
    }

    private boolean dummyIpCheck(String ipString) {
        int[] ip = new int[4];
        long ipNumbers = 0;

        String[] parts = ipString.split("\\.");

        for (int i = 0; i < 4; i++) {
            ipNumbers += ip[i] << (24 - (8 * i));
        }

        return ipNumbers < Long.MAX_VALUE / 2;
    }

    public void testGet() {
        String url ="https://httpbin.org/get";
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
        String result = response.getBody();
        System.out.println(result);
    }

    public boolean testPost() throws JSONException {
        String url ="https://httpbin.org/post";

        JSONObject json = new JSONObject();
        json.put("name", "park");
        json.put("age", 18);

        String requestJson = "{\"queriedQuestion\":\"Lorem Ipsum Dolor Sit Amet?\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(requestJson,headers);
        ResponseEntity<URL> response = restTemplate.exchange(url, HttpMethod.POST, entity, URL.class);
        URL result = response.getBody();
        System.out.println(gson.toJson(result));

        return dummyIpCheck(result.getOrigin());
    }
}