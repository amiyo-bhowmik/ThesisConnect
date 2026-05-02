package com.example.ThesisConnect.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String root() {
        return "forward:/login.html";
    }

    @GetMapping("/home")
    public String home() {
        return "forward:/index.html";
    }

    @GetMapping("/login")
    public String login() {
        return "forward:/login.html";
    }

    @GetMapping("/discover")
    public String discover() {
        return "forward:/discover.html";
    }

    @GetMapping("/groups")
    public String groups() {
        return "forward:/groups.html";
    }

    @GetMapping("/create-group")
    public String createGroup() {
        return "forward:/create-group.html";
    }

    @GetMapping("/available-groups")
    public String availableGroups() {
        return "forward:/available-groups.html";
    }

    @GetMapping("/notifications")
    public String notifications() {
        return "forward:/notifications.html";
    }

    @GetMapping("/group-details")
    public String groupDetails() {
        return "forward:/group-details.html";
    }
}
