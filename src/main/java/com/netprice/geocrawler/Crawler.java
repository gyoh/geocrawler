package com.netprice.geocrawler;

import com.netprice.geocrawler.listener.CrawlerListener;
import com.netprice.geocrawler.listener.GeoPlanetListener;
import com.netprice.geocrawler.listener.PersistenceListener;
import com.netprice.geocrawler.listener.PrintListener;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author gyo
 */
public abstract class Crawler implements Job {

    protected static final String USAGE =
            "Usage: java -jar geocrawler.jar " +
            "[ gnavi | tabelog | hotpepper ] [ print | store | list | export ]";

    private static final Logger logger =
            LoggerFactory.getLogger(Crawler.class);

    protected final List<CrawlerListener> listeners =
            new ArrayList<CrawlerListener>();

    public static void main(String[] args) {
        try {
            switch (args.length) {
            case 0:
                Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
                scheduler.start();
                schedule(scheduler, GnaviCrawler.class);
                schedule(scheduler, TabelogCrawler.class);
                schedule(scheduler, HotpepperCrawler.class);
                break;
            case 2:
                String service = args[0].substring(0, 1).toUpperCase() +
                        args[0].substring(1);
                Class<?> clazz = Class.forName(
                        "com.netprice.geocrawler." + service + "Crawler");
                Method method = clazz.getMethod(args[1]);
                method.invoke(clazz.newInstance());
                break;
            default:
                System.out.println(USAGE);
                break;
            }
        } catch (Exception e) {
            logger.error("Exception occured:", e);
        }

    }

    public static void schedule(Scheduler scheduler, Class clazz)
            throws SchedulerException {
        // Schedule crawler job
        JobDetail jobDetail = new JobDetail(
                clazz.getSimpleName() + " Job", null, clazz);

        // fire every day at 3:00
//        Trigger trigger = TriggerUtils.makeDailyTrigger(3, 0);
//        trigger.setName(clazz.getSimpleName() + " Trigger");
        DateIntervalTrigger trigger = new DateIntervalTrigger(
                clazz.getSimpleName() + " Trigger", null,
                DateIntervalTrigger.IntervalUnit.DAY, 2);

        scheduler.scheduleJob(jobDetail, trigger);
    }

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        JobDetail jobDetail = context.getJobDetail();
        logger.info("Executing {} at {}", jobDetail.getName(), new Date());
        store();
        logger.info("Finished {} at {}", jobDetail.getName(), new Date());
    }

    public void print() {
        addListener(new GeoPlanetListener());
        addListener(new PrintListener());
        fetch();
    }

    public void store() {
        addListener(new GeoPlanetListener());
        addListener(new PersistenceListener());
        fetch();
    }

    protected abstract void fetch();

    protected void addListener(CrawlerListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

}
