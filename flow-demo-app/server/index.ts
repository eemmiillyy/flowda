import { Kafka } from "kafkajs";
import { WebSocketServer } from "ws";
import * as fs from "fs";
import * as path from "path";

type Data = {
  before?: {
    summed: number;
  };
  after?: {
    summed: number;
  };
  op: string;
};

// Kafka consumer
const clientId = "jsjsj";
const apiKey = "AyfPvH/Z626h6OS9rlfDPVlSj7EvnoxUZEIoBj4BpHU=";
const topicName = clientId + ".inventory.custom_output_table_name";

const kafkaClient = new Kafka({
  brokers: ["localhost:9093"],
  clientId,
  sasl: {
    mechanism: "scram-sha-256",
    username: clientId,
    password: apiKey,
  },
  connectionTimeout: 6000,
  authenticationTimeout: 6000,
  logLevel: 0,
  ssl: {
    rejectUnauthorized: false,
    ca: [fs.readFileSync("/private/etc/ssl/flowda/ca-cert", "utf-8")],
  },
});
const p = path.join(__dirname, clientId + ".json");

const writeToFile = (message: string) => {
  if (!fs.existsSync(p) || fs.readFileSync(p, "utf-8") === "") {
    fs.writeFileSync(p, `[${message}]`, { flag: "w" });
  } else {
    // read content
    const content = fs.readFileSync(p, "utf-8");
    // json parse it
    const existing: Data[] = JSON.parse(content);

    const newMessage = JSON.parse(message);

    existing.push(newMessage);

    // stringify new message
    const formatted = Object.assign([], existing);

    // overwrite entire file contents
    fs.writeFileSync(p, JSON.stringify(formatted));
  }
};
const consumer = kafkaClient.consumer({ groupId: clientId });

const consume = async () => {
  await consumer.run({
    // Only runs if there is an update. It doesn't run for all topics
    eachMessage: async ({ message }) => {
      console.log("NEW MESSAGE FROM KAFKA...", message.value.toString());
      writeToFile(message.value.toString());
      wsServer.clients.forEach((client) => {
        client.send(message.value.toString());
      });
    },
  });
};

// Web socket server
const wsServer = new WebSocketServer({
  port: 4000,
});

// On client connection try and consume
wsServer.on("connection", async (client) => {
  const description = await consumer.describeGroup();
  // No group members,
  const needsRestart =
    description.state === "Dead" || description.state === "Empty";
  if (needsRestart) {
    await consumer.connect();
    await consumer.subscribe({
      fromBeginning: true,
      topic: topicName,
    });
    consume();
  }
  if (description.state === "Stable") {
    const content = fs.readFileSync(p, "utf-8");
    // If there are no more messages to consume read them from the file
    const parsed: Data[] = JSON.parse(content);

    parsed.forEach((msg) => {
      wsServer.clients.forEach((client) => {
        client.send(JSON.stringify(msg));
      });
    });
  }
});

wsServer.on("close", async () => {
  await consumer.stop();
  await consumer.disconnect();
});
