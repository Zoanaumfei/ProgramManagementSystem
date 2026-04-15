package com.oryzem.programmanagementsystem.modules.projectmanagement.support;

import java.util.Locale;
import java.util.UUID;

public final class ProjectIds {

    private ProjectIds() {
    }

    public static String newProjectId() {
        return newId("PRJ");
    }

    public static String newProjectTemplateId() {
        return newId("TMP");
    }

    public static String newProjectOrganizationId() {
        return newId("PRJORG");
    }

    public static String newProjectMemberId() {
        return newId("PRJMBR");
    }

    public static String newProjectPhaseId() {
        return newId("PRJPH");
    }

    public static String newProjectMilestoneId() {
        return newId("PRJMS");
    }

    public static String newProjectPhaseTemplateId() {
        return newId("PHT");
    }

    public static String newProjectMilestoneTemplateId() {
        return newId("PMT");
    }

    public static String newProjectStructureTemplateId() {
        return newId("PST");
    }

    public static String newProjectStructureLevelTemplateId() {
        return newId("PSLT");
    }

    public static String rootProjectStructureNodeId(String projectId) {
        return projectId + "-ROOT";
    }

    public static String newProjectStructureNodeId() {
        return newId("PRJSN");
    }

    public static String newDeliverableId() {
        return newId("PRJDLV");
    }

    public static String newDeliverableTemplateId() {
        return newId("DT");
    }

    public static String newSubmissionId() {
        return newId("PRJSUB");
    }

    public static String newSubmissionDocumentId() {
        return newId("PRJSDOC");
    }

    private static String newId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }
}
