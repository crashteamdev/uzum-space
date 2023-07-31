package dev.crashteam.uzumspace.config

import dev.crashteam.uzumspace.proxy.interceptor.CookieHeaderRequestInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.http.client.*
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate


@Configuration
class RestTemplateConfig {

    @Bean
    fun restTemplate(simpleHttpRequestFactory: ClientHttpRequestFactory): RestTemplate {
        val restTemplate = RestTemplate(simpleHttpRequestFactory)
        restTemplate.errorHandler = object : DefaultResponseErrorHandler() {
            override fun hasError(statusCode: HttpStatus): Boolean {
                return false
            }
        }
        return restTemplate
    }

    @Bean
    fun lkRestTemplate(
        simpleHttpRequestFactory: HttpComponentsClientHttpRequestFactory,
        cookieHeaderRequestInterceptor: CookieHeaderRequestInterceptor
    ): RestTemplate {
        val restTemplate = RestTemplate(simpleHttpRequestFactory)
        restTemplate.errorHandler = object : DefaultResponseErrorHandler() {
            override fun hasError(statusCode: HttpStatus): Boolean {
                return false
            }
        }
        restTemplate.interceptors.add(RemoveHeaderHttpRequestInterceptor())
        return restTemplate
    }

//    @Bean
//    fun lkRestTemplate(
//        proxyHttpRequestFactory: HttpComponentsClientHttpRequestFactory,
//        cookieHeaderRequestInterceptor: CookieHeaderRequestInterceptor
//    ): RestTemplate {
//        val restTemplate = RestTemplate(proxyHttpRequestFactory)
//        restTemplate.errorHandler = object : DefaultResponseErrorHandler() {
//            override fun hasError(statusCode: HttpStatus): Boolean {
//                return false
//            }
//        }
//        restTemplate.interceptors.add(cookieHeaderRequestInterceptor)
//        return restTemplate
//    }

    class RemoveHeaderHttpRequestInterceptor : ClientHttpRequestInterceptor {
        override fun intercept(
            request: HttpRequest,
            body: ByteArray,
            execution: ClientHttpRequestExecution
        ): ClientHttpResponse {
            val headers: HttpHeaders = request.headers
            headers.remove(HttpHeaders.ACCEPT)

            return execution.execute(request, body)
        }
    }

}
