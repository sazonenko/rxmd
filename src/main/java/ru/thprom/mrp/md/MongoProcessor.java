package ru.thprom.mrp.md;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by void on 02.02.16
 */
public class MongoProcessor {
	private final Logger log = LoggerFactory.getLogger(getClass());

	public void process() throws Exception {
		DataSource ds  = new DataSource();
		Observable<Integer> src = Observable.range(0, 100);
		src.map(v -> ds.getValue())
				.subscribe(v -> System.out.println("got value: "+v));
	}

	private class DataSource {

		AtomicInteger value = new AtomicInteger(0);
		long paused = 0;

		public DataSource() {

		}

		public Integer getValue() {
			int result = -1 ;
			if (paused == 0) {
				result = value.incrementAndGet();
			} else {
				long now = System.currentTimeMillis();
				if (now - paused > 30000) {
					paused = 0;
				}
			}
			if (result % 10 == 0) {
				paused = System.currentTimeMillis();
			}
			return result > 0 ? result : null;
		}
	}

	public static void main(String args[]) throws Exception {
		new MongoProcessor().process();
	}
}
