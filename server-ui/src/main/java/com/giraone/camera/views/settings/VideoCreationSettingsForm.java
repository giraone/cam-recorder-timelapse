package com.giraone.camera.views.settings;

import com.giraone.camera.service.FileViewService;
import com.giraone.camera.service.api.VideoCreationSettings;
import com.giraone.camera.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.SpringComponent;
import jakarta.annotation.security.PermitAll;
import org.springframework.context.annotation.Scope;

@SpringComponent
@Scope("prototype") // prototype scope generates a fresh object for every method call or dependency injection
@PermitAll
@Route(value = "video-creation-settings", layout = MainLayout.class)
@PageTitle("Video Creation Settings | Cam Recorder")
public class VideoCreationSettingsForm extends FormLayout {

    IntegerField moduloSelectImage = new IntegerField("Modulo select of images (1 = Use every image, 2 = use every 2nd image)");
    IntegerField frameRate = new IntegerField("Frame rate (5-60) - Normal = 25 or 30 fps");

    Button save = new Button("Save");
    Button close = new Button("Cancel");

    Binder<VideoCreationSettings> binder = new BeanValidationBinder<>(VideoCreationSettings.class);
    FileViewService fileViewService;

    public VideoCreationSettingsForm(FileViewService fileViewService) {

        this.fileViewService = fileViewService;

        addClassName("video-creation-settings-form");
        binder.bindInstanceFields(this);

        moduloSelectImage.setMin(1);
        moduloSelectImage.setMax(64);
        moduloSelectImage.setStepButtonsVisible(true);

        frameRate.setMin(5);
        frameRate.setMax(60);
        frameRate.setStepButtonsVisible(true);
        Div fpsSuffix = new Div();
        fpsSuffix.setText("fps");
        frameRate.setSuffixComponent(fpsSuffix);

        FormLayout formLayout = new FormLayout();
        formLayout.setMinWidth(98, Unit.PERCENTAGE);
        formLayout.add(
            new Paragraph("Settings for video creation from multiple images"),
            moduloSelectImage, frameRate,
            createButtonsLayout()
        );
        formLayout.setResponsiveSteps(
            // Use one column by default
            new ResponsiveStep("0", 1)
        );

        add(formLayout);
        addSaveListener(this::saveVideoCreationSettings);
        binder.setBean(VideoCreationSettings.getCurrent());
    }
    
    private void saveVideoCreationSettings(VideoCreationSettingsForm.SaveEvent event) {
        VideoCreationSettings.setCurrent(event.geVideoCreationSettings());
    }

    private Component createButtonsLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        save.addClickShortcut(Key.ENTER);
        close.addClickShortcut(Key.ESCAPE);

        save.addClickListener(_ -> validateAndSave());
        close.addClickListener(_ -> fireEvent(new CloseEvent(this)));

        binder.addStatusChangeListener(e -> save.setEnabled(binder.isValid()));
        return new HorizontalLayout(save, close);
    }

    //------------------------------------------------------------------------------------------------------------------

    // Events
    public static abstract class VideoCreationSettingsFormEvent extends ComponentEvent<VideoCreationSettingsForm> {
        private final VideoCreationSettings VideoCreationSettings;

        protected VideoCreationSettingsFormEvent(VideoCreationSettingsForm source, VideoCreationSettings VideoCreationSettings) {
            super(source, false);
            this.VideoCreationSettings = VideoCreationSettings;
        }

        public VideoCreationSettings geVideoCreationSettings() {
            return VideoCreationSettings;
        }
    }

    public static class SaveEvent extends VideoCreationSettingsFormEvent {
        SaveEvent(VideoCreationSettingsForm source, VideoCreationSettings videoCreationSettings) {
            super(source, videoCreationSettings);
        }
    }

    public static class CloseEvent extends VideoCreationSettingsFormEvent {
        CloseEvent(VideoCreationSettingsForm source) {
            super(source, null);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }

    private void validateAndSave() {
        if (binder.isValid()) {
            fireEvent(new SaveEvent(this, binder.getBean()));
        }
    }
}

