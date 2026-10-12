#!/usr/bin/env python3
"""Generates the resource files of the MC Tower Defense mod.

Writes block states, block/item models, the German and English language files and the
builder's wand texture plus the mod icon into mod/src/main/resources.

Run from the repository root:  python3 tools/gen_assets.py
"""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "mod" / "src" / "main" / "resources"
ASSETS = RES / "assets" / "mctd"

ELEMENTS = ["light", "darkness", "water", "fire", "nature", "earth"]

# (id, category, elements) - must match io.github.dalpner.mctd.core.Towers
TOWERS = [
    ("arrow", "basic", []), ("cannon", "basic", []),
    ("light", "element", ["light"]), ("darkness", "element", ["darkness"]), ("water", "element", ["water"]),
    ("fire", "element", ["fire"]), ("nature", "element", ["nature"]), ("earth", "element", ["earth"]),
    ("twilight", "dual", ["light", "darkness"]), ("ice", "dual", ["light", "water"]),
    ("lightning", "dual", ["light", "fire"]), ("sunspear", "dual", ["light", "nature"]),
    ("gold", "dual", ["light", "earth"]), ("tidal", "dual", ["darkness", "water"]),
    ("hellfire", "dual", ["darkness", "fire"]), ("plague", "dual", ["darkness", "nature"]),
    ("jinx", "dual", ["darkness", "earth"]), ("steam", "dual", ["water", "fire"]),
    ("spring", "dual", ["water", "nature"]), ("mud", "dual", ["water", "earth"]),
    ("forge", "dual", ["fire", "nature"]), ("magma", "dual", ["fire", "earth"]),
    ("quake", "dual", ["nature", "earth"]),
    ("moon", "triple", ["light", "darkness", "water"]), ("eclipse", "triple", ["light", "darkness", "fire"]),
    ("soultree", "triple", ["light", "darkness", "nature"]), ("obelisk", "triple", ["light", "darkness", "earth"]),
    ("prism", "triple", ["light", "water", "fire"]), ("rainbow", "triple", ["light", "water", "nature"]),
    ("crystal", "triple", ["light", "water", "earth"]), ("sun_altar", "triple", ["light", "fire", "nature"]),
    ("meteor", "triple", ["light", "fire", "earth"]), ("treasury", "triple", ["light", "nature", "earth"]),
    ("mist", "triple", ["darkness", "water", "fire"]), ("swamp", "triple", ["darkness", "water", "nature"]),
    ("abyss", "triple", ["darkness", "water", "earth"]), ("blight", "triple", ["darkness", "fire", "nature"]),
    ("volcano", "triple", ["darkness", "fire", "earth"]), ("necropolis", "triple", ["darkness", "nature", "earth"]),
    ("geyser", "triple", ["water", "fire", "nature"]), ("lava_flow", "triple", ["water", "fire", "earth"]),
    ("primeval", "triple", ["water", "nature", "earth"]), ("wildfire", "triple", ["fire", "nature", "earth"]),
]

# vanilla textures per element: (shaft, head, crystal)
ELEMENT_TEXTURES = {
    "light": ("quartz_pillar", "glowstone", "sea_lantern"),
    "darkness": ("obsidian", "crying_obsidian", "amethyst_block"),
    "water": ("prismarine_bricks", "dark_prismarine", "blue_ice"),
    "fire": ("nether_bricks", "magma", "shroomlight"),
    "nature": ("oak_log", "flowering_azalea_leaves", "emerald_block"),
    "earth": ("packed_mud", "dripstone_block", "raw_gold_block"),
}
BASIC_TEXTURES = {
    "arrow": ("oak_planks", "dispenser_front", "target_side"),
    "cannon": ("smooth_stone", "blast_furnace_front", "coal_block"),
}
BASE_TEXTURE = {"basic": "cobblestone", "element": "stone_bricks", "dual": "polished_deepslate",
                "triple": "polished_blackstone_bricks"}
ACCENT_TEXTURE = {1: "stone", 2: "iron_block", 3: "gold_block"}

# ---------------------------------------------------------------------------------------- texts

