import React from "react";

type Props = {
  ws: WebSocket;
};
export const Message = ({ ws }: Props) => {
  const [message, setMessages] = React.useState<string>("");

  ws.onopen = (w) => {
    console.log("opened", w);
    ws.onmessage = (message) => {
      setMessages(message.data.toString());
    };
  };
  return <p>{message}</p>;
};
