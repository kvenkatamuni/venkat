package com.paanini.jiffy.controller;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import javax.servlet.http.HttpServletRequest;

@Controller
public class AnthillErrorController implements ErrorController {
    @RequestMapping(method = RequestMethod.GET, value = "/error")
    public String getErrorPath(HttpServletRequest request) {
        return "error-generic.html";
    }

}
