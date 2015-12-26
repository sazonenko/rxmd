package ru.thprom.mrp.md;

import org.apache.camel.Message;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.rx.ReactiveCamel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

/**
 * Created by void on 09.12.15
 */
public class RxRouteBuilder extends RouteBuilder {
	private final Logger log = LoggerFactory.getLogger(getClass());

	@Produce(uri = "direct:end")
	private ProducerTemplate end;

	@Override
	public void configure() throws Exception {
		log.debug("before configure RX routes");
		ReactiveCamel reactiveCamel = new ReactiveCamel(getContext());
		Observable<Message> src = reactiveCamel.toObservable("direct:start");

		src		.map(m -> m.getBody(String.class))
				.doOnNext(System.out::println)
				.subscribe(s -> end.sendBody(s));

	}

}
