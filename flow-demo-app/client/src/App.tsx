import React from "react";
import "./App.css";

function App() {
  var ws = new WebSocket(`ws://localhost:4000`);
  ws.onopen = (w) => {
    console.log("opened", w);
  };
  ws.onmessage = (message) => {
    setMessages(message.data.toString());
  };

  const [message, setMessages] = React.useState<string>("");

  return (
    <div className="App">
      <header className="App-header">
        <div>
          <p>{message}</p>
        </div>
      </header>
    </div>
  );
}

export default App;
