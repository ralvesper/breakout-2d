#!/usr/bin/env python3
"""
Gera os assets do Breakout: o atlas de sprites (PNG + .atlas) e os efeitos sonoros (WAV).

Nenhuma dependencia externa - PNG e escrito na mao com zlib, WAV com o modulo wave da
biblioteca padrao. Rodar de qualquer lugar:

    python3 tools/generate_assets.py

Os arquivos vao para assets/sprites/ e assets/sfx/. Regerar e sempre seguro: o script
sobrescreve tudo que produz e nao le nada de volta.
"""

import math
import pathlib
import struct
import wave
import zlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
SPRITES = ROOT / "assets" / "sprites"
SFX = ROOT / "assets" / "sfx"

ATLAS_W, ATLAS_H = 512, 128
SS = 4  # supersampling: desenha 4x maior e reduz, para ter bordas suaves


# --------------------------------------------------------------------- canvas

class Canvas:
    """RGBA simples em memoria, com supersampling para antialias."""

    def __init__(self, width, height):
        self.w = width * SS
        self.h = height * SS
        self.px = bytearray(self.w * self.h * 4)

    def blend(self, x, y, rgba):
        if not (0 <= x < self.w and 0 <= y < self.h):
            return
        i = (y * self.w + x) * 4
        r, g, b, a = rgba
        if a >= 255:
            self.px[i:i + 4] = bytes((r, g, b, 255))
            return
        inv = (255 - a) / 255.0
        f = a / 255.0
        for k, c in enumerate((r, g, b)):
            self.px[i + k] = int(self.px[i + k] * inv + c * f)
        self.px[i + 3] = max(self.px[i + 3], a)

    def downsample(self):
        """Media dos SS x SS subpixels -> imagem final, com alpha correto."""
        out_w, out_h = self.w // SS, self.h // SS
        out = bytearray(out_w * out_h * 4)
        for y in range(out_h):
            for x in range(out_w):
                r = g = b = a = 0
                for dy in range(SS):
                    for dx in range(SS):
                        i = ((y * SS + dy) * self.w + (x * SS + dx)) * 4
                        pa = self.px[i + 3]
                        # pre-multiplica para a media nao sujar as bordas transparentes
                        r += self.px[i] * pa
                        g += self.px[i + 1] * pa
                        b += self.px[i + 2] * pa
                        a += pa
                o = (y * out_w + x) * 4
                if a:
                    out[o] = min(255, r // a)
                    out[o + 1] = min(255, g // a)
                    out[o + 2] = min(255, b // a)
                out[o + 3] = a // (SS * SS)
        return out_w, out_h, bytes(out)


def rounded_rect(canvas, x, y, w, h, radius, top, bottom):
    """Retangulo arredondado com gradiente vertical de `top` para `bottom`."""
    x, y, w, h, radius = x * SS, y * SS, w * SS, h * SS, radius * SS
    for py in range(y, y + h):
        t = (py - y) / max(h - 1, 1)
        color = tuple(int(top[k] + (bottom[k] - top[k]) * t) for k in range(3))
        for px in range(x, x + w):
            # distancia ate o retangulo interno define o canto arredondado
            dx = max(x + radius - px, px - (x + w - 1 - radius), 0)
            dy = max(y + radius - py, py - (y + h - 1 - radius), 0)
            if dx * dx + dy * dy <= radius * radius:
                canvas.blend(px, py, (*color, 255))


def highlight(canvas, x, y, w, h, alpha=70):
    """
    Clareia o terco superior da forma que JA esta desenhada, com queda suave.

    Importante nao pintar branco por cima: isso vira uma tarja chapada em vez de brilho.
    So pixels ja opacos sao tocados, entao o canto arredondado continua arredondado.
    """
    x, y, w = x * SS, y * SS, w * SS
    band = max(h // 2, 1) * SS
    for py in range(y, y + band):
        fade = (1.0 - (py - y) / band) ** 2
        for px in range(x, x + w):
            i = (py * canvas.w + px) * 4
            if 0 <= i < len(canvas.px) and canvas.px[i + 3]:
                for k in range(3):
                    canvas.px[i + k] = min(255, int(canvas.px[i + k] + alpha * fade))


def wall(canvas, x, y, w, h):
    """
    Parede lateral: barra com chanfro nas duas bordas.

    Simetrica de proposito - a mesma regiao serve para a parede da esquerda e a da
    direita, sem precisar espelhar nada na hora de desenhar.

    Sem detalhe que dependa da altura (juntas, rebites): no jogo a parede e esticada de
    64 px para ~75 unidades de mundo, e qualquer marca horizontal viraria uma faixa
    gorda. O chanfro horizontal e o degrade vertical esticam sem deformar.
    """
    base = (0.26, 0.29, 0.38)
    edge = (0.56, 0.61, 0.74)
    xs, ys, ws, hs = x * SS, y * SS, w * SS, h * SS

    for px in range(xs, xs + ws):
        t = (px - xs) / max(ws - 1, 1)
        # 0 nas bordas, 1 no meio: claro na quina, escuro no centro
        depth = (1.0 - 2 * abs(t - 0.5)) ** 0.6
        column = tuple(edge[k] + (base[k] - edge[k]) * depth for k in range(3))
        for py in range(ys, ys + hs):
            fade = 1.0 - 0.22 * (py - ys) / max(hs - 1, 1)
            canvas.blend(px, py, tuple(int(255 * c * fade) for c in column) + (255,))


def disc(canvas, cx, cy, radius, inner, outer):
    """Esfera com sombreamento radial, luz vinda de cima a esquerda."""
    cx, cy, radius = cx * SS, cy * SS, radius * SS
    for py in range(int(cy - radius) - 1, int(cy + radius) + 2):
        for px in range(int(cx - radius) - 1, int(cx + radius) + 2):
            dx, dy = px - cx + 0.5, py - cy + 0.5
            dist = math.hypot(dx, dy)
            if dist > radius:
                continue
            # deslocar o centro da luz cria o volume
            lit = math.hypot(dx + radius * 0.3, dy + radius * 0.3) / (radius * 1.6)
            t = min(lit, 1.0)
            color = tuple(int(inner[k] + (outer[k] - inner[k]) * t) for k in range(3))
            canvas.blend(px, py, (*color, 255))


# ---------------------------------------------------------------------- atlas

REGIONS = [
    # nome,      x,   y,  w,   h
    ("brick1",   0,   0, 112, 48),
    ("brick2", 112,   0, 112, 48),
    ("brick3", 224,   0, 112, 48),
    ("powerup", 336,  0,  48, 48),
    ("paddle",   0,  48, 128, 16),
    ("ball",   128,  48,  32, 32),
    ("pixel",  168,  48,   8,  8),
    ("wall",   384,   0,  28, 64),
]

BRICK_COLORS = [
    ((0.42, 0.84, 1.0), (0.20, 0.55, 0.88)),
    ((1.0, 0.82, 0.38), (0.90, 0.52, 0.16)),
    ((1.0, 0.48, 0.52), (0.82, 0.20, 0.32)),
]


def rgb(triple):
    return tuple(int(c * 255) for c in triple)


def build_atlas():
    canvas = Canvas(ATLAS_W, ATLAS_H)
    by_name = {name: (x, y, w, h) for name, x, y, w, h in REGIONS}

    for index, (top, bottom) in enumerate(BRICK_COLORS):
        x, y, w, h = by_name[f"brick{index + 1}"]
        rounded_rect(canvas, x + 1, y + 1, w - 2, h - 2, 6, rgb(top), rgb(bottom))
        highlight(canvas, x + 1, y + 1, w - 2, h - 2)

    x, y, w, h = by_name["powerup"]
    rounded_rect(canvas, x + 2, y + 2, w - 4, h - 4, 10, (0.62 * 255, 1.0 * 255, 0.6 * 255),
                 (0.16 * 255, 0.70 * 255, 0.38 * 255))
    highlight(canvas, x + 2, y + 2, w - 4, h - 4)

    x, y, w, h = by_name["paddle"]
    rounded_rect(canvas, x, y, w, h, 7, (242, 246, 252), (150, 166, 196))
    highlight(canvas, x, y, w, h, alpha=55)

    x, y, w, h = by_name["ball"]
    disc(canvas, x + w / 2, y + h / 2, w / 2 - 1, (255, 255, 255), (158, 174, 204))

    x, y, w, h = by_name["wall"]
    wall(canvas, x, y, w, h)

    x, y, w, h = by_name["pixel"]
    rounded_rect(canvas, x, y, w, h, 0, (255, 255, 255), (255, 255, 255))

    return canvas.downsample()


def write_png(path, width, height, pixels):
    raw = bytearray()
    stride = width * 4
    for y in range(height):
        raw.append(0)  # filtro "None" nesta scanline
        raw.extend(pixels[y * stride:(y + 1) * stride])

    def chunk(tag, payload):
        body = tag + payload
        return struct.pack(">I", len(payload)) + body + struct.pack(
            ">I", zlib.crc32(body) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    path.write_bytes(png)


def write_atlas_descriptor(path, image_name, width, height):
    lines = [image_name, f"size: {width},{height}", "format: RGBA8888",
             "filter: Linear,Linear", "repeat: none"]
    for name, x, y, w, h in REGIONS:
        lines += [name, "  rotate: false", f"  xy: {x}, {y}", f"  size: {w}, {h}",
                  f"  orig: {w}, {h}", "  offset: 0, 0", "  index: -1"]
    path.write_text("\n".join(lines) + "\n")


# ----------------------------------------------------------------------- audio

RATE = 44100


def write_wav(path, samples):
    with wave.open(str(path), "w") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(RATE)
        out.writeframes(b"".join(
            struct.pack("<h", max(-32767, min(32767, int(s * 32767)))) for s in samples))


def tone(start_hz, end_hz, seconds, kind="square", volume=0.35, decay=4.0):
    total = int(RATE * seconds)
    phase = 0.0
    for i in range(total):
        t = i / total
        hz = start_hz + (end_hz - start_hz) * t
        phase += 2 * math.pi * hz / RATE
        wave_value = math.sin(phase)
        if kind == "square":
            wave_value = 1.0 if wave_value >= 0 else -1.0
        elif kind == "triangle":
            wave_value = 2 / math.pi * math.asin(max(-1.0, min(1.0, wave_value)))
        # envelope exponencial: ataque instantaneo, queda rapida. Som de arcade.
        yield wave_value * volume * math.exp(-decay * t)


def build_sounds():
    SFX.mkdir(parents=True, exist_ok=True)
    write_wav(SFX / "wall.wav", tone(200, 190, 0.055, "square", 0.22, 6))
    write_wav(SFX / "paddle.wav", tone(300, 340, 0.075, "square", 0.30, 5))
    write_wav(SFX / "brick.wav", tone(620, 480, 0.090, "square", 0.28, 7))
    write_wav(SFX / "launch.wav", tone(240, 520, 0.110, "triangle", 0.26, 3))
    write_wav(SFX / "powerup.wav", tone(420, 900, 0.220, "triangle", 0.30, 2))
    write_wav(SFX / "lose.wav", tone(320, 110, 0.420, "square", 0.26, 2.2))


def main():
    SPRITES.mkdir(parents=True, exist_ok=True)
    width, height, pixels = build_atlas()
    write_png(SPRITES / "breakout.png", width, height, pixels)
    write_atlas_descriptor(SPRITES / "breakout.atlas", "breakout.png", width, height)
    build_sounds()
    print(f"atlas  {width}x{height} -> {SPRITES/'breakout.png'}")
    print(f"sons   6 arquivos      -> {SFX}")


if __name__ == "__main__":
    main()
