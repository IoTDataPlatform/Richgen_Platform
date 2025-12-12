package iot.data.platform.rich.flink.consumer.consumer;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.streaming.api.functions.co.KeyedCoProcessFunction;
import org.apache.flink.util.Collector;
import iot.data.platform.rich.flink.consumer.rich.RichHit;

public class DeduplicateRichHitsFunction extends KeyedCoProcessFunction<String, RichHit, RichHit, RichHit> {

    private transient ValueState<RichHit> hitState;
    private transient ValueState<Long> timerState;

    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) {
        hitState = getRuntimeContext().getState(new ValueStateDescriptor<>("pendingHitState", RichHit.class));
        timerState = getRuntimeContext().getState(new ValueStateDescriptor<>("timerState", Long.class));
    }

    @Override
    public void processElement1(RichHit hit, Context ctx, Collector<RichHit> out) throws Exception {
        hitState.update(hit);

        Long oldTimer = timerState.value();
        if (oldTimer != null) {
            ctx.timerService().deleteProcessingTimeTimer(oldTimer);
        }

        long timeout = ctx.timerService().currentProcessingTime() + 1000;
        ctx.timerService().registerProcessingTimeTimer(timeout);
        timerState.update(timeout);
    }

    @Override
    public void processElement2(RichHit signal, Context ctx, Collector<RichHit> out) throws Exception {
        hitState.update(signal);
    }

    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<RichHit> out) throws Exception {
        RichHit hitStateVal = hitState.value();
        if (hitStateVal != null) {
            out.collect(hitStateVal);
            hitState.clear();
        }
        timerState.clear();
    }
}
