#!/bin/bash

# "glob" not available on Windows
# ffmpeg -framerate 1 -pattern_type glob -i "nr-*.jpg" -c:v libx264 -pix_fmt yuv420p -r 30 video.mp4

ffmpeg -framerate 1 -i nr-%04d.jpg -c:v libx264 -r 30 -pix_fmt yuv420p -r 30 video.mp4


