package com.giraone.streaming.views.settings;

import com.giraone.streaming.service.CameraSettingsService;
import com.giraone.streaming.service.model.CameraSettings;
import com.giraone.streaming.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;

import java.io.IOException;

@SpringComponent
@Scope("prototype")
@PermitAll
@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Camera Settings | Cam Recorder")
public class CameraSettingsForm extends FormLayout {

    private static final Logger LOGGER = LoggerFactory.getLogger(CameraSettingsForm.class);

    IntegerField loopDelaySeconds = new IntegerField("Delay between each picture");
    ComboBox<CameraSettingsService.FrameSize> frameSize = new ComboBox<>("Frame Size (Resolution)",  CameraSettingsService.FrameSize.ALL);
    IntegerField jpegQuality = new IntegerField("JPEG Quality (0=best)");

    Checkbox blackPixelCorrect = new Checkbox("Black Pixel Correct");
    Checkbox whitePixelCorrect = new Checkbox("White Pixel Correct");
    Checkbox gammaCorrect = new Checkbox("Gamma Correct");
    Checkbox lensCorrect = new Checkbox("Lens Correct");

    Checkbox horizontalMirror = new Checkbox("Horizontal Mirror");
    Checkbox verticalFlip = new Checkbox("Vertical Flip");

    ComboBox<CameraSettingsService.Level> brightness = new ComboBox<>("Brightness", CameraSettingsService.Level.ALL);
    ComboBox<CameraSettingsService.Level> contrast = new ComboBox<>("Contrast", CameraSettingsService.Level.ALL);
    ComboBox<CameraSettingsService.Level> sharpness = new ComboBox<>("Sharpness", CameraSettingsService.Level.ALL);
    ComboBox<CameraSettingsService.Level> saturation = new ComboBox<>("Saturation", CameraSettingsService.Level.ALL);
    ComboBox<CameraSettingsService.Level> denoise = new ComboBox<>("Denoise", CameraSettingsService.Level.ALL);
    ComboBox<CameraSettingsService.SpecialEffect> specialEffect = new ComboBox<>("Special Effect",  CameraSettingsService.SpecialEffect.ALL);

    Checkbox autoWhitebalance = new Checkbox("Auto Whitebalance");
    Checkbox autoWhitebalanceGain = new Checkbox("Auto Whitebalance Gain");
    ComboBox<CameraSettingsService.WhiteBalanceMode> whitebalanceMode = new ComboBox<>("Whitebalance Mode",  CameraSettingsService.WhiteBalanceMode.ALL);

    Checkbox exposureCtrlSensor = new Checkbox("Exposure Control using Sensor");
    Checkbox exposureCtrlDsp = new Checkbox("Exposure Control using DSP");
    ComboBox<CameraSettingsService.Level> autoExposureLevel = new ComboBox<>("Auto Exposure Level", CameraSettingsService.Level.ALL);
    IntegerField autoExposureValue = new IntegerField("Auto Exposure Value");
    Checkbox autoExposureGainControl = new Checkbox("Auto Exposure Gain Control");
    IntegerField autoExposureGainValue = new IntegerField("Auto Exposure Gain Value");
    IntegerField autoExposureGainCeiling = new IntegerField("Auto Exposure Gain Ceiling");

    Button save = new Button("Save");
    Button close = new Button("Cancel");

    Binder<CameraSettings> binder = new BeanValidationBinder<>(CameraSettings.class);
    CameraSettingsService cameraSettingsService;

