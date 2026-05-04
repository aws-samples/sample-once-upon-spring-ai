#!/usr/bin/env bash
#
# Regenerate chapter*-maven*/ folders from JBang sources.
# Called manually for bootstrap and by .github/workflows/maven-sync.yml
# on every push to main that touches chapter[1-5]/**/*.java.
#
# chapterN/ is always the source of truth. chapter*-maven*/ are generated
# and must never be hand-edited.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

echo "Regenerating Maven projects under $REPO_ROOT ..."

# Remove previous output. Glob is anchored to REPO_ROOT via cd above.
rm -rf chapter*-maven chapter*-maven-*

# Single-entry-point chapters
jbang export maven --force -O chapter1-maven         chapter1/DungeonMasterSimple.java
jbang export maven --force -O chapter2-maven         chapter2/DungeonMasterWithBuiltInTools.java
jbang export maven --force -O chapter3-maven         chapter3/DungeonMasterWithCustomTools.java

# Chapter 4 has two entry points
jbang export maven --force -O chapter4-maven-server  chapter4/DiceRollMcpServer.java
jbang export maven --force -O chapter4-maven-client  chapter4/DungeonMasterMCPClient.java

# Chapter 5 — multi-module Maven project. Each entry point becomes a module;
# parent pom is the hand-authored template in scripts/.
mkdir -p chapter5-maven
jbang export maven --force -O chapter5-maven/rules       chapter5/agents/rules/RulesAgent.java
jbang export maven --force -O chapter5-maven/character   chapter5/agents/character/CharacterAgent.java
jbang export maven --force -O chapter5-maven/gamemaster  chapter5/agents/gamemaster/GameMasterOrchestrator.java
jbang export maven --force -O chapter5-maven/utils       chapter5/utils/CreateKnowledgeBase.java
cp scripts/chapter5-parent-pom.xml chapter5-maven/pom.xml

echo "Done. Generated folders:"
ls -d chapter*-maven chapter*-maven-* 2>/dev/null | sort
