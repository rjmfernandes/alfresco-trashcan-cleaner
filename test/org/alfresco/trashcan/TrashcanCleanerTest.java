package org.alfresco.trashcan;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.UserTransaction;

import junit.framework.TestCase;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ApplicationContextHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

/**
 * 
 * @author rjmfernandes@gmail.com
 * 
 */
public class TrashcanCleanerTest extends TestCase {

	private static final int BATCH_SIZE = 1000;

	private static Log logger = LogFactory.getLog(TrashcanCleanerTest.class);

	private static ApplicationContext applicationContext = ApplicationContextHelper
			.getApplicationContext();
	protected NodeService nodeService;
	protected TransactionService transactionService;
	protected Repository repository;
	protected AuthenticationComponent authenticationComponent;

	@Override
	public void setUp() {
		nodeService = (NodeService) applicationContext.getBean("nodeService");
		authenticationComponent = (AuthenticationComponent) applicationContext
				.getBean("authenticationComponent");
		transactionService = (TransactionService) applicationContext
				.getBean("transactionComponent");
		repository = (Repository) applicationContext
				.getBean("repositoryHelper");

		// Authenticate as the system user
		authenticationComponent.setSystemUserAsCurrentUser();
	}

	@Override
	public void tearDown() {
		authenticationComponent.clearCurrentSecurityContext();
	}

	public void testCleanSimple() throws Throwable {
		cleanBatchTest(1, 0);
	}

	public void testCleanBatch() throws Throwable {
		cleanBatchTest(BATCH_SIZE + 1, 1);
	}

	private void cleanBatchTest(int nodesCreate, int nodesRemain)
			throws Throwable {
		UserTransaction userTransaction1 = transactionService
				.getUserTransaction();
		try {
			userTransaction1.begin();
			TrashcanCleaner cleaner = new TrashcanCleaner(nodeService,BATCH_SIZE,-1);
			createAndDeleteNodes(nodesCreate);
			long nodesToDelete = getNumberOfNodesInTrashcan();
			logger.info(String.format("Existing nodes to delete: %s",
					nodesToDelete));
			cleaner.clean();
			nodesToDelete = getNumberOfNodesInTrashcan();
			logger.info(String.format("Existing nodes to delete after: %s",
					nodesToDelete));
			assertEquals(nodesToDelete, nodesRemain);
			logger.info("Clean trashcan...");
			cleaner.clean();
			userTransaction1.commit();
		} catch (Throwable e) {
			try {
				userTransaction1.rollback();
			} catch (IllegalStateException ee) {
			}
			throw e;
		}
	}

	private void createAndDeleteNodes(int n) {
		for (int i = n; i > 0; i--) {
			createAndDeleteNode();
		}

	}

	private void createAndDeleteNode() {
		NodeRef companyHome = repository.getCompanyHome();
		String name = "Sample (" + System.currentTimeMillis() + ")";
		Map<QName, Serializable> contentProps = new HashMap<QName, Serializable>();
		contentProps.put(ContentModel.PROP_NAME, name);
		ChildAssociationRef association = nodeService.createNode(companyHome,
				ContentModel.ASSOC_CONTAINS, QName.createQName(
						NamespaceService.CONTENT_MODEL_PREFIX, name),
				ContentModel.TYPE_CONTENT, contentProps);
		nodeService.deleteNode(association.getChildRef());

	}
	
	private long getNumberOfNodesInTrashcan() {
		StoreRef archiveStore = new StoreRef("archive://SpacesStore");
		NodeRef archiveRoot = nodeService.getRootNode(archiveStore);
		List<ChildAssociationRef> childAssocs = nodeService
				.getChildAssocs(archiveRoot);
		return childAssocs.size();

	}

}
