#!/usr/bin/env bash
set -e

# Converts input audio file to specified format or generates silence.
# Usage:
#   ./convert_audio.sh input.mp3 output.mp3
#   ./convert_audio.sh --silent output.mp3 [duration]
# Where duration is in seconds (default 1).

TARGET_RATE=44100
TARGET_CH=2
BITRATE=192k

if [ "$1" == "--silent" ]; then
  out="$2"
  duration="${3:-1}"
  ffmpeg -y -f lavfi -i anullsrc=r=$TARGET_RATE:cl=stereo -t "$duration" -ac $TARGET_CH -b:a $BITRATE "$out"
else
  in="$1"
  out="$2"
  ffmpeg -y -i "$in" -ar $TARGET_RATE -ac $TARGET_CH -b:a $BITRATE "$out"
fi
