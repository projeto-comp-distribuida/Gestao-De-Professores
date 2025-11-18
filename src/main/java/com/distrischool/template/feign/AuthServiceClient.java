package com.distrischool.template.feign;

import com.distrischool.template.dto.auth.ApiResponse;
import com.distrischool.template.dto.auth.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client para comunicação com o serviço de autenticação
 */
@FeignClient(name = "auth-service", url = "${microservice.auth.url:http://microservice-auth-dev:8080}")
public interface AuthServiceClient {

    /**
     * Busca um usuário por Auth0 ID
     */
    @GetMapping("/api/v1/users/auth0/{auth0Id}")
    ApiResponse<UserResponse> getUserByAuth0Id(@PathVariable String auth0Id);

    /**
     * Verifica se o usuário tem uma role específica
     * Retorna true se o usuário tem a role especificada
     */
    @GetMapping("/api/v1/users/{userId}/has-role")
    ApiResponse<Boolean> hasRole(@PathVariable Long userId, @RequestParam String role);
}






