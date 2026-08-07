#!/bin/bash

set -e

if [ $# -lt 2 ]; then
  exit 1
fi

kill $1

rm -rf ~/NetLogo-Installer
cp -r $2 ~/NetLogo-Installer
rm -rf $2

cd ~/NetLogo-Installer/bin

./NetLogo\ Installer
