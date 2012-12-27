package org.alfresco.schedule;

import org.alfresco.repo.lock.JobLockService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * 
 * @author rjmfernandes@gmail.com
 * 
 */
public abstract class AbstractScheduledLockedJob extends QuartzJobBean {

	private ScheduledJobLockExecuter locker;

	@Override
	protected final void executeInternal(final JobExecutionContext jobContext)
			throws JobExecutionException {
		if (locker == null) {
			JobLockService jobLockServiceBean = (JobLockService) jobContext
					.getJobDetail().getJobDataMap().get("jobLockService");
			if (jobLockServiceBean == null)
				throw new JobExecutionException(
						"Missing setting for bean jobLockService");
			String name = (String) jobContext.getJobDetail().getJobDataMap()
					.get("name");
			String jobName = name == null ? this.getClass().getSimpleName()
					: name;
			locker = new ScheduledJobLockExecuter(jobLockServiceBean, jobName,
					this);
		}
		locker.execute(jobContext);
	}

	public abstract void executeJob(JobExecutionContext jobContext)
			throws JobExecutionException;
}
