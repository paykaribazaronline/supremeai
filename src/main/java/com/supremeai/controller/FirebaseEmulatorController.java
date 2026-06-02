package com.supremeai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;

@RestController
@RequestMapping("/__/firebase")
public class FirebaseEmulatorController {
    public FirebaseEmulatorController(String apiKey, String authDomain, String databaseURL, String projectId, String storageBucket, String messagingSenderId, String appId, String measurementId) {
        this.apiKey = apiKey;
        this.authDomain = authDomain;
        this.databaseURL = databaseURL;
        this.projectId = projectId;
        this.storageBucket = storageBucket;
        this.messagingSenderId = messagingSenderId;
        this.appId = appId;
        this.measurementId = measurementId;
    }










    @GetMapping("/init.js")
    public ResponseEntity<String> getFirebaseInit() {
        String js = String.format(
            "if (typeof firebase === 'undefined') throw new Error('Firebase not loaded');\n" +
            "if (!firebase.apps.length) {\n" +
            "  firebase.initializeApp({\n" +
            "    apiKey: '%s',\n" +
            "    authDomain: '%s',\n" +
            "    databaseURL: '%s',\n" +
            "    projectId: '%s',\n" +
            "    storageBucket: '%s',\n" +
            "    messagingSenderId: '%s',\n" +
            "    appId: '%s',\n" +
            "    measurementId: '%s'\n" +
            "  });\n" +
            "}\n",
            apiKey, authDomain, databaseURL, projectId, storageBucket, messagingSenderId, appId, measurementId
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.parseMediaType("application/javascript"));
        return ResponseEntity.ok().headers(headers).body(js);
    }
}
