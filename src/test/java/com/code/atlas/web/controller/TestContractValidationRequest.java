package com.code.atlas.web.controller;

import jakarta.validation.constraints.NotBlank;

record TestContractValidationRequest(@NotBlank String name) {
}
