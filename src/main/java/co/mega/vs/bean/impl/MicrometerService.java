package co.mega.vs.bean.impl;

import co.mega.vs.bean.IMicrometerService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MicrometerService implements IMicrometerService {

    private final Map<String, Timer> timerMap = new ConcurrentHashMap<>();
    private final Map<String, Counter> counterMap = new ConcurrentHashMap<>();
    private final Map<String,Gauge> gaugeMap = new ConcurrentHashMap<>();

    @Autowired
    private MeterRegistry meterRegistry;

    private static Duration[] buckets = generateBucket();

    public Timer time(String name) {
        if (!timerMap.containsKey(name)) {
            Timer timer = Timer.builder(name)
                    .publishPercentileHistogram()
                    .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                    .percentilePrecision(5)
                    .distributionStatisticExpiry(Duration.ofMinutes(5))
                    .sla(buckets)
                    .minimumExpectedValue(Duration.ofMillis(30))
                    .maximumExpectedValue(Duration.ofSeconds(3)).register(meterRegistry);
            timerMap.put(name, timer);
        }

        return timerMap.get(name);
    }

    public Counter counter(String name) {
        if (!counterMap.containsKey(name)) {
            Counter counter = Counter.builder(name)
                    .register(meterRegistry);
            counterMap.put(name, counter);
        }
        return counterMap.get(name);
    }


    public static Duration[] generateBucket() {
        List<Duration> durations = new ArrayList<>();
        for (int i = 1000; i <= 3000;) {
            durations.add(Duration.ofMillis(i));
            if (i < 2000) {
                i += 20;
            } else {
                i += 50;
            }
        }
        Duration[] rt = new Duration[durations.size()];
        durations.toArray(rt);
        return rt;
    }

    public Gauge gauge(String name) {
        if (!gaugeMap.containsKey(name)) {
            AtomicInteger atomicInteger = new AtomicInteger();
            Gauge gauge = Gauge.builder(name, atomicInteger, AtomicInteger::get)
//                    .tag("gauge", "gauge")
//                    .description("gauge")
                    .register(meterRegistry);
            gaugeMap.put(name, gauge);
        }

//        Gauge.builder();
        return gaugeMap.get(name);
    }

}