TOWER_NAMES = {
    "arrow": ("Pfeilturm", "Arrow Tower",
              "Schneller Allrounder, trifft Boden- und Luftgegner.", "Fast all-rounder, hits ground and air."),
    "cannon": ("Kanonenturm", "Cannon Tower",
               "Flächenschaden gegen Gruppen.", "Area damage against groups."),
    "light": ("Lichtturm", "Light Tower",
              "Lichtstrahlen springen auf weitere Gegner über.", "Beams of light jump to further enemies."),
    "darkness": ("Schattenturm", "Shadow Tower",
                 "Wuchtige Treffer, die Gegner verfluchen.", "Heavy hits that curse enemies."),
    "water": ("Wasserturm", "Water Tower",
              "Wasserschwälle verlangsamen Gegner.", "Water bursts slow enemies down."),
    "fire": ("Feuerturm", "Fire Tower",
             "Feuerbälle mit Flächenschaden setzen Gegner in Brand.",
             "Fireballs with splash damage set enemies on fire."),
    "nature": ("Naturturm", "Nature Tower",
               "Giftdornen treffen mehrere Gegner gleichzeitig.", "Poison thorns hit several enemies at once."),
    "earth": ("Erdturm", "Earth Tower",
              "Felsbrocken mit großem Flächenschaden, betäuben manchmal.",
              "Boulders with large splash damage, sometimes stun."),
    "twilight": ("Zwielichtturm", "Twilight Tower",
                 "Nutzt Licht oder Dunkelheit – was stärker ist. Zusatzschaden nach Lebenspunkten.",
                 "Uses light or darkness, whichever is stronger. Bonus damage by max health."),
    "ice": ("Eisturm", "Ice Tower",
            "Starke Verlangsamung, friert Gegner manchmal ein.", "Strong slow, sometimes freezes enemies."),
    "lightning": ("Blitzturm", "Lightning Tower",
                  "Kettenblitze springen zwischen vielen Gegnern.", "Chain lightning jumps between many enemies."),
    "sunspear": ("Sonnenspeer", "Sunspear",
                 "Enorme Reichweite, mehr Schaden auf Entfernung.", "Huge range, more damage at a distance."),
    "gold": ("Goldturm", "Gold Tower",
             "Mehr Kopfgeld für Gegner in Reichweite und Gold pro Welle.",
             "More bounty for enemies in range and gold per wave."),
    "tidal": ("Gezeitenturm", "Tidal Tower",
              "Flutwellen stoßen Gegner auf dem Pfad zurück.", "Tidal waves push enemies back along the path."),
    "hellfire": ("Höllenfeuerturm", "Hellfire Tower",
                 "Ein Strahl, der immer heißer wird – ideal gegen Bosse.",
                 "A beam that keeps getting hotter – ideal against bosses."),
    "plague": ("Seuchenturm", "Plague Tower",
               "Eine Seuche, die auf benachbarte Gegner überspringt.", "A plague that spreads to nearby enemies."),
    "jinx": ("Fluchturm", "Jinx Tower",
             "Verflucht alle Gegner in Reichweite.", "Curses every enemy in range."),
    "steam": ("Dampfturm", "Steam Tower",
              "Heißer Dampf trifft alle Gegner in Reichweite.", "Hot steam hits every enemy in range."),
    "spring": ("Quellturm", "Spring Tower",
               "Unterstützung: erhöht das Angriffstempo naher Türme.",
               "Support: increases the attack speed of nearby towers."),
    "mud": ("Schlammturm", "Mud Tower",
            "Schlamm verlangsamt alle Bodengegner in Reichweite.", "Mud slows every ground enemy in range."),
    "forge": ("Schmiede", "Forge",
              "Unterstützung: erhöht den Schaden naher Türme.", "Support: increases the damage of nearby towers."),
    "magma": ("Magmaturm", "Magma Tower",
              "Lavabomben mit riesigem Flächenschaden und Brand.", "Lava bombs with huge splash damage and burning."),
    "quake": ("Bebenturm", "Quake Tower",
              "Erdbeben treffen alle Bodengegner und betäuben sie.",
              "Earthquakes hit all ground enemies and stun them."),
    "moon": ("Mondturm", "Moon Tower",
             "Mondlicht verflucht und verlangsamt ganze Gruppen.", "Moonlight curses and slows whole groups."),
    "eclipse": ("Sonnenfinsternis", "Eclipse",
                "Gewaltige Einzelschläge mit dem stärksten seiner drei Elemente.",
                "Massive single hits with the strongest of its three elements."),
    "soultree": ("Seelenbaum", "Soul Tree",
                 "Trifft vier Ziele und erntet geschwächte Seelen sofort.",
                 "Hits four targets and instantly reaps weakened souls."),
    "obelisk": ("Obelisk", "Obelisk",
                "Stärkt Schaden und Tempo naher Türme und greift selbst an.",
                "Boosts damage and speed of nearby towers and attacks as well."),
    "prism": ("Prismaturm", "Prism Tower",
              "Gebrochenes Licht springt durch ganze Wellen.", "Refracted light jumps through entire waves."),
    "rainbow": ("Regenbogenturm", "Rainbow Tower",
                "Mehrere Strahlen, jeder im stärksten Element.", "Several beams, each in the strongest element."),
    "crystal": ("Kristallturm", "Crystal Tower",
                "Präzise Schüsse mit hoher Chance auf kritische Treffer.",
                "Precise shots with a high critical chance."),
    "sun_altar": ("Sonnenaltar", "Sun Altar",
                  "Sonnenglut verbrennt alle Gegner in Reichweite.", "Solar flares burn every enemy in range."),
    "meteor": ("Meteorturm", "Meteor Tower",
               "Meteoriten mit riesigem Flächenschaden, betäuben.", "Meteors with huge splash damage that stun."),
    "treasury": ("Schatzkammer", "Treasury",
                 "Viel mehr Kopfgeld und reichlich Gold pro Welle.", "Much more bounty and lots of gold per wave."),
    "mist": ("Nebelturm", "Mist Tower",
             "Giftiger Nebel verflucht und verlangsamt alle in Reichweite.",
             "Toxic mist curses and slows everyone in range."),
    "swamp": ("Sumpfturm", "Swamp Tower",
              "Stapelndes Gift und Verlangsamung für alle in Reichweite.",
              "Stacking poison and slow for everyone in range."),
    "abyss": ("Abgrund", "Abyss",
              "Schleudert Gruppen weit auf dem Pfad zurück.", "Hurls groups far back along the path."),
    "blight": ("Verderbnis", "Blight",
               "Brennende Seuche, die sich rasend ausbreitet.", "A burning plague that spreads like wildfire."),
    "volcano": ("Vulkan", "Volcano",
                "Ausbrüche treffen alle Bodengegner und setzen sie in Brand.",
                "Eruptions hit all ground enemies and set them on fire."),
    "necropolis": ("Nekropole", "Necropolis",
                   "Fünf Ziele gleichzeitig, geschwächte Gegner sterben sofort.",
                   "Five targets at once; weakened enemies die instantly."),
    "geyser": ("Geysir", "Geyser",
               "Heiße Fontänen: Flächenschaden, Rückstoß und Verlangsamung.",
               "Hot fountains: splash damage, knockback and slow."),
    "lava_flow": ("Lavastrom", "Lava Flow",
                  "Zähe Lava: riesiger Flächenschaden, Brand und Verlangsamung.",
                  "Viscous lava: huge splash damage, burning and slow."),
    "primeval": ("Urwald", "Primeval Forest",
                 "Sechs Giftranken gleichzeitig.", "Six poison vines at once."),
    "wildfire": ("Flächenbrand", "Wildfire",
                 "Brennende Kettenblitze.", "Burning chain lightning."),
}

