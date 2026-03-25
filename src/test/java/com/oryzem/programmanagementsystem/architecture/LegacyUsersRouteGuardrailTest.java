package com.oryzem.programmanagementsystem.architecture;

import com.oryzem.programmanagementsystem.platform.users.api.UserManagementController;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyUsersRouteGuardrailTest {

    @Test
    void onlyTheExistingLegacyControllerMayMapRoutesUnderApiUsers() throws ClassNotFoundException {
        List<Class<?>> controllers = discoverControllers();
        List<String> violatingMappings = new ArrayList<>();

        for (Class<?> controllerClass : controllers) {
            if (!isController(controllerClass)) {
                continue;
            }
            String[] classMappings = requestMappings(controllerClass);
            for (Method method : controllerClass.getDeclaredMethods()) {
                for (String methodMapping : requestMappings(method)) {
                    for (String classMapping : classMappings.length == 0 ? new String[] {""} : classMappings) {
                        String fullPath = normalizePath(classMapping, methodMapping);
                        if (fullPath.startsWith("/api/users")
                                && !controllerClass.equals(UserManagementController.class)) {
                            violatingMappings.add(controllerClass.getName() + "#" + method.getName() + " -> " + fullPath);
                        }
                    }
                }
            }
        }

        assertThat(violatingMappings)
                .as("new backend routes must not be added on top of the legacy /api/users surface")
                .isEmpty();
    }

    private List<Class<?>> discoverControllers() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));

        List<Class<?>> controllers = new ArrayList<>();
        for (var candidate : scanner.findCandidateComponents("com.oryzem.programmanagementsystem")) {
            controllers.add(Class.forName(candidate.getBeanClassName()));
        }
        return controllers;
    }

    private boolean isController(Class<?> type) {
        return AnnotatedElementUtils.hasAnnotation(type, RestController.class)
                || AnnotatedElementUtils.hasAnnotation(type, Controller.class);
    }

    private String[] requestMappings(Class<?> type) {
        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(type, RequestMapping.class);
        if (requestMapping == null) {
            return new String[0];
        }
        List<String> mappings = values(requestMapping.value(), requestMapping.path());
        return mappings.toArray(String[]::new);
    }

    private String[] requestMappings(Method method) {
        List<String> mappings = new ArrayList<>();
        mappings.addAll(annotationValues(method, RequestMapping.class));
        mappings.addAll(annotationValues(method, GetMapping.class));
        mappings.addAll(annotationValues(method, PostMapping.class));
        mappings.addAll(annotationValues(method, PutMapping.class));
        mappings.addAll(annotationValues(method, DeleteMapping.class));
        return mappings.isEmpty() ? new String[] {""} : mappings.toArray(String[]::new);
    }

    private <A extends Annotation> List<String> annotationValues(Method method, Class<A> annotationType) {
        A annotation = AnnotatedElementUtils.findMergedAnnotation(method, annotationType);
        if (annotation == null) {
            return List.of();
        }
        if (annotation instanceof RequestMapping requestMapping) {
            return values(requestMapping.value(), requestMapping.path());
        }
        if (annotation instanceof GetMapping getMapping) {
            return values(getMapping.value(), getMapping.path());
        }
        if (annotation instanceof PostMapping postMapping) {
            return values(postMapping.value(), postMapping.path());
        }
        if (annotation instanceof PutMapping putMapping) {
            return values(putMapping.value(), putMapping.path());
        }
        if (annotation instanceof DeleteMapping deleteMapping) {
            return values(deleteMapping.value(), deleteMapping.path());
        }
        return List.of();
    }

    private List<String> values(String[] value, String[] path) {
        if (value.length > 0) {
            return Arrays.asList(value);
        }
        if (path.length > 0) {
            return Arrays.asList(path);
        }
        return List.of("");
    }

    private String normalizePath(String classMapping, String methodMapping) {
        String left = classMapping == null ? "" : classMapping.trim();
        String right = methodMapping == null ? "" : methodMapping.trim();
        if (left.endsWith("/") && right.startsWith("/")) {
            return left.substring(0, left.length() - 1) + right;
        }
        if (!left.endsWith("/") && !right.isBlank() && !right.startsWith("/")) {
            return left + "/" + right;
        }
        return left + right;
    }
}
