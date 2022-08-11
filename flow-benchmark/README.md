## Benchmarking

Start docker image services
Start application server in development mode
Submit job through app with desired query to monitor (TODO - automate this)
with environmentId set to emily.

`mvn install`

`cd ../`
`./setupFlink.sh` If broken pipe shows in terminal, services are not ready. Wait and re rerun.
`./setupConnector.sh` for simple query benchmark
Different databases and jobs are needed for running simple versus complex benchmarks. After running simple benchmark, change the environmentId in `setupConnector.sh` to "complex" and the connectionString in the first post request to "mysqltwo".
`./setupConnector.sh -c` for complex query benchmark
Change environment Id to complex in `BenchmarkRunner.java`
`mvn clean verify`
`java -jar target/benchmarks.jar`

> Need to manually change the environmentId you want to create with setupConnector.sh TODO replace with bash arg.
> Need to then use that environmentId in `BechmarkRunner.java` then rebuild and run.
> Need to reset the database entirely before running a benchmark
> You can only have ONE job per database running at a time.

## Building

`mvn clean verify`

## Running

`java -jar target/benchmarks.jar`
