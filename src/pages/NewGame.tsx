import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useGameStore } from "../store";
import { sendInquiry } from "../api";
import {
  validateForm,
  formatInitPrompt,
  GENDERS,
  RACES,
  CLASSES,
} from "../utils";
import type { CharacterForm, ValidationErrors } from "../types";
import "./NewGame.css";

export default function NewGame() {
  const navigate = useNavigate();
  const store = useGameStore();
  const [form, setForm] = useState<CharacterForm>({
    name: "",
    gender: "",
    race: "",
    characterClass: "",
    serverUrl: store.serverUrl || "",
  });
  const [errors, setErrors] = useState<ValidationErrors>({});
  const [loading, setLoading] = useState(false);
  const [loadingPhase, setLoadingPhase] = useState("");
  const [submitError, setSubmitError] = useState<string | null>(null);

  const update = (field: keyof CharacterForm, value: string) =>
    setForm((f) => ({ ...f, [field]: value }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const v = validateForm(form);
    if (Object.keys(v).length > 0) {
      setErrors(v);
      return;
    }
    setErrors({});
    setSubmitError(null);
    setLoading(true);

    // Staged loading messages
    const phases = [
      `⚒️ Forging ${form.name || "hero"}...`,
      `🛡️ ${form.race} ${form.characterClass} is spawning...`,
      "🌍 Building the realm...",
      "📜 The storyline unfolds...",
    ];
    let phaseIdx = 0;
    setLoadingPhase(phases[0]);
    const interval = setInterval(() => {
      phaseIdx++;
      if (phaseIdx < phases.length) {
        setLoadingPhase(phases[phaseIdx]);
      }
    }, 2500);

    try {
      const response = await sendInquiry(
        form.serverUrl,
        formatInitPrompt(form),
      );
      clearInterval(interval);
      setLoadingPhase("✨ Your adventure begins!");
      store.setConnection(form.serverUrl, form.name, response);
      await new Promise((r) => setTimeout(r, 600));
      navigate(`/game/${form.name}`);
    } catch (err) {
      clearInterval(interval);
      setSubmitError(err instanceof Error ? err.message : "Failed to connect");
    } finally {
      setLoading(false);
    }
  };

  const field = (
    id: string,
    label: string,
    key: keyof CharacterForm,
    opts?: string[],
    placeholder?: string,
  ) => (
    <div className="form-group">
      <label htmlFor={id} className="form-label">
        {label}
      </label>
      {opts ? (
        <select
          id={id}
          className={`form-input${errors[key] ? " input-error" : ""}`}
          value={form[key]}
          onChange={(e) => update(key, e.target.value)}
        >
          <option value="" disabled>
            Choose {label.toLowerCase()}
          </option>
          {opts.map((o) => (
            <option key={o} value={o}>
              {o}
            </option>
          ))}
        </select>
      ) : (
        <input
          id={id}
          type="text"
          className={`form-input${errors[key] ? " input-error" : ""}`}
          placeholder={placeholder || `Enter ${label.toLowerCase()}`}
          value={form[key]}
          onChange={(e) => update(key, e.target.value)}
        />
      )}
      {errors[key] && <span className="field-error">{errors[key]}</span>}
    </div>
  );

  return (
    <div className="new-game-view">
      <div className="form-card glass-card">
        <h1 className="form-title">⚔️ Forge Your Hero</h1>
        <p className="form-subtitle">Create a character and enter the realm</p>
        <form className="character-form" onSubmit={handleSubmit}>
          {submitError && <div className="submit-error">{submitError}</div>}
          {field("name", "Character Name", "name")}
          {field("gender", "Gender", "gender", GENDERS)}
          {field("race", "Race", "race", RACES)}
          {field("class", "Class", "characterClass", CLASSES)}
          <div className="form-divider">
            <span>Connection</span>
          </div>
          {field(
            "url",
            "Server URL",
            "serverUrl",
            undefined,
            "Leave empty for local proxy",
          )}
          <button type="submit" className="start-button" disabled={loading}>
            {loading ? loadingPhase : "Begin Adventure"}
          </button>
        </form>
      </div>
    </div>
  );
}
