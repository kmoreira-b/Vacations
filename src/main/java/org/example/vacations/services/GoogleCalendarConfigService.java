package org.example.vacations.services;

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Configuration
@Profile("!test")
public class GoogleCalendarConfigService {

    @Value("${google.oauth.credentials}")
    private String clientSecretPath;

    @Value("${google.oauth.tokens.dir}")
    private String tokensDir;

    @Bean
    public Calendar googleCalendar() throws Exception {
        var http = GoogleNetHttpTransport.newTrustedTransport();
        var json = GsonFactory.getDefaultInstance();

        try (var reader = Files.newBufferedReader(Path.of(clientSecretPath))) {
            var secrets = GoogleClientSecrets.load(json, reader);

            var flow = new GoogleAuthorizationCodeFlow.Builder(
                    http, json, secrets, List.of(CalendarScopes.CALENDAR))
                    .setDataStoreFactory(new FileDataStoreFactory(Path.of(tokensDir).toFile()))
                    .setAccessType("offline")
                    .build();

            // If 8888 is busy, change to .setPort(0)
            var receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            var credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

            return new Calendar.Builder(http, json, credential)
                    .setApplicationName("Vacations")
                    .build();
        }
    }
}