ELEMENT_NAMES = {"light": ("Licht", "Light"), "darkness": ("Dunkelheit", "Darkness"), "water": ("Wasser", "Water"),
                 "fire": ("Feuer", "Fire"), "nature": ("Natur", "Nature"), "earth": ("Erde", "Earth"),
                 "neutral": ("Neutral", "Neutral")}
KIND_NAMES = {"normal": ("Normal", "Normal"), "fast": ("Schnell", "Fast"), "mass": ("Masse", "Mass"),
              "air": ("Luft", "Air"), "boss": ("Boss", "Boss"), "final_boss": ("Endboss", "Final Boss"),
              "elemental": ("Elementar-Wächter", "Element Guardian")}
ABILITY_NAMES = {"immune": ("Immun", "Immune"), "regen": ("Regeneration", "Regeneration"),
                 "healer": ("Heiler", "Healer"), "undead": ("Untot", "Undead"), "split": ("Spaltend", "Splitting"),
                 "armored": ("Gepanzert", "Armored")}
DIFFICULTY_NAMES = {"easy": ("Leicht", "Easy"), "normal": ("Normal", "Normal"), "hard": ("Schwer", "Hard")}
LAYOUT_NAMES = {
    "serpentine": ("Schlange", "Serpentine", "Vier lange Bahnen – der Klassiker.", "Four long lanes – the classic."),
    "spiral": ("Spirale", "Spiral", "Die längste Strecke: eine Spirale ins Zentrum.",
               "The longest route: a spiral into the centre."),
    "zigzag": ("Zickzack", "Zigzag", "Kurzer Weg mit weit getrennten Bahnen – schwer!",
               "Short route with far-apart lanes – hard!"),
}
TARGET_NAMES = {
    "first": ("Erster", "First", "Greift den Gegner an, der am weitesten gekommen ist.",
              "Attacks the enemy that got furthest."),
    "last": ("Letzter", "Last", "Greift den Gegner an, der am wenigsten weit ist.",
             "Attacks the enemy that is furthest behind."),
    "strongest": ("Stärkster", "Strongest", "Greift den Gegner mit den meisten Lebenspunkten an.",
                  "Attacks the enemy with the most health."),
    "weakest": ("Schwächster", "Weakest", "Greift den Gegner mit den wenigsten Lebenspunkten an.",
                "Attacks the enemy with the least health."),
    "closest": ("Nächster", "Closest", "Greift den Gegner an, der dem Turm am nächsten ist.",
                "Attacks the enemy closest to the tower."),
}
RESULTS = {
    "ok": ("OK", "OK"),
    "not_enough_gold": ("Nicht genug Gold!", "Not enough gold!"),
    "locked": ("Benötigt höhere Element-Stufen!", "Requires higher element levels!"),
    "max_tier": ("Bereits auf höchster Stufe.", "Already at the highest tier."),
    "occupied": ("Hier steht bereits etwas.", "Something is already here."),
    "not_buildable": ("Hier kann nicht gebaut werden.", "You cannot build here."),
    "no_tower": ("Hier steht kein Turm.", "There is no tower here."),
    "no_essence": ("Keine Element-Essenz übrig.", "No element essence left."),
    "element_maxed": ("Dieses Element hat bereits Stufe 3.", "This element is already at level 3."),
    "game_over": ("Das Spiel ist vorbei.", "The game is over."),
    "no_more_waves": ("Es gibt keine weiteren Wellen.", "There are no more waves."),
    "still_spawning": ("Die aktuelle Welle erscheint noch.", "The current wave is still spawning."),
}
CATEGORIES = {"basic": ("Basisturm", "Basic tower"), "element": ("Elementturm", "Element tower"),
              "dual": ("Doppelelement-Turm", "Dual element tower"),
              "triple": ("Dreifachelement-Turm", "Triple element tower")}

