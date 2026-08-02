# pyRoulette

<img width="1024" height="512" alt="banner" src="https://github.com/user-attachments/assets/c0288a1e-eeea-4127-bda1-5d296fa95e81" />

![Version](https://img.shields.io/badge/version-1.1-brightgreen)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21+-blue)
![Server](https://img.shields.io/badge/Spigot%20%2F%20Paper-supported-ff8fd2)
![Economy](https://img.shields.io/badge/Economy-Vault-yellow)

Animated roulette tables for Minecraft servers.

## Overview

pyRoulette is a casino-style roulette plugin built with Minecraft display entities.

Admins can create roulette tables anywhere, and players can right-click the wheel to open a betting menu.  
The wheel supports number bets, color bets, and column bets, with configurable payouts and visuals.

## Features

- Animated roulette wheel
- Idle animation while waiting for bets
- Smooth spin animation with slowdown
- Persistent roulette tables
- Automatic respawn when chunks load
- Number bets: 0, 00, and 1-36
- Color bets: red, black, and green
- Column bets: column 1, column 2, and column 3
- Configurable payouts
- Configurable min/max bets
- Configurable max bets per player
- Vault economy support
- GUI betting menu
- Chat-based bet amount input
- Supports amounts like `1000`, `1k`, and `1.5m`
- Custom head textures for roulette blocks
- Configurable display scale, offsets, pointer, text, and hologram
- Local roulette sounds
- Local roulette messages
- Audit log for bets, wins, losses, refunds, and results
- Hex color support
- Optional bStats support

## Commands

```txt
/pyroulette create [radius]
/pyroulette remove <id>
/pyroulette list
/pyroulette reload
