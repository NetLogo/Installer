#!/bin/bash

set -e

xdg-mime install $2

xdg-mime default NetLogo-$1.desktop text/nlogo
xdg-mime default NetLogo-$1.desktop text/nlogox
xdg-mime default NetLogo3D-$1.desktop text/nlogo3d
xdg-mime default NetLogo3D-$1.desktop text/nlogox3d
