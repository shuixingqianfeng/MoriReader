#!/usr/bin/env python3
"""Exercise the real Android touch path and verify navigation reaches Search."""

import re
import subprocess
import time


def adb(*arguments: str, capture: bool = False) -> str:
    result = subprocess.run(
        ["adb", *arguments],
        check=True,
        stdout=subprocess.PIPE if capture else None,
        text=True,
    )
    return result.stdout if capture else ""


def main() -> int:
    size_output = adb("shell", "wm", "size", capture=True)
    match = re.search(r"(?:Physical|Override) size:\s*(\d+)x(\d+)", size_output)
    if not match:
        raise ValueError(f"unable to parse Android display size: {size_output!r}")
    width, height = (int(value) for value in match.groups())
    start_x, start_y = round(width * 0.14), round(height * 0.89)
    end_x, end_y = round(width * 0.87), start_y

    adb("logcat", "-c")
    adb("shell", "input", "swipe", str(start_x), str(start_y), str(end_x), str(end_y), "240")
    time.sleep(1.2)

    navigation_log = adb("logcat", "-d", "-s", "MoriReaderNavigation:I", "*:S", capture=True)
    with open("navigation-after.log", "w", encoding="utf-8") as output:
        output.write(navigation_log)
    if "selected=SEARCH" not in navigation_log:
        raise AssertionError("drag ended at the Search entry but the Search screen did not open")
    print(f"navigation drag=({start_x},{start_y})->({end_x},{end_y}) search screen visible")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
