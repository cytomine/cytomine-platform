package be.cytomine.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class CustomErrorController implements ErrorController {

    @Value("${application.serverURL}")
    String serverURL;

    @RequestMapping("/error")
    @ResponseBody
    String error(HttpServletRequest request, HttpServletResponse response) {


        int statusCode = response.getStatus();

        String subject = switch (statusCode) {
            case 401 -> "Authorization required";
            case 404 -> "Resource not found";
            default-> "Internal Error";
        };

        String message = switch (statusCode) {
            case 401 -> "This page is not publicly available. To access it, please login first or use a valid token.";
            case 404 -> "The page does not exists.";
            default-> "";
        };
        return TEMPLATE.formatted(statusCode,CSS, statusCode, subject, message, serverURL);
    }


    private static final String CSS = """
                h1{
                font-size:80px;
                font-weight:800;
                text-align:center;
                font-family: 'Roboto', sans-serif;
                }
                h2
                {
                font-size:25px;
                text-align:center;
                font-family: 'Roboto', sans-serif;
                margin-top:-40px;
                }
                p{
                text-align:center;
                font-family: 'Roboto', sans-serif;
                font-size:12px;
                }
                
                .container
                {
                width:300px;
                margin: 0 auto;
                margin-top:15%;
                color: #1e3148;
                }                
                """;

    private static final String TEMPLATE = """
                <html>
                <head>
                <title>Cytomine error %s</title>
                <link href="https://fonts.googleapis.com/css?family=Roboto:700" rel="stylesheet">
                <style>
                   %s 
                </style>
                </head>
                <body>
                <div class="container">
                <h1>%s</h1>
                <h2>%s</h2>
                <p>%s</p>
                <p><a href="%s">Go back to Cytomine homepage</a></p>
                </div>
                </body>
                </html>      
                """;
}