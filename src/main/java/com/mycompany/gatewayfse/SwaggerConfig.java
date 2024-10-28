/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.gatewayfse;

import io.swagger.v3.jaxrs2.integration.OpenApiServlet;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
//import io.swagger.v3.oas.integration.servlet.OpenApiServlet;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class SwaggerConfig implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("GatewayFSE API")
                        .version("1.0")
                        .description("Documentazione API del progetto GatewayFSE"));

        SwaggerConfiguration oasConfig = new SwaggerConfiguration()
                .openAPI(openAPI)
                .prettyPrint(true)
                .resourcePackages(java.util.Collections.singleton("com.mycompany.gatewayfse"));

        OpenApiServlet openApiServlet = new OpenApiServlet();
        sce.getServletContext().addServlet("OpenApiServlet", openApiServlet).addMapping("/openapi");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Nessuna azione necessaria in questo caso.
    }
}
