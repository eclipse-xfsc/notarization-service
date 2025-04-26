@ext1
Feature: CP.NOTAR.E1.00053 Readiness Checkups

    All components MUST reflect after bootstrap and during runtime the correctness of the service
    functionality by reflecting it over health endpoints. The health endpoint MUST return failure (red), if
    any internal behavior failure or misconfiguration occurs (not just the software running state). This
    means for instance to check continuously during the runtime:

    - A unreachable configured Services results in failed state
    - Configured Service Endpoints needs to be checked for readiness during runtime, if not
    reachable, it results in failure state
    - Check depending components (Database, Microservice etc.) behind it, if not reachable, it
    results in failed state

    Take a look at the following endpoints of the services:
        - /q/health/ready

    It can look like the following:

    {
        "status": "UP",
        "checks": [
            {
                "name": "Reactive PostgreSQL connections health check",
                "status": "UP",
                "data": {
                    "<default>": "UP"
                }
            },
            {
                "name": "Database connections health check",
                "status": "UP",
                "data": {
                    "<default>": "UP"
                }
            },
            {
                "name": "SmallRye Reactive Messaging - readiness check",
                "status": "UP",
                "data": {
                    "operator-request-changed": "[OK]",
                    "requestor-request-changed": "[OK]"
                }
            }
        ]
    }

@happypath
Scenario: The request-processing service does support readiness checks
  Given The 'request-processing' service is running
  When I perform a readiness check at the 'request-processing' service
  Then I receive a response that contains information about
        | Reactive PostgreSQL connections health check  |
        | Database connections health check |
        | SmallRye Reactive Messaging - readiness check |

@happypath
Scenario: The profile service does support readiness checks
  Given The 'profile' service is running
  When I perform a readiness check at the 'profile' service
  Then I receive a response that contains information about
        | Reactive PostgreSQL connections health check  |
        | Database connections health check |
