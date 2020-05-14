package com.glamrock.licenses.services;

import com.glamrock.licenses.clients.OrganizationDiscoveryClient;
import com.glamrock.licenses.clients.OrganizationFeignClient;
import com.glamrock.licenses.clients.OrganizationRestTemplateClient;
import com.glamrock.licenses.config.ServiceConfig;
import com.glamrock.licenses.model.License;
import com.glamrock.licenses.model.Organization;
import com.glamrock.licenses.repository.LicenseRepository;
import com.glamrock.licenses.utils.UserContextHolder;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class LicenseService {
    private static final Logger logger = LoggerFactory.getLogger(LicenseService.class);

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    ServiceConfig serviceConfig;

    @Autowired
    OrganizationDiscoveryClient organizationDiscoveryClient;

    @Autowired
    OrganizationRestTemplateClient organizationRestTemplateClient;

    @Autowired
    OrganizationFeignClient organizationFeignClient;

    private Organization retrieveOrgInfo(String organizationId, String clientType) {
        Organization organization = null;

        switch(clientType) {
            case "discovery" :
                logger.debug("I am using the discovery client");
                organization = organizationDiscoveryClient.getOrganizaton(organizationId);
                break;
            case "rest" :
                logger.debug("I am using the rest client");
                organization = organizationRestTemplateClient.getOrganization(organizationId);
                break;
            case "feign":
                logger.debug("I am using the feign client");
                organization = organizationFeignClient.getOrganization(organizationId);
                break;
            default:
                organization = organizationRestTemplateClient.getOrganization(organizationId);
        }

        return organization;
    }

    public License getLicense(String organizationId, String licenseId, String clientType) {
        License license = licenseRepository.findByOrOrganizationIdAndLicenseId(organizationId, licenseId);

        Organization organization = retrieveOrgInfo(organizationId, clientType);

        return license
                .withOrganizationName(organization.getName())
                .withContactName(organization.getContactName())
                .withContactEmail(organization.getContactEmail())
                .withContactPhone(organization.getContactPhone())
                .withComment(serviceConfig.getExampleProperty());
    }

    @HystrixCommand(
            fallbackMethod = "buildFallbackLicenseList",
//            commandProperties =
//                    {
//                            @HystrixProperty(
//                                name="execution.isolation.thread.timeoutInMilliseconds",
//                                value="10000"
//                            )
//                    }
            threadPoolKey = "licenseByOrgThreadPool",
            threadPoolProperties = {
                    @HystrixProperty(name="coreSize", value="30"),
                    @HystrixProperty(name="maxQueueSize", value="10")
            },
            commandProperties = {
                    @HystrixProperty(name="circuitBreaker.requestVolumeThreshold", value="10"),
                    @HystrixProperty(name="circuitBreaker.errorThresholdPercentage", value="75"),
                    @HystrixProperty(name="circuitBreaker.sleepWindowInMilliseconds", value="7000"),
                    @HystrixProperty(name="metrics.rollingStats.timeInMilliseconds", value="15000"),
                    @HystrixProperty(name="metrics.rollingStats.numBuckets", value="5")
            }
            )
    public List<License> getLicenseByOrg(String organizationId) {
        logger.debug("LicenseService.getLicensesByOrg  Correlation id: {}", UserContextHolder.getContext().getCorrelationId());
        randomlyRunLong();
        return licenseRepository.findByOrOrganizationId(organizationId);
    }

    public void saveLicense(License license) {
        license.withId(UUID.randomUUID().toString());
        licenseRepository.save(license);
    }

    public void updateLicense(License license) {
        licenseRepository.save(license);
    }

    public void deleteLicese(License license) {
        licenseRepository.delete(license);
    }

    private void randomlyRunLong(){
        Random rand = new Random();

        int randomNum = rand.nextInt((3 - 1) + 1) + 1;

        if (randomNum==3) sleep();
    }

    private void sleep(){
        try {
            Thread.sleep(11000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private List<License> buildFallbackLicenseList(String organizationId){
        List<License> fallbackList = new ArrayList<>();
        License license = new License()
                .withId("0000000-00-00000")
                .withOrganizationId( organizationId )
                .withProductName("Sorry no licensing information currently available");

        fallbackList.add(license);
        return fallbackList;
    }
}
