package com.noserbulgaria.micromarket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/**
 * Configuration class to provide the metadata as a DTO-based JSON instdead of exposing PageImpl directly.
 * <p>
 * This applies to controller methods that return Page<?>.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class SpringDataWebConfig {}
