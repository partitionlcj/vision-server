package co.mega.vs.bean;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;

public interface IMicrometerService {

    Timer time(String name);

    Counter counter(String name);

    Gauge gauge(String name);

}
