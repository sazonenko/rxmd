package ru.thprom.mrp.md;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spring.SpringCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.jms.connection.JmsTransactionManager;

import javax.jms.ConnectionFactory;

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

	@Bean
	public SpringCamelContext camelContext() throws Exception {
		SpringCamelContext camelContext = SpringCamelContext.springCamelContext("camel.xml");
		camelContext.addComponent("activemq", activemq());
		camelContext.addRoutes(new RouteBuilder() {
			@Override
			public void configure() throws Exception {
				from(env.getProperty("broker.queue.incoming")).to("direct:start");
				from("direct:end").to("activemq:result");
			}
		});


		camelContext.addRoutes(rxRouteBuilder());
		return camelContext;
	}

	@Bean
	public RxRouteBuilder rxRouteBuilder() {
		RxRouteBuilder rxRouteBuilder = new RxRouteBuilder();
		FileStore fileStore = new FileStore();
		fileStore.setStoreRoot(env.getProperty("md.store.root"));
		rxRouteBuilder.setFileStore(fileStore);
		return rxRouteBuilder;
	}

	@Bean
	public ActiveMQComponent activemq() {
		ActiveMQComponent activeMQComponent = new ActiveMQComponent();
		activeMQComponent.setTransacted(true);
		activeMQComponent.setConnectionFactory(connectionFactory());
		activeMQComponent.setTransactionManager(new JmsTransactionManager(connectionFactory()));
		return activeMQComponent;
	}

	@Bean
	public ConnectionFactory connectionFactory() {
 		PooledConnectionFactory connectionFactory = new PooledConnectionFactory();
		connectionFactory.setMaxConnections(env.getProperty("broker.maxConnections", Integer.class));
		log.debug("broker URL: {}", env.getProperty("broker.connectionUrl"));
		connectionFactory.setConnectionFactory(
				new ActiveMQConnectionFactory(env.getProperty("broker.connectionUrl"))
		);
		return connectionFactory;
	}
}
