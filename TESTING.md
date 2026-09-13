# Focused test checklist

## Link/API
- Valid Discord one-time code links once.
- Invalid/expired code is rejected.
- Session survives Minecraft restart.
- Wrong API URL shows an error without crashing.

## Attempt/world
- `/speedrun-start` creates one pending attempt.
- Mod receives the run but does not display the seed on its own control screen.
- Official world screen receives exact seed, Survival, cheats off, bonus chest off.
- Editing the seed causes the loaded world not to start as the official attempt / server rejects a mismatch.
- A second random world cannot be submitted under the official Run ID.

## Timer
- Starts only after correct official world is loaded.
- ESC in singleplayer freezes active time.
- Returning to game resumes active time.
- Staying paused for several minutes does not increase active time.
- Closing/reopening the world does not fabricate active time.
- Minecraft restart resumes the existing API attempt instead of generating a new seed.

## Dye detection
- Each of the 16 Dye item IDs increments progress once.
- Duplicates do not increment progress.
- Removing a Dye decreases current progress.
- Run only finishes when all 16 are simultaneously in inventory.

## Fairness
- Creative/Spectator/Adventure invalidates/rejects run.
- Enabling commands/cheats is rejected when detected.
- Seed mismatch is rejected.
- Timer cannot move backwards server-side.
- Finish without all 16 is rejected.

## Network/race cases
- API offline during a run does not crash Minecraft.
- Temporary API failure can recover on later heartbeat.
- Rapid 16/16 detection sends only one finish at a time.
- Repeated K presses/screens do not create extra attempts.
