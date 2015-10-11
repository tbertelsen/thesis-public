#!/bin/bash
for f in "$@" ; do
	[[ "$f" =~ ^(.*)\.(e|o|pe|po)([0-9]+)([\-\.]([0-9]+))?$ ]]
	name="${BASH_REMATCH[1]}"
	outType="${BASH_REMATCH[2]}"
	job="${BASH_REMATCH[3]}"
	task="${BASH_REMATCH[5]}"
	if [[ -n "$job" ]]; then
		# echo "Output file: $f"
		# echo "name=$name  outType=$outType job=$job   task=$task   "
		dir="log-$name/$job/$outType/"
		mkdir -p "$dir"
		mv -v "$f" "$dir"
	fi
done
