import "./App.css";
import { Message } from "./Message";

function App() {
  // Websocket Client
  const ws = new WebSocket(`ws://localhost:4000`);
  return (
    <div className="App">
      <header className="App-header">
        <div>
          <Message ws={ws}></Message>
        </div>
      </header>
    </div>
  );
}

export default App;
