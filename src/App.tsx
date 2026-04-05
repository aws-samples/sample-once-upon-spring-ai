import { BrowserRouter, Routes, Route } from "react-router-dom";
import { GameProvider } from "./store";
import NewGame from "./pages/NewGame";
import Game from "./pages/Game";

export default function App() {
  return (
    <GameProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<NewGame />} />
          <Route path="/game/:characterName" element={<Game />} />
        </Routes>
      </BrowserRouter>
    </GameProvider>
  );
}
