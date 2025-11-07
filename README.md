# Duplicationless

This is not a gameplay mod — it’s a **library**.

I made this because I was *tired* of writing the same data tracking and config code again and again in my mods.

So instead of copy-pasting the same 900 lines every time, I decided to pack them all into one place — and here we are.

## 🧠 What it does

* **JsonConfig**
  A small JSON config helper, copied from my [Jsonate](https://www.curseforge.com/minecraft/mc-mods/jsonate).

* **BlockStorage**
  An abstract class that makes `SavedData` for blocks actually bearable to work with.
  Copied and redesigned from my [BlockTrackerApi](https://www.curseforge.com/minecraft/mc-mods/blocktrackerapi).

* **EntityTracker**
  A utility for chunk-based entity tracking.
    * O(1) query time.
    * Dimension-aware.
    * Server thread only.
    * Copied from my [EntityChunkData](https://www.curseforge.com/minecraft/mc-mods/entitychunkdata), now refactored and more powerful.

* **Helper events & extensions**
  A few lightweight mixins to make things easier — like `BlockChangeEvent` and `EntityChunkChangeEvent`.

---

## ☕ Final words
This project basically exists because I got lazy (in a productive way).

If you’re also tired of rewriting entity or block tracking logic, maybe you’ll find it useful too.