
# Upload job TODO: if broken pipe then RETRY with exponential backoff
OUTPUT=$(curl -X POST -H "Expect:" -F "jarfile=@flow-flink-job/target/flow.flink.job-1.0-SNAPSHOT.jar" http://localhost:8081/jars/upload) 

# Get job Id
JOBID=$(echo $OUTPUT |sed -e 's/",.*//' |sed 's/.*flink-web-upload\///')
echo $JOBID

if [ -z ${JOBID} ]; then
    echo "Issue uploading job to flink! If broken pipe is output, retry in a few seconds. If another error is show, make sure that the flink job has been packaged and output to flow.flink.job-1.0-SNAPSHOT.jar and that services are up and healthy." 
    exit 1
fi

# Update settings.json 
sed -i "" "s/jars\/.*.*run/jars\/$JOBID\/run/" ./flow-core/src/main/resources/Settings.json

# Check envs are set
if [ -z ${STAGE} ]; then echo "stage is unset! Run export STAGE=development" && exit 1; fi
if [ -z ${SECRET} ]; then echo "secret is unset! export SECRET=XXXXX" && exit 1; fi

#Start server
cd flow-core
eval java -jar target/flow.core-1.0-SNAPSHOT.jar  


