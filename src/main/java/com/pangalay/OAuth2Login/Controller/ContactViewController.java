package com.pangalay.OAuth2Login.Controller;

import com.google.api.services.people.v1.model.Person;
import com.pangalay.OAuth2Login.Service.GoogleContactsService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Controller
public class ContactViewController {

    private final GoogleContactsService contactsService;

    public ContactViewController(GoogleContactsService contactsService) {
        this.contactsService = contactsService;
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/contacts")
    public String contacts(OAuth2AuthenticationToken authentication, Model model) {
        try {
            List<Person> contacts = contactsService.getContacts(authentication);
            model.addAttribute("contacts", contacts);
            return "contacts";
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to fetch contacts: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/contacts/new")
    public String newContactForm() {
        return "new-contact";
    }

    @PostMapping("/contacts/new")
    public String createContact(
            OAuth2AuthenticationToken authentication,
            @RequestParam String givenName,
            @RequestParam String familyName,
            @RequestParam(required = false) String emailAddress,
            @RequestParam(required = false) String phoneNumber) {
        try {
            contactsService.createContact(authentication, givenName, familyName, emailAddress, phoneNumber);
            return "redirect:/contacts";
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
            return "redirect:/error?message=" + e.getMessage();
        }
    }

    @GetMapping("/contacts/edit")
    public String editContactForm(@RequestParam String resourceName, Model model,
            OAuth2AuthenticationToken authentication) {
        try {
            // Fetch the specific contact by resourceName
            Person contact = contactsService.getContact(authentication, resourceName);
            model.addAttribute("contact", contact);
            model.addAttribute("resourceName", resourceName);
            return "edit-contact";
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to fetch contact: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/contacts/edit")
    public String updateContact(
            OAuth2AuthenticationToken authentication,
            @RequestParam String resourceName,
            @RequestParam String givenName,
            @RequestParam String familyName,
            @RequestParam(required = false) String emailAddress,
            @RequestParam(required = false) String phoneNumber,
            Model model) {
        try {
            contactsService.updateContact(authentication, resourceName, givenName, familyName, emailAddress,
                    phoneNumber);
            return "redirect:/contacts";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to update contact: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/contacts/delete")
    public String deleteContact(
            OAuth2AuthenticationToken authentication,
            @RequestParam String resourceName) {
        try {
            contactsService.deleteContact(authentication, resourceName);
            return "redirect:/contacts";
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
            return "redirect:/error?message=" + e.getMessage();
        }
    }
}