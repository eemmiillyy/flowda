import express from "express";
import { Kafka } from "kafkajs";
import WebSocket, { WebSocketServer } from "ws";
import { WEBSOCKET_PORT, BACKEND_PORT } from "./constants";

const app = express();

app.get("/", (req, res) => {
  res.send("Hello World!");
});

app.listen(() => {
  return console.log(
    `Express is listening at http://localhost:${BACKEND_PORT}`
  );
});

const clientId = "example-app";

const kafkaClient = new Kafka({
  brokers: ["localhost:9093"],
  clientId,
});

const consumer = kafkaClient.consumer({ groupId: clientId });

type Data = {
  before?: {
    summedQ: number;
  };
  after?: {
    summedQ: number;
  };
  op: string;
};

const consume = async () => {
  await consumer.connect();
  await consumer.subscribe({
    fromBeginning: true,
    topic: "dbserver1.inventory.products_on_hand_output",
  });
  await consumer.run({
    // Only runs if there is an update. It doesn't run for all topics
    eachMessage: async ({ message }) => {
      console.log("NEW MESSAGE FROM KAFKA...", message.value.toString());
      ws.send(message.value.toString());
    },
  });
};

const wss = new WebSocketServer({
  port: 4000,
});
const ws = new WebSocket(`ws://localhost:${WEBSOCKET_PORT}`);
wss.on("connection", function connection(ws) {
  ws.on("message", function message(data) {
    const parsed: Data = JSON.parse(data.toString());
    const formatted = parsed.after?.summedQ ?? "";
    // Broadcast to all clients
    wss.clients.forEach((client) => {
      client.send(formatted.toString());
    });
  });
});
consume();
