#!/usr/bin/env python3
"""Builds the MC Tower Defense modpack.

Creates
  * MC-Tower-Defense-<version>.mrpack       - Modrinth modpack (Prism Launcher, Modrinth App, ATLauncher, ...)
  * MC-Tower-Defense-Server-<version>.zip   - ready-to-run Fabric server (optional, --server-zip)

The mod itself goes into the pack's overrides; all other mods are resolved from Modrinth (newest release for the
configured Minecraft version) and referenced by URL and hash, as the .mrpack format expects.

Usage:
  python3 modpack/build_mrpack.py --mod-jar mod/build/libs/mctd-1.0.0.jar --out dist [--server-zip]
"""
import argparse
import hashlib
import json
import pathlib
import sys
import urllib.parse
import urllib.request
import zipfile

HERE = pathlib.Path(__file__).resolve().parent
USER_AGENT = "dalpner/MC_TowerDefense modpack-builder (github.com/dalpner/MC_TowerDefense)"
MODRINTH = "https://api.modrinth.com/v2"
FABRIC_META = "https://meta.fabricmc.net/v2"


def get(url, binary=False):
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=60) as response:
        data = response.read()
    return data if binary else json.loads(data.decode("utf-8"))


def latest_loader(minecraft):
    for entry in get(f"{FABRIC_META}/versions/loader/{minecraft}"):
        if entry["loader"].get("stable"):
            return entry["loader"]["version"]
    raise SystemExit(f"No stable Fabric loader found for Minecraft {minecraft}")


def latest_installer():
    for entry in get(f"{FABRIC_META}/versions/installer"):
        if entry.get("stable"):
            return entry["version"]
    raise SystemExit("No stable Fabric installer found")


def resolve_mod(slug, minecraft):
    query = urllib.parse.urlencode({
        "loaders": json.dumps(["fabric"]),
        "game_versions": json.dumps([minecraft]),
    })
    versions = get(f"{MODRINTH}/project/{slug}/version?{query}")
    if not versions:
        return None
    releases = [v for v in versions if v.get("version_type") == "release"] or versions
    version = releases[0]
    files = version["files"]
    primary = next((f for f in files if f.get("primary")), files[0])
    return {
        "slug": slug,
        "version": version["version_number"],
        "filename": primary["filename"],
        "url": primary["url"],
        "hashes": {"sha1": primary["hashes"]["sha1"], "sha512": primary["hashes"]["sha512"]},
        "size": primary["size"],
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--mod-jar", required=True, type=pathlib.Path, help="the built mctd jar")
    parser.add_argument("--out", default="dist", type=pathlib.Path)
    parser.add_argument("--version", help="pack version (defaults to the mod version from the jar name)")
    parser.add_argument("--server-zip", action="store_true", help="also build a ready-to-run server zip")
    parser.add_argument("--offline", action="store_true",
                        help="do not contact Modrinth/Fabric (for testing; the pack then only contains the mod)")
    args = parser.parse_args()

    pack = json.loads((HERE / "pack.json").read_text(encoding="utf-8"))
    minecraft = pack["minecraft"]
    mod_jar = args.mod_jar
    if not mod_jar.is_file():
        raise SystemExit(f"Mod jar not found: {mod_jar}")
    version = args.version or mod_jar.stem.split("-", 1)[-1]
    args.out.mkdir(parents=True, exist_ok=True)

    loader = pack.get("loaderVersion", "latest")
    if loader == "latest":
        loader = "0.16.9" if args.offline else latest_loader(minecraft)

    files = []
    resolved = []
    for mod in ([] if args.offline else pack["mods"]):
        info = resolve_mod(mod["slug"], minecraft)
        if info is None:
            message = f"{mod['slug']}: no version for Minecraft {minecraft}"
            if mod.get("required"):
                raise SystemExit(message)
            print("WARNING " + message + " - skipped", file=sys.stderr)
            continue
        info["env"] = {"client": mod["client"], "server": mod["server"]}
        resolved.append(info)
        files.append({
            "path": f"mods/{info['filename']}",
            "hashes": info["hashes"],
            "env": info["env"],
            "downloads": [info["url"]],
            "fileSize": info["size"],
        })
        print(f"  {info['slug']:<14} {info['version']}")

    index = {
        "formatVersion": 1,
        "game": "minecraft",
        "versionId": version,
        "name": pack["name"],
        "summary": pack["summary"],
        "files": files,
        "dependencies": {"minecraft": minecraft, pack["loader"]: loader},
    }

    base = pack["name"].replace(" ", "-")
    mrpack = args.out / f"{base}-{version}.mrpack"
    with zipfile.ZipFile(mrpack, "w", zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("modrinth.index.json", json.dumps(index, indent=2, ensure_ascii=False))
        zf.write(mod_jar, f"overrides/mods/{mod_jar.name}")
        overrides = HERE / "overrides"
        for path in sorted(overrides.rglob("*")):
            if path.is_file():
                zf.write(path, "overrides/" + path.relative_to(overrides).as_posix())
    print(f"Wrote {mrpack} (Minecraft {minecraft}, Fabric Loader {loader}, {len(files)} mods + MC Tower Defense)")

    if args.server_zip:
        server = args.out / f"{base}-Server-{version}.zip"
        with zipfile.ZipFile(server, "w", zipfile.ZIP_DEFLATED) as zf:
            zf.write(mod_jar, f"mods/{mod_jar.name}")
            for info in resolved:
                if info["env"]["server"] == "unsupported":
                    continue
                data = get(info["url"], binary=True)
                if hashlib.sha1(data).hexdigest() != info["hashes"]["sha1"]:
                    raise SystemExit(f"Hash mismatch for {info['filename']}")
                zf.writestr(f"mods/{info['filename']}", data)
            launcher_note = ""
            if not args.offline:
                installer = latest_installer()
                launcher = get(f"{FABRIC_META}/versions/loader/{minecraft}/{loader}/{installer}/server/jar", binary=True)
                zf.writestr("fabric-server-launch.jar", launcher)
                launcher_note = "fabric-server-launch.jar ist bereits enthalten."
            zf.writestr("eula.txt", "# Lies https://aka.ms/MinecraftEULA und setze den Wert auf true.\neula=false\n")
            zf.writestr("start.bat", "java -Xmx4G -jar fabric-server-launch.jar nogui\npause\n")
            zf.writestr("start.sh", "#!/bin/sh\njava -Xmx4G -jar fabric-server-launch.jar nogui\n")
            zf.writestr("LIESMICH-SERVER.txt", (
                f"MC Tower Defense {version} – Server für Minecraft {minecraft} (Fabric Loader {loader})\n\n"
                f"{launcher_note}\n"
                "1. Java 21 installieren.\n"
                "2. eula.txt öffnen und eula=true setzen (nachdem du die Minecraft-EULA gelesen hast).\n"
                "3. start.bat (Windows) bzw. start.sh (Linux/macOS) ausführen. Beim ersten Start lädt der\n"
                "   Fabric-Launcher den Minecraft-Server automatisch herunter.\n"
                "4. Alle Spieler brauchen das Modpack (MC-Tower-Defense-*.mrpack) im Launcher.\n"))
        print(f"Wrote {server}")


if __name__ == "__main__":
    main()
