package com.ericsson.eiffel.remrem.publish.helper;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeoutException;

@Component("rmqHelper") @Slf4j public class RMQHelper {

    private static final int CHANNEL_COUNT = 100;
    private static final Random random = new Random();
    @Value("${rabbitmq.host}") private String host;
    @Value("${rabbitmq.exchange.name}") private String exchangeName;
    private Connection rabbitConnection;
    private List<Channel> rabbitChannels;

    @PostConstruct public void init() {
        log.info("RMQHelper init ...");
        initCli();
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);         
            rabbitConnection = factory.newConnection();
            rabbitChannels = new ArrayList<>();

            for (int i = 0; i < CHANNEL_COUNT; i++) {
                rabbitChannels.add(rabbitConnection.createChannel());
            }
        } catch (IOException | TimeoutException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void initCli() {
    	if (host == null) {
    		Yaml yaml = new Yaml();
    		try {
    			String fileName = "application.yml";
    			ClassLoader classLoader = getClass().getClassLoader(); 
    			URL url = classLoader.getResource(fileName);
    			File file = new File(url.getFile());
    			InputStream ios = new FileInputStream(file);

    			// Parse the YAML file and return the output as a series of Maps and Lists
    			Map<String,Object> result = (Map<String,Object>)yaml.load(ios);
    			Map<String,Object> rmq = (Map<String,Object>)result.get("rabbitmq");
    			host = (String)rmq.get("host");
    			Map<String,Object> rmqExchange = (Map<String,Object>)rmq.get("exchange");
    			exchangeName = (String)rmqExchange.get("name");
    			int i = 2;
    		} catch (Exception e) {
    			e.printStackTrace();
    		}    		
    	}
    }
    
    @PreDestroy
    public void cleanUp() throws IOException {
        log.info("RMQHelper cleanUp ...");
        if (rabbitConnection!=null){
            rabbitConnection.close();
            rabbitConnection = null;
        } else {
            log.warn("rabbitConnection is null when cleanUp");
        }
    }

    public void send(String routingKey, String msg) throws IOException {
        giveMeRandomChannel()
            .basicPublish(exchangeName, routingKey, MessageProperties.BASIC, msg.getBytes());
    }


    private Channel giveMeRandomChannel() {
        return rabbitChannels.get(random.nextInt(rabbitChannels.size()));
    }
}
