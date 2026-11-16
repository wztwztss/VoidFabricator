from pathlib import Path
import re

root = Path(r"E:\wzt\MC\modcreater\VoidFabricator\src\main\resources\assets\voidfabricator\lang")


def load(path):
    keys = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.startswith("#"):
            keys.append(line.split("=", 1)[0])
    duplicates = sorted({key for key in keys if keys.count(key) > 1})
    return set(keys), duplicates


zh, zh_dup = load(root / "zh_CN.lang")
en, en_dup = load(root / "en_US.lang")
print(f"ZH_KEYS={len(zh)} EN_KEYS={len(en)}")
print(f"ZH_DUP={zh_dup}")
print(f"EN_DUP={en_dup}")
print(f"ZH_ONLY={sorted(zh - en)}")
print(f"EN_ONLY={sorted(en - zh)}")

java_root = Path(r"E:\wzt\MC\modcreater\VoidFabricator\src\main\java")
direct_reference = re.compile(r'"(voidfabricator(?:\.[A-Za-z0-9_]+)+)"')
used = set()
for source in java_root.rglob("*.java"):
    used.update(direct_reference.findall(source.read_text(encoding="utf-8")))
# Registry/unlocalized base names use item.<id>.name and tile.<id>.name instead.
used -= {"voidfabricator.machine_recipe_pool_pattern", "voidfabricator.void_fabricator"}
print(f"DIRECT_JAVA_KEYS={len(used)}")
print(f"MISSING_ZH={sorted(used - zh)}")
print(f"MISSING_EN={sorted(used - en)}")
