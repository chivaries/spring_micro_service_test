package com.glamrock.licenses.clients;

import com.glamrock.licenses.model.Organization;
import com.glamrock.licenses.repository.OrganizationRedisRepository;
import com.glamrock.licenses.utils.UserContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class OrganizationRestTemplateClient {
    @Autowired
    RestTemplate restTemplate;

//    @Autowired
//    OAuth2RestTemplate restTemplate;

    @Autowired
    OrganizationRedisRepository organizationRedisRepository;

    private static final Logger logger = LoggerFactory.getLogger(OrganizationRestTemplateClient.class);

    private Organization checkRedisCache(String organizationId) {
        try {
            return organizationRedisRepository.findOrganization(organizationId);
        } catch (Exception ex) {
            logger.error("Error encountered while trying to retrieve organization {} check Redis Cache.  Exception {}", organizationId, ex);
            return null;
        }
    }

    private void cacheOrganizationObject(Organization org) {
        try {
            organizationRedisRepository.saveOrganization(org);
        } catch (Exception ex) {
            logger.error("Unable to cache organization {} in Redis. Exception {}", org.getId(), ex);
        }
    }
/*
    public Organization getOrganization(String organizationId) {
        logger.debug(">>> In Licensing Service.getOrganization: {}. Thread Id: {}", UserContextHolder.getContext().getCorrelationId(), Thread.currentThread().getId());
        ResponseEntity<Organization> restExchange =
                restTemplate.exchange(
                        // ribbon 지원 RestTemplate 를 사용할때 유레카 서비스 ID 로 URL 을 만든다.
                        // 실제 서비스 위치와 포트는 추상화됨
                        "http://organizationservice/v1/organizations/{organizationId}",

                        // ribbon 지원 RestTemplate 를 사용할때 주울 서비스 ID 로 URL 을 만든다.
                        // 실제 서비스 위치와 포트는 추상화됨
                        //"http://zuulserver/api/organization/v1/organizations/{organizationId}",
                        HttpMethod.GET,
                        null, Organization.class, organizationId
                );

        return restExchange.getBody();
    }
*/

    public Organization getOrganization(String organizationId) {
        logger.debug("In Licensing Service.getOrganization: {}", UserContextHolder.getContext().getCorrelationId());

        Organization org = checkRedisCache(organizationId);

        if (org != null) {
            logger.debug("I have successfully retrieved an organization {} from the redis cache: {}", organizationId, org);
            return org;
        }

        logger.debug("Unable to locate organization from the redis cache: {}.", organizationId);

        ResponseEntity<Organization> restExchange =
            restTemplate.exchange(
                    //"http://localhost:5555/api/organization/v1/organizations/{organizationId}",
                    "http://zuulserver:5555/api/organization/v1/organizations/{organizationId}",
                    HttpMethod.GET,
                    null, Organization.class, organizationId);

        org = restExchange.getBody();

        if (org != null) {
            cacheOrganizationObject(org);
        }

        return org;
    }
}
