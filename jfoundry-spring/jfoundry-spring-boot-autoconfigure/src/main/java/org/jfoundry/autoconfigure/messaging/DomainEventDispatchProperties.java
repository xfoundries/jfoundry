package org.jfoundry.autoconfigure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jfoundry.domain.event")
public class DomainEventDispatchProperties {

    /**
     * Enables automatic domain event dispatch at application-service boundaries.
     */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
