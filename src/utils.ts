import type { CharacterForm, ValidationErrors } from "./types";

export function validateForm(form: CharacterForm): ValidationErrors {
  const errors: ValidationErrors = {};
  if (!form.name) errors.name = "Character name is required";
  if (!form.gender) errors.gender = "Gender is required";
  if (!form.race) errors.race = "Race is required";
  if (!form.characterClass) errors.characterClass = "Class is required";
  if (
    form.serverUrl &&
    !form.serverUrl.startsWith("http://") &&
    !form.serverUrl.startsWith("https://")
  ) {
    errors.serverUrl = "Must start with http:// or https://";
  }
  return errors;
}

export function formatInitPrompt(form: CharacterForm): string {
  return `Create a new player named ${form.name} who is a ${form.gender} ${form.race} ${form.characterClass}. You can then welcome them to the game. Describe the surroundings of the player and create an atmosphere that the player can bounce off of. Don't make more than 100 words.`;
}

export const GENDERS = ["Male", "Female", "Non-binary"];
export const RACES = [
  "Human",
  "Elf",
  "Dwarf",
  "Halfling",
  "Gnome",
  "Half-Elf",
  "Half-Orc",
  "Tiefling",
  "Dragonborn",
];
export const CLASSES = [
  "Barbarian",
  "Bard",
  "Cleric",
  "Druid",
  "Fighter",
  "Monk",
  "Paladin",
  "Ranger",
  "Rogue",
  "Sorcerer",
  "Warlock",
  "Wizard",
];
