# MC Tower Defense

Ein Minecraft-Modpack, mit dem du **Tower Defense wie in Warcraft 3** spielst – angelehnt an den
Klassiker *Element TD*: sechs Elemente, 43 Türme, 50 Wellen mit schnellen, massenhaften, fliegenden
und Boss-Gegnern, Element-Essenzen und Elementar-Wächter.

- **Minecraft 1.21.1** mit **Fabric** (Modpack-Format `.mrpack` für Prism Launcher, Modrinth App, ATLauncher …)
- Herzstück ist die eigene Mod **MC Tower Defense** (`mod/`), dazu Fabric API und Performance-Mods
  (Sodium, Lithium, FerriteCore, Mod Menu).
- Spielbar allein oder im **Koop** (gemeinsames Gold und gemeinsame Leben), auf Deutsch und Englisch.

## Inhalt

- [So nutzt du das Modpack](#so-nutzt-du-das-modpack)
- [Mit Freunden spielen](#mit-freunden-spielen)
- [Häufige Fragen](#häufige-fragen)
- [So spielst du](#so-spielst-du)
- [Elemente](#elemente)
- [Gegner](#gegner)
- [Türme](#türme)
- [Befehle](#befehle)
- [Aus dem Quellcode bauen](#aus-dem-quellcode-bauen)

## So nutzt du das Modpack

### Was du brauchst

- **Minecraft Java Edition** (gekauft, mit Microsoft-Konto).
- Einen Launcher, der Modrinth-Modpacks (`.mrpack`) importieren kann:
  **[Prism Launcher](https://prismlauncher.org)** (empfohlen, Windows/macOS/Linux) oder die
  **[Modrinth App](https://modrinth.com/app)**. Beide laden Java 21, Minecraft, Fabric und alle Mods selbst
  herunter – du musst nichts von Hand installieren.
- Etwa 4 GB freien Arbeitsspeicher für Minecraft.

### Schritt 1: Modpack-Datei herunterladen

Gebaut wird das Modpack automatisch von GitHub Actions bei jedem Push.

- **Aus einem Release (am einfachsten):** Auf der GitHub-Seite des Projekts rechts auf **Releases**
  klicken und bei der neuesten Version `MC-Tower-Defense-<version>.mrpack` herunterladen. Ein Release
  entsteht, sobald ein Versions-Tag wie `v1.0.0` gepusht wird.
- **Aus dem neuesten Build:** Oben auf den Tab **Actions** → links *Build mod and modpack* → den obersten
  Lauf mit grünem Haken öffnen → ganz unten unter **Artifacts** auf `mc-tower-defense` klicken (dafür
  musst du bei GitHub angemeldet sein). Die heruntergeladene ZIP-Datei entpacken – darin liegt
  `MC-Tower-Defense-<version>.mrpack`. Artefakte werden 90 Tage aufbewahrt.

### Schritt 2: In den Launcher importieren

**Prism Launcher**

1. Prism Launcher installieren und starten, oben rechts unter *Konten* dein Microsoft-Konto hinzufügen.
2. **Instanz hinzufügen** → links **Importieren** → die `.mrpack`-Datei auswählen → **OK**.
3. Empfohlen: Instanz anklicken → **Bearbeiten** → *Einstellungen* → *Java* → Haken bei *Speicher* und
   *Maximaler Speicherverbrauch* auf **4096 MiB** setzen.
4. Instanz doppelklicken. Beim ersten Start werden Minecraft 1.21.1, Fabric und alle Mods geladen –
   das dauert ein paar Minuten.

**Modrinth App**

1. Modrinth App installieren und mit deinem Microsoft-Konto anmelden.
2. Auf **+** (*Create instance*) klicken → **From file** / *Import* wählen → die `.mrpack`-Datei auswählen.
3. Die neue Instanz unter *Library* starten (**Play**).

Die CurseForge-App kann `.mrpack`-Dateien nicht importieren.

### Schritt 3: Spielen

1. Im Hauptmenü **Einzelspieler** → **Neue Welt erstellen** (Welttyp und Spielmodus sind egal) →
   **Neue Welt erstellen**.
2. Sobald du in der Welt bist, erscheint im Chat **[Spiel starten]** – anklicken (oder `/td lobby`
   eingeben).
3. In der Lobby eine **Karte** (Zeile) und eine **Schwierigkeit** anklicken. Du wirst in deine eigene
   Arena gebracht und bekommst den **Baustab**. Wie es weitergeht, steht unter
   [So spielst du](#so-spielst-du).

Die Spielsprache ist im Modpack auf Deutsch voreingestellt (änderbar unter *Optionen* → *Sprache*).
Deine Partie wird mit der Welt gespeichert: Welt verlassen, später wieder betreten, und das Spiel geht
weiter (eine angefangene Welle startet dann neu).

### Updates

Für eine neue Version die neue `.mrpack` herunterladen und wie oben als neue Instanz importieren. Deine
Welten nimmst du mit, indem du den Ordner `saves` aus der alten Instanz in die neue kopierst
(Prism: Instanz anklicken → *Ordner*).

## Mit Freunden spielen

Alle Mitspieler brauchen **dieselbe Version** des Modpacks. Im Koop teilt ihr euch Gold, Leben und
Elemente.

- **Im selben Netzwerk (LAN):** Einer startet eine Welt, öffnet das Pausenmenü und wählt **Im LAN öffnen**.
  Die anderen finden die Welt unter **Mehrspieler**. Danach entweder in der Lobby
  *Spiel von … beitreten* anklicken oder `/td join <Spielername>` eingeben.
- **Über das Internet:** Im Download liegt zusätzlich `MC-Tower-Defense-Server-<version>.zip` – ein
  fertiger Fabric-Server mit allen nötigen Mods.
  1. Auf dem Server-PC [Java 21](https://adoptium.net) installieren und die ZIP-Datei entpacken.
  2. In `eula.txt` den Wert auf `eula=true` setzen (nachdem du die Minecraft-EULA gelesen hast).
  3. `start.bat` (Windows) bzw. `start.sh` (Linux/macOS) ausführen. Beim ersten Start lädt der
     Fabric-Launcher den Minecraft-Server automatisch.
  4. Port **25565** im Router freigeben. Mitspieler verbinden sich über **Mehrspieler** →
     **Server hinzufügen** mit deiner IP-Adresse.

## Häufige Fragen

- **Mein Launcher kann die Datei nicht öffnen.** Nur Launcher mit Modrinth-Unterstützung (Prism Launcher,
  Modrinth App, ATLauncher) können `.mrpack` importieren, die CurseForge-App nicht.
- **Ich kann in der Arena keine Blöcke abbauen oder setzen.** Das ist Absicht: In der Arena bist du im
  Abenteuermodus. Türme baust du mit dem Baustab; außerhalb der Arena gilt wieder dein normaler
  Spielmodus.
- **Ich habe den Baustab verloren.** Einfach `/td return` eingeben oder die Arena neu betreten – du
  bekommst ihn automatisch zurück.
- **Die Schwierigkeit hat sich auf „Leicht“ geändert.** Auf „Friedlich“ würden die Gegner sofort
  verschwinden, deshalb stellt die Mod beim Spielstart automatisch auf „Leicht“ um.
- **Das Spiel ruckelt.** Sodium ist bereits enthalten. Hilft das nicht, unter *Optionen* →
  *Grafikeinstellungen* die *Partikel* auf *Verringert* stellen.

## So spielst du

1. Nach dem Betreten der Welt im Chat auf **[Spiel starten]** klicken (oder `/td lobby`).
2. In der Lobby eine **Karte** (Zeile) und eine **Schwierigkeit** wählen. Du landest in deiner eigenen
   Arena in einer separaten Dimension. Deine Welt bleibt unberührt, Spielmodus und Position werden beim
   Verlassen wiederhergestellt.
3. Du bekommst den **Baustab**:
   - **Rechtsklick auf Gras** → Bau-Menü (alle 43 Türme, gesperrte sind grau).
   - **Rechtsklick auf einen Turm** → aufrüsten (Stufe II/III), verkaufen, Zielmodus wählen
     (Erster, Letzter, Stärkster, Schwächster, Nächster).
   - **Schleichen + Rechtsklick auf Gras** → den zuletzt gebauten Turm direkt noch einmal bauen.
   - **Rechtsklick in die Luft** → Hauptmenü (Status, Elemente, nächste Welle, Wellenvorschau, Hilfe).
   - Mit dem Stab in der Hand zeigt ein Partikelring die Reichweite des anvisierten Turms, daneben stehen
     seine Werte.
4. Per **Doppelsprung fliegst du**, um die ganze Arena zu überblicken.
5. Gegner kommen aus dem Portal und laufen zur Burg. Jeder, der durchkommt, kostet **Leben**
   (Bosse 5, der Endboss 20).
6. **Gold** gibt es für jeden besiegten Gegner, für jede geschaffte Welle, als **Zinsen** (3 % deines
   Goldes) und als Bonus, wenn du die nächste Welle früher startest.
7. **Element-Essenzen** bekommst du zu Beginn und mit den Wellen 5, 9, 13 … 45. Jede Essenz hebt ein
   Element um eine Stufe (max. 3). Beim Kauf erscheint ein **Elementar-Wächter** – besiegt bringt er
   viel Gold.
8. Überstehe **50 Wellen**. Alle 10 Wellen kommt ein Boss, in Welle 50 der Endboss (ein Riese). Danach
   kannst du im **Endlosmodus** weiterspielen.

Das Spiel **pausiert automatisch**, solange kein Mitspieler in der Arena ist, und wird mit der Welt
gespeichert. Wenn du die Welt mitten in einer Welle verlässt, startet diese Welle beim nächsten Mal neu.

| Schwierigkeit | Leben | Startgold | Lebenspunkte der Gegner |
|---|---|---|---|
| Leicht | 50 | 220 | 75 % |
| Normal | 30 | 160 | 110 % |
| Schwer | 20 | 150 | 140 % |

| Karte | Pfadlänge | Charakter |
|---|---|---|
| Schlange | 206 Blöcke | Vier lange Bahnen, Türme zwischen den Bahnen treffen zwei Bahnen – der Klassiker. |
| Spirale | 283 Blöcke | Längste Strecke, eine Spirale ins Zentrum – die entspannteste Karte. |
| Zickzack | 166 Blöcke | Kurz und mit weit getrennten Bahnen – die schwerste Karte. |

## Elemente

Die sechs Elemente bilden einen Kreis. Jedes Element macht **doppelten Schaden** gegen das nächste und nur
**halben Schaden** gegen das vorherige:

**☀ Licht → ☾ Dunkelheit → ≈ Wasser → ♨ Feuer → ❀ Natur → ▲ Erde → ☀ Licht**

Jede Welle hat ein Element (siehe HUD-Vorschau oben links). Wer passende Konter baut, gewinnt.
Basistürme und der Endboss sind neutral (immer 100 %). Einige Türme (Zwielicht, Sonnenfinsternis,
Regenbogen) wählen gegen jedes Ziel automatisch ihr stärkstes Element.

## Gegner

| Typ | Besonderheit |
|---|---|
| Normal | 10 Gegner mit normalen Lebenspunkten |
| Schnell | 10 schnelle Gegner mit weniger Leben |
| Masse | 24 kleine Gegner – ideal für Flächenschaden |
| Luft | 8 fliegende Gegner – Kanonen-, Erd-, Magma-, Beben-, Schlamm-, Vulkan-, Meteor-, Lavastrom- und Abgrundtürme treffen sie nicht |
| Boss | Alle 10 Wellen ein einzelner, sehr starker Gegner; nicht betäubbar, halb so stark verlangsamt, kostet 5 Leben |
| Endboss | Welle 50: ein Riese mit enormen Lebenspunkten, kostet 20 Leben |
| Elementar-Wächter | Erscheint beim Kauf einer Elementstufe |

Ab Welle 11 bekommen Wellen **Fähigkeiten** (später auch zwei auf einmal):

| Fähigkeit | Wirkung |
|---|---|
| ✪ Immun | Kann nicht verlangsamt, betäubt oder zurückgestoßen werden |
| ✚ Regeneration | Heilt sich jede Sekunde um 2 % |
| ♥ Heiler | Heilt sich und Gegner in der Nähe alle 3 Sekunden |
| ☠ Untot | Steht nach dem ersten Tod mit halbem Leben wieder auf |
| ❖ Spaltend | Zerfällt beim Tod in zwei kleinere Gegner |
| ■ Gepanzert | Jeder Treffer wird um einen festen Wert verringert – große Treffer lohnen sich |

Jedes Element hat eigene Mobs, z. B. Feuer: Zombifizierte Piglins, Füchse, Magmawürfel, Lohen und ein
Hoglin-Boss; Dunkelheit: Witherskelette, Endermen, Endermiten, Phantome und der Wärter.
Über jedem Gegner zeigt ein farbiger Lebensbalken Element und Fähigkeiten.

## Türme

Effekte im Überblick: **Fläche** (Splash), **Kette**, **Mehrfachziel**, **Puls** (alle in Reichweite),
**Aufheizen** (mehr Schaden pro Treffer), **Verlangsamen**, **Betäuben**, **Brand**, **Gift** (stapelt),
**Seuche** (springt über), **Fluch** (Ziel erleidet mehr Schaden), **Rückstoß**, **Hinrichten**,
**% der Lebenspunkte**, **Kritische Treffer**, **Auren** (Schaden/Tempo für Türme in der Nähe) und
**Gold-Türme**. Verkaufen bringt 75 % zurück, in derselben Bauphase sogar 100 %.

#### Basistürme (ohne Element, sofort verfügbar)

| Turm | Elemente | Kosten I / II / III | Reichweite | Besonderheit |
|---|---|---|---|---|
| **Pfeilturm** | – | 40 / 90 / 220 | 6–7 | Schneller Allrounder, trifft Boden- und Luftgegner. |
| **Kanonenturm** | – | 60 / 130 / 320 | 5.5–6.5 | Flächenschaden gegen Gruppen. *(nur Boden)* |

#### Elementtürme (Stufe N braucht das Element auf Stufe N)

| Turm | Elemente | Kosten I / II / III | Reichweite | Besonderheit |
|---|---|---|---|---|
| **Lichtturm** | ☀ Licht | 120 / 280 / 700 | 6.5–7.5 | Lichtstrahlen springen auf weitere Gegner über. |
| **Schattenturm** | ☾ Dunkelheit | 120 / 280 / 700 | 6.5–7 | Wuchtige Treffer, die Gegner verfluchen. |
| **Wasserturm** | ≈ Wasser | 120 / 280 / 700 | 6–7 | Wasserschwälle verlangsamen Gegner. |
| **Feuerturm** | ♨ Feuer | 120 / 280 / 700 | 6–7 | Feuerbälle mit Flächenschaden setzen Gegner in Brand. |
| **Naturturm** | ❀ Natur | 120 / 280 / 700 | 6.5–7.5 | Giftdornen treffen mehrere Gegner gleichzeitig. |
| **Erdturm** | ▲ Erde | 120 / 280 / 700 | 5.5–6.5 | Felsbrocken mit großem Flächenschaden, betäuben manchmal. *(nur Boden)* |

#### Doppelelement-Türme (beide Elemente auf Stufe N)

| Turm | Elemente | Kosten I / II / III | Reichweite | Besonderheit |
|---|---|---|---|---|
| **Zwielichtturm** | ☀ Licht, ☾ Dunkelheit | 350 / 800 / 2000 | 7–7.5 | Nutzt Licht oder Dunkelheit – was stärker ist. Zusatzschaden nach Lebenspunkten. |
| **Eisturm** | ☀ Licht, ≈ Wasser | 350 / 800 / 2000 | 6.5–7.5 | Starke Verlangsamung, friert Gegner manchmal ein. |
| **Blitzturm** | ☀ Licht, ♨ Feuer | 350 / 800 / 2000 | 7–8 | Kettenblitze springen zwischen vielen Gegnern. |
| **Sonnenspeer** | ☀ Licht, ❀ Natur | 350 / 800 / 2000 | 11–13 | Enorme Reichweite, mehr Schaden auf Entfernung. |
| **Goldturm** | ☀ Licht, ▲ Erde | 350 / 800 / 2000 | 6.5–7.5 | Mehr Kopfgeld für Gegner in Reichweite und Gold pro Welle. |
| **Gezeitenturm** | ☾ Dunkelheit, ≈ Wasser | 350 / 800 / 2000 | 6.5–7.5 | Flutwellen stoßen Gegner auf dem Pfad zurück. |
| **Höllenfeuerturm** | ☾ Dunkelheit, ♨ Feuer | 350 / 800 / 2000 | 6.5–7.5 | Ein Strahl, der immer heißer wird – ideal gegen Bosse. |
| **Seuchenturm** | ☾ Dunkelheit, ❀ Natur | 350 / 800 / 2000 | 6.5–7.5 | Eine Seuche, die auf benachbarte Gegner überspringt. |
| **Fluchturm** | ☾ Dunkelheit, ▲ Erde | 350 / 800 / 2000 | 5.5–6.5 | Verflucht alle Gegner in Reichweite. |
| **Dampfturm** | ≈ Wasser, ♨ Feuer | 350 / 800 / 2000 | 5–6 | Heißer Dampf trifft alle Gegner in Reichweite. |
| **Quellturm** | ≈ Wasser, ❀ Natur | 350 / 800 / 2000 | 5.5–6.5 | Unterstützung: erhöht das Angriffstempo naher Türme. |
| **Schlammturm** | ≈ Wasser, ▲ Erde | 350 / 800 / 2000 | 5–6 | Schlamm verlangsamt alle Bodengegner in Reichweite. *(nur Boden)* |
| **Schmiede** | ♨ Feuer, ❀ Natur | 350 / 800 / 2000 | 5.5–6.5 | Unterstützung: erhöht den Schaden naher Türme. |
| **Magmaturm** | ♨ Feuer, ▲ Erde | 350 / 800 / 2000 | 6–7 | Lavabomben mit riesigem Flächenschaden und Brand. *(nur Boden)* |
| **Bebenturm** | ❀ Natur, ▲ Erde | 350 / 800 / 2000 | 5.5–6.5 | Erdbeben treffen alle Bodengegner und betäuben sie. *(nur Boden)* |

#### Dreifachelement-Türme (alle drei Elemente auf Stufe N)

| Turm | Elemente | Kosten I / II / III | Reichweite | Besonderheit |
|---|---|---|---|---|
| **Mondturm** | ☀ Licht, ☾ Dunkelheit, ≈ Wasser | 1000 / 2200 / 5500 | 7–8 | Mondlicht verflucht und verlangsamt ganze Gruppen. |
| **Sonnenfinsternis** | ☀ Licht, ☾ Dunkelheit, ♨ Feuer | 1000 / 2200 / 5500 | 7.5–8.5 | Gewaltige Einzelschläge mit dem stärksten seiner drei Elemente. |
| **Seelenbaum** | ☀ Licht, ☾ Dunkelheit, ❀ Natur | 1000 / 2200 / 5500 | 7–8 | Trifft vier Ziele und erntet geschwächte Seelen sofort. |
| **Obelisk** | ☀ Licht, ☾ Dunkelheit, ▲ Erde | 1000 / 2200 / 5500 | 6.5–7.5 | Stärkt Schaden und Tempo naher Türme und greift selbst an. |
| **Prismaturm** | ☀ Licht, ≈ Wasser, ♨ Feuer | 1000 / 2200 / 5500 | 7–8 | Gebrochenes Licht springt durch ganze Wellen. |
| **Regenbogenturm** | ☀ Licht, ≈ Wasser, ❀ Natur | 1000 / 2200 / 5500 | 7–8 | Mehrere Strahlen, jeder im stärksten Element. |
| **Kristallturm** | ☀ Licht, ≈ Wasser, ▲ Erde | 1000 / 2200 / 5500 | 8–9 | Präzise Schüsse mit hoher Chance auf kritische Treffer. |
| **Sonnenaltar** | ☀ Licht, ♨ Feuer, ❀ Natur | 1000 / 2200 / 5500 | 6–7 | Sonnenglut verbrennt alle Gegner in Reichweite. |
| **Meteorturm** | ☀ Licht, ♨ Feuer, ▲ Erde | 1000 / 2200 / 5500 | 8–9 | Meteoriten mit riesigem Flächenschaden, betäuben. *(nur Boden)* |
| **Schatzkammer** | ☀ Licht, ❀ Natur, ▲ Erde | 1000 / 2200 / 5500 | 7–8 | Viel mehr Kopfgeld und reichlich Gold pro Welle. |
| **Nebelturm** | ☾ Dunkelheit, ≈ Wasser, ♨ Feuer | 1000 / 2200 / 5500 | 6–7 | Giftiger Nebel verflucht und verlangsamt alle in Reichweite. |
| **Sumpfturm** | ☾ Dunkelheit, ≈ Wasser, ❀ Natur | 1000 / 2200 / 5500 | 6–7 | Stapelndes Gift und Verlangsamung für alle in Reichweite. |
| **Abgrund** | ☾ Dunkelheit, ≈ Wasser, ▲ Erde | 1000 / 2200 / 5500 | 7–8 | Schleudert Gruppen weit auf dem Pfad zurück. *(nur Boden)* |
| **Verderbnis** | ☾ Dunkelheit, ♨ Feuer, ❀ Natur | 1000 / 2200 / 5500 | 7–8 | Brennende Seuche, die sich rasend ausbreitet. |
| **Vulkan** | ☾ Dunkelheit, ♨ Feuer, ▲ Erde | 1000 / 2200 / 5500 | 6–7 | Ausbrüche treffen alle Bodengegner und setzen sie in Brand. *(nur Boden)* |
| **Nekropole** | ☾ Dunkelheit, ❀ Natur, ▲ Erde | 1000 / 2200 / 5500 | 6.5–7.5 | Fünf Ziele gleichzeitig, geschwächte Gegner sterben sofort. |
| **Geysir** | ≈ Wasser, ♨ Feuer, ❀ Natur | 1000 / 2200 / 5500 | 7–8 | Heiße Fontänen: Flächenschaden, Rückstoß und Verlangsamung. |
| **Lavastrom** | ≈ Wasser, ♨ Feuer, ▲ Erde | 1000 / 2200 / 5500 | 6.5–7.5 | Zähe Lava: riesiger Flächenschaden, Brand und Verlangsamung. *(nur Boden)* |
| **Urwald** | ≈ Wasser, ❀ Natur, ▲ Erde | 1000 / 2200 / 5500 | 7–8 | Sechs Giftranken gleichzeitig. |
| **Flächenbrand** | ♨ Feuer, ❀ Natur, ▲ Erde | 1000 / 2200 / 5500 | 7–8 | Brennende Kettenblitze. |

## Befehle

Alles geht auch über den Baustab. Die Befehle sind eine Alternative:

| Befehl | Wirkung |
|---|---|
| `/td` oder `/td help` | Kurzanleitung im Chat |
| `/td lobby` | Lobby (neues Spiel / beitreten) bzw. Hauptmenü |
| `/td start [easy\|normal\|hard] [serpentine\|spiral\|zigzag]` | Spiel direkt starten, z. B. `/td start hard zigzag` (Leicht/Normal/Schwer, Schlange/Spirale/Zickzack) |
| `/td join <spieler>` | Dem Spiel eines anderen beitreten (Koop) |
| `/td menu` · `/td elements` | Hauptmenü · Element-Menü |
| `/td next` | Nächste Welle sofort starten (Bonusgold) |
| `/td info` | Spielstand im Chat |
| `/td leave` · `/td return` | Arena verlassen (Spiel pausiert) · zurückkehren |
| `/td quit` | Spiel dauerhaft verlassen |
| `/td stop` | Spiel beenden und Arena entfernen (Spielleiter) |
| `/td admin gold/essence/lives <n>` | Admin-Hilfen (OP) |

## Aus dem Quellcode bauen

Voraussetzungen: Java 21 und Python 3.

```bash
# Mod bauen und testen (Ergebnis: mod/build/libs/mctd-<version>.jar)
cd mod
./gradlew build

# Modpack bauen (löst Fabric API, Sodium usw. über die Modrinth-API auf)
cd ..
python3 modpack/build_mrpack.py --mod-jar mod/build/libs/mctd-1.0.0.jar --out dist --server-zip

# Modelle, Blockzustände, Übersetzungen und Texturen neu erzeugen
python3 tools/gen_assets.py
```

GitHub Actions (`.github/workflows/build.yml`) macht genau das bei jedem Push und stellt `.mrpack`,
Server-Zip und Mod-Jar als Artefakt bereit. Vorher laufen automatische Tests im echten Spiel:

- **Server-Rauchtest** (`./gradlew runSmokeTest`): Ein Minecraft-Server mit der Mod baut ohne Spieler eine
  Arena, setzt Türme und spielt die ersten Wellen.
- **Client-Test** (`./gradlew runClientTest`, in CI ohne Bildschirm über `xvfb-run`): Das Spiel legt wie
  ein Spieler eine Einzelspielerwelt an, startet über die Lobby eine Partie, baut Türme mit dem Baustab,
  öffnet die Menüs, spielt eine Welle, speichert, lädt die Welt neu und verlässt die Partie. Dabei prüft
  es Modelle, Texturen und Übersetzungen. Die Screenshots liegen im Artefakt `client-test-screenshots`.
- **Server-Zip-Test:** Der fertige Server aus dem Modpack (mit Fabric API, Lithium und FerriteCore von
  Modrinth) spielt dieselbe Partie wie der Rauchtest.

Nur wenn alle Tests bestehen, wird das Modpack hochgeladen. Ein Tag `v*` erzeugt zusätzlich ein Release.

### Projektstruktur

```
mod/                         Fabric-Mod (Minecraft 1.21.1, Yarn-Mappings)
  src/main/java/.../core/    Spielkern ohne Minecraft-Abhängigkeit: Elemente, Türme, Wellen, Simulation
  src/main/java/.../game/    Spielsitzungen: Arena, Gegner-Mobs, Speichern, Interaktion
  src/main/java/.../menu/    Truhen-Menüs (Lobby, Hauptmenü, Bauen, Turm, Elemente)
  src/main/java/.../mixin/   Die Arena-Dimension gilt nicht als „experimentelle Welteinstellung“
  src/client/java/           HUD, Partikeleffekte, Reichweitenanzeige, automatischer Client-Test
  src/main/resources/        Arena-Dimension, Modelle, Übersetzungen (de/en)
  src/test/java/             Selbsttests und Balance-Simulation (Bot spielt komplette Partien)
modpack/                     pack.json (Mod-Liste), Overrides, Builder für .mrpack und Server-Zip
tools/gen_assets.py          Generator für Modelle, Blockzustände, Texte und Texturen
```

Die Balance wurde mit einem Bot getestet, der komplette Partien spielt (`BalanceSim`): Auf *Leicht*
gewinnt er immer, auf *Normal* etwa zwei von drei Partien, auf *Schwer* nur selten. Wer Elemente gezielt
kontert und Unterstützungstürme nutzt, spielt deutlich besser als der Bot.
