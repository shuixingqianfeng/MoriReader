#!/usr/bin/env python3
"""Exercise the real Android touch path and verify navigation reaches Search."""

import re
import subprocess
import time
import xml.etree.ElementTree as ET


PACKAGE = "io.github.shuixingqianfeng.morireader"


def adb(*arguments: str, capture: bool = False) -> str:
    result = subprocess.run(
        ["adb", *arguments],
        check=True,
        stdout=subprocess.PIPE if capture else None,
        text=capture,
    )
    return result.stdout if capture else ""


def screenshot(path: str) -> None:
    with open(path, "wb") as output:
        subprocess.run(["adb", "exec-out", "screencap", "-p"], check=True, stdout=output)


def center(bounds: str) -> tuple[int, int]:
    values = [int(value) for value in re.findall(r"\d+", bounds)]
    if len(values) != 4:
        raise ValueError(f"invalid Android bounds: {bounds!r}")
    left, top, right, bottom = values
    return round((left + right) / 2), round((top + bottom) / 2)


def navigation_centers(xml_text: str) -> tuple[tuple[int, int], tuple[int, int]]:
    root = ET.fromstring(xml_text)

    def find(label: str) -> tuple[int, int]:
        candidates = [
            node
            for node in root.iter("node")
            if node.attrib.get("content-desc") == label and node.attrib.get("bounds")
        ]
        if not candidates:
            raise ValueError(f"unable to locate navigation entry {label!r} in Android UI hierarchy")
        # The navigation icon is the lowest matching semantic node on screen.
        return max((center(node.attrib["bounds"]) for node in candidates), key=lambda point: point[1])

    return find("书库"), find("搜索")


def main() -> int:
    time.sleep(0.8)
    adb("shell", "uiautomator", "dump", "/sdcard/morireader-window.xml")
    hierarchy = adb("exec-out", "cat", "/sdcard/morireader-window.xml", capture=True)
    with open("morireader-window.xml", "w", encoding="utf-8") as output:
        output.write(hierarchy)
    screenshot("morireader-navigation-before.png")

    start, end = navigation_centers(hierarchy)
    adb("logcat", "-c")
    adb("shell", "input", "swipe", str(start[0]), str(start[1]), str(end[0]), str(end[1]), "420")
    time.sleep(1.2)
    screenshot("morireader-navigation-after.png")

    navigation_log = adb("logcat", "-d", "-s", "MoriReaderNavigation:I", "*:S", capture=True)
    with open("navigation-after.log", "w", encoding="utf-8") as output:
        output.write(navigation_log)
    if "selected=SEARCH" not in navigation_log:
        raise AssertionError(
            f"drag {start}->{end} did not open Search; navigation log={navigation_log!r}"
        )
    print(f"navigation drag={start}->{end} search screen visible")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
