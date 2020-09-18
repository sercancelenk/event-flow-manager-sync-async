package byzas.libs.flow.manager.util.extensions;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public interface MapSupport {
    default Map<String, Object> mergeKafkaProps(Map<String, Object> from, Map<String, Object> to) {
        if (from == null) {
            return to;
        }
        Map<Object, String> invertedFromMap = MapUtils.invertMap(from);
        return from.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> {
                            if (invertedFromMap.get(v2).equalsIgnoreCase("spring.json.trusted.packages")) {
                                return StringUtils.join(Arrays.asList(StringUtils.deleteWhitespace(String.valueOf(v2)).trim(),
                                        StringUtils.deleteWhitespace(String.valueOf(v1)).trim()), ",");
                            } else {
                                return v2;
                            }
                        },
                        () -> new HashMap<>(to)));
    }
}