package dev.crashteam.uzumspace.config

import dev.crashteam.uzumspace.config.security.UserCreationFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.*
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(
    private val userCreationFilter: UserCreationFilter,
) {

    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private lateinit var issuer: String

    @Bean
    @Order(1)
    fun basicAuthWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .cors().configurationSource(createCorsConfigSource()).and()
            .securityMatcher(pathMatchers("/actuator/**", "/v1/similar/products")).anonymous().and()
            .authorizeExchange { spec ->
                run {
                    spec.pathMatchers("/actuator/**", "/v1/similar/products").permitAll()
                }
            }
            .build()
    }

    @Bean
    @Order(2)
    fun oAuthWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .cors().configurationSource(createCorsConfigSource()).and()
            .authorizeExchange().anyExchange().authenticated().and()
            .oauth2ResourceServer()
            .jwt()
            .jwtDecoder(jwtDecoder())
            .jwtAuthenticationConverter(jwtAuthenticationConverter()).and()
            .and()
            .addFilterAt(userCreationFilter, SecurityWebFiltersOrder.AUTHORIZATION)
            .build()
    }

    @Bean
    fun jwtDecoder(): ReactiveJwtDecoder {
        val jwtDecoder = ReactiveJwtDecoders.fromOidcIssuerLocation(issuer) as NimbusReactiveJwtDecoder
        val withIssuer = JwtValidators.createDefaultWithIssuer(issuer)
        val withAudience: OAuth2TokenValidator<Jwt> = DelegatingOAuth2TokenValidator(
            withIssuer,
            JwtTimestampValidator()
        )
        jwtDecoder.setJwtValidator(withAudience)

        return jwtDecoder
    }

    fun jwtAuthenticationConverter(): ReactiveJwtAuthenticationConverter {
        val converter = JwtGrantedAuthoritiesConverter()
        converter.setAuthoritiesClaimName("permissions")
        converter.setAuthorityPrefix("")
        val reactiveJwtGrantedAuthoritiesConverterAdapter = ReactiveJwtGrantedAuthoritiesConverterAdapter(converter)
        val reactiveJwtAuthenticationConverter = ReactiveJwtAuthenticationConverter()
        reactiveJwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
            reactiveJwtGrantedAuthoritiesConverterAdapter
        )

        return reactiveJwtAuthenticationConverter
    }

    private fun createCorsConfigSource(): CorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()
        config.applyPermitDefaultValues()
        config.addAllowedMethod(HttpMethod.PATCH)
        config.addAllowedMethod(HttpMethod.PUT)
        config.addAllowedMethod(HttpMethod.DELETE)
        config.addAllowedMethod(HttpMethod.OPTIONS)
        config.allowCredentials = true
        config.allowedOrigins = null
        config.allowedOriginPatterns = listOf("*")
        config.allowedHeaders = listOf("*")
        config.addExposedHeader("*")
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
