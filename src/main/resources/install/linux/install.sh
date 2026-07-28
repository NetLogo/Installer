#!/bin/bash

set -e

for name in NetLogo NetLogo3D HubNetClient Behaviorsearch; do

cat > /usr/share/applications/$name-$2.desktop << EOL
[Desktop Entry]
Version=1.0
Type=Application
Name=$name $2
Exec=$1/bin/$name
Icon=$1/icons/$name.png
Terminal=false
Categories=Education;Science;Java
EOL

done

update-desktop-database
