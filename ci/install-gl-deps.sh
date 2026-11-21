#!/usr/bin/env bash
set -euo pipefail

if ! command -v apt-get >/dev/null 2>&1; then
    echo "error: apt-get is required; install Xvfb, Mesa, and GLFW's X11 runtime libraries manually." >&2
    exit 1
fi

# Runtime dependencies for GLFW's X11 backend and software OpenGL under Xvfb.
packages=(
    xvfb
    xauth
    libgl1
    libglx-mesa0
    libgl1-mesa-dri
    libx11-6
    libxrandr2
    libxinerama1
    libxcursor1
    libxi6
    libxxf86vm1
    libxkbcommon0
)

missing=()
for package in "${packages[@]}"; do
    if ! dpkg-query -W -f='${Status}' "$package" 2>/dev/null | grep -q '^install ok installed$'; then
        missing+=("$package")
    fi
done

if ((${#missing[@]} == 0)); then
    echo "GL dependencies already installed."
    exit 0
fi

sudo=()
if ((EUID != 0)); then
    if command -v sudo >/dev/null 2>&1; then
        sudo=(sudo)
    else
        echo "error: root or sudo is required to install: ${missing[*]}" >&2
        exit 1
    fi
fi

export DEBIAN_FRONTEND=noninteractive
"${sudo[@]}" apt-get update
"${sudo[@]}" apt-get install -y --no-install-recommends "${missing[@]}"
