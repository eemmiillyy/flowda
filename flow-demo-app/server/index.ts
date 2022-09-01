import { Kafka } from "kafkajs";
import { WebSocketServer } from "ws";
import * as fs from "fs";
import * as path from "path";

// Shape of simple query message output from kafka
type Data = {
  before?: {
    summed: number;
  };
  after?: {
    summed: number;
  };
  op: string;
};

// Web socket server
const wsServer = new WebSocketServer({
  port: 4000,
});

// Kafka consumer config
const environmentId = "simple"; // May need changing
const clientId = "emily"; // May need changing - if so, create a new file called [clientId].json with an empty array ([])
const apiKey = "bleepbloop"; // May need changing
const topicName = environmentId + ".inventory.custom_output_table_name";

const kafkaClient = new Kafka({
  // Change the host to the remote IP of the machine for testing production
  brokers: ["localhost:9093"],
  clientId: clientId,
  sasl: {
    mechanism: "scram-sha-256",
    username: clientId,
    password: apiKey,
  },
  connectionTimeout: 6000,
  authenticationTimeout: 6000,
  logLevel: 0,
});

// TODO if this file does not already exist on disk, instead of failing, create it.
const p = path.join(__dirname, environmentId + ".json");

const writeToFile = (message: string) => {
  if (!fs.existsSync(p) || fs.readFileSync(p, "utf-8") === "") {
    fs.writeFileSync(p, `[${message}]`, { flag: "w" });
  } else {
    // Read content from file
    const content = fs.readFileSync(p, "utf-8");
    // JSON parse the existing content
    const existing: Data[] = JSON.parse(content);
    // JSON parse the new message
    const newMessage = JSON.parse(message);
    // Merge the two in memory
    existing.push(newMessage);
    // Stringify the merged content together
    const formatted = Object.assign([], existing);
    // Overwrite entire file contents with the stringified content
    fs.writeFileSync(p, JSON.stringify(formatted));
  }
};

const consumer = kafkaClient.consumer({ groupId: clientId });

// When a client connects to the websocket,
// instantiante the kafka client if it does not yet exist
// or send the contents from on disk memory ([clientId].json file) if it already exists.
// Note that [clientId].json file MUST exist, and contain an empty array ([]) to start.
wsServer.on("connection", async () => {
  const description = await consumer.describeGroup();
  const containsClient = [
    ...description.members.map((member) => member.clientId),
  ].includes(clientId);
  console.log(
    "client is already connected and listening for new messages...",
    containsClient
  );

  if (!containsClient) {
    try {
      // Should execute once and only once.
      await consumer.connect();
      await consumer.subscribe({
        fromBeginning: true,
        topic: topicName,
      });
      await consumer.run({
        eachMessage: async ({ message }) => {
          console.log("NEW MESSAGE FROM KAFKA...", message.value.toString());
          // Write new message to file before sending it.
          writeToFile(message.value.toString());
          wsServer.clients.forEach((client) => {
            client.send(message.value.toString());
          });
        },
      });
    } catch (e) {}
  } else {
    console.log("sending cache from file");
    const content = fs.readFileSync(p, "utf-8");
    const parsed: Data[] = JSON.parse(content);
    parsed.forEach((msg) => {
      wsServer.clients.forEach((client) => {
        client.send(JSON.stringify(msg));
      });
    });
  }
});

wsServer.on("close", async () => {
  console.log("client disconnected");
  await consumer.stop();
  await consumer.disconnect();
});
