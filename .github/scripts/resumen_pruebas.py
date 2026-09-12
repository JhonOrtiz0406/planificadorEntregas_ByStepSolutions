#!/usr/bin/env python3
"""Resumen legible de las pruebas y la cobertura del backend para GitHub Actions.

Uso: python3 .github/scripts/resumen_pruebas.py <carpeta-backend>
Escribe markdown en $GITHUB_STEP_SUMMARY (o en stdout si no existe).
"""
import os
import sys
import glob
import xml.etree.ElementTree as ET

backend = sys.argv[1] if len(sys.argv) > 1 else "backend"
salida = os.environ.get("GITHUB_STEP_SUMMARY")


def escribir(texto):
    if salida:
        with open(salida, "a", encoding="utf-8") as f:
            f.write(texto + "\n")
    else:
        print(texto)


def nombre_modulo(ruta):
    partes = ruta.replace("\\", "/").split("/")
    try:
        return partes[partes.index("build") - 1]
    except ValueError:
        return "?"


# ── Pruebas (JUnit XML) ───────────────────────────────────────────────
modulos = {}
for archivo in glob.glob(f"{backend}/**/build/test-results/test/*.xml", recursive=True):
    try:
        raiz = ET.parse(archivo).getroot()
    except ET.ParseError:
        continue
    m = nombre_modulo(archivo)
    d = modulos.setdefault(m, {"total": 0, "fallidas": 0, "omitidas": 0, "seg": 0.0, "detalle": []})
    d["total"] += int(raiz.get("tests", 0))
    d["fallidas"] += int(raiz.get("failures", 0)) + int(raiz.get("errors", 0))
    d["omitidas"] += int(raiz.get("skipped", 0))
    d["seg"] += float(raiz.get("time", 0) or 0)
    for caso in raiz.iter("testcase"):
        for fallo in list(caso.findall("failure")) + list(caso.findall("error")):
            mensaje = (fallo.get("message") or "").splitlines()[0][:160]
            d["detalle"].append(f"{caso.get('classname','').split('.')[-1]}.{caso.get('name')} — {mensaje}")

# ── Cobertura (JaCoCo XML) ────────────────────────────────────────────
cobertura = {}
for archivo in glob.glob(f"{backend}/**/build/reports/jacoco/**/*.xml", recursive=True):
    try:
        raiz = ET.parse(archivo).getroot()
    except (ET.ParseError, OSError):
        continue
    for contador in raiz.findall("counter"):
        if contador.get("type") == "LINE":
            cubierto = int(contador.get("covered", 0))
            perdido = int(contador.get("missed", 0))
            if cubierto + perdido:
                cobertura[nombre_modulo(archivo)] = 100.0 * cubierto / (cubierto + perdido)

if not modulos:
    escribir("### 🧪 Pruebas\n\n> No se generaron reportes de pruebas.\n")
    sys.exit(0)

total = sum(d["total"] for d in modulos.values())
fallidas = sum(d["fallidas"] for d in modulos.values())
omitidas = sum(d["omitidas"] for d in modulos.values())
icono = "✅" if fallidas == 0 else "❌"

escribir(f"### 🧪 Pruebas del backend — {icono} {total - fallidas}/{total} pasaron\n")
escribir("| Módulo | Pruebas | Fallidas | Omitidas | Tiempo | Cobertura de líneas |")
escribir("|---|---:|---:|---:|---:|---:|")
for m in sorted(modulos):
    d = modulos[m]
    cob = f"{cobertura[m]:.0f}%" if m in cobertura else "—"
    marca = "✅" if d["fallidas"] == 0 else "❌"
    escribir(f"| {marca} `{m}` | {d['total']} | {d['fallidas']} | {d['omitidas']} | {d['seg']:.1f}s | {cob} |")
escribir(f"| **Total** | **{total}** | **{fallidas}** | **{omitidas}** | | |")
escribir("")

fallos = [f"`{m}` → {x}" for m, d in modulos.items() for x in d["detalle"]]
if fallos:
    escribir("<details><summary>❌ Pruebas fallidas</summary>\n")
    for f in fallos[:40]:
        escribir(f"- {f}")
    escribir("\n</details>\n")
