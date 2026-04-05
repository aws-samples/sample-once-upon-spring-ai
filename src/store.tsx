import { createContext, useContext, useState, type ReactNode } from "react";
import type { StoryOutput } from "./types";

interface GameState {
  serverUrl: string;
  characterName: string;
  initialResponse: StoryOutput | null;
  setConnection: (url: string, name: string, initial?: StoryOutput) => void;
  reset: () => void;
}

const KEY = "rpg-gm-server-url";
const GameContext = createContext<GameState | null>(null);

export function GameProvider({ children }: { children: ReactNode }) {
  const [serverUrl, setServerUrl] = useState(
    () => localStorage.getItem(KEY) ?? "",
  );
  const [characterName, setCharacterName] = useState("");
  const [initialResponse, setInitialResponse] = useState<StoryOutput | null>(
    null,
  );

  const setConnection = (url: string, name: string, initial?: StoryOutput) => {
    setServerUrl(url);
    setCharacterName(name);
    if (initial) setInitialResponse(initial);
    localStorage.setItem(KEY, url);
  };

  const reset = () => {
    setServerUrl("");
    setCharacterName("");
    setInitialResponse(null);
    localStorage.removeItem(KEY);
  };

  return (
    <GameContext.Provider
      value={{
        serverUrl,
        characterName,
        initialResponse,
        setConnection,
        reset,
      }}
    >
      {children}
    </GameContext.Provider>
  );
}

export function useGameStore() {
  const ctx = useContext(GameContext);
  if (!ctx) throw new Error("useGameStore must be inside GameProvider");
  return ctx;
}
