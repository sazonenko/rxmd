package ru.thprom.mrp.md;

import org.apache.camel.CamelContext;
import org.apache.camel.Message;
import org.apache.camel.rx.ReactiveCamel;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import rx.Observable;

/**
 * Created by void on 09.12.15
 */
public class Main {

	public static void main(String[] args) {

		ApplicationContext appContext = new AnnotationConfigApplicationContext(CamelContextConfiguration.class);
		CamelContext camelContext = appContext.getBean("camelContext", CamelContext.class);
		ReactiveCamel rx = new ReactiveCamel(camelContext);

		Observable<Message> src = rx.toObservable("activemq:MyMessages");
		src.subscribe(s -> System.out.println(s));
	}
}
