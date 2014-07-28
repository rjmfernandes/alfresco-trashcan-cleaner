/*
 * Copyright (C) 2005-2013 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
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
import org.springframework.context.ApplicationContext;

/**
 * 
 * Test class for {@link org.alfresco.trashcan.TrashcanCleaner TrashcanCleaner}.
 * 
 * @author Rui Fernandes
 * 
 */
public class TrashcanCleanerTest extends TestCase
{

	private static final int BATCH_SIZE = 1000;

	// private static Log logger = LogFactory.getLog(TrashcanCleanerTest.class);

	private static ApplicationContext applicationContext = ApplicationContextHelper
	        .getApplicationContext();
	protected NodeService nodeService;
	protected TransactionService transactionService;
	protected Repository repository;
	protected AuthenticationComponent authenticationComponent;

	/**
	 * 
	 * Sets services and current user as system.
	 * 
	 */
	@Override
	public void setUp()
	{
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

	/**
	 * 
	 * Clears security context.
	 * 
	 */
	@Override
	public void tearDown()
	{
		authenticationComponent.clearCurrentSecurityContext();
	}

	/**
	 * 
	 * Tests that existing just one node deleted the cleaning of the trashcan
	 * will delete it using the default configuration.
	 * 
	 * @throws Throwable
	 */
	public void testCleanSimple() throws Throwable
	{
		cleanBatchTest(1, 0);
	}

	/**
	 * 
	 * Tests that existing the maximum number of nodes to be deleted in a single
	 * trashcan clean execution plus one, after the deletion just a single node
	 * is present in archive.
	 * 
	 * @throws Throwable
	 */
	public void testCleanBatch() throws Throwable
	{
		cleanBatchTest(BATCH_SIZE + 1, 1);
	}

	/**
	 * 
	 * Generic method that asserts that for the <b>nodesCreate</b> existing on
	 * archive store the execution of trashcan clean will leave remaining
	 * undeleted <b>nodesRemain</b>.
	 * 
	 * @param nodesCreate
	 * @param nodesRemain
	 * @throws Throwable
	 */
	private void cleanBatchTest(int nodesCreate, int nodesRemain)
	        throws Throwable
	{
		UserTransaction userTransaction1 = transactionService
		        .getUserTransaction();
		try
		{
			userTransaction1.begin();
			TrashcanCleaner cleaner = new TrashcanCleaner(nodeService,
			        BATCH_SIZE, -1);
			createAndDeleteNodes(nodesCreate);
			long nodesToDelete = cleaner.getNumberOfNodesInTrashcan();
			System.out.println(String.format("Existing nodes to delete: %s",
			        nodesToDelete));
			cleaner.clean();
			nodesToDelete = cleaner.getNumberOfNodesInTrashcan();
			System.out.println(String.format(
			        "Existing nodes to delete after: %s", nodesToDelete));
			assertEquals(nodesRemain,nodesToDelete);
			System.out.println("Clean trashcan...");
			cleaner.clean();
			userTransaction1.commit();
		} catch (Throwable e)
		{
			try
			{
				userTransaction1.rollback();
			} catch (IllegalStateException ee)
			{
			}
			throw e;
		}
	}

	/**
	 * 
	 * Creates and deletes the specified number of nodes.
	 * 
	 * @param n
	 */
	private void createAndDeleteNodes(int n)
	{
		for (int i = n; i > 0; i--)
		{
			createAndDeleteNode();
		}

	}

	/**
	 * 
	 * Creates and delete a single node whose name is based on the current time
	 * in milliseconds.
	 * 
	 */
	private void createAndDeleteNode()
	{
		NodeRef companyHome = repository.getCompanyHome();
		String name = "Sample (" + System.currentTimeMillis() + ")";
		Map<QName, Serializable> contentProps = new HashMap<QName, Serializable>();
		contentProps.put(ContentModel.PROP_NAME, name);
		ChildAssociationRef association = nodeService.createNode(companyHome,
		        ContentModel.ASSOC_CONTAINS,
		        QName.createQName(NamespaceService.CONTENT_MODEL_PREFIX, name),
		        ContentModel.TYPE_CONTENT, contentProps);
		nodeService.deleteNode(association.getChildRef());

	}

}
