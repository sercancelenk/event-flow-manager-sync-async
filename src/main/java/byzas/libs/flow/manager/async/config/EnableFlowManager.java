package byzas.libs.flow.manager.async.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(EnableFlowManagerConfiguration.class)
public @interface EnableFlowManager {
}