# key: (german, english)
TEXTS = {
    "itemGroup.mctd.main": ("MC Tower Defense", "MC Tower Defense"),
    "item.mctd.builder_wand": ("Baustab", "Builder's Wand"),
    "item.mctd.builder_wand.tooltip.ground": ("Rechtsklick auf Gras: Turm bauen", "Right-click grass: build a tower"),
    "item.mctd.builder_wand.tooltip.tower": ("Rechtsklick auf Turm: aufrüsten / verkaufen",
                                             "Right-click a tower: upgrade / sell"),
    "item.mctd.builder_wand.tooltip.sneak": ("Schleichen + Rechtsklick: letzten Turm erneut bauen",
                                             "Sneak + right-click: build the last tower again"),
    "item.mctd.builder_wand.tooltip.air": ("Rechtsklick in die Luft: Menü", "Right-click the air: menu"),

    "mctd.stat.element": ("Element: %s", "Element: %s"),
    "mctd.stat.adaptive": ("Element: das stärkste von %s", "Element: the strongest of %s"),
    "mctd.stat.damage": ("Schaden: %s (%s pro Sekunde)", "Damage: %s (%s per second)"),
    "mctd.stat.speed": ("Angriffe pro Sekunde: %s", "Attacks per second: %s"),
    "mctd.stat.range": ("Reichweite: %s Blöcke", "Range: %s blocks"),
    "mctd.stat.ground_only": ("Kann keine Luftgegner treffen", "Cannot hit air enemies"),
    "mctd.stat.requires": ("Benötigt:", "Requires:"),
    "mctd.effect.splash": ("Flächenschaden: %s Blöcke Radius, %s%%", "Splash: %s blocks radius, %s%%"),
    "mctd.effect.multi": ("Trifft %s Ziele gleichzeitig", "Hits %s targets at once"),
    "mctd.effect.chain": ("Kette: springt %s-mal (−%s%% pro Sprung)", "Chain: jumps %s times (−%s%% per jump)"),
    "mctd.effect.pulse": ("Trifft alle Gegner in Reichweite", "Hits every enemy in range"),
    "mctd.effect.ramp": ("+%s%% Schaden pro Treffer auf dasselbe Ziel (max. +%s%%)",
                         "+%s%% damage per hit on the same target (max +%s%%)"),
    "mctd.effect.slow": ("Verlangsamt um %s%% für %s s", "Slows by %s%% for %s s"),
    "mctd.effect.stun": ("%s%% Chance, %s s zu betäuben", "%s%% chance to stun for %s s"),
    "mctd.effect.burn": ("Brand: %s%% des Schadens pro Sekunde für %s s", "Burn: %s%% of the damage per second for %s s"),
    "mctd.effect.poison": ("Gift: %s%% des Schadens pro Sekunde, bis zu %s Stapel",
                           "Poison: %s%% of the damage per second, up to %s stacks"),
    "mctd.effect.curse": ("Fluch: Ziel erleidet %s%% mehr Schaden für %s s",
                          "Curse: target takes %s%% more damage for %s s"),
    "mctd.effect.knockback": ("Stößt Gegner %s Blöcke zurück", "Pushes enemies back %s blocks"),
    "mctd.effect.execute": ("Tötet Nicht-Bosse unter %s%% Leben sofort", "Instantly kills non-bosses below %s%% health"),
    "mctd.effect.percent_hp": ("+%s%% der max. Lebenspunkte des Ziels als Schaden",
                               "+%s%% of the target's max health as damage"),
    "mctd.effect.crit": ("%s%% Chance auf %s-fachen kritischen Schaden", "%s%% chance for %sx critical damage"),
    "mctd.effect.distance": ("Bis zu +%s%% Schaden auf große Entfernung", "Up to +%s%% damage at long range"),
    "mctd.effect.disease": ("Seuche: %s%% Schaden/s für %s s, springt %s Blöcke weit über",
                            "Plague: %s%% damage/s for %s s, spreads %s blocks"),
    "mctd.effect.aura_damage": ("Aura: +%s%% Schaden für Türme in Reichweite", "Aura: +%s%% damage for towers in range"),
    "mctd.effect.aura_speed": ("Aura: +%s%% Angriffstempo für Türme in Reichweite",
                               "Aura: +%s%% attack speed for towers in range"),
    "mctd.effect.bounty": ("+%s%% Kopfgeld für Gegner, die in Reichweite sterben",
                           "+%s%% bounty for enemies dying in range"),
    "mctd.effect.wave_gold": ("+%s Gold für jede geschaffte Welle", "+%s gold for every cleared wave"),

    "mctd.wave.number": ("Welle %s", "Wave %s"),
    "mctd.msg.peaceful": ("Tower Defense braucht Monster – die Schwierigkeit wurde auf „Leicht“ gestellt.",
                          "Tower defense needs monsters – the difficulty was set to Easy."),
    "mctd.msg.leak": ("−%s Leben! Noch %s Leben.", "−%s lives! %s lives left."),
    "mctd.msg.wave_start": ("▶ %s – %s Gegner", "▶ %s – %s enemies"),
    "mctd.msg.wave_cleared": ("✔ Welle %s geschafft! +%s Gold (davon %s Zinsen)",
                              "✔ Wave %s cleared! +%s gold (incl. %s interest)"),
    "mctd.msg.next_wave": ("Als Nächstes: %s – startet in %s s", "Next: %s – starts in %s s"),
    "mctd.msg.essence": ("✦ Neue Element-Essenz! (%s verfügbar) Öffne das Element-Menü mit dem Baustab.",
                         "✦ New element essence! (%s available) Open the element menu with the wand."),
    "mctd.msg.element_bought": ("Element %s auf Stufe %s erhöht!", "Element %s raised to level %s!"),
    "mctd.msg.guardian": ("Ein Wächter des Elements %s greift an!", "A guardian of %s attacks!"),
    "mctd.msg.victory": ("SIEG! %s Wellen überstanden · %s Gegner besiegt · %s durchgelassen · %s Gold verdient.",
                         "VICTORY! %s waves survived · %s enemies defeated · %s leaked · %s gold earned."),
    "mctd.msg.defeat": ("NIEDERLAGE in Welle %s · %s Gegner besiegt · %s durchgelassen · %s Gold verdient.",
                        "DEFEAT in wave %s · %s enemies defeated · %s leaked · %s gold earned."),
    "mctd.msg.restarted": ("Neues Spiel gestartet!", "New game started!"),
    "mctd.msg.rejoin": ("Willkommen zurück! Dein Tower-Defense-Spiel wartet (Baustab → Menü).",
                        "Welcome back! Your tower defense game is waiting (wand → menu)."),
    "mctd.msg.welcome": ("[MC Tower Defense]", "[MC Tower Defense]"),
    "mctd.msg.welcome.start": ("[Spiel starten]", "[Start a game]"),
    "mctd.msg.welcome.start.hover": ("Öffnet die Lobby: Karte und Schwierigkeit wählen",
                                     "Opens the lobby: choose map and difficulty"),
    "mctd.msg.welcome.help": ("[Hilfe]", "[Help]"),
    "mctd.msg.already_in_game": ("Du bist bereits in einem Spiel. Verlasse es mit /td quit.",
                                 "You are already in a game. Leave it with /td quit."),
    "mctd.msg.no_dimension": ("Die Arena-Dimension fehlt. Ist die Mod korrekt installiert?",
                              "The arena dimension is missing. Is the mod installed correctly?"),
    "mctd.msg.started": ("Spiel gestartet: %s, Karte „%s“. Viel Erfolg!", "Game started: %s, map %s. Good luck!"),
    "mctd.msg.tutorial": (
        "Baustab: Rechtsklick auf Gras = Turm bauen, auf einen Turm = aufrüsten/verkaufen, in die Luft = Menü. "
        "Schleichen + Rechtsklick baut den zuletzt gebauten Turm erneut. Doppelsprung = Fliegen.",
        "Wand: right-click grass = build a tower, a tower = upgrade/sell, the air = menu. "
        "Sneak + right-click builds the last tower again. Double jump = fly."),
    "mctd.msg.joined": ("%s ist dem Spiel beigetreten!", "%s joined the game!"),
    "mctd.msg.not_in_game": ("Du bist in keinem Tower-Defense-Spiel.", "You are not in a tower defense game."),
    "mctd.msg.left": ("Du hast die Arena verlassen. Ohne Spieler ist das Spiel pausiert. Zurück mit /td return.",
                      "You left the arena. Without players the game is paused. Come back with /td return."),
    "mctd.msg.quit": ("Du hast das Spiel verlassen.", "You quit the game."),
    "mctd.msg.member_quit": ("%s hat das Spiel verlassen.", "%s quit the game."),
    "mctd.msg.stopped": ("Das Spiel wurde beendet.", "The game has ended."),
    "mctd.msg.early_bonus": ("Früher Start: +%s Gold", "Early start: +%s gold"),
    "mctd.msg.built": ("%s gebaut (−%s Gold)", "%s built (−%s gold)"),
    "mctd.msg.upgraded": ("Aufgerüstet: %s", "Upgraded: %s"),
    "mctd.msg.sold": ("%s verkauft (+%s Gold)", "%s sold (+%s gold)"),
    "mctd.msg.endless": ("Endlosmodus! Wie lange hältst du durch?", "Endless mode! How long can you hold out?"),
    "mctd.msg.target_not_in_game": ("Dieser Spieler ist in keinem Spiel.", "That player is not in a game."),
    "mctd.msg.only_owner": ("Nur der Spielleiter kann das Spiel beenden.", "Only the game's owner can end it."),
    "mctd.msg.bad_arguments": ("Unbekannte Schwierigkeit oder Karte.", "Unknown difficulty or map."),
    "mctd.msg.info": ("Welle %s/%s · Gold %s · Leben %s · Essenzen %s · Elemente: %s",
                      "Wave %s/%s · Gold %s · Lives %s · Essences %s · Elements: %s"),

    "mctd.title.wave": ("Welle %s", "Wave %s"),
    "mctd.title.essence": ("✦ Neue Element-Essenz ✦", "✦ New element essence ✦"),
    "mctd.title.victory": ("SIEG!", "VICTORY!"),
    "mctd.title.defeat": ("NIEDERLAGE", "DEFEAT"),
    "mctd.title.stats": ("Welle %s · %s Gegner besiegt", "Wave %s · %s enemies defeated"),

    "mctd.bossbar.waiting": ("Tower Defense", "Tower Defense"),
    "mctd.bossbar.victory": ("Sieg! Alle Wellen überstanden", "Victory! All waves survived"),
    "mctd.bossbar.defeat": ("Niederlage", "Defeat"),
    "mctd.bossbar.paused": ("Pausiert – kein Spieler in der Arena", "Paused – no player in the arena"),
    "mctd.bossbar.build": ("Bauphase – Welle %s startet in %s s", "Build phase – wave %s starts in %s s"),
    "mctd.bossbar.wave": ("%s – noch %s", "%s – %s left"),

    "mctd.hud.wave": ("Welle %s/%s", "Wave %s/%s"),
    "mctd.hud.wave_endless": ("Welle %s (endlos)", "Wave %s (endless)"),
    "mctd.hud.paused": ("⏸ Pausiert", "⏸ Paused"),
    "mctd.hud.next_in": ("Nächste Welle in %s s", "Next wave in %s s"),
    "mctd.hud.creeps_left": ("Gegner übrig: %s", "Enemies left: %s"),
    "mctd.hud.victory": ("Sieg!", "Victory!"),
    "mctd.hud.defeat": ("Niederlage", "Defeat"),
    "mctd.hud.upcoming": ("Als Nächstes:", "Coming up:"),
    "mctd.hud.tower_hint": ("Rechtsklick: aufrüsten / verkaufen", "Right-click: upgrade / sell"),

    "mctd.menu.lobby": ("Tower Defense – Neues Spiel", "Tower Defense – New Game"),
    "mctd.menu.lobby.info": ("So geht's", "How it works"),
    "mctd.menu.lobby.info.1": ("Wähle eine Karte (Zeile) und eine Schwierigkeit.",
                               "Pick a map (row) and a difficulty."),
    "mctd.menu.lobby.info.2": ("Baue Türme, sammle Essenzen, wähle Elemente.",
                               "Build towers, collect essences, choose elements."),
    "mctd.menu.lobby.info.3": ("Überstehe 50 Wellen – samt Endboss!", "Survive 50 waves – including the final boss!"),
    "mctd.menu.lobby.path_length": ("Pfadlänge: %s Blöcke", "Path length: %s blocks"),
    "mctd.menu.lobby.difficulty": ("%s Leben · %s Startgold · %s%% Gegner-Lebenspunkte",
                                   "%s lives · %s starting gold · %s%% enemy health"),
    "mctd.menu.lobby.click_start": ("Klicken zum Starten", "Click to start"),
    "mctd.menu.lobby.join": ("Spiel von %s beitreten", "Join %s's game"),
    "mctd.menu.lobby.join.info": ("Welle %s · %s · %s Spieler", "Wave %s · %s · %s players"),
    "mctd.menu.lobby.click_join": ("Klicken zum Beitreten", "Click to join"),

    "mctd.menu.main": ("Tower Defense", "Tower Defense"),
    "mctd.menu.main.status": ("Spielstand", "Status"),
    "mctd.menu.main.status.wave": ("Welle: %s / %s", "Wave: %s / %s"),
    "mctd.menu.main.status.gold": ("Gold: %s", "Gold: %s"),
    "mctd.menu.main.status.lives": ("Leben: %s", "Lives: %s"),
    "mctd.menu.main.status.essences": ("Element-Essenzen: %s", "Element essences: %s"),
    "mctd.menu.main.status.kills": ("Besiegt: %s · Durchgelassen: %s", "Defeated: %s · Leaked: %s"),
    "mctd.menu.main.status.settings": ("%s · %s", "%s · %s"),
    "mctd.menu.main.elements": ("Elemente", "Elements"),
    "mctd.menu.main.elements.info": ("Verfügbare Essenzen: %s", "Available essences: %s"),
    "mctd.menu.main.next": ("Nächste Welle starten", "Start next wave"),
    "mctd.menu.main.next.countdown": ("Startet sonst in %s s", "Otherwise starts in %s s"),
    "mctd.menu.main.next.bonus": ("Bonus für frühen Start: +%s Gold", "Early start bonus: +%s gold"),
    "mctd.menu.main.next.running": ("Gegner sind noch unterwegs – die Wellen überlappen!",
                                    "Enemies are still walking – waves will overlap!"),
    "mctd.menu.main.preview": ("Wellenvorschau", "Wave preview"),
    "mctd.menu.main.help": ("Hilfe", "Help"),
    "mctd.menu.main.help.info": ("Erklärt Elemente, Türme und Gegner im Chat",
                                 "Explains elements, towers and enemies in chat"),
    "mctd.menu.main.leave": ("Arena verlassen", "Leave arena"),
    "mctd.menu.main.leave.info": ("Zurück in deine Welt. Das Spiel pausiert.", "Back to your world. The game pauses."),
    "mctd.menu.main.return": ("Zur Arena", "To the arena"),
    "mctd.menu.main.return.info": ("Zurück in dein laufendes Spiel", "Back into your running game"),
    "mctd.menu.main.restart": ("Neues Spiel", "New game"),
    "mctd.menu.main.restart.info": ("Gleiche Karte und Schwierigkeit, frische Arena",
                                    "Same map and difficulty, fresh arena"),
    "mctd.menu.main.endless": ("Endlosmodus", "Endless mode"),
    "mctd.menu.main.endless.info": ("Weiterspielen mit immer stärkeren Wellen", "Keep playing with ever stronger waves"),
    "mctd.menu.main.stop": ("Spiel beenden", "End game"),
    "mctd.menu.main.stop.info": ("Beendet das Spiel für alle und entfernt die Arena",
                                 "Ends the game for everyone and removes the arena"),
    "mctd.menu.main.quit": ("Spiel verlassen", "Quit game"),
    "mctd.menu.main.quit.info": ("Verlässt das Spiel dauerhaft", "Leaves the game for good"),
    "mctd.menu.confirm": ("Zum Bestätigen erneut klicken!", "Click again to confirm!"),

    "mctd.menu.elements": ("Elemente", "Elements"),
    "mctd.menu.elements.essences": ("Element-Essenzen: %s", "Element essences: %s"),
    "mctd.menu.elements.info.1": ("Jede Essenz erhöht ein Element um eine Stufe.",
                                  "Each essence raises an element by one level."),
    "mctd.menu.elements.info.2": ("Stufe N schaltet Stufe N der Elementtürme frei.",
                                  "Level N unlocks tier N of element towers."),
    "mctd.menu.elements.info.3": ("Essenzen gibt es zu Beginn und alle vier Wellen.",
                                  "You get essences at the start and every four waves."),
    "mctd.menu.elements.level": ("Stufe %s / %s", "Level %s / %s"),
    "mctd.menu.elements.strong": ("Stark gegen: %s", "Strong against: %s"),
    "mctd.menu.elements.weak": ("Schwach gegen: %s", "Weak against: %s"),
    "mctd.menu.elements.maxed": ("Höchste Stufe erreicht", "Maximum level reached"),
    "mctd.menu.elements.no_essence": ("Keine Essenz verfügbar", "No essence available"),
    "mctd.menu.elements.buy": ("Klicken: Stufe %s kaufen (1 Essenz)", "Click: buy level %s (1 essence)"),
    "mctd.menu.elements.guardian": ("Achtung: Ein Elementar-Wächter erscheint!", "Beware: an element guardian appears!"),
    "mctd.menu.back": ("Zurück", "Back"),
    "mctd.menu.close": ("Schließen", "Close"),
    "mctd.menu.build": ("Turm bauen", "Build tower"),
    "mctd.menu.build.gold": ("Gold: %s", "Gold: %s"),
    "mctd.menu.build.levels": ("Elemente: %s", "Elements: %s"),
    "mctd.menu.build.hint": ("Tipp: Schleichen + Rechtsklick baut den letzten Turm erneut",
                             "Tip: sneak + right-click builds the last tower again"),
    "mctd.menu.build.triples": ("Dreifachelement-Türme", "Triple element towers"),
    "mctd.menu.build.triples.info": ("Die mächtigsten Türme (drei Elemente)", "The mightiest towers (three elements)"),
    "mctd.menu.build.click": ("Klicken zum Bauen", "Click to build"),
    "mctd.menu.cost": ("Kosten: %s Gold", "Cost: %s gold"),
    "mctd.menu.locked": ("Gesperrt – benötigt höhere Element-Stufen", "Locked – requires higher element levels"),
    "mctd.menu.tower.kills": ("Besiegt: %s · Schaden: %s", "Kills: %s · Damage: %s"),
    "mctd.menu.tower.buffs": ("Verstärkt: +%s%% Schaden, +%s%% Tempo", "Buffed: +%s%% damage, +%s%% speed"),
    "mctd.menu.tower.ramp": ("Aufheizung: %s", "Heat: %s"),
    "mctd.menu.tower.maxed": ("Höchste Stufe", "Maximum tier"),
    "mctd.menu.tower.upgrade": ("Aufrüsten auf Stufe %s", "Upgrade to tier %s"),
    "mctd.menu.tower.upgrade.click": ("Klicken zum Aufrüsten", "Click to upgrade"),
    "mctd.menu.tower.sell": ("Verkaufen", "Sell"),
    "mctd.menu.tower.sell.value": ("Erlös: %s Gold", "Refund: %s gold"),
    "mctd.menu.tower.sell.full": ("Volle Erstattung (in dieser Bauphase gebaut)", "Full refund (built this build phase)"),
    "mctd.menu.tower.mode.active": ("Aktiv", "Active"),
    "mctd.menu.tower.mode.click": ("Klicken zum Auswählen", "Click to select"),

    "mctd.help.title": ("═══ MC Tower Defense ═══", "═══ MC Tower Defense ═══"),
    "mctd.help.1": ("Gegner laufen den Pfad entlang. Wer die Burg erreicht, kostet dich Leben.",
                    "Enemies walk along the path. Each one reaching the castle costs lives."),
    "mctd.help.2": ("Baue Türme mit dem Baustab: Rechtsklick auf Gras neben dem Pfad.",
                    "Build towers with the wand: right-click grass next to the path."),
    "mctd.help.3": ("Gold gibt es für besiegte Gegner, geschaffte Wellen und Zinsen.",
                    "You earn gold for defeated enemies, cleared waves and interest."),
    "mctd.help.4": ("Element-Essenzen (Start + alle vier Wellen) erhöhen die Stufe eines Elements.",
                    "Element essences (start + every four waves) raise an element's level."),
    "mctd.help.5": ("Elementtürme brauchen ihr Element, Doppel- und Dreifachtürme mehrere.",
                    "Element towers need their element, dual and triple towers several."),
    "mctd.help.6": ("Jede Welle hat ein Element: stark = doppelter, schwach = halber Schaden.",
                    "Every wave has an element: strong = double, weak = half damage."),
    "mctd.help.7": ("Gegnertypen: Normal, Schnell, Masse, Luft (nicht jeder Turm trifft sie), Boss.",
                    "Enemy kinds: normal, fast, mass, air (not every tower hits them), boss."),
    "mctd.help.8": ("Fähigkeiten: Immun, Regeneration, Heiler, Untot, Spaltend, Gepanzert.",
                    "Abilities: immune, regeneration, healer, undead, splitting, armored."),
    "mctd.help.9": ("Alle 10 Wellen kommt ein Boss, in Welle 50 der Endboss.",
                    "Every 10 waves a boss appears, in wave 50 the final boss."),
    "mctd.help.wheel": ("Elementkreis:", "Element wheel:"),
    "mctd.help.commands": ("Befehle: /td lobby · /td menu · /td next · /td info · /td leave · /td return · /td quit · /td stop",
                           "Commands: /td lobby · /td menu · /td next · /td info · /td leave · /td return · /td quit · /td stop"),
}


