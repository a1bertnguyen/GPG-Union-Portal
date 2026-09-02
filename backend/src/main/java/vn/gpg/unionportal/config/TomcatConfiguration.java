package vn.gpg.unionportal.config;

import org.apache.coyote.http11.Http11Nio2Protocol;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TomcatConfiguration {

    /**
     * Java 24 on this Windows host cannot initialize Tomcat's default NIO selector
     * (it fails its internal loopback pipe). NIO2 is an official Tomcat HTTP/1.1
     * connector and avoids that selector implementation.
     */
    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatConnector() {
        return factory -> factory.setProtocol(Http11Nio2Protocol.class.getName());
    }
}
