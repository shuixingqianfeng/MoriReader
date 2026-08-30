#!/usr/bin/env python3
"""Exercise the real Android touch path and verify navigation reaches Search."""

import re
import subprocess
import time
import xml.etree.ElementTree as ET


def adb(*arguments: str, capture: bool = False) -> str:
    result = subprocess.run(
        ["adb", *arguments],
        check=True,
        stdout=subprocess.PIPE if capture else None,
        text=True,
    )
    return result.stdout if capture else ""


def ui_tree() -> bytes:
    adb("shell", "uiautomator", "dump", "/sdcard/window.xml")
    return subprocess.run(
        ["adb", "exec-out", "cat", "/sdcard/window.xml"],
        check=True,
        stdout=subprocess.PIPE,
    ).stdout


def center(bounds: str) -> tuple[int, int]:
    values = [int(value) for value in re.findall(r"\d+", bounds)]
    if len(values) != 4:
        raise ValueError(f"invalid Android bounds: {bounds}")
    left, top, right, bottom = values
    return (left + right) // 2, (top + bottom) // 2


def main() -> int:
    before = ET.fromstring(ui_tree())
    nodes = list(before.iter("node"))
    library = next(node for node in nodes if node.attrib.get("content-desc") == "书库")
    search = next(node for node in nodes if node.attrib.get("content-desc") == "搜索")
    start_x, start_y = center(library.attrib["bounds"])
    end_x, end_y = center(search.attrib["bounds"])
    adb("shell", "input", "swipe", str(start_x), str(start_y), str(end_x), str(end_y), "240")
    time.sleep(1.2)

    after_xml = ui_tree()
    with open("navigation-after.xml", "wb") as output:
        output.write(after_xml)
    after = ET.fromstring(after_xml)
    if not any(node.attrib.get("text") == "搜索" for node in after.iter("node")):
        raise AssertionError("drag ended at the Search orb but the Search screen did not open")
    print(f"navigation drag=({start_x},{start_y})->({end_x},{end_y}) search screen visible")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
