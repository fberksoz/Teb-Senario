package com.serdarkarakas.teb.controllers;

import com.serdarkarakas.teb.services.KafkaConsumerService;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Controller
@RequestMapping("/")
public class HomeController {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HomeController.class);

    Map<String, Integer> fileAndLines = new HashMap<String, Integer>();

    private String logDir = "/Users/serdarkarakas/Projects/rest/";

    int lineIndex = 0;

    @Autowired
    private KafkaConsumerService kafkaConsumerService;

    @GetMapping("/home")
    public String home(Model model) {
        listenFolderChange();
        return "home";
    }


    public void listenFolderChange() {
        String monitorDirectory = logDir;
        FileAlterationObserver observer = new FileAlterationObserver(monitorDirectory);

        LOGGER.info("Start ACTIVITY, Monitoring " + monitorDirectory);
        observer.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onDirectoryCreate(File file) {
                LOGGER.info("New Folder Created:" + file.getName());
            }

            @Override
            public void onDirectoryDelete(File file) {
                LOGGER.info("Folder Deleted:" + file.getName());
            }

            @Override
            public void onFileCreate(File file) {
                LOGGER.info("File Created:" + file.getName() + ": YOUR ACTION");
            }

            @Override
            public void onFileChange(File file) {
                LOGGER.info("File Change:" + file.getName() + ": YOUR ACTION");
                readFile(file.getName());

            }

            @Override
            public void onFileDelete(File file) {
                LOGGER.info("File Deleted:" + file.getName() + ": NO ACTION");
            }
        });

        /* Set to monitor changes for 500 ms */
        FileAlterationMonitor monitor = new FileAlterationMonitor(500, observer);
        try {
            monitor.start();
        } catch (Exception e) {
            LOGGER.error("UNABLE TO MONITOR SERVER" + e.getMessage());
            e.printStackTrace();

        }
    }

    public void readFile(String path) {

        if (!fileAndLines.containsKey(path)) {
            fileAndLines.put(path, 0);
        }

        StringBuilder sb = new StringBuilder();

        Path mPath = Paths.get(logDir + path);

        try (BufferedReader br = Files.newBufferedReader(mPath)) {

            long lineCount = Files.lines(mPath).count();


            for (int i = fileAndLines.get(path); i < lineCount; i++) {
                /*String sline = Files.readAllLines(mPath).get(i);*/
                String sline;
                try (Stream<String> lines = Files.lines(mPath)) {
                    sline = lines.skip((fileAndLines.get(path))).findFirst().get();
                    kafkaConsumerService.sendMessage(sline);
                    /*lineIndex++;*/
                    fileAndLines.replace(path, fileAndLines.get(path) + 1);
                } catch (Exception e) {
                    LOGGER.debug(e.toString());
                }
            }

        } catch (IOException e) {
            System.err.format("IOException: %s%n", e);
        }


    }
}
