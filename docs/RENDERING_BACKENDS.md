# Rendering backend policy

Quest Ledger targets both rendering backends available in Minecraft 26.2:

- Vulkan
- OpenGL

The mod does not contain a Vulkan renderer and an OpenGL renderer. It uses one backend-neutral GUI path provided by Minecraft and Fabric.

## Allowed rendering APIs

Client UI code should use:

- `Screen` and standard Minecraft widgets;
- `GuiGraphicsExtractor` for rectangles, text, sprites, and item rendering;
- Fabric's `HudElementRegistry` for HUD registration;
- Minecraft and Blaze3D render-state abstractions when a custom render state is eventually required.

These APIs record GUI render state for Minecraft to execute through whichever backend the player selected.

## Prohibited backend assumptions

Do not add direct calls to:

- `org.lwjgl.opengl.*`;
- `GL11`, `GL20`, or other raw OpenGL symbols;
- native Vulkan commands;
- direct framebuffer or pipeline manipulation tied to one backend;
- shaders that bypass Minecraft's supported rendering pipeline.

A contribution containing one of these APIs requires replacement with an appropriate Blaze3D or Minecraft abstraction before merge.

## Current implementation

The editor screen uses standard widgets plus `GuiGraphicsExtractor.fill` and `GuiGraphicsExtractor.text`.

The quest HUD is registered through `HudElementRegistry` and emits the same extracted GUI operations. Its slide-in effect changes only coordinates and opacity over time, so it has no backend-specific path.

## Verification matrix

Every release candidate should be checked with:

1. a clean OpenGL launch;
2. a clean Vulkan launch;
3. UI scaling at small, normal, and large values;
4. opening and closing the ledger repeatedly;
5. editing and saving through both Builder and QuestScript modes;
6. HUD visibility, resize behavior, and animation.

CI verifies compilation and tests. A graphical launch test remains a manual release check until automated Minecraft client smoke tests are added.
