package org.alfresco.schedule;

import org.alfresco.repo.lock.JobLockService;
import org.alfresco.repo.lock.LockAcquisitionException;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.util.VmShutdownListener.VmShutdownException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * 
 * @author Rui Fernandes
 * 
 */

public class ScheduledJobLockExecuter
{

	private static Log logger = LogFactory
	        .getLog(ScheduledJobLockExecuter.class.getName());

	private static final long LOCK_TTL = 30000L;
	private static ThreadLocal<Pair<Long, String>> lockThreadLocal = new ThreadLocal<Pair<Long, String>>();
	private JobLockService jobLockService;
	private QName lockQName;
	private AbstractScheduledLockedJob job;

	public ScheduledJobLockExecuter(JobLockService jobLockService, String name,
	        AbstractScheduledLockedJob job)
	{
		this.jobLockService = jobLockService;
		lockQName = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI,
		        name);
		this.job = job;
	}

	public void execute(JobExecutionContext jobContext)
	        throws JobExecutionException
	{
		try
		{
			if (logger.isDebugEnabled())
			{
				logger.debug(String.format("   Job %s started.",
				        lockQName.getLocalName()));
			}
			refreshLock();
			job.executeJob(jobContext);
			if (logger.isDebugEnabled())
			{
				logger.debug(String.format("   Job %s completed.",
				        lockQName.getLocalName()));
			}
		} catch (LockAcquisitionException e)
		{
			// Job being done by another process
			if (logger.isDebugEnabled())
			{
				logger.debug(String.format("   Job %s already underway.",
				        lockQName.getLocalName()));
			}
		} catch (VmShutdownException e)
		{
			// Aborted
			if (logger.isDebugEnabled())
			{
				logger.debug(String.format("   Job %s aborted.",
				        lockQName.getLocalName()));
			}
		} finally
		{
			releaseLock();
		}
	}

	/**
	 * Lazily update the job lock
	 * 
	 */
	private void refreshLock()
	{
		Pair<Long, String> lockPair = lockThreadLocal.get();
		if (lockPair == null)
		{
			String lockToken = jobLockService.getLock(lockQName, LOCK_TTL);
			Long lastLock = new Long(System.currentTimeMillis());
			// We have not locked before
			lockPair = new Pair<Long, String>(lastLock, lockToken);
			lockThreadLocal.set(lockPair);
		} else
		{
			long now = System.currentTimeMillis();
			long lastLock = lockPair.getFirst().longValue();
			String lockToken = lockPair.getSecond();
			// Only refresh the lock if we are past a threshold
			if (now - lastLock > (long) (LOCK_TTL / 2L))
			{
				jobLockService.refreshLock(lockToken, lockQName, LOCK_TTL);
				lastLock = System.currentTimeMillis();
				lockPair = new Pair<Long, String>(lastLock, lockToken);
			}
		}
	}

	/**
	 * Release the lock after the job completes
	 */
	private void releaseLock()
	{
		Pair<Long, String> lockPair = lockThreadLocal.get();
		if (lockPair != null)
		{
			// We can't release without a token
			try
			{
				jobLockService.releaseLock(lockPair.getSecond(), lockQName);
			} finally
			{
				// Reset
				lockThreadLocal.set(null);
			}
		}
		// else: We can't release without a token
	}

}
