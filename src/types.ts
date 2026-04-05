export interface CharacterForm {
  name: string;
  gender: string;
  race: string;
  characterClass: string;
  serverUrl: string;
}

export interface ValidationErrors {
  name?: string;
  gender?: string;
  race?: string;
  characterClass?: string;
  serverUrl?: string;
}

export interface DiceRoll {
  dice_type: string;
  result: string | number;
  reason: string;
}

export interface StoryOutput {
  response: string;
  actions_suggestions: string[];
  details: string;
  dices_rolls: DiceRoll[];
}

export interface ParsedMessage {
  role: "user" | "assistant";
  text?: string;
  storyOutput?: StoryOutput;
}

export interface AbilityScores {
  strength: number;
  dexterity: number;
  constitution: number;
  intelligence: number;
  wisdom: number;
  charisma: number;
}

export interface InventoryItem {
  item_name: string;
  quantity: number;
}

export interface CharacterStats {
  character_id: string;
  name: string;
  character_class: string;
  race: string;
  gender: string;
  level: number;
  experience: number;
  stats: AbilityScores;
  inventory: InventoryItem[];
  created_at: string;
}