def build_lang():
    de, en = {}, {}

    def put(key, de_text, en_text):
        de[key] = de_text
        en[key] = en_text

    for key, (d, e) in TEXTS.items():
        put(key, d, e)
    for tid, (dn, endn, dd, ed) in TOWER_NAMES.items():
        put(f"mctd.tower.{tid}", dn, endn)
        put(f"mctd.tower.{tid}.desc", dd, ed)
        put(f"block.mctd.{tid}_tower", dn, endn)
    for key, (d, e) in ELEMENT_NAMES.items():
        put(f"mctd.element.{key}", d, e)
    for key, (d, e) in KIND_NAMES.items():
        put(f"mctd.creep_kind.{key}", d, e)
    for key, (d, e) in ABILITY_NAMES.items():
        put(f"mctd.ability.{key}", d, e)
    for key, (d, e) in DIFFICULTY_NAMES.items():
        put(f"mctd.difficulty.{key}", d, e)
    for key, (d, e, dd, ed) in LAYOUT_NAMES.items():
        put(f"mctd.layout.{key}", d, e)
        put(f"mctd.layout.{key}.desc", dd, ed)
    for key, (d, e, dd, ed) in TARGET_NAMES.items():
        put(f"mctd.target.{key}", d, e)
        put(f"mctd.target.{key}.desc", dd, ed)
    for key, (d, e) in RESULTS.items():
        put(f"mctd.result.{key}", d, e)
    for key, (d, e) in CATEGORIES.items():
        put(f"mctd.category.{key}", d, e)
    return de, en


