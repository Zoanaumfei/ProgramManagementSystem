package com.oryzem.programmanagementsystem.app.config;

import java.util.Map;

interface RdsSecretProvider {

    Map<String, Object> load(String secretId, String region);
}
