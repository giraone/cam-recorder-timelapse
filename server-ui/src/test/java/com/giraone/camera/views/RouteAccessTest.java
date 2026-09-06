package com.giraone.camera.views;

import com.giraone.camera.views.images.ImagesView;
import com.giraone.camera.views.settings.CameraSettingsForm;
import com.giraone.camera.views.settings.VideoCreationSettingsForm;
import com.giraone.camera.views.settings.WorkflowSettingsForm;
import com.giraone.camera.views.status.CameraStatusView;
import com.giraone.camera.views.videos.VideosView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vaadin 25 applies the access annotations of the parent layouts too, not only the one of the view itself.
 * A view without an accessible parent layout fails at navigation time with
 * "Denied access to view '...' due to parent layout '...' access rules", so the rules are asserted here.
 */
class RouteAccessTest {

    private static final Principal AUTHENTICATED_USER = () -> "boss";

    private final AccessAnnotationChecker accessAnnotationChecker = new AccessAnnotationChecker();

    @ParameterizedTest
    @ValueSource(classes = {
        MainLayout.class,
        ImagesView.class,
        VideosView.class,
        CameraSettingsForm.class,
        WorkflowSettingsForm.class,
        VideoCreationSettingsForm.class,
        CameraStatusView.class
    })
    void authenticatedUserHasAccess(Class<? extends Component> target) {
        assertThat(accessAnnotationChecker.hasAccess(target, AUTHENTICATED_USER, role -> true)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(classes = {
        MainLayout.class,
        ImagesView.class,
        VideosView.class,
        CameraSettingsForm.class,
        WorkflowSettingsForm.class,
        VideoCreationSettingsForm.class,
        CameraStatusView.class
    })
    void anonymousUserHasNoAccess(Class<? extends Component> target) {
        assertThat(accessAnnotationChecker.hasAccess(target, null, role -> false)).isFalse();
    }

    @Test
    void loginViewIsAnonymouslyAccessible() {
        assertThat(accessAnnotationChecker.hasAccess(LoginView.class, null, role -> false)).isTrue();
    }
}
