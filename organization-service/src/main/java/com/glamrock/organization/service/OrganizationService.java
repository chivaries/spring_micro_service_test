package com.glamrock.organization.service;

import com.glamrock.organization.events.source.SimpleSourceBean;
import com.glamrock.organization.model.Organization;
import com.glamrock.organization.repository.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class OrganizationService {
    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    SimpleSourceBean simpleSourceBean;

    public Organization getOrg(String organizationId) {
        Optional<Organization> organization = organizationRepository.findById(organizationId);

        if (!organization.isPresent()) {
            throw new NullPointerException("organizationId-" + organizationId);
        }
        return organization.get();
    }

    public void saveOrg(Organization organization) {
        organization.setId(UUID.randomUUID().toString());

        organizationRepository.save(organization);
        simpleSourceBean.publishOrgChange("SAVE", organization.getId());
    }

    public void updateOrg(Organization organization) {
        organizationRepository.save(organization);
        simpleSourceBean.publishOrgChange("UPDATE", organization.getId());
    }

    public void deleteOrg(Organization organization) {
        organizationRepository.delete(organization);
        simpleSourceBean.publishOrgChange("DELETE", organization.getId());
    }
}
