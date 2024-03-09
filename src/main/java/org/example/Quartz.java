package org.example;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import java.io.IOException;

public class Quartz implements Job {
    private Logger LOG = Logger.getLogger(Quartz.class);
    XmlController xmlController = new XmlController();

    public void execute(JobExecutionContext jExeCtx) throws JobExecutionException {//handle JobExecutionException

        //debug message
        LOG.debug("Quartz is running......");
        try {
            xmlController.connectURL();
            LOG.debug("Connection completed...");
        } catch (IOException e) {
            System.out.print(e.getMessage());
            LOG.error("Exception occured", new Exception("Connection failed.."));
        }
    }

}
