package org.example;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import java.io.IOException;

public class Quartz implements Job {
    private final Logger LOG = Logger.getLogger(Quartz.class);
    private static final String NAME_OF_URL = "https://www.tcmb.gov.tr/kurlar/today.xml";
    XmlController xmlController = new XmlController();

    public void execute(JobExecutionContext jExeCtx) throws JobExecutionException {//handle JobExecutionException

        // debug message
        LOG.debug("Quartz is running......");
        try {
            // connect here
            xmlController.getCurrenciesFromURL(NAME_OF_URL);
        } catch (IOException e) {
            // connection failed
            LOG.error("Exception occurred", new Exception("Connection failed.."));
        }
    }

}
