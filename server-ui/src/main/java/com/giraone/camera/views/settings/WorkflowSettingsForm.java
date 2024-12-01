package com.giraone.camera.views.settings;

import com.giraone.camera.service.FileViewService;
import com.giraone.camera.service.api.Settings;
import com.giraone.camera.service.api.WorkflowSettings;
import com.giraone.camera.service.model.Status;
import com.giraone.camera.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
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

import java.time.Duration;

@SpringComponent
@Scope("prototype")
@PermitAll
@Route(value = "workflow-settings", layout = MainLayout.class)
@PageTitle("Workflow Settings | Cam Recorder")
public class WorkflowSettingsForm extends FormLayout {

    Checkbox restart = new Checkbox("Restart");
    Checkbox pause = new Checkbox("Pause");
    IntegerField delayMs = new IntegerField("Delay (ms) till next action");

    Checkbox blinkOnSuccess = new Checkbox("Blink on success");
    Checkbox blinkOnFailure = new Checkbox("Blink on failure");
    
    Checkbox flashLedForPicture = new Checkbox("Flash LED");
    IntegerField flashDurationMs = new IntegerField("Flash Duration");

    Button save = new Button("Save");
    Button close = new Button("Cancel");

    Binder<WorkflowSettings> binder = new BeanValidationBinder<>(WorkflowSettings.class);
    Settings settings;
    FileViewService fileViewService;

    public WorkflowSettingsForm(FileViewService fileViewService) {

        this.fileViewService = fileViewService;

        addClassName("camera-settings-form");
        binder.bindInstanceFields(this);

        restart.setHelperText("Restart after next image upload.");
        pause.setHelperText("Device/camera will be paused.");
        delayMs.setMin(100);
        delayMs.setMax(3_600_000);
        Div secondsSuffix = new Div();
        secondsSuffix.setText("ms");
        delayMs.setSuffixComponent(secondsSuffix);

        flashDurationMs.setMin(1);
        flashDurationMs.setMax(3600);
        Div milliSecondsSuffix = new Div();
        milliSecondsSuffix.setText("milliseconds");
        flashDurationMs.setSuffixComponent(milliSecondsSuffix);

        FormLayout formLayout = new FormLayout();
        formLayout.setMinWidth(98, Unit.PERCENTAGE);
        formLayout.add(
            new Paragraph("Actions"), restart, pause,
            new Paragraph("LED"), blinkOnSuccess, blinkOnFailure,
            new Paragraph("Flash"), flashLedForPicture, flashDurationMs,
            createButtonsLayout());
        formLayout.setResponsiveSteps(
            // Use one column by default
            new ResponsiveStep("0", 1),
            // Use 3 columns, if the layout's width exceeds 640px
            new ResponsiveStep("640px", 3));

        add(formLayout);
        addSaveListener(this::saveWorkflowSettings);
        settings = fileViewService.loadSettings().block(Duration.ofSeconds(5));
        binder.setBean(settings.getWorkflow());
    }
    
    private void saveWorkflowSettings(WorkflowSettingsForm.SaveEvent event) {
        Status status = fileViewService.storeSettings(settings).block(Duration.ofSeconds(5));
        if (status.success()) {
            Notification.show("Saved!");
        } else {
            Notification.show("Cannot save settings: " + status.error());
        }
    }

    private Component createButtonsLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        save.addClickShortcut(Key.ENTER);
        close.addClickShortcut(Key.ESCAPE);

        save.addClickListener(event -> validateAndSave());
        close.addClickListener(event -> fireEvent(new CloseEvent(this)));

        binder.addStatusChangeListener(e -> save.setEnabled(binder.isValid()));
        return new HorizontalLayout(save, close);
    }

    //------------------------------------------------------------------------------------------------------------------

    // Events
    public static abstract class WorkflowSettingsFormEvent extends ComponentEvent<WorkflowSettingsForm> {
        private final WorkflowSettings workflowSettings;

        protected WorkflowSettingsFormEvent(WorkflowSettingsForm source, WorkflowSettings workflowSettings) {
            super(source, false);
            this.workflowSettings = workflowSettings;
        }

        public WorkflowSettings geWorkflowSettings() {
            return workflowSettings;
        }
    }

    public static class SaveEvent extends WorkflowSettingsFormEvent {
        SaveEvent(WorkflowSettingsForm source, WorkflowSettings WorkflowSettings) {
            super(source, WorkflowSettings);
        }
    }

    public static class CloseEvent extends WorkflowSettingsFormEvent {
        CloseEvent(WorkflowSettingsForm source) {
            super(source, null);
        }
    }

    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }

    private void validateAndSave() {
        if (binder.isValid()) {
            fireEvent(new SaveEvent(this, binder.getBean()));
        }
    }
}

