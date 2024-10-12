package com.giraone.streaming.views.contacts;

import com.giraone.streaming.data.Contact;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.ListDataProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ContactsViewTest {

    static {
        // Prevent Vaadin Development mode to launch browser window
        System.setProperty("vaadin.launch-browser", "false");
    }

    @Autowired
    private ContactsView contactsView;

    @Test
    public void formShownWhenContactSelected() {
        Grid<Contact> grid = contactsView.grid;
        Contact firstContact = getFirstItem(grid);

        ContactForm form = contactsView.form;

        assertFalse(form.isVisible());
        grid.asSingleSelect().setValue(firstContact);
        assertTrue(form.isVisible());
        assertEquals(firstContact.getFirstName(), form.firstName.getValue());
    }

    private Contact getFirstItem(Grid<Contact> grid) {
        return ((ListDataProvider<Contact>) grid.getDataProvider()).getItems().iterator().next();
    }
}
