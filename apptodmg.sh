#!/bin/bash
app=$1
dmg=${app/%app/dmg}
wrk=${dmg/%dmg/wrk.dmg}
tmp=/tmp/$app
base=$(basename "$app")
name=${base/%.app/}
hdiutil create -fs HFSX -layout SPUD -size 1m "$wrk" -volname "$name"
mkdir -p "$tmp"
hdiutil attach "$wrk" -noautoopen -mountpoint "$tmp"
ditto "$app" "$tmp/${name}.app/"
hdiutil detach "$tmp"
hdiutil convert "$wrk" -format UDZO -imagekey zlib-level=9 -o "$dmg"
rm -f "$wrk"
