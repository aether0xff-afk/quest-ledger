# Asking GPT to write QuestScript

Quest Ledger does not call an LLM. Copy the relevant documentation and the prompt below into ChatGPT or another LLM, then paste the returned QuestScript into code mode.

## Prompt template

```text
Write a Minecraft Quest Ledger quest using QuestScript v0.2.

Rules:
- Return only QuestScript source. Do not use Markdown code fences.
- Use only functions, properties, animations, and syntax from the supplied documentation.
- Never invent a Minecraft resource ID or QuestScript function.
- Use namespaced IDs such as "minecraft:diamond".
- Add parentheses whenever AND and OR are mixed.
- Do not use hold, remove after, duration, or timer fields.
- If the goal can be measured from Minecraft state, use an automatic condition.
- If the goal is subjective or is a build/project completion that Minecraft cannot reliably detect, use manual.checked == true.
- A manual condition may be combined with measurable conditions for a hybrid quest.
- Do not add conditions that I did not request.

QuestScript documentation:
[Paste docs/QUESTSCRIPT.md and, when useful, docs/QUEST_TYPES.md here]

My quest:
[Describe the quest here]
```

## Automatic example

### Request

```text
네더에서 고대 잔해를 4개 이상 캐거나 네더라이트 주괴를 하나 가지고 있으면 완료되는 퀘스트를 만들어줘. 밀랍 인장 애니메이션을 사용해.
```

### Expected result

```questscript
quest "네더라이트 준비" {
  complete when {
    (
      player.dimension == "minecraft:the_nether"
      and stat.mined("minecraft:ancient_debris") >= 4
    )
    or inventory.count("minecraft:netherite_ingot") >= 1
  }

  animation "wax_seal"
}
```

## Manual example

### Request

```text
금 공장을 완성하면 내가 직접 완료 확인을 누르는 퀘스트를 만들어줘.
```

### Expected result

```questscript
quest "금 공장 완성" {
  complete when {
    manual.checked == true
  }

  animation "page_fold"
}
```

## Hybrid example

### Request

```text
금 공장을 완성했다고 직접 확인하고, 인벤토리에 금 주괴가 64개 이상 있으면 완료되는 퀘스트를 만들어줘.
```

### Expected result

```questscript
quest "금 공장 가동 확인" {
  complete when {
    manual.checked == true
    and inventory.count("minecraft:gold_ingot") >= 64
  }

  animation "wax_seal"
}
```

## Clarifying ambiguous goals

Ask GPT to distinguish the actual measurable target.

- “네더라이트 100개 캐기” is ambiguous because netherite ingots are not mined blocks.
- “고대 잔해 100개 채굴” maps to `stat.mined("minecraft:ancient_debris") >= 100`.
- “네더라이트 주괴 100개 보유” maps to `inventory.count("minecraft:netherite_ingot") >= 100`.
- “금 공장 만들기” normally maps to `manual.checked == true` because generic Minecraft state cannot prove that an arbitrary farm design is complete.

The mod parses and validates pasted code. Invalid code is not executed or saved.
