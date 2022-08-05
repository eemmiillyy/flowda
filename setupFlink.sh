curl -X POST -H "Expect:" -F "jarfile=@flow-flink-job/target/flow.flink.job-1.0-SNAPSHOT.jar" http://localhost:8081/jars/upload 
# | egrep -i '(?<=flink-web-upload\/).+(?=(.*?)","status)'