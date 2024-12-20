package com.giraone.camera.views.settings;

import com.giraone.camera.service.FileViewService;
import com.giraone.camera.service.api.CameraSettings;
import com.giraone.camera.service.api.Settings;
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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
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
@Route(value = "camera-settings", layout = MainLayout.class)
@PageTitle("Camera Settings | Cam Recorder")
public class CameraSettingsForm extends FormLayout {

    IntegerField clockFrequencyHz = new IntegerField("Clock Frequency Hz");
    ComboBox<CameraSettings.FrameSize> frameSize = new ComboBox<>("Frame Size (Resolution)", CameraSettings.FrameSize.ALL);
    IntegerField jpegQuality = new IntegerField("JPEG Quality (0 = best, 63 = worst), for QXGS > 8, QHD >= 12");

    Checkbox blackPixelCorrect = new Checkbox("Black Pixel Correct");
    Checkbox whitePixelCorrect = new Checkbox("White Pixel Correct");
    Checkbox gammaCorrect = new Checkbox("Gamma Correct");
    Checkbox lensCorrect = new Checkbox("Lens Correct");

    Checkbox horizontalMirror = new Checkbox("Horizontal Mirror");
    Checkbox verticalFlip = new Checkbox("Vertical Flip");

    ComboBox<CameraSettings.Level> brightness = new ComboBox<>("Brightness", CameraSettings.Level.ALL);
    ComboBox<CameraSettings.Level> contrast = new ComboBox<>("Contrast", CameraSettings.Level.ALL);
    ComboBox<CameraSettings.Level> sharpness = new ComboBox<>("Sharpness", CameraSettings.Level.ALL);
    ComboBox<CameraSettings.Level> saturation = new ComboBox<>("Saturation", CameraSettings.Level.ALL);
    ComboBox<CameraSettings.Level> denoise = new ComboBox<>("Denoise", CameraSettings.Level.ALL);
    ComboBox<CameraSettings.SpecialEffect> specialEffect = new ComboBox<>("Special Effect", CameraSettings.SpecialEffect.ALL);

    Checkbox autoWhitebalance = new Checkbox("Auto Whitebalance");
    Checkbox autoWhitebalanceGain = new Checkbox("Auto Whitebalance Gain");
    ComboBox<CameraSettings.WhiteBalanceMode> whitebalanceMode = new ComboBox<>("Whitebalance Mode", CameraSettings.WhiteBalanceMode.ALL);

    Checkbox exposureCtrlSensor = new Checkbox("Exposure Control using Sensor");
    Checkbox exposureCtrlDsp = new Checkbox("Exposure Control using DSP");
    ComboBox<CameraSettings.Level> autoExposureLevel = new ComboBox<>("Auto Exposure Level", CameraSettings.Level.ALL);
    IntegerField autoExposureValue = new IntegerField("Auto Exposure Value");
    Checkbox autoExposureGainControl = new Checkbox("Exposure Gain, when peformed by sensor");
    IntegerField autoExposureGainValue = new IntegerField("Auto Exposure Gain Value");
    IntegerField autoExposureGainCeiling = new IntegerField("Auto Exposure Gain Ceiling");

    Button save = new Button("Save");
    Button close = new Button("Cancel");

    Binder<CameraSettings> binder = new BeanValidationBinder<>(CameraSettings.class);
    Settings settings;

    FileViewService fileViewService;

