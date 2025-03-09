package com.pangalay.OAuth2Login.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.ListConnectionsResponse;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PhoneNumber;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleContactsService {

    private final OAuth2AuthorizedClientService authorizedClientService;

    public GoogleContactsService(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    private PeopleService getPeopleService(OAuth2AuthenticationToken authentication)
            throws IOException, GeneralSecurityException {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(),
                authentication.getName());

        @SuppressWarnings("deprecation")
        GoogleCredential credential = new GoogleCredential().setAccessToken(client.getAccessToken().getTokenValue());

        return new PeopleService.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("ContactManager")
                .build();
    }

    public List<Person> getContacts(OAuth2AuthenticationToken authentication)
            throws IOException, GeneralSecurityException {
        PeopleService peopleService = getPeopleService(authentication);

        ListConnectionsResponse response = peopleService.people().connections()
                .list("people/me")
                .setPersonFields("names,emailAddresses,phoneNumbers")
                .execute();

        return response.getConnections() != null ? response.getConnections() : Collections.emptyList();
    }

    public Person createContact(OAuth2AuthenticationToken authentication, String givenName, String familyName,
            String emailAddress, String phoneNumber) throws IOException, GeneralSecurityException {
        PeopleService peopleService = getPeopleService(authentication);

        Person contactToCreate = new Person();

        Name name = new Name()
                .setGivenName(givenName)
                .setFamilyName(familyName);
        contactToCreate.setNames(Collections.singletonList(name));

        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            PhoneNumber number = new PhoneNumber().setValue(phoneNumber);
            contactToCreate.setPhoneNumbers(Collections.singletonList(number));
        }

        if (emailAddress != null && !emailAddress.isEmpty()) {
            EmailAddress email = new EmailAddress().setValue(emailAddress);
            contactToCreate.setEmailAddresses(Collections.singletonList(email));
        }

        return peopleService.people().createContact(contactToCreate).execute();
    }

    public Person updateContact(OAuth2AuthenticationToken authentication, String resourceName,
            String givenName, String familyName, String emailAddress, String phoneNumber)
            throws IOException, GeneralSecurityException {

        PeopleService peopleService = getPeopleService(authentication);

        // Fetch the existing contact to get metadata and etag
        Person existingContact = peopleService.people().get(resourceName)
                .setPersonFields("names,emailAddresses,phoneNumbers,metadata")
                .execute();

        if (existingContact == null) {
            throw new IOException("Contact not found: " + resourceName);
        }

        // Create a new Person object for the update
        Person contactToUpdate = new Person();
        contactToUpdate.setResourceName(resourceName);

        // Include etag for proper update
        if (existingContact.getEtag() != null) {
            contactToUpdate.setEtag(existingContact.getEtag());
        }

        // Update name
        if (givenName != null || familyName != null) {
            List<Name> names = new ArrayList<>();
            Name name = new Name().setGivenName(givenName).setFamilyName(familyName);

            if (existingContact.getNames() != null && !existingContact.getNames().isEmpty()) {
                name.setMetadata(existingContact.getNames().get(0).getMetadata());
            }
            names.add(name);
            contactToUpdate.setNames(names);
        }

        // Handle phone numbers
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            List<PhoneNumber> phoneNumbers = new ArrayList<>();
            PhoneNumber number = new PhoneNumber().setValue(phoneNumber);

            if (existingContact.getPhoneNumbers() != null && !existingContact.getPhoneNumbers().isEmpty()) {
                number.setMetadata(existingContact.getPhoneNumbers().get(0).getMetadata());
            }
            phoneNumbers.add(number);
            contactToUpdate.setPhoneNumbers(phoneNumbers);
        }

        // Handle email addresses
        if (emailAddress != null && !emailAddress.isEmpty()) {
            List<EmailAddress> emailAddresses = new ArrayList<>();
            EmailAddress email = new EmailAddress().setValue(emailAddress);

            if (existingContact.getEmailAddresses() != null && !existingContact.getEmailAddresses().isEmpty()) {
                email.setMetadata(existingContact.getEmailAddresses().get(0).getMetadata());
            }
            emailAddresses.add(email);
            contactToUpdate.setEmailAddresses(emailAddresses);
        }

        // Perform the update with etag included
        return peopleService.people().updateContact(resourceName, contactToUpdate)
                .setUpdatePersonFields("names,phoneNumbers,emailAddresses")
                .execute();
    }

    public void deleteContact(OAuth2AuthenticationToken authentication, String resourceName)
            throws IOException, GeneralSecurityException {
        PeopleService peopleService = getPeopleService(authentication);
        peopleService.people().deleteContact(resourceName).execute();
    }

    public Person getContact(OAuth2AuthenticationToken authentication, String resourceName)
            throws IOException, GeneralSecurityException {
        PeopleService peopleService = getPeopleService(authentication);

        return peopleService.people().get(resourceName)
                .setPersonFields("names,emailAddresses,phoneNumbers")
                .execute();
    }
}