# ---------------------------------------------------------------------------------------- models

def faces(texture):
    return {side: {"texture": texture} for side in ("north", "south", "east", "west", "up", "down")}


def cube(frm, to, texture):
    return {"from": frm, "to": to, "faces": faces(texture)}


def parent_model(tier):
    elements = [
        cube([0, 0, 0], [16, 3, 16], "#base"),
        cube([2, 3, 2], [14, 4, 14], "#base"),
        cube([3, 4, 3], [13, 13, 13], "#shaft"),
        cube([2, 13, 2], [14, 16, 14], "#head"),
    ]
    if tier >= 2:
        for x, z in ((2, 2), (12, 2), (2, 12), (12, 12)):
            elements.append(cube([x, 16, z], [x + 2, 18, z + 2], "#accent"))
        elements.append(cube([5, 16, 5], [11, 21, 11], "#crystal"))
    if tier >= 3:
        for x, z in ((0, 0), (13, 0), (0, 13), (13, 13)):
            elements.append(cube([x, 3, z], [x + 3, 11, z + 3], "#accent"))
        elements.append(cube([6, 21, 6], [10, 26, 10], "#crystal"))
        elements.append(cube([7, 26, 7], [9, 28, 9], "#accent"))
    return {
        "parent": "minecraft:block/block",
        "ambientocclusion": False,
        "textures": {"particle": "#shaft"},
        "elements": elements,
    }


def tower_textures(tid, category, elements, tier):
    if category == "basic":
        shaft, head, crystal = BASIC_TEXTURES[tid]
    else:
        a = ELEMENT_TEXTURES[elements[0]]
        b = ELEMENT_TEXTURES[elements[1]] if len(elements) > 1 else a
        c = ELEMENT_TEXTURES[elements[2]] if len(elements) > 2 else b
        shaft, head, crystal = a[0], b[1], c[2]
    return {
        "base": f"minecraft:block/{BASE_TEXTURE[category]}",
        "shaft": f"minecraft:block/{shaft}",
        "head": f"minecraft:block/{head}",
        "crystal": f"minecraft:block/{crystal}",
        "accent": f"minecraft:block/{ACCENT_TEXTURE[tier]}",
        "particle": f"minecraft:block/{shaft}",
    }


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


# ---------------------------------------------------------------------------------------- textures

def make_textures():
    try:
        from PIL import Image, ImageDraw
    except ImportError:
        print("Pillow not installed - skipping textures", file=sys.stderr)
        return

    # builder's wand, 16x16
    wand = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = wand.load()
    stick_dark, stick_light = (92, 60, 32, 255), (138, 94, 52, 255)
    for i in range(2, 12):
        x, y = i, 15 - i
        px[x, y] = stick_dark
        if x + 1 < 16:
            px[x + 1, y] = stick_light
    gold = (230, 190, 60, 255)
    for x, y in ((10, 6), (11, 5), (9, 5), (10, 4)):
        px[x, y] = gold
    gem = [(255, 232, 115, 255), (155, 77, 255, 255), (61, 139, 255, 255),
           (255, 85, 34, 255), (85, 221, 51, 255), (192, 128, 64, 255)]
    coords = [(12, 1), (13, 2), (14, 3), (11, 2), (12, 3), (13, 4)]
    for (x, y), c in zip(coords, gem):
        px[x, y] = c
    px[12, 2] = (255, 255, 255, 255)
    px[13, 3] = (240, 240, 255, 255)
    out = ASSETS / "textures" / "item" / "builder_wand.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    wand.save(out)

    # mod icon, 128x128: a tower in front of the element wheel
    icon = Image.new("RGBA", (128, 128), (24, 26, 38, 255))
    draw = ImageDraw.Draw(icon)
    colors = [(255, 232, 115), (155, 77, 255), (61, 139, 255), (255, 85, 34), (85, 221, 51), (192, 128, 64)]
    for i, c in enumerate(colors):
        start = -90 + i * 60
        draw.pieslice([8, 8, 120, 120], start, start + 60, fill=c + (255,))
    draw.ellipse([30, 30, 98, 98], fill=(24, 26, 38, 255))
    draw.rectangle([46, 92, 82, 104], fill=(120, 120, 128, 255))
    draw.rectangle([52, 52, 76, 92], fill=(150, 150, 160, 255))
    draw.rectangle([48, 44, 80, 54], fill=(110, 110, 120, 255))
    for x in (48, 58, 68, 76):
        draw.rectangle([x, 38, x + 4, 44], fill=(110, 110, 120, 255))
    draw.polygon([(64, 14), (72, 30), (64, 40), (56, 30)], fill=(255, 255, 255, 255))
    draw.rectangle([60, 64, 68, 76], fill=(40, 40, 50, 255))
    out = ASSETS / "icon.png"
    icon.save(out)


