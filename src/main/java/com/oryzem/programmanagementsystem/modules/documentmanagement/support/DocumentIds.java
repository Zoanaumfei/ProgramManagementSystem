package com.oryzem.programmanagementsystem.modules.documentmanagement.support;

import java.util.UUID;

public final class DocumentIds {

    private DocumentIds() {
    }

    public static String newDocumentId() {
        return newId("DOC");
    }

    public static String newBindingId() {
        return newId("DBN");
    }

    private static String newId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }
}
