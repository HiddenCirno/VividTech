#!/usr/bin/env python3
"""Create a dev-only GTLCore jar with GTLCore's AE2 GUI mixin removed.

GTLCore's `ae2.gui.FastCellMEStorageScreenMixin` targets an inherited AE2
method and is not compatible with the AE2 15.4.11 dev runtime used by this
project. This script copies the GTLCore dev jar and removes that one mixin
entry; it does not modify the GTLCore source tree.
"""
import json
import os
import sys
import zipfile

if len(sys.argv) != 3:
    print("usage: patch-gtlc-dev-jar.py <gtlcore-dev.jar> <output.jar>")
    raise SystemExit(2)

src, dst = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(src) as zin:
    entries = {info.filename: zin.read(info.filename) for info in zin.infolist()}

config = json.loads(entries["gtlcore.mixin.json"].decode("utf-8"))
for section in ("mixins", "client", "server"):
    if section in config:
        config[section] = [m for m in config[section]
                           if m != "ae2.gui.FastCellMEStorageScreenMixin"]
entries["gtlcore.mixin.json"] = json.dumps(config, indent=2).encode("utf-8")

os.makedirs(os.path.dirname(os.path.abspath(dst)), exist_ok=True)
with zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED) as zout:
    for name, data in entries.items():
        zout.writestr(name, data)
print("patched", dst)
