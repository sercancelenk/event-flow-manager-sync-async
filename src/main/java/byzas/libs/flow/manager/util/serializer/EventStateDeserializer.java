package byzas.libs.flow.manager.util.serializer;

import byzas.libs.flow.manager.async.model.event.EventStateDto;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import static java.util.Optional.ofNullable;

public class EventStateDeserializer extends StdDeserializer<EventStateDto> {

    private static final String EVENT_NAME_ATTRIBUTE = "eventName";
    private static final String STEP_ATTRIBUTE = "step";
    private static final String EVENT_ID_ATTRIBUTE = "eventId";
    private static final String RETRY_COUNT_ATTRIBUTE = "retryCount";
    private static final String PROCESSED_ATTRIBUTE = "processed";

    protected EventStateDeserializer() {
        this(null);
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
    }

    private EventStateDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public EventStateDto deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
        try {
            ObjectCodec oc = jsonParser.getCodec();
            JsonNode root = oc.readTree(jsonParser);
            String eventName = root.get(EVENT_NAME_ATTRIBUTE).asText();
            int step = root.get(STEP_ATTRIBUTE).intValue();
            String eventId = root.get(EVENT_ID_ATTRIBUTE).asText();
            int retryCount = ofNullable(root.get(RETRY_COUNT_ATTRIBUTE)).map(JsonNode::asInt).orElse(0);
            boolean processed = ofNullable(root.get(PROCESSED_ATTRIBUTE)).map(JsonNode::asBoolean).orElse(false);
            return EventStateDto.builder().eventName(eventName).step(step).eventId(eventId).retryCount(retryCount).processed(processed).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}