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

# RulesAgent.java declares `//FILES gm_knowledge_base.json=./../../utils/gm_knowledge_base.json`.
# The source copy is gitignored (it's produced by participants running CreateKnowledgeBase.java),
# so a fresh checkout on CI does not have it and `jbang export` would abort with
# "Failed to get contents from resource './../../utils/gm_knowledge_base.json'".
# The committed Maven copy (chapter5-maven/rules/src/main/resources/) carries the
# same content and is our distribution seed — use it to satisfy //FILES if the
# participant-generated source is absent, then remove the temporary seed afterwards.
KB_SEED_IN_MAVEN="chapter5-maven/rules/src/main/resources/gm_knowledge_base.json"
KB_SOURCE="chapter5/utils/gm_knowledge_base.json"
SEEDED_FROM_MAVEN=false
if [ ! -f "$KB_SOURCE" ] && [ -f "$KB_SEED_IN_MAVEN" ]; then
    cp "$KB_SEED_IN_MAVEN" "$KB_SOURCE"
    SEEDED_FROM_MAVEN=true
fi

# Remove previous output. Glob is anchored to REPO_ROOT via cd above.
rm -rf chapter*-maven chapter*-maven-*

# Single-entry-point chapters
jbang export maven --force -O chapter1-maven         chapter1/GameMasterSimple.java
jbang export maven --force -O chapter2-maven         chapter2/GameMasterWithBuiltInTools.java
jbang export maven --force -O chapter3-maven         chapter3/GameMasterWithCustomTools.java

# Chapter 4 has two entry points
jbang export maven --force -O chapter4-maven-server  chapter4/DiceRollMcpServer.java
jbang export maven --force -O chapter4-maven-client  chapter4/GameMasterMCPClient.java

# Chapter 5 — multi-module Maven project. Each entry point becomes a module;
# parent pom is the hand-authored template in scripts/.
mkdir -p chapter5-maven
pushd chapter5-maven > /dev/null
jbang export maven --force -O rules       "$REPO_ROOT/chapter5/agents/rules/RulesAgent.java"
jbang export maven --force -O character   "$REPO_ROOT/chapter5/agents/character/CharacterAgent.java"
jbang export maven --force -O gamemaster  "$REPO_ROOT/chapter5/agents/gamemaster/GameMasterOrchestrator.java"
jbang export maven --force -O utils       "$REPO_ROOT/chapter5/utils/CreateKnowledgeBase.java"
popd > /dev/null
cp scripts/chapter5-parent-pom.xml chapter5-maven/pom.xml

# If we seeded the source KB from the committed Maven copy, remove it again so
# the gitignored path stays clean on disk. The export already copied it into
# chapter5-maven/rules/src/main/resources/ for distribution.
if [ "$SEEDED_FROM_MAVEN" = true ]; then
    rm -f "$KB_SOURCE"
fi

echo "Done. Generated folders:"
ls -d chapter*-maven chapter*-maven-* 2>/dev/null | sort
