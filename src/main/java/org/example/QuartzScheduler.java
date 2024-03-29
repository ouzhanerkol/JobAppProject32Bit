package org.example;

import org.apache.log4j.BasicConfigurator;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

public class QuartzScheduler {
    private static final String NAME_OF_JOB = "Job1";
    private static final String NAME_OF_GROUP = "group1";
    private static final String NAME_OF_TRIGGER = "triggerStart";

    //create variable scheduler of type Scheduler
    private static Scheduler scheduler;

    //main() method starts with exception handling
    public static void main(String args[]) throws Exception {

        BasicConfigurator.configure();

        //ResourceBundle resourceBundle = ResourceBundle.getBundle("config.properties", Locale.ENGLISH);
        //resourceBundle.getString("last.modified");

        //show message to know about the main thread
        System.out.println(" The name of the QuartzScheduler main thread is: " + Thread.currentThread().getName());

        //initialize scheduler instance from Quartz
        scheduler = new StdSchedulerFactory().getScheduler();

        //start scheduler
        scheduler.start();

        //create scheduler trigger based on the time interval
        Trigger triggerNew =  createTrigger();


        //schedule trigger
        scheduleJob(triggerNew);

    }

    //create scheduleJob() method to schedule a job
    private static void scheduleJob(Trigger triggerNew) throws Exception {

        //create an instance of the JoDetails to connect Quartz job to the CreateQuartzJob
        JobDetail jobInstance = JobBuilder.newJob(Quartz.class).withIdentity(NAME_OF_JOB, NAME_OF_GROUP).build();

        //invoke scheduleJob method to connect the Quartz scheduler to the jobInstance and the triggerNew
        scheduler.scheduleJob(jobInstance, triggerNew);

    }

    private static Trigger createTrigger() {

        //initialize time interval
        int TIME_INTERVAL = 60;

        //create a trigger to be returned from the method
        Trigger triggerNew = TriggerBuilder.newTrigger().withIdentity(NAME_OF_TRIGGER, NAME_OF_GROUP)
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(TIME_INTERVAL).repeatForever())
                .build();

        // triggerNew to schedule it in main() method
        return triggerNew;
    }
}
