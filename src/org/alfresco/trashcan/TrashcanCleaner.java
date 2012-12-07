package org.alfresco.trashcan;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * @author rjmfernandes@gmail.com
 * 
 */
public class TrashcanCleaner {

	private static Log logger = LogFactory.getLog(TrashcanCleaner.class);

	private NodeService nodeService;
	private String archiveStoreUrl = "archive://SpacesStore";
	private int deleteBatchCount = 1000;
	private int daysToKeep = -1;
	private static final long DAYS_TO_MILLIS = 1000 * 60 * 60 * 24;

	public TrashcanCleaner(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public TrashcanCleaner(NodeService nodeService, int deleteBatchCount,
			int daysToKeep) {
		this(nodeService);
		this.deleteBatchCount = deleteBatchCount;
		this.daysToKeep = daysToKeep;
	}

	public TrashcanCleaner(NodeService nodeService, String archiveStoreUrl,
			int deleteBatchCount, int daysToKeep) {
		this(nodeService, deleteBatchCount, daysToKeep);
		this.archiveStoreUrl = archiveStoreUrl;
	}

	public void clean() {
		List<NodeRef> nodes = getBatchToDelete();

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Number of nodes to delete: %s", nodes
					.size()));
		}

		deleteNodes(nodes);

		if (logger.isDebugEnabled()) {
			logger.debug("Nodes deleted");
		}
	}

	private void deleteNodes(List<NodeRef> nodes) {
		for (int i = nodes.size(); i > 0; i--) {
			nodeService.deleteNode(nodes.get(i - 1));
		}
	}

	private List<NodeRef> getBatchToDelete() {
		List<ChildAssociationRef> childAssocs = getTrashcanChildAssocs();
		List<NodeRef> nodes = new ArrayList<NodeRef>(deleteBatchCount);
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Found %s nodes on trashcan",
					childAssocs.size()));
		}
		return fillBatchToDelete(nodes, childAssocs);
	}

	private List<NodeRef> fillBatchToDelete(List<NodeRef> batch,
			List<ChildAssociationRef> trashChildAssocs) {
		for (int j = trashChildAssocs.size(); j > 0
				&& batch.size() < deleteBatchCount; j--) {
			ChildAssociationRef childAssoc = trashChildAssocs.get(j - 1);
			NodeRef childRef = childAssoc.getChildRef();
			if (olderThanDaysToKeep(childRef)) {
				batch.add(childRef);
			}
		}
		return batch;
	}

	private List<ChildAssociationRef> getTrashcanChildAssocs() {
		StoreRef archiveStore = new StoreRef(archiveStoreUrl);
		NodeRef archiveRoot = nodeService.getRootNode(archiveStore);
		return nodeService.getChildAssocs(archiveRoot);
	}

	private boolean olderThanDaysToKeep(NodeRef node) {
		Date archivedDate = (Date) nodeService.getProperty(node,
				ContentModel.PROP_ARCHIVED_DATE);
		return daysToKeep * DAYS_TO_MILLIS < System.currentTimeMillis()
				- archivedDate.getTime();
	}

}
