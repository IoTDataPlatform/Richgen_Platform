package iot.data.platform.richgen;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RichSourceConnector extends SourceConnector {

    public static final String CFG_NAME = "rich.name";
    public static final String CFG_TRACK_TOPIC = "track.topic";
    public static final String CFG_HIT_TOPIC = "hit.topic";

    private Map<String, String> configProps;

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public void start(Map<String, String> props) {
        this.configProps = props;
    }

    @Override
    public Class<? extends Task> taskClass() {
        return RichSourceTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        return List.of(new HashMap<>(configProps));
    }

    @Override
    public void stop() {
    }

    @Override
    public ConfigDef config() {
        return new ConfigDef()
                .define(
                        CFG_NAME,
                        ConfigDef.Type.STRING,
                        "rich-simulator",
                        ConfigDef.Importance.LOW,
                        "Logical source name"
                )
                .define(
                        CFG_TRACK_TOPIC,
                        ConfigDef.Type.STRING,
                        ConfigDef.Importance.HIGH,
                        "Kafka topic for tracks"
                )
                .define(
                        CFG_HIT_TOPIC,
                        ConfigDef.Type.STRING,
                        ConfigDef.Importance.HIGH,
                        "Kafka topic for hits"
                );
    }
}
