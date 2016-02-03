package ru.thprom.mrp.md;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.rx.ObservableBody;
import org.apache.camel.test.spring.DisableJmx;
import org.apache.camel.testng.CamelSpringTestSupport;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.testng.Assert;
import org.testng.annotations.Test;
import ru.thprom.mrp.md.process.RxMongo;
import ru.thprom.mrp.md.process.RxRange;
import rx.Observable;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Created by void on 09.12.15
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisableJmx(false)
public class PlainTest extends CamelSpringTestSupport {

	private static final String FS = "/";
	private Environment env;

    protected CamelContext camelContext;

	private RxMongo rxMongo;

	@EndpointInject(uri = "mock:result")
    protected MockEndpoint mockResult;

    @Produce(uri = "direct:startTest")
    protected ProducerTemplate start;

    @Test
    public void testFilePositive() throws Exception {
		String fileName = "package_14e72963-2464-4570-bb80-5209bcff12fc.xml";
        File body = new File(this.getClass().getResource(fileName).toURI());

        mockResult.expectedMessageCount(1);

		Date now = new Date();
		SimpleDateFormat month = new SimpleDateFormat("yyyy-MM");
		SimpleDateFormat day = new SimpleDateFormat("dd");
		String storeDir = env.getProperty("md.store.root") + FS + month.format(now) + FS + day.format(now) + FS + env.getProperty("md.test.code") + FS;

		File result = new File(storeDir, fileName);
		result.delete();

		start.sendBodyAndHeader(body, Constants.HEADER_CAMEL_FILE_NAME, fileName);

		MockEndpoint.assertIsSatisfied(camelContext);

		log.debug("before testing result file");
		Assert.assertTrue(result.exists(), "Is file exist");
		Assert.assertTrue(FileUtils.contentEquals(body, result), "Is files are same?");
    }

	@Test
	public void testInputProcess() {
		Observable<Map> mongo = rxMongo.toObservable();
		mongo.take(1).subscribe(
				System.out::println,
				Throwable::printStackTrace,
				() -> System.out.println("Done")
		);
	}

	@Override
    protected AbstractApplicationContext createApplicationContext() {
        AbstractApplicationContext applicationContext = new AnnotationConfigApplicationContext(CamelContextConfiguration.class);
        camelContext = applicationContext.getBean("camelContext", CamelContext.class);
        env = applicationContext.getBean(Environment.class);
        addTestRoutes(env);

		rxMongo = (RxMongo) applicationContext.getBean("mongoStore");

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

