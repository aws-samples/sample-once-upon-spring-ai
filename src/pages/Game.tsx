import { useState, useEffect, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useGameStore } from "../store";
import { sendInquiry, fetchUser } from "../api";
import type { ParsedMessage, CharacterStats, StoryOutput } from "../types";
import { marked } from "marked";
import "./Game.css";

marked.setOptions({ breaks: true, gfm: true });

export default function Game() {
  const { characterName } = useParams<{ characterName: string }>();
  const store = useGameStore();
  const navigate = useNavigate();
  const scrollRef = useRef<HTMLDivElement>(null);

  const [stats, setStats] = useState<CharacterStats | null>(null);
  const [messages, setMessages] = useState<ParsedMessage[]>(() => {
    if (store.initialResponse) {
      return [
        { role: "assistant" as const, storyOutput: store.initialResponse },
      ];
    }
    return [];
  });
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState<string | null>(null);
  const [input, setInput] = useState("");
  // Track which action was chosen per assistant message index
  const [chosenActions, setChosenActions] = useState<Record<number, string>>(
    {},
  );

  useEffect(() => {
    if (!store.serverUrl && !characterName) {
      navigate("/");
      return;
    }
    if (characterName) {
      fetchUser(store.serverUrl, characterName)
        .then(setStats)
        .catch(() => {});
    }
  }, [store.serverUrl, characterName, navigate]);

  useEffect(() => {
    scrollRef.current?.scrollTo({
      top: scrollRef.current.scrollHeight,
      behavior: "smooth",
    });
  }, [messages, sending]);

  const handleSend = async (text: string, fromMsgIndex?: number) => {
    if (!text.trim() || sending) return;
    // Track which action was chosen
    if (fromMsgIndex !== undefined) {
      setChosenActions((prev) => ({ ...prev, [fromMsgIndex]: text }));
    }
    setSending(true);
    setSendError(null);
    setInput("");
    setMessages((m) => [...m, { role: "user", text }]);
    try {
      const out = await sendInquiry(store.serverUrl, text);
      setMessages((m) => [...m, { role: "assistant", storyOutput: out }]);
    } catch (e) {
      setSendError(e instanceof Error ? e.message : "Failed to send");
    } finally {
      setSending(false);
    }
  };

  // Find the index of the last assistant message
  const lastAssistantIdx = (() => {
    for (let i = messages.length - 1; i >= 0; i--) {
      if (messages[i].role === "assistant") return i;
    }
    return -1;
  })();

  return (
    <div className="game-view">
      <aside className="game-sidebar">
        <StatsPanel stats={stats} />
      </aside>
      <main className="game-main glass-card">
        <div className="messages-scroll" ref={scrollRef}>
          {messages.length === 0 && !sending && (
            <p className="empty-msg">✦ Your adventure awaits ✦</p>
          )}
          {messages.map((msg, i) => (
            <div key={i} className={`msg msg-${msg.role}`}>
              <span className="msg-tag">
                {msg.role === "user" ? "⚔ You" : "🧙 Game Master"}
              </span>
              {msg.role === "user" ? (
                <p className="msg-text">{msg.text}</p>
              ) : msg.storyOutput ? (
                <StoryBlock
                  output={msg.storyOutput}
                  isLatest={i === lastAssistantIdx && !sending}
                  chosenAction={chosenActions[i]}
                  onAction={(text) => handleSend(text, i)}
                />
              ) : null}
            </div>
          ))}
          {sending && (
            <div className="msg msg-assistant">
              <span className="msg-tag">🧙 Game Master</span>
              <TypingFlavor />
            </div>
          )}
        </div>
        {sendError && <div className="send-error">{sendError}</div>}
        <form
          className="player-input"
          onSubmit={(e) => {
            e.preventDefault();
            handleSend(input);
          }}
        >
          <input
            type="text"
            placeholder="What do you do next?"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            disabled={sending}
            aria-label="Chat message"
          />
          <button type="submit" disabled={sending || !input.trim()}>
            Send
          </button>
        </form>
      </main>
    </div>
  );
}

