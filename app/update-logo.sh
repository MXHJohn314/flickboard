#!/usr/bin/env bash
set -euo pipefail
# Requires https://libresprite.github.io/, imagemagick
# Layer-splits and rescales logo for use by Android

mkdir -p tmp

# Crop out border
BORDERCROP=2,2,30,30

# Rescale by an integer factor to minimize the impact of renderer antialiasing
SCALEFACTOR=800%

# Android adaptive icons want the icon to be at 108dp, of which 66dp is safe from masking by the system
# 50% border leaves us with 54dp, which seems to be a decent balance
BORDERFACTOR=50%

for layer in background foreground; do
    libresprite --batch --layer $layer logo.aseprite --crop $BORDERCROP --save-as tmp/logo_$layer.png
    if [ $layer = background ]; then
        BORDERBG='#39314bff'
    else
        BORDERBG='#00000000'
    fi
    magick tmp/logo_$layer.png \
        -filter point -resize $SCALEFACTOR \
        -bordercolor $BORDERBG -border $BORDERFACTOR \
        src/main/res/drawable/logo_$layer.png
done

# libresprite --batch logo.aseprite --save-as tmp/logo_standalone.png
# magick tmp/logo_standalone.png \
#     -filter point -resize $SCALEFACTOR \
#     src/main/res/drawable/logo_standalone.png
