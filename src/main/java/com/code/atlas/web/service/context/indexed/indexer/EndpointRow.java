package com.code.atlas.web.service.context.indexed.indexer;

public record EndpointRow(String httpMethod, String path, String controller, String service) {
}