function StatsPanel({ stats }: { stats: CharacterStats | null }) {
  if (!stats)
    return (
      <div className="stats-panel glass-card">
        <div className="portrait">⚔️</div>
        <p className="panel-loading">Awaiting hero data…</p>
      </div>
    );
  const s = stats.stats;
  const abilities: [string, number][] = [
    ["STR", s.strength],
    ["DEX", s.dexterity],
    ["CON", s.constitution],
    ["INT", s.intelligence],
    ["WIS", s.wisdom],
    ["CHA", s.charisma],
  ];
  return (
    <div className="stats-panel glass-card">
      <div className="portrait">⚔️</div>
      <h2 className="char-name">{stats.name}</h2>
      <div className="stat-section-label">Identity</div>
      <div className="stat-row">
        <span className="stat-lbl">Gender</span>
        <span className="stat-val">{stats.gender}</span>
      </div>
      <div className="stat-row">
        <span className="stat-lbl">Race</span>
        <span className="stat-val">{stats.race}</span>
      </div>
      <div className="stat-row">
        <span className="stat-lbl">Class</span>
        <span className="stat-val">{stats.character_class}</span>
      </div>
      <div className="stat-section-label">Vitals</div>
      <div className="stat-row">
        <span className="stat-lbl">Level</span>
        <span className="stat-val">{stats.level}</span>
      </div>
      <div className="stat-row">
        <span className="stat-lbl">XP</span>
        <span className="stat-val">{stats.experience}</span>
      </div>
      <div className="stat-section-label">Abilities</div>
      <div className="abilities">
        {abilities.map(([lbl, val]) => (
          <div key={lbl} className="ability-box">
            <span className="ability-lbl">{lbl}</span>
            <span className="ability-val">{val}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

function StoryBlock({
  output,
  isLatest,
  chosenAction,
  onAction,
}: {
  output: StoryOutput;
  isLatest: boolean;
  chosenAction?: string;
  onAction: (s: string) => void;
}) {
  const html = marked.parse(output.response) as string;
  const hasActions = output.actions_suggestions?.length > 0;
  const isPast = !isLatest || !!chosenAction;

  return (
    <>
      <div className="narrative" dangerouslySetInnerHTML={{ __html: html }} />
      {output.dices_rolls?.length > 0 && (
        <div className="dice-rolls">
          {output.dices_rolls.map((r, i) => (
            <DiceChip
              key={i}
              type={r.dice_type}
              result={String(r.result)}
              reason={r.reason}
            />
          ))}
        </div>
      )}
      {hasActions && (
        <div className="inline-actions">
          {output.actions_suggestions.map((a, i) => {
            const isChosen = chosenAction === a;
            const isFaded = isPast && !isChosen;
            return (
              <button
                key={i}
                className={`suggestion-btn${isChosen ? " chosen" : ""}${isFaded ? " faded" : ""}`}
                onClick={() => !isPast && onAction(a)}
                disabled={isPast}
              >
                {a}
              </button>
            );
          })}
        </div>
      )}
      {output.details && <p className="gm-details">📋 {output.details}</p>}
    </>
  );
}

const DICE_COLORS: Record<string, string> = {
  d4: "#e74c3c",
  d6: "#27ae60",
  d8: "#2980b9",
  d10: "#8e44ad",
  d12: "#e67e22",
  d20: "#c0392b",
  d100: "#16a085",
};

function DiceChip({
  type,
  result,
  reason,
}: {
  type: string;
  result: string;
  reason: string;
}) {
  const [svgFailed, setSvgFailed] = useState(false);
  const bg = DICE_COLORS[type] || "#7f8c8d";
  return (
    <span className="dice-chip" title={reason}>
      <span className="dice-image-wrapper">
        {!svgFailed ? (
          <>
            <img
              src={`/dice/${type}.svg`}
              alt={`${type} dice`}
              className="dice-svg"
              onError={() => setSvgFailed(true)}
            />
            <span className="dice-result-overlay">{result}</span>
          </>
        ) : (
          <span
            className="dice-shape"
            style={{ background: `linear-gradient(135deg, ${bg}, ${bg}dd)` }}
          >
            {result}
          </span>
        )}
      </span>
      <span>
        <span className="dice-label">{type}</span>
        {reason && <span className="dice-reason"> · {reason}</span>}
      </span>
    </span>
  );
}

const DM_PHRASES = [
  "🎲 Rolling for fate...",
  "📜 Consulting the ancient scrolls...",
  "🔮 Peering into the crystal ball...",
  "🐉 Whispering to the dragons...",
  "🌙 Reading the stars...",
  "📖 Turning the page of destiny...",
];

function TypingFlavor() {
  const [phrase, setPhrase] = useState(
    () => DM_PHRASES[Math.floor(Math.random() * DM_PHRASES.length)],
  );

  useEffect(() => {
    const interval = setInterval(() => {
      setPhrase(DM_PHRASES[Math.floor(Math.random() * DM_PHRASES.length)]);
    }, 3000);
    return () => clearInterval(interval);
  }, []);

  return <p className="typing-flavor">{phrase}</p>;
}
