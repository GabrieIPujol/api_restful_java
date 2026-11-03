package com.senac.tsi.Formula1Api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

// coloca os links do HATEOAS na chave de API
@Component
class ApiKeyModelAssembler implements RepresentationModelAssembler<ApiKey, EntityModel<ApiKey>> {

    @Override
    public EntityModel<ApiKey> toModel(ApiKey apiKey) {
        return EntityModel.of(apiKey,
                linkTo(methodOn(ApiKeyController.class).getApiKeyById(apiKey.getId())).withSelfRel(),
                linkTo(methodOn(ApiKeyController.class).updateApiKey(apiKey.getId(), null, null)).withRel("update"),
                linkTo(methodOn(ApiKeyController.class).deleteApiKey(apiKey.getId(), null)).withRel("delete"),
                linkTo(methodOn(ApiKeyController.class).getAllApiKeys(Pageable.unpaged())).withRel("apiKeys")
        );
    }
}
