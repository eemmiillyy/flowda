import express from "express";
import { Kafka } from "kafkajs";
import WebSocket, { WebSocketServer } from "ws";
import { WEBSOCKET_PORT, BACKEND_PORT } from "./constants";

const clientId = (Math.random() + 1).toString(36).substring(10);

const kafkaClient = new Kafka({
  brokers: ["localhost:9093"],
  clientId,
});

const consumer = kafkaClient.consumer({ groupId: clientId });

type Data = {
  before?: {
    summed: number;
  };
  after?: {
    summed: number;
  };
  op: string;
};

const wss = new WebSocketServer({
  port: 4000,
});

const listOfClients = [];
const ws = new WebSocket(`ws://localhost:${WEBSOCKET_PORT}`);
wss.on("connection", function connection(ws) {
  ws.on("message", function message(data) {
    const parsed: Data = JSON.parse(data.toString());
    const formatted = parsed.after?.summed ?? "";
    // console.log("WS RECEIVED MESSAGE..", parsed);
    // console.log(wss.clients.size);
    // Broadcast to all clients
    // Save the previous messages in persistent storage and broadcast to all new clients upon joining.
    wss.clients.forEach((client) => {
      client.send(formatted.toString());
    });
  });
});

const consume = async () => {
  await consumer.connect();
  await consumer.subscribe({
    fromBeginning: true,
    topic: "eemmiillyy.inventory.products_on_hand_output",
  });
  await consumer.run({
    // Only runs if there is an update. It doesn't run for all topics
    eachMessage: async ({ message }) => {
      // console.log("NEW MESSAGE FROM KAFKA...", message.value.toString());
      ws.send(message.value.toString());
    },
  });
};

consume();
// if (wsOpen) {

// }
