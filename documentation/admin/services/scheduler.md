# Scheduler

The scheduler service triggers tasks which have to be routinely called in other services.

## Configuration

The timings for the triggers can be configured with the following environment variables:

| Environment Variable                  | Description                                                                                   | Default            |
| ------------------------------------- | --------------------------------------------------------------------------------------------- | ------------------ |
| CRON_PRUNE_TERMINATED                 | Trigger for deleting terminated sessions in request-processing                                | 0 0 \* ? \* \* \*  |
| CRON_PRUNE_TIMEOUT                    | Trigger for deleting sessions where nothing happened for a period of time except for creation | 0 0 \* ? \* \* \*  |
| CRON_PRUNE_SUBMISSION_TIMEOUT         | Trigger for deleting sessions where no submission took place within a period of time          | 0 0 \* ? \* \* \*  |
| CRON_PRUNE_HTTP_AUDIT_LOGS            | Trigger for deletion of audit logs                                                            | 0 0 0 \*/1 \* ? \* |
| CRON_ISSUE_REVOCATION_CREDENTIALS     | Trigger for issuance of the revocation credentials                                            | 0 0 \* ? \* \* \*  |
| CRON_PROFILE_REQUEST_OUTSTANDING_DIDS | Trigger for requesting outstanding DIDs                                                       | 0 0 \* ? \* \* \*  |


**NOTE:** detailed documentation of the configuration values with prefix `QUARKUS_` can be found within the Quarkus framework: https://quarkus.io/guides/all-config

The values are given in Cron syntax in the style of quartz scheduler.
See [online-documentation](http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html) for more information.

Note that the triggers don't delete directly at the point in time they fire.
The called services have properties to define retention periods for the different items within their configuration.
Audit logs for example are kept for 6 Years by default. With the defaults shown above this period would be checked each day.
