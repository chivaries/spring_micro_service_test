package com.glamrock.licenses.services;

import com.netflix.discovery.converters.Auto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DiscoveryService {
    @Autowired
    RestTemplate restTemplate;

}
