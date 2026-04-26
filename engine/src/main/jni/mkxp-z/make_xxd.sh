#!/bin/sh
set -eu

# Generate embedded assets and shaders for mkxp-z includes.
mkdir -p xxd/assets xxd/shader

rm -f xxd/assets/*.xxd
rm -f xxd/shader/*.xxd

for path in assets/*; do
  [ -f "$path" ] || continue
  name="$(basename "$path")"
  printf "Generating assets/%s.xxd\n" "$name"
  xxd -i "$path" "xxd/assets/$name.xxd"
done

for path in shader/*; do
  [ -f "$path" ] || continue
  name="$(basename "$path")"
  printf "Generating shader/%s.xxd\n" "$name"
  xxd -i "$path" "xxd/shader/$name.xxd"
done
