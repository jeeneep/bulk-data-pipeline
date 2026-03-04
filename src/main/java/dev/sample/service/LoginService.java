package dev.sample.service;

import org.springframework.stereotype.Service;

@Service
public class LoginService {

    public String login(String id) {

        if ("test1".equals(id)) {
            return "TEST1";
        } else if ("test2".equals(id)) {
            return "TEST2";
        } else {
            return "INVALID";
        }
    }
}