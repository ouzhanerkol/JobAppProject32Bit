package org.example;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Date;

public class Quartz implements Job {
    private Logger log = Logger.getLogger(Quartz.class);
    public void execute(JobExecutionContext jExeCtx) throws JobExecutionException {//handle JobExecutionException

        //debug message
        log.debug(new Date() + " Quartz is running......");
    }
}
