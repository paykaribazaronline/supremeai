package com.supremeai.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/__/firebase")
public class FirebaseEmulatorController {

  @Value("${firebase.api.key:}")
  private String apiKey;

  @Value("${firebase.auth.domain:supremeai-a.firebaseapp.com}")
  private String authDomain;

  @Value(
      "${firebase.database.url:https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/}")
  private String databaseURL;

  @Value("${firebase.project.id:supremeai-a}")
  private String projectId;

  @Value("${firebase.storage.bucket:supremeai-a.firebasestorage.app}")
  private String storageBucket;

  @Value("${firebase.messaging.sender.id:565236080752}")
  private String messagingSenderId;

  @Value("${firebase.app.id:1:565236080752:web:572bb9313db9afb355d4b5}")
  private String appId;

  @Value("${firebase.measurement.id:G-1234567890}")
  private String measurementId;

  @GetMapping("/init.js")
  public ResponseEntity<String> getFirebaseInit() {
    String js =
        String.format(
            "if (typeof firebase === 'undefined') throw new Error('Firebase not loaded');\n"
                + "if (!firebase.apps.length) {\n"
                + "  firebase.initializeApp({\n"
                + "    apiKey: '%s',\n"
                + "    authDomain: '%s',\n"
                + "    databaseURL: '%s',\n"
                + "    projectId: '%s',\n"
                + "    storageBucket: '%s',\n"
                + "    messagingSenderId: '%s',\n"
                + "    appId: '%s',\n"
                + "    measurementId: '%s'\n"
                + "  });\n"
                + "}\n",
            apiKey,
            authDomain,
            databaseURL,
            projectId,
            storageBucket,
            messagingSenderId,
            appId,
            measurementId);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(
        org.springframework.http.MediaType.parseMediaType("application/javascript"));
    return ResponseEntity.ok().headers(headers).body(js);
  }
}
