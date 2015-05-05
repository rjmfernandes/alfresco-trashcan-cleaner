This library adds a scheduled job that will empty your Alfresco trashcan according to configuration. 
It receives as parameters (configured on alfresco-global.properties):

trashcan.cron=0 30 * * * ?

trashcan.daysToKeep=1

trashcan.deleteBatchCount=1000

In the above configuration the scheduled process will clean all deleted items older than one day to a maximum of 1000 (each execution) each hour at the middle of the hour (30 minutes). 
In case you wish to delete all items (to the max number set) irrespective of the archived date just set trashcan.daysToKeep to -1.

The major differences with existing addon (http://addons.alfresco.com/addons/trashcan-cleaner) is the fact this job is not based on search engine and the scheduled job is cluster aware (uses the Alfresco org.alfresco.schedule.ScheduledJobLockExecuter).

This has been tested for:

- Alfresco Enterprise 4.1.1.3 (use version https://github.com/rjmfernandes/alfresco-trashcan-cleaner/tree/1.2.0)

- Alfresco Community 4.2.e (use version - https://github.com/rjmfernandes/alfresco-trashcan-cleaner/tree/v2.0.0) - only unit tests

- Alfresco Enterprise 4.2.0 (last version - https://github.com/rjmfernandes/alfresco-trashcan-cleaner/tree/v2.1.0) - only unit tests

If you want to add log related to this job, just add to log4j.properties:

log4j.logger.org.alfresco.trashcan=debug

This new version takes into account the new way Alfresco 4.2 trashes content keeping a secondary parent reference inside trashcan to the original owner (those references dont have an archive date associated with it what caused an error when trying past versions of the cleaner with 4.2). 
The new trashcan cleaner does not try to delete those references.
