package byzas.libs.flow.manager.util.serializer;

import byzas.libs.flow.manager.async.config.kafka.props.EventsConfig;
import byzas.libs.flow.manager.async.config.kafka.props.Step;
import byzas.libs.flow.manager.async.model.event.EventDto;
import byzas.libs.flow.manager.util.SpringContext;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Optional.ofNullable;

@Slf4j
public class EventDeserializer<T> extends StdDeserializer<EventDto<T>> {

    private static final String NAME_ATTRIBUTE = "name";
    private static final String DATA_ATTRIBUTE = "data";
    private static final String STEP_ATTRIBUTE = "step";
    private static final String FLOW_ID_ATTRIBUTE = "flowId";
    private static final String ID_ATTRIBUTE = "id";
    private static final String WEB_CONTEXT_ATTRIBUTE = "webContext";
    private static final String TRANSACTION_ID = "transactionId";

    protected EventDeserializer() {
        this(null);
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
    }

    private EventDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public EventDto<T> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
        try {
            ObjectCodec oc = jsonParser.getCodec();
            JsonNode root = oc.readTree(jsonParser);
            log.debug("EVENT-DESERIALIZER : {}", root.toPrettyString());
            ObjectMapper mapper = new ObjectMapper();
            String eventName = root.get(NAME_ATTRIBUTE).asText();
            int step = ofNullable(root.get(STEP_ATTRIBUTE)).map(JsonNode::intValue).orElse(0);
            String id = ofNullable(root.get(ID_ATTRIBUTE)).map(JsonNode::asText).orElse(UUID.randomUUID().toString());
            String dataJson = root.get(DATA_ATTRIBUTE).toString();
            String transactionId = root.get(TRANSACTION_ID).toString();
            Long flowId = ofNullable(root.get(FLOW_ID_ATTRIBUTE)).map(JsonNode::longValue).orElse(null);
            Optional<JsonNode> maybeWebContextJsonNode = ofNullable(root.get(WEB_CONTEXT_ATTRIBUTE));

            Map<Integer, Step> steps = ofNullable(((EventsConfig) SpringContext.applicationContext.getBean("eventsConfig")).getEvents().get(eventName))
                    .orElseThrow(() -> new RuntimeException(String.format("Event not found : %s", eventName)));
            Class clazz = Class.forName(steps.get(step).getConsumer().getDataClass());
            T eventData = (T) mapper.readValue(dataJson, clazz);

            return EventDto.<T>builder().data(eventData).name(eventName).step(step).id(id)
                    .transactionId(transactionId)
                    .flowId(flowId).build();
        } catch (Exception e) {
            log.error("Exception when deserializing event class", e);
            throw new RuntimeException(e);
        }
    }
}