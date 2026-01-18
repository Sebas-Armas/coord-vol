package com.coordvol.auth_service.config;

import java.util.UUID;

import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class SecurityAuditorAware implements ReactiveAuditorAware<UUID> {@Override

    public Mono<UUID> getCurrentAuditor() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .cast(UUID.class)
                .doOnNext(userId -> log.debug("Current auditor: {}", userId))
                .switchIfEmpty(Mono.defer(() ->{
                    log.debug("No authentication context, using system user for Audit");
                    return Mono.just(UUID.fromString("00000000-0000-0000-0000-000000000000"));
                }));
    }

}
