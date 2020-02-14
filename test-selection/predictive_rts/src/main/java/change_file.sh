#!/bin/bash
find . -name '*.java' | while read line; do
    echo "'$line'"
    git log --oneline --pretty=format:"$line" -- $line >> log.log
    echo "" >> log.log
done