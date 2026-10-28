package com.zuehlke.securesoftwaredevelopment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/my/service-history")
    public String showServiceHistory() {
        return "service-history";
    }

    @GetMapping("/my/services/new")
    public String showCreateService() {
        return "create-service";
    }

    @GetMapping("/home")
    public String showHome() {
        return "home";
    }
}
