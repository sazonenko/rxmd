package ru.thprom.mrp.md;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.spring.javaconfig.SingleRouteCamelConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.jms.connection.JmsTransactionManager;

import javax.jms.ConnectionFactory;

/**
 * Created by void on 28.01.16
 */
@PropertySources({
		@PropertySource("classpath:default.properties"),
		@PropertySource(value = "classpath:rxtest.properties", ignoreResourceNotFound = true)
})
@Configuration
public class CamelContextConfiguration extends SingleRouteCamelConfiguration implements InitializingBean {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private Environment env;


	@Override
	public RouteBuilder route() {
		return new RouteBuilder() {
			@Override
			public void configure() throws Exception {
				from(env.getProperty("md.test.queue"))
						.setHeader(Constants.HEADER_PACKAGE_PRODUCER, constant(env.getProperty("md.test.code")))
						.to("direct:start");
				from("direct:end").to("activemq:result");
			}
		};
	}

	@Bean
	public MongoStore mongoStore() {
		MongoStore mongoStore = new MongoStore();
		mongoStore.setConnectionURI(env.getProperty("mongo.url"));
		mongoStore.setDatabaseName(env.getProperty("mongo.database"));
		mongoStore.connect();
		return mongoStore;
	}

	@Bean
	public RxRouteBuilder rxRouteBuilder() {
		RxRouteBuilder rxRouteBuilder = new RxRouteBuilder();
		FileStore fileStore = new FileStore();
		fileStore.setStoreRoot(env.getProperty("md.store.root"));
		rxRouteBuilder.setFileStore(fileStore);
		rxRouteBuilder.setMongoStore(mongoStore());
		return rxRouteBuilder;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		ApplicationContext applicationContext = getApplicationContext();
		RouteBuilder rxRoute = (RouteBuilder) applicationContext.getBean("rxRouteBuilder");
		CamelContext camelContext = (CamelContext) applicationContext.getBean("camelContext");
		camelContext.addRoutes(rxRoute);
	}

	/**
	 * Returns the CamelContext which support Spring
	 */
	@Override
	protected CamelContext createCamelContext() throws Exception {
		return new SpringCamelContext(getApplicationContext());
	}

	@Override
	protected void setupCamelContext(CamelContext camelContext) throws Exception {
		camelContext.addComponent("activemq", activemq());
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
