package byzas.libs.flow.manager.util.extensions;

import byzas.libs.flow.manager.async.model.exception.JobAlreadyExistsException;
import byzas.libs.flow.manager.async.model.exception.JobNotFoundException;
import org.apache.logging.log4j.Logger;
import org.quartz.*;

import java.util.Calendar;
import java.util.*;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public interface SchedulerSupport {

    Scheduler getScheduler();

    Logger getLogger();

    default String generateCronExpression(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return String.format("%1$s %2$s %3$s %4$s %5$s %6$s %7$s",
                calendar.get(Calendar.SECOND),
                calendar.get(Calendar.MINUTE), calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, "?", calendar.get(Calendar.YEAR));
    }

    default String generateCronExpressionForEveryMinutes(int minutes) {
        String sec = "0", min = "0", hour = "*", day = "1/1", wday = "*", month = "?", year = "*";
        if (minutes <= 60) {
            min = "0/" + minutes;
        } else if (minutes >= (60 * 24)) {
            hour = "12";
            day = "1/" + (minutes / (60 * 24));
        } else if (minutes > 60) {
            hour = "0/" + (minutes / 60);
        }
        return String.format("%1$s %2$s %3$s %4$s %5$s %6$s %7$s", sec, min, hour, day, wday, month, year);
    }

    default void delete(String jobName, boolean silent) {
        delete(jobName, jobName + "_group", silent);
    }

    default void delete(String jobName, String jobGroupName, boolean silent) {
        try {
            JobKey jobKey = JobKey.jobKey(jobName, jobGroupName);
            boolean jobExists = getScheduler().checkExists(jobKey);
            if (!jobExists) {
                String message = String.format("Job not found : %s%s%s", jobKey.getName(), ":", jobKey.getGroup());
                if (!silent) {
                    getLogger().error(message);
                    throw new JobNotFoundException(jobName);
                } else {
                    getLogger().warn(message);
                }
            } else {
                getScheduler().deleteJob(jobKey);
            }
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    default void schedule(String jobName, String cronExpression, Class jobDetailClass, Map<String, String> context, boolean override, boolean silent) {
        schedule(jobName, jobName + "_group", jobName + "_trigger",
                jobName + "_triggerGroup", cronExpression, jobDetailClass, context, override, silent, Optional.empty(), Optional.empty());
    }

    default void schedule(String jobName, String cronExpression, Class jobDetailClass, Map<String, String> context, boolean override, boolean silent, Optional<Date> startDate) {
        schedule(jobName, jobName + "_group", jobName + "_trigger",
                jobName + "_triggerGroup", cronExpression, jobDetailClass, context, override, silent, startDate, Optional.empty());
    }

    default void schedule(String jobName, String cronExpression, Class jobDetailClass, Map<String, String> context, boolean override, boolean silent, Optional<Date> startDate,
                          Optional<TimeZone> timeZone) {
        schedule(jobName, jobName + "_group", jobName + "_trigger",
                jobName + "_triggerGroup", cronExpression, jobDetailClass, context, override, silent, startDate, timeZone);
    }

    default void schedule(String jobName, String jobGroupName, String triggerName,
                          String triggerGroupName, Date date, Class jobDetailClass, Map<String, String> context, boolean override, boolean silent,
                          Optional<Date> startDate, Optional<TimeZone> timeZone) {
        schedule(jobName, jobGroupName, triggerName,
                triggerGroupName, generateCronExpression(date), jobDetailClass, context, override, silent, startDate, timeZone);
    }

    default void schedule(String jobName, String jobGroupName, String triggerName,
                          String triggerGroupName, String cronExpression, Class jobDetailClass, Map<String, String> context, boolean override,
                          boolean silent, Optional<Date> startDate, Optional<TimeZone> timeZone) {
        schedule(jobName, jobGroupName, triggerName, triggerGroupName, cronExpression, jobDetailClass, context, true, override, silent, startDate, timeZone);
    }

    default void schedule(String jobName, String jobGroupName, String cronExpression, Class jobDetailClass,
                          Map<String, String> context) {
        schedule(jobName, jobGroupName, String.format("%s_trigger", jobName), String.format("%s_trigger_group", jobName)
                , cronExpression, jobDetailClass, context, false, false, false, Optional.empty(), Optional.empty());
    }

    default void schedule(String jobName, String jobGroupName, String triggerName,
                          String triggerGroupName, String cronExpression, Class jobDetailClass, Map<String, String> context, boolean checkJobExists, boolean override,
                          boolean silent, Optional<Date> startDate, Optional<TimeZone> timeZone) {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroupName);
        try {
            if (checkJobExists) {
                boolean jobExists = getScheduler().checkExists(jobKey);
                if (jobExists) {
                    if (override) {
                        // also deletes existing triggers
                        getScheduler().deleteJob(jobKey);
                        schedule(jobKey, triggerName, triggerGroupName, cronExpression, jobDetailClass, context, startDate, timeZone);
                    } else {
                        if (silent) {
                            getLogger().warn("Job with identity already exists : {}{}{}", jobKey.getName(), ":", jobKey.getGroup());
                        } else {
                            getLogger().error("Job with identity already exists : {}{}{}", jobKey.getName(), ":", jobKey.getGroup());
                            throw new JobAlreadyExistsException(jobName);
                        }
                    }
                } else {
                    schedule(jobKey, triggerName, triggerGroupName, cronExpression, jobDetailClass, context, startDate, timeZone);
                }
            } else {
                schedule(jobKey, triggerName, triggerGroupName, cronExpression, jobDetailClass, context, startDate, timeZone);
            }
        } catch (ObjectAlreadyExistsException o) {
            getLogger().debug("Other instance scheduled the job {}{}{}, no problem,go ahead", jobKey.getName(), ":", jobKey.getGroup());
        } catch (SchedulerException se) {
            throw new RuntimeException(se);
        }
    }

    default void schedule(JobKey jobKey, String triggerName, String triggerGroupName, String cronExpression, Class jobDetailClass, Map<String, String> context
            , Optional<Date> startDate, Optional<TimeZone> timeZone) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(jobDetailClass)
                .withIdentity(jobKey)
                .storeDurably()
                .build();
        jobDetail.getJobDataMap().putAll(context);
        TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroupName);
        CronScheduleBuilder cronScheduleBuilder = cronSchedule(cronExpression);
        if (timeZone.isPresent()) {
            cronScheduleBuilder = cronScheduleBuilder.inTimeZone(timeZone.get());
        } else {
            cronScheduleBuilder = cronScheduleBuilder.inTimeZone(TimeZone.getDefault());
        }
        TriggerBuilder triggerBuilder = newTrigger()
                .forJob(jobDetail)
                .withIdentity(triggerKey)
                .withSchedule(cronScheduleBuilder.withMisfireHandlingInstructionDoNothing());
        Trigger trigger;
        if (startDate.isPresent()) {
            trigger = triggerBuilder.startAt(startDate.get()).build();
        } else {
            trigger = triggerBuilder.startNow().build();
        }
        getScheduler().scheduleJob(jobDetail, trigger);
    }
}
