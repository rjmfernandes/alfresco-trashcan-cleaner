This library adds a scheduled job that will empty your Alfresco trashcan according to configuration. It receives as parameters (configured on alfresco-global.properties):

`trashcan.cron=0 30 * * * ?`

`trashcan.daysToKeep=1`

`trashcan.deleteBatchCount=1000`

In the above configuration the scheduled process will clean all deleted items older than one day to a maximum of 1000 (each execution) each hour at the middle of the hour (30 minutes). In case you wish to delete all items (to the max number set) irrespective of the archived date just set trashcan.daysToKeep to -1.

The major differences with existing addon (http://addons.alfresco.com/addons/trashcan-cleaner) is the fact this job is not based on search engine and the scheduled job is cluster aware (uses the Alfresco [org.alfresco.schedule.ScheduledJobLockExecuter](https://svn.alfresco.com/repos/alfresco-open-mirror/alfresco/HEAD/root/projects/repository/source/java/org/alfresco/schedule/ScheduledJobLockExecuter.java)).

This has been tested for:

- Alfresco Enterprise 4.1.1.3 (use version https://code.google.com/p/alfresco-trashcan-cleaner/source/browse/?name=1.2.0)

- Alfresco Community 4.2.e (use version - https://code.google.com/p/alfresco-trashcan-cleaner/source/browse/?name=v2.0.0) - only unit tests

- Alfresco Enterprise 4.2.0  (last version - https://code.google.com/p/alfresco-trashcan-cleaner/source/browse/?name=v2.1.0)  - only unit tests

If you want to add log related to this job, just add to log4j.properties:

`log4j.logger.org.alfresco.trashcan=debug`

