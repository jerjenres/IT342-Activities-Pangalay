package com.pangalay.OAuth2Login.Controller;

import com.google.api.services.people.v1.model.Person;
import com.pangalay.OAuth2Login.Service.GoogleContactsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contacts")
public class ContactApiController {

    private final GoogleContactsService contactsService;

    public ContactApiController(GoogleContactsService contactsService) {
        this.contactsService = contactsService;
    }

    @GetMapping
    public ResponseEntity<List<Person>> getAllContacts(OAuth2AuthenticationToken authentication) {
        try {
            List<Person> contacts = contactsService.getContacts(authentication);
            return ResponseEntity.ok(contacts);
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    public ResponseEntity<Person> addContact(
            OAuth2AuthenticationToken authentication,
            @RequestBody Map<String, String> contactDetails) {
        try {
            Person newContact = contactsService.createContact(
                authentication,
                contactDetails.get("givenName"),
                contactDetails.get("familyName"),
                contactDetails.get("emailAddress"),
                contactDetails.get("phoneNumber")
            );
            return ResponseEntity.ok(newContact);
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{resourceName}")
    public ResponseEntity<Person> updateContact(
            OAuth2AuthenticationToken authentication,
            @PathVariable String resourceName,
            @RequestBody Map<String, String> contactDetails) {
        try {
            Person updatedContact = contactsService.updateContact(
                authentication,
                resourceName,
                contactDetails.get("givenName"),
                contactDetails.get("familyName"),
                contactDetails.get("emailAddress"),
                contactDetails.get("phoneNumber")
            );
            return ResponseEntity.ok(updatedContact);
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{resourceName}")
    public ResponseEntity<Void> deleteContact(
            OAuth2AuthenticationToken authentication,
            @PathVariable String resourceName) {
        try {
            contactsService.deleteContact(authentication, resourceName);
            return ResponseEntity.ok().build();
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}