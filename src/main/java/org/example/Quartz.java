package org.example;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import java.io.IOException;

public class Quartz implements Job {
    private Logger log = Logger.getLogger(Quartz.class);
    XmlController xmlController = new XmlController();

    public void execute(JobExecutionContext jExeCtx) throws JobExecutionException {//handle JobExecutionException

        //debug message
        log.debug("Quartz is running......");
        try {
            xmlController.connectURL();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
