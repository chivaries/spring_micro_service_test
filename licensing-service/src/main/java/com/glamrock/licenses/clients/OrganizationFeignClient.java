package com.glamrock.licenses.clients;

import com.glamrock.licenses.model.Organization;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

// FeignClient annotation 으로 조직서비스를 feign 에서 확인
@FeignClient("organizationservice")
public interface OrganizationFeignClient {
    // 엔드포인트와 액션을 정의
    @RequestMapping(
            method= RequestMethod.GET,
            value="/v1/organizations/{organizationId}",
            consumes="application/json"
    )
    Organization getOrganization(@PathVariable("organizationId") String organizationId);
}