    public CameraSettingsForm(FileViewService fileViewService) {

        this.fileViewService = fileViewService;

        addClassName("camera-settings-form");
        binder.bindInstanceFields(this);

        clockFrequencyHz.setHelperText("Either 16000000 or 20000000");
        frameSize.setItemLabelGenerator(CameraSettings.FrameSize::getLabel);
        jpegQuality.setMin(0);
        jpegQuality.setMax(63);
        gammaCorrect.setClassName("label-bold");
        gammaCorrect.setHelperText("Absolutely necessary.");
        lensCorrect.setHelperText("Small improvement of quality and brighter images.");
        whitePixelCorrect.setHelperText("Remove white pixels in the image. White pixels occur more often, than block pixels.");
        blackPixelCorrect.setHelperText("Remove black pixels in the image. Black pixels occur not so often, than white pixels.");

        brightness.setItemLabelGenerator(Enum::name);
        contrast.setItemLabelGenerator(Enum::name);
        sharpness.setItemLabelGenerator(Enum::name);
        saturation.setItemLabelGenerator(Enum::name);
        denoise.setItemLabelGenerator(Enum::name);

        whitebalanceMode.setItemLabelGenerator(Enum::name);

        autoExposureLevel.setItemLabelGenerator(Enum::name);
        autoExposureLevel.setHelperText("(no real influence)");
        autoExposureValue.setMin(0);
        autoExposureValue.setMax(1023);
        autoExposureValue.setHelperText("0 to 1023");
        autoExposureValue.setStepButtonsVisible(true);
        autoExposureGainValue.setMin(0);
        autoExposureGainValue.setMax(30);
        autoExposureGainValue.setHelperText("0 to 30");
        autoExposureGainValue.setStepButtonsVisible(true);
        autoExposureGainCeiling.setMin(0);
        autoExposureGainCeiling.setMax(6);
        autoExposureGainCeiling.setHelperText("0=2x, 1=4x, 2=8x, 3=16x, 4=32x, 5=64x, 6=128x");
        autoExposureGainCeiling.setStepButtonsVisible(true);

        FormLayout formLayout = new FormLayout();
        formLayout.setMinWidth(98, Unit.PERCENTAGE);
        formLayout.add(
            new Paragraph("Clock"), clockFrequencyHz, new Hr(),
            new Paragraph("Image Size and Quality"), frameSize, jpegQuality,
            new Hr(), new Hr(), new Hr(),
            new Paragraph("Pixel Correction"), blackPixelCorrect, whitePixelCorrect,
            new Paragraph("Image Correction"), gammaCorrect, lensCorrect,
            new Paragraph("Mirror/Flip"), horizontalMirror, verticalFlip,
            new Paragraph("Brightness/Contrast"), brightness, contrast,
            new Paragraph("Image Enhancement"), sharpness, saturation,
            new Paragraph("Effects"), denoise, specialEffect,
            new Hr(), new Hr(), new Hr(),
            autoWhitebalance, autoWhitebalanceGain, whitebalanceMode,
            new Hr(), new Hr(), new Hr(),
            new Paragraph("Exposure performed by ..."), exposureCtrlSensor, exposureCtrlDsp,
            new Paragraph("Exposure Control, when performed by DSP"), autoExposureLevel, autoExposureValue,
            autoExposureGainControl, autoExposureGainValue, autoExposureGainCeiling,
            createButtonsLayout());
        formLayout.setResponsiveSteps(
            // Use one column by default
            new ResponsiveStep("0", 1),
            // Use 3 columns, if the layout's width exceeds 640px
            new ResponsiveStep("640px", 3));

        add(formLayout);
        addSaveListener(this::saveCameraSettings);
        settings = fileViewService.loadSettings().block(Duration.ofSeconds(5));
        setCameraSettings(settings.getCamera());
    }

    private void setCameraSettings(CameraSettings cameraSettings) {
        binder.setBean(cameraSettings);
    }

    private void saveCameraSettings(CameraSettingsForm.SaveEvent event) {
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

    private void addTooltip(TextField component, String text) {
        Button button = new Button(new Icon(VaadinIcon.INFO_CIRCLE));
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE,
            ButtonVariant.LUMO_ICON);
        component.setSuffixComponent(button);
        component.setTooltipText(text);
        Tooltip tooltip = component.getTooltip().withManual(true);
        button.addClickListener(event -> {
            tooltip.setOpened(!tooltip.isOpened());
        });
    }

}

