package ru.thprom.mrp.md;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.DisableJmx;
import org.apache.camel.testng.CamelSpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.testng.annotations.Test;

/**
 * Created by void on 09.12.15
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisableJmx(false)
public class PlainTest extends CamelSpringTestSupport {

    protected CamelContext camelContext;

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint mockResult;

    @Produce(uri = "direct:startTest")
    protected ProducerTemplate start;

    @Test
    public void testPositive() throws Exception {
        String body = "<test/>";

        mockResult.expectedBodiesReceived(body);

        start.sendBodyAndHeader(body, "foo", "bar");

        MockEndpoint.assertIsSatisfied(camelContext);
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        AbstractApplicationContext applicationContext = new AnnotationConfigApplicationContext(CamelContextConfiguration.class);
//        AbstractApplicationContext applicationContext = new ClassPathXmlApplicationContext("beans.xml");
        camelContext = applicationContext.getBean("camelContext", CamelContext.class);
        Environment env = applicationContext.getBean(Environment.class);
        addTestRoutes(env);
        return applicationContext;
    }

    private void addTestRoutes(final Environment env) {
        try {
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:startTest").to(env.getProperty("md.test.queue"));
                    from("activemq:result").to("mock:result");
                }
            });
        } catch (Exception e) {
            log.error("Can't add routes in context", e);
        }
    }
}

