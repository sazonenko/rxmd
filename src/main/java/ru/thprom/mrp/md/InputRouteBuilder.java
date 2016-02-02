package ru.thprom.mrp.md;

import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.rx.ReactiveCamel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import rx.Observable;

/**
 * Created by void on 09.12.15
 */
@Component("rxRoute")
public class InputRouteBuilder extends RouteBuilder {
	private final Logger log = LoggerFactory.getLogger(getClass());

	private FileStore fileStore;
	private MongoStore mongoStore;

	@Override
	public void configure() throws Exception {
		log.debug("before configure RX routes");

		ModelCamelContext context = getContext();
		ProducerTemplate end = context.createProducerTemplate();

		ReactiveCamel reactiveCamel = new ReactiveCamel(context);
		Observable<Message> src = reactiveCamel.toObservable("direct:start");

		src     .doOnNext(m -> fileStore.storeFile(m))
				.doOnNext(m -> mongoStore.saveEvent(
						m.getHeader(Constants.HEADER_CAMEL_FILE_NAME, String.class),
						m.getHeader(Constants.HEADER_ATTACHMENT_PATH, String.class)))
				.doOnNext(m -> end.sendBody("direct:end", m))
				.subscribe(
						m -> log.debug("Received file: {}", m.getHeader(Constants.HEADER_CAMEL_FILE_NAME, String.class)),
						error -> log.error("Error in input route : " , error)
				);

	}

	public void setFileStore(FileStore fileStore) {
		this.fileStore = fileStore;
	}

	public void setMongoStore(MongoStore mongoStore) {
		this.mongoStore = mongoStore;
	}
}
