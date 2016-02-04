package ru.thprom.mrp.md.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.thprom.mrp.md.MongoStore;
import rx.Observable;
import rx.Producer;
import rx.Subscriber;
import rx.internal.operators.BackpressureUtils;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by void on 03.02.16
 */
public final class RxMongo implements Observable.OnSubscribe<Map> {
	private final Logger log = LoggerFactory.getLogger(getClass());

	private AtomicLong requested = new AtomicLong(0);
	private MongoStore mongoStore;
	ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

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
			if (n <= 0) {
				return;
			}
			// increment with overflow check
			if (BackpressureUtils.getAndAddRequest(requested, n) != 0) {
				return;   //Thread safety// ? только 1 поток генерит значения ?
			}

			long batchSize = n;
			do {
				if (child.isUnsubscribed()) {
					return;
				}
				long emitted = 0;
				while (batchSize > 0) {
					Map<String, Object> value = getValue();
					child.onNext(value);
					if (child.isUnsubscribed()) {
						return;
					}
					batchSize--;
				}

				batchSize = requested.getAndAdd(emitted);
			} while (batchSize > 0);

/*
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
*/


			executor.schedule(() ->{
				Map<String, Object> value = mongoStore.getIncomeEvent();
				resumeProcess(value);
			}, 5, TimeUnit.SECONDS);

		}

	}

	private Map<String, Object> getValue() {
		Map<String, Object> incomeEvent = mongoStore.getIncomeEvent();
		return incomeEvent;
	}
}
