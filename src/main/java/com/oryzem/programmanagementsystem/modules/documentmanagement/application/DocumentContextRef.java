package com.oryzem.programmanagementsystem.modules.documentmanagement.application;

import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;

public record DocumentContextRef(
        DocumentContextType contextType,
        String contextId) {
}
