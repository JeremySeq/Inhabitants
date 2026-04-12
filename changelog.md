# Inhabitants Changelogs

## v1.1.0 Update

### New Features & Changes
**Bogre**
- Fully structure rework
- better Pathfinding
- Skilling system, Skills Animations separated 
into 3 states `start`, `loop`, `end`
- A new behaviour, delivering to chests in case the player is not around
- tuned combat behavior, more intence
  - Shockwaves are avoidable by jumping if you time it well
  - Adjusted speeds, rotations, detections values
  - Smooth rotation + rotation value change depends on whether Bogre is in `Neutral`, `Aggressive`, or `Skilling` states 
- Taming is removed from the system

**Skills & Recipes**
- Cooking, Carving & Transformation using JSON's, all recipes must be placed in `data/inhabitants/bogre_recipes/`
- Cooking using `time_ticks` from recipes
- Carving & Transformation uses `hammer_hits` from recipes
- New visuals feedback for Carving & Transformation skills
- Cauldron GUI
  - Heat system + Feedbacks
  - Bogre will get aggressive in case a Player placed a non ingredient food
  
**Items**
- Item Removed: Baneful potato
- Item Adjusted: Dimensional snack → Dimensional serving + new effect [**Immaterial**]
- Item Renamed:  Monster meal → Baked brains
- Item Renamed:  Spider soup → Marinated spider
- New Item: Uncanny pottage, eating it triggers Reverse Growth effect
- New Item: Javelin, Crafted diagonal Stick, Feather, Impaler Spike
- New Item: Swamp Lair Map, Obtained by Cartographer Villager
- New Item: Drill, Can be crafted in Crafting table
- New 2 Items:
  - Impaler Head: Obtained by charged Creeper explosion
  - Dripstone Impaler Head: Obtained by charged Creeper explosion 
- New Enchantment: Diamond tip, Raise drill speed to Diamond tier
- New Enchantment: Thermal capacity, Increase thermal capacity twice
- Giant bone: triggers shockwave by 25% chance on crit
- New sprites for most of the items

**Others**
- New visual feedbacks for Concussion effect
- Audio: new Lowpass filter, currently used in **Immaterial** & **Concussion** effects
- JEI Compatibility for Cooking, Carving & Transformation recipes
- New textures for Impaler
- FPV animations system by JSON's
- New Cauldron textures
- New Damage source: Impaled
- Arabic language supported

### Bugs & Fix's
**Impaler**
- Impaler model/texture had z-fighting
- added restrict rules for Impaler spawning
  - no more spawning in Deep dark
  - adjusted spawn chance between flat world and non-flat world
**Others**
- Crossbow spike loaded: added client side model registration to replace the crossbow model dynamically