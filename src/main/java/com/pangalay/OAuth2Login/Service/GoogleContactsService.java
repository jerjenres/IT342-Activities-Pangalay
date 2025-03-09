package com.pangalay.OAuth2Login.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.people.v1.PeopleService;
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
import java.util.Collections;
import java.util.List;

@Service
public class GoogleContactsService {

    private final OAuth2AuthorizedClientService authorizedClientService;

    public GoogleContactsService(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    private PeopleService getPeopleService(OAuth2AuthenticationToken authentication) throws IOException, GeneralSecurityException {
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

    public List<Person> getContacts(OAuth2AuthenticationToken authentication) throws IOException, GeneralSecurityException {
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
        
        // Add email addresses similarly
        
        return peopleService.people().createContact(contactToCreate).execute();
    }

    public Person updateContact(OAuth2AuthenticationToken authentication, String resourceName, 
                                 String givenName, String familyName, String emailAddress, 
                                 String phoneNumber) throws IOException, GeneralSecurityException {
        PeopleService peopleService = getPeopleService(authentication);
        
        Person contactToUpdate = new Person();
        contactToUpdate.setResourceName(resourceName);
        
        Name name = new Name()
                .setGivenName(givenName)
                .setFamilyName(familyName);
        contactToUpdate.setNames(Collections.singletonList(name));
        
        // Add phone numbers and email addresses similarly
        
        return peopleService.people().updateContact(resourceName, contactToUpdate)
                .setUpdatePersonFields("names,emailAddresses,phoneNumbers")
                .execute();
    }

    public void deleteContact(OAuth2AuthenticationToken authentication, String resourceName) 
            throws IOException, GeneralSecurityException {
        PeopleService peopleService = getPeopleService(authentication);
        peopleService.people().deleteContact(resourceName).execute();
    }
}