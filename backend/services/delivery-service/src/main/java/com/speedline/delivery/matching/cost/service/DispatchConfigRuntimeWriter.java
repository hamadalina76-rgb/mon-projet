package com.speedline.delivery.matching.cost.service;

/**
 * DISP-205: atomic Redis publication for grouped dispatch configuration.
 */
public interface DispatchConfigRuntimeWriter {

    String KEY_CONFIG_VERSION = "dispatch:config:version";
    String KEY_COST_COMPONENTS = "dispatch:cost:components";
    String KEY_GENERAL = "dispatch:config:general";
    String KEY_INTERNAL_EXTERNAL = "dispatch:config:internal-external";
    String KEY_BUNDLING = "dispatch:config:bundling";
    String KEY_EXCLUSIVITY = "dispatch:config:exclusivity";

    void publishAtomic(long version, String componentsJson, String generalJson,
                       String internalExternalJson, String bundlingJson, String exclusivityJson);

    long readPublishedVersion();
}
