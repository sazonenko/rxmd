package ru.thprom.mrp.md;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;

/**
 * Created by void on 11.12.15
 */
@Configuration
@PropertySources({
		@PropertySource("classpath:default.properties"),
		@PropertySource(value = "classpath:rxtest.properties", ignoreResourceNotFound = true)
})
public class MainContext {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private Environment env;


}
