# eval docker-compose down

# eval docker-compose up -d

# Upload job TODO: if broken pipe then RETRY with exponential backoff
OUTPUT=$(curl -X POST -H "Expect:" -F "jarfile=@flow-flink-job/target/flow.flink.job-1.0-SNAPSHOT.jar" http://localhost:8081/jars/upload) 
# OUTPUT='{"filename":"/tmp/flink-web-f0f65247-16a3-473d-a197-0bfe81871acc/flink-web-upload/e0d2e0a9-8a13-4f9d-b825-acbc4dcd77e3_flow.flink.job-1.0-SNAPSHOT.jar","status":"success"}'

# Get job Id
JOBID=$(echo $OUTPUT |sed -e 's/",.*//' |sed 's/.*flink-web-upload\///')
echo $JOBID

# Update settings.json 
sed -i "" "s/jars\/.*.*run/jars\/$JOBID\/run/" ./flow-core/src/main/resources/Settings.json

# Check envs are set
if [ -z ${STAGE} ]; then echo "stage is unset! Run export STAGE=development" && exit 1; fi
if [ -z ${SECRET} ]; then echo "secret is unset! export SECRET=XXXXX" && exit 1; fi

#Start server
cd flow-core
eval java -jar target/flow.core-1.0-SNAPSHOT.jar  


