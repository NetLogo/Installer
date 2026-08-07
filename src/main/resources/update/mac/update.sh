#!/bin/bash

set -e

if [ $# -lt 2 ]; then
  exit 1
fi

kill $1

rm -rf "/Applications/NetLogo Installer.app"
cp -r $2 "/Applications/NetLogo Installer.app"
rm -rf $2

open "/Applications/NetLogo Installer.app"
