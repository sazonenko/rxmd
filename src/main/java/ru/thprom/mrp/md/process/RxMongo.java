package ru.thprom.mrp.md.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.thprom.mrp.md.MongoStore;
import rx.Observable;
import rx.Producer;
import rx.Subscriber;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by void on 03.02.16
 */
public final class RxMongo implements Observable.OnSubscribe<Map> {
	private final Logger log = LoggerFactory.getLogger(getClass());

	private MongoStore mongoStore;

	@Override
	public void call(Subscriber<? super Map> subscriber) {
		MongoProducer producer = new MongoProducer(subscriber);
		subscriber.setProducer(producer);
	}

	public Observable<Map> toObservable() {
		return Observable.create(this);
	}

	public void setMongoStore(MongoStore mongoStore) {
		this.mongoStore = mongoStore;
	}

	class MongoProducer implements Producer {

		private final Subscriber<? super Map> child;

		public MongoProducer(Subscriber<? super Map> child) {
			this.child = child;
		}

		@Override
		public void request(long n) {
			log.debug("requested {}", n);
			Map<String, Object> incomeEvent = null;
			try {
				while (null == incomeEvent) {
					if (child.isUnsubscribed()) {
						return;
					}
					incomeEvent = mongoStore.getIncomeEvent();
					if (null == incomeEvent) {
						TimeUnit.SECONDS.sleep(5);
					}
				}
			} catch (InterruptedException e) {
				// let them go
			}
			child.onNext(incomeEvent);
		}

	}
}