    public CameraSettingsForm(CameraSettingsService cameraSettingsService) {

        this.cameraSettingsService = cameraSettingsService;

        addClassName("camera-settings-form");
        binder.bindInstanceFields(this);

        loopDelaySeconds.setMin(1);
        loopDelaySeconds.setMax(3600);
        Div secondsSuffix = new Div();
        secondsSuffix.setText("seconds");
        loopDelaySeconds.setSuffixComponent(secondsSuffix);
        frameSize.setItemLabelGenerator(Enum::name);
        jpegQuality.setMin(0);
        jpegQuality.setMax(63);

        brightness.setItemLabelGenerator(Enum::name);
        contrast.setItemLabelGenerator(Enum::name);
        sharpness.setItemLabelGenerator(Enum::name);
        saturation.setItemLabelGenerator(Enum::name);
        denoise.setItemLabelGenerator(Enum::name);

        whitebalanceMode.setItemLabelGenerator(Enum::name);

        autoExposureLevel.setItemLabelGenerator(Enum::name);
        autoExposureValue.setMin(0);
        autoExposureValue.setMax(1024);
        autoExposureGainCeiling.setHelperText("0 to 1024");
        autoExposureValue.setStepButtonsVisible(true);
        autoExposureGainValue.setMin(0);
        autoExposureGainValue.setMax(30);
        autoExposureGainCeiling.setHelperText("0 to 30");
        autoExposureGainValue.setStepButtonsVisible(true);
        autoExposureGainCeiling.setMin(0);
        autoExposureGainCeiling.setMax(6);
        autoExposureGainCeiling.setHelperText("0=2x, 1=4x, 2=8x, 3=16x, 4=32x, 5=64x, 6=128x");
        autoExposureGainCeiling.setStepButtonsVisible(true);

        FormLayout formLayout = new FormLayout();
        formLayout.setWidthFull(); // TODO: does not work
        formLayout.add(new Label("Cameras Settings"), loopDelaySeconds,
            frameSize, jpegQuality,
            blackPixelCorrect, whitePixelCorrect,
            gammaCorrect, lensCorrect,
            horizontalMirror, verticalFlip,
            brightness, contrast,
            sharpness, saturation,
            denoise, specialEffect,
            new Label("White Balance"), autoWhitebalance, autoWhitebalanceGain, whitebalanceMode,
            exposureCtrlSensor, exposureCtrlDsp,
            new Label("Exposure"), autoExposureLevel,
            autoExposureValue, autoExposureGainControl,
            autoExposureGainValue, autoExposureGainCeiling,
            createButtonsLayout());
        formLayout.setResponsiveSteps(
            // Use one column by default
            new ResponsiveStep("0", 1),
            // Use 2 columns, if the layout's width exceeds 320px
            new ResponsiveStep("320px", 2),
            // Use 4 columns, if the layout's width exceeds 640px
            new ResponsiveStep("640px", 4));

        add(formLayout);

        addSaveListener(this::saveCameraSettings);
        //addCloseListener(e -> closeEditor());

        setCameraSettings(cameraSettingsService.getSettings());
    }

    private void setCameraSettings(CameraSettings cameraSettings) {
        binder.setBean(cameraSettings);
    }

    private void saveCameraSettings(CameraSettingsForm.SaveEvent event) {
        try {
            cameraSettingsService.storeSetting();
            Notification.show("Saved into \"" + cameraSettingsService.getFile().getAbsolutePath() + "\"");
        } catch (IOException e) {
            LOGGER.warn("Cannot save!", e);
            Notification.show("Cannot save settings: " + e.getMessage());
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
    public static abstract class CameraSettingsFormEvent extends ComponentEvent<CameraSettingsForm> {
        private final CameraSettings cameraSettings;

        protected CameraSettingsFormEvent(CameraSettingsForm source, CameraSettings cameraSettings) {
            super(source, false);
            this.cameraSettings = cameraSettings;
        }

        public CameraSettings getCameraSettings() {
            return cameraSettings;
        }
    }

    public static class SaveEvent extends CameraSettingsFormEvent {
        SaveEvent(CameraSettingsForm source, CameraSettings cameraSettings) {
            super(source, cameraSettings);
        }
    }

    public static class CloseEvent extends CameraSettingsFormEvent {
        CloseEvent(CameraSettingsForm source) {
            super(source, null);
        }
    }

    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }

    public Registration addCloseListener(ComponentEventListener<CloseEvent> listener) {
        return addListener(CloseEvent.class, listener);
    }

    private void validateAndSave() {
        if (binder.isValid()) {
            fireEvent(new SaveEvent(this, binder.getBean()));
        }
    }
}

