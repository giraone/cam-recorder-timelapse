#!/bin/bash

typeset -i i=1

for file in img-*.jpg; do
  nr=$(printf "%04d" $i)
  echo $file nr-${nr}.jpg
  mv $file nr-${nr}.jpg
  let i+=1
done
