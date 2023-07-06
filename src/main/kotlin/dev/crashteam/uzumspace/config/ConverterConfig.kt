package dev.crashteam.uzumspace.config

import dev.crashteam.uzumspace.converter.DataConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.support.ConversionServiceFactoryBean

@Configuration
@ComponentScan(
    basePackages = [
        "dev.crashteam.uzumspace.converter",
    ]
)
class ConverterConfig {

    @Bean
    @Primary
    fun conversionServiceFactoryBean(converters: Set<DataConverter<*, *>>): ConversionServiceFactoryBean {
        val conversionServiceFactoryBean = ConversionServiceFactoryBean()
        conversionServiceFactoryBean.setConverters(converters)

        return conversionServiceFactoryBean
    }
}
