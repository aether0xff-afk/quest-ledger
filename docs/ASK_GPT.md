# Asking GPT to write QuestScript

Quest Ledger does not call an LLM. Copy the relevant documentation and the prompt below into ChatGPT or another LLM, then paste the returned QuestScript into code mode.

## Prompt template

```text
Write a Minecraft Quest Ledger quest using QuestScript v0.1.

Rules:
- Return only QuestScript source. Do not use Markdown code fences.
- Use only functions, properties, animations, and syntax from the supplied documentation.
- Never invent a Minecraft resource ID or QuestScript function.
- Use namespaced IDs such as "minecraft:diamond".
- Add parentheses whenever AND and OR are mixed.
- If the goal cannot be observed reliably from game state, use manual.checked.
- Do not add conditions that I did not request.

QuestScript documentation:
[Paste docs/QUESTSCRIPT.md here]

My quest:
[Describe the quest here]
```

## Example request

```text
네더에서 고대 잔해를 4개 이상 캤거나 네더라이트 주괴를 하나 가지고 있으면 완료되는 퀘스트를 만들어줘. 완료 조건은 1초 동안 유지되어야 하고 밀랍 인장 애니메이션을 사용해.
```

## Expected result

```questscript
quest "네더라이트 준비" {
  complete when {
    (
      player.dimension == "minecraft:the_nether"
      and stat.mined("minecraft:ancient_debris") >= 4
    )
    or inventory.count("minecraft:netherite_ingot") >= 1
  }

  hold 1s
  animation "wax_seal"
}
```

The mod will parse and validate pasted code. Invalid code is not executed or saved.