def main():
    blockstates = ASSETS / "blockstates"
    block_models = ASSETS / "models" / "block"
    item_models = ASSETS / "models" / "item"
    for tier in (1, 2, 3):
        write_json(block_models / f"tower_t{tier}.json", parent_model(tier))
    for tid, category, elements in TOWERS:
        name = f"{tid}_tower"
        variants = {}
        for tier in (1, 2, 3):
            write_json(block_models / f"{name}_t{tier}.json", {
                "parent": f"mctd:block/tower_t{tier}",
                "textures": tower_textures(tid, category, elements, tier),
            })
            variants[f"tier={tier}"] = {"model": f"mctd:block/{name}_t{tier}"}
        write_json(blockstates / f"{name}.json", {"variants": variants})
        write_json(item_models / f"{name}.json", {
            "parent": f"mctd:block/{name}_t2",
            "display": {
                "gui": {"rotation": [30, 225, 0], "translation": [0, -1.75, 0], "scale": [0.5, 0.5, 0.5]},
                "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.2, 0.2, 0.2]},
                "fixed": {"rotation": [0, 0, 0], "translation": [0, -1, 0], "scale": [0.4, 0.4, 0.4]},
            },
        })
    write_json(item_models / "builder_wand.json", {
        "parent": "minecraft:item/handheld",
        "textures": {"layer0": "mctd:item/builder_wand"},
    })
    de, en = build_lang()
    write_json(ASSETS / "lang" / "de_de.json", dict(sorted(de.items())))
    write_json(ASSETS / "lang" / "en_us.json", dict(sorted(en.items())))
    make_textures()
    print(f"Generated assets for {len(TOWERS)} towers, {len(de)} translation keys")


if __name__ == "__main__":
    main()
