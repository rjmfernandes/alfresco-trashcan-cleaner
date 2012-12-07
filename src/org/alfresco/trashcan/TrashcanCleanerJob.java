package org.alfresco.trashcan;

import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.schedule.AbstractScheduledLockedJob;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.transaction.TransactionService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * 
 * @author rjmfernandes@gmail.com
 *
 */
public class TrashcanCleanerJob extends AbstractScheduledLockedJob {

	protected NodeService nodeService;
	protected TransactionService transactionService;
	protected AuthenticationComponent authenticationComponent;

	@Override
	public void executeJob(JobExecutionContext jobContext)
			throws JobExecutionException {
		setServices(jobContext);
		authenticationComponent.setSystemUserAsCurrentUser();
		cleanInTransaction();
	}

	private void cleanInTransaction() {
		RetryingTransactionCallback<Object> txnWork = new RetryingTransactionCallback<Object>() {
			public Object execute() throws Exception {
				TrashcanCleaner cleaner = new TrashcanCleaner(nodeService);
				cleaner.clean();
				return null;
			}
		};
		transactionService.getRetryingTransactionHelper().doInTransaction(
				txnWork);
	}

	private void setServices(JobExecutionContext jobContext) {
		nodeService = (NodeService) jobContext.getJobDetail().getJobDataMap()
				.get("nodeService");
		transactionService = (TransactionService) jobContext.getJobDetail()
				.getJobDataMap().get("transactionService");
		authenticationComponent = (AuthenticationComponent) jobContext
				.getJobDetail().getJobDataMap().get("authenticationComponent");

	}

}
