# census-rm-exception-manager
This service provides a read-only API for case details.

# How to run
The Case Api service requires a Postgres instance to be running which contains the new casev2 schema.
Postgres can be started using either census-rm-docker-dev or "docker-compose up -d postgres-database".

# How to test
make test
