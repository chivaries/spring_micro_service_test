package com.glamrock.zuulsvr.filters;

import com.glamrock.zuulsvr.model.AbTestingRoute;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

//@Component
public class SpecialRoutesFilter extends ZuulFilter {
    private static final int FILTER_ORDER =  1;
    private static final boolean SHOULD_FILTER =true;

    @Autowired
    FilterUtils filterUtils;

    @Autowired
    RestTemplate restTemplate;

    @Override
    public String filterType() {
        return filterUtils.ROUTE_FILTER_TYPE;
    }

    @Override
    public int filterOrder() {
        return FILTER_ORDER;
    }

    @Override
    public boolean shouldFilter() {
        return SHOULD_FILTER;
    }

    private ProxyRequestHelper helper = new ProxyRequestHelper();

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();

        AbTestingRoute abTestingRoute = getAbRoutingInfo(filterUtils.getServiceId());

        if (abTestingRoute != null && useSpecialRoute(abTestingRoute)) {
            String route = buildRouteString(ctx.getRequest().getRequestURI(),
                    abTestingRoute.getEndpoint(),
                    filterUtils.getServiceId());
            forwardToSpecialRoute(route);
        }

        return null;
    }

    private void forwardToSpecialRoute(String route) {
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletRequest request = context.getRequest();

        MultiValueMap<String, String> headers = this.helper.buildZuulRequestHeaders(request);
        MultiValueMap<String, String> params = this.helper.buildZuulRequestQueryParams(request);

        String verb = getVerb(request);
        InputStream requestEntity = getRequestBody(request);

        if (request.getContentLength() < 0) {
            context.setChunkedRequestBody();
        }

        this.helper.addIgnoredHeaders();
        CloseableHttpClient httpClient = null;
        HttpResponse response = null;

        try {
            httpClient = HttpClients.createDefault();
            response = forward(httpClient, verb, route, request, headers, params, requestEntity);
            setResponse(response);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                httpClient.close();
            } catch(IOException ex) {}
        }
    }

    private void setResponse(HttpResponse response) throws IOException {
        this.helper.setResponse(response.getStatusLine().getStatusCode(),
                response.getEntity() == null ? null : response.getEntity().getContent(),
                revertHeaders(response.getAllHeaders()));
    }

    private MultiValueMap<String,String> revertHeaders(Header[] headers) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();

        for (Header header : headers) {
            String name = header.getName();

            if (!map.containsKey(name)) {
                map.put(name, new ArrayList<String>());
            }

            map.get(name).add(header.getValue());
        }

        return map;
    }

    private HttpResponse forward(CloseableHttpClient httpClient, String verb, String uri,
                                 HttpServletRequest request, MultiValueMap<String, String> headers,
                                 MultiValueMap<String, String> params, InputStream requestEntity) throws Exception {
        Map<String, Object> info = this.helper.debug(verb, uri, headers, params, requestEntity);
        URL host = new URL(uri);
        HttpHost httpHost = getHttpHost(host);

        HttpRequest httpRequest;
        int contentLength = request.getContentLength();

        InputStreamEntity entity = new InputStreamEntity(requestEntity, contentLength,
                request.getContentType() != null ? ContentType.create(request.getContentType()) : null);

        switch (verb.toUpperCase()) {
            case "POST":
                HttpPost httpPost = new HttpPost(uri);
                httpRequest = httpPost;
                httpPost.setEntity(entity);
                break;
            case "PUT":
                HttpPut httpPut = new HttpPut(uri);
                httpRequest = httpPut;
                httpPut.setEntity(entity);
                break;
            case "PATCH":
                HttpPatch httpPatch = new HttpPatch(uri );
                httpRequest = httpPatch;
                httpPatch.setEntity(entity);
                break;
            default:
                httpRequest = new BasicHttpRequest(verb, uri);
        }

        try {
            httpRequest.setHeaders(convertHeaders(headers));
            HttpResponse zuulResponse = forwardRequest(httpClient, httpHost, httpRequest);

            return zuulResponse;
        }finally {

        }
    }

    private HttpResponse forwardRequest(CloseableHttpClient httpClient, HttpHost httpHost, HttpRequest httpRequest) throws IOException {
        return httpClient.execute(httpHost, httpRequest);
    }

    private Header[] convertHeaders(MultiValueMap<String, String> headers) {
        List<Header> list = new ArrayList();
        for (String name : headers.keySet()) {
            for (String value : headers.get(name)) {
                list.add(new BasicHeader(name, value));
            }
        }
        return list.toArray(new BasicHeader[0]);
    }

    private HttpHost getHttpHost(URL host) {
        HttpHost httpHost = new HttpHost(host.getHost(), host.getPort(), host.getProtocol());
        return httpHost;
    }

    private InputStream getRequestBody(HttpServletRequest request) {
        InputStream requestEntity = null;
        try {
            requestEntity = request.getInputStream();
        } catch (IOException ex) {
            // no requestBody is ok.
        }
        return requestEntity;
    }

    private String getVerb(HttpServletRequest request) {
        String sMethod = request.getMethod();
        return sMethod.toUpperCase();
    }

    private String buildRouteString(String oldEndPoint, String newEndPoint, String serviceId) {
        int index = oldEndPoint.indexOf(serviceId);

        String strippedRoute = oldEndPoint.substring(index + serviceId.length());
        System.out.println("Target route: " + String.format("%s/%s", newEndPoint, strippedRoute));

        return String.format("%s/%s", newEndPoint, strippedRoute);
    }

    private boolean useSpecialRoute(AbTestingRoute abTestingRoute) {
        Random random = new Random();

        if (abTestingRoute.getActive().equals("N")) return false;

        int value = random.nextInt( (10 - 1) + 1 ) + 1;

        if (abTestingRoute.getWeight() < value) return true;

        return false;
    }

    private AbTestingRoute getAbRoutingInfo(String serviceId) {
        ResponseEntity<AbTestingRoute> restExchange = null;

        try {
            restExchange = restTemplate.exchange(
                "http://specialroutesservice/v1/route/abtesting/{serviceId}",
                HttpMethod.GET,
                null, AbTestingRoute.class, serviceId);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) return null;
            throw ex;
        }

        return restExchange.getBody();
    }
}
