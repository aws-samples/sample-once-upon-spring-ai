import type { StoryOutput, CharacterStats } from "./types";

function baseUrl(serverUrl: string) {
  if (!serverUrl) return "";
  return serverUrl.replace(/\/+$/, "");
}

export async function sendInquiry(
  serverUrl: string,
  question: string,
): Promise<StoryOutput> {
  const res = await fetch(`${baseUrl(serverUrl)}/inquire`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ question }),
  });
  if (!res.ok) throw new Error(`Request failed: ${res.status}`);
  const data = await res.json();

  const raw = data.response ?? data;
  if (typeof raw === "string") {
    const cleaned = raw
      .replace(/```json\n?/g, "")
      .replace(/```/g, "")
      .trim();
    return JSON.parse(cleaned) as StoryOutput;
  }
  return raw as StoryOutput;
}

export async function fetchUser(
  serverUrl: string,
  userName: string,
): Promise<CharacterStats> {
  const res = await fetch(`${baseUrl(serverUrl)}/user/${userName}`);
  if (!res.ok) throw new Error(`Request failed: ${res.status}`);
  return (await res.json()) as CharacterStats;
}
