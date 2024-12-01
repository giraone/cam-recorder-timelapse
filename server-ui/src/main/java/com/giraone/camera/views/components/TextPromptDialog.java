package com.giraone.camera.views.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.util.function.Consumer;

@Route("text-prompt-dialog")
public class TextPromptDialog extends Dialog {

    private TextField textField;

    public TextPromptDialog(String title, String label, String initialValue, String placeHolder, Consumer<String> consumer) {
        super();
        setHeaderTitle(title);
        VerticalLayout dialogLayout = createDialogLayout(label, initialValue, placeHolder);
        add(dialogLayout);
        Button saveButton = createSaveButton(consumer);
        Button cancelButton = new Button("Cancel", e -> {
            consumer.accept(null);
            close();
        });
        getFooter().add(cancelButton);
        getFooter().add(saveButton);
    }

    public void open() {
        super.open();
        // Center the button within the example
        //        getStyle().set("position", "fixed").set("top", "0").set("right", "0")
        //            .set("bottom", "0").set("left", "0").set("display", "flex")
        //            .set("align-items", "center").set("justify-content", "center");
    }

    private VerticalLayout createDialogLayout(String label, String initialValue, String placeHolder) {
        textField = new TextField(label, initialValue, placeHolder);
        VerticalLayout dialogLayout = new VerticalLayout(textField);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "18rem").set("max-width", "100%");
        return dialogLayout;
    }

    private Button createSaveButton(Consumer<String> consumer) {
        Button saveButton = new Button("OK", e -> {
            consumer.accept(textField.getValue());
            close();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return saveButton;
    }
}
