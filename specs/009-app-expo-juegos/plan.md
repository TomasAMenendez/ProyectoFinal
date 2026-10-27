# Implementation Plan: Juegos de exposición en tablet

**Branch**: `009-app-expo-juegos` | **Date**: 2026-10-25 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/009-app-expo-juegos/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Crear `AppParaExpo` junto a `ProyectoParaRasperrypiV5`, sin modificar ese proyecto ni `ServidorDePC`. Es una aplicación estática con React, TypeScript y Vite. Los tres juegos usan copias locales de los catálogos y de los dibujos originales. La navegación vive en memoria, así la misma interfaz funciona en un sitio, en un subdirectorio y al abrir un archivo. Una compilación publica `dist/`. Otra, desde el mismo código, deja `offline/AppParaExpo-Offline.html` con scripts, estilos, datos, imágenes y voces adentro. Las voces de Micaela se generan una vez con la función Cartesia que ya usa el servidor de la PC y quedan guardadas en la aplicación. La planilla solo se escribe desde la versión publicada, al cerrar una partida.

## Technical Context

**Language/Version**: TypeScript 5 y React 18 o 19, empaquetados con Vite 6. Node.js 20 o superior para preparar el proyecto. El archivo sin internet no necesita Node para abrirse.

**Primary Dependencies**: React y Vite. `vite-plugin-singlefile` solo en la compilación autónoma. Vitest para las reglas de juego. Sin enrutador, sin fuentes externas y sin bibliotecas de red. La generación de voz reutiliza `ServidorDePC/tts_cartesia.py` sin editarlo. La compresión de los WAV usa ffmpeg si está instalado.

**Storage**: Catálogos JSON, PNG y audios dentro de `AppParaExpo`. El nombre de la visita queda en memoria hasta recargar. La versión publicada envía una fila a Google Sheets mediante un Apps Script. La versión autónoma no guarda ni envía resultados.

**Testing**: Vitest para sorteo, pistas, vidas, victoria, derrota y reinicio. `vite build` para la versión publicada. La compilación autónoma se abre con la red desactivada. Revisión visual en 360×640, 800×1280, 1280×800 y 1366×768.

**Target Platform**: Tablet en horizontal como prioridad. También computadora, tablet vertical y celular vertical. Navegadores de comprobación: Chrome y Edge actuales. La versión publicada se sirve por HTTPS, en la raíz o en un subdirectorio.

**Project Type**: Aplicación web estática, independiente del piloto Android.

**Performance Goals**: El nombre y la entrada a un juego se completan en menos de 30 segundos. La consecuencia de un error se ve en menos de un segundo. El tablero de una tablet de 1280×800 no pide desplazamiento para jugar.

**Constraints**: No se modifica `ProyectoParaRasperrypiV5` ni `ServidorDePC`. No hay servidor propio. El archivo autónomo no hace solicitudes ni incluye la credencial de la planilla. Las voces no se piden durante el juego. Sombras usa cinco parejas. La silueta del delfín se copia desde el archivo existente con acento.

**Scale/Scope**: Una bienvenida, un menú y tres juegos. Catálogos actuales: 54 sombras, 67 adivinanzas y 10 ejercicios de contar. Una fila por partida terminada en la versión publicada.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

La constitución 1.2.0 gobierna el rediseño Android de TEO. Esta feature es una aplicación nueva de exposición. El piloto Android, el peluche y el servidor quedan fuera de sus cambios.

| Principio | Resultado | Cómo se cumple |
|---|---|---|
| I. Conservar el funcionamiento existente | Pasa | Los catálogos y dibujos se leen y se copian. No se edita el peluche, la app Android ni el servidor. |
| II. Mantener la base técnica | Excepción justificada | La app Android sigue en Kotlin. La exposición pide una web independiente. La excepción está en Complexity Tracking. |
| III. Respetar las referencias visuales | Excepción justificada | No hay boceto de esta exposición en `docs/ui-ux`. La composición la fija `spec.md`. Se reutiliza el logo oficial, no una captura como interfaz. |
| IV. Conservar la identidad acordada | Excepción acotada | El logo oficial se copia y el confeti no tapa el mensaje. La paleta marfil del piloto Android no se traslada a estos juegos: la especificación pide una presentación infantil colorida. |
| V. Mostrar datos y estados reales | Pasa | Las respuestas salen de los JSON. No se inventan cantidades, pistas ni dibujos. El delfín usa la silueta que ya existe. |
| VI. Garantizar una interfaz adaptable | Pasa en la web | Áreas táctiles amplias, contraste, alto visible, áreas seguras y los cuatro tamaños de `quickstart.md`. Las unidades son de CSS porque no es una vista Android. |
| VII. Separar decisiones pendientes | Pasa | Tiempo de uso, pulsador, agenda sin TEO y modo noche no entran en esta feature. |
| VIII. Desarrollar por bloques | Pasa | Este plan cubre solo la exposición. `tasks.md` queda para el comando siguiente. |
| IX. Verificar antes de declarar terminado | Pasa | `quickstart.md` separa reglas automáticas, compilación publicada, archivo local sin red y revisión de los cuatro tamaños. |

Revisión posterior al diseño: los contratos no agregan pantallas del piloto ni escrituras al proyecto original. La planilla es un destino nuevo, limitado a la versión publicada. No aparecieron violaciones sin justificación.

## Project Structure

### Documentation (this feature)

```text
specs/009-app-expo-juegos/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── recorrido-ui.md
│   ├── registro-planilla.md
│   └── voces.md
└── tasks.md              # lo crea /speckit-tasks, no este comando
```

### Source Code (repository root)

```text
AppParaExpo/
├── src/
│   ├── componentes/          # botones, vidas, cierre, aviso de pista
│   ├── pantallas/            # nombre, menú y cada juego
│   ├── juegos/
│   │   ├── comun/            # sorteo, vidas, pistas, reinicio
│   │   ├── sombras/
│   │   ├── adivina/
│   │   └── contar/
│   ├── datos/                # copias de los tres JSON
│   ├── estilos/
│   ├── recursos/
│   │   ├── imagenes/
│   │   └── voces/
│   ├── audio/
│   └── servicios/            # registro de planilla, vacío en la entrega autónoma
├── public/
├── scripts/
│   ├── copiar-recursos.mjs
│   └── generar-voces.py
├── dist/                     # versión publicada
├── offline/
│   └── AppParaExpo-Offline.html
├── planilla-apps-script.txt
├── package.json
├── vite.config.ts
├── .env.example
└── README.md
```

**Structure Decision**: Un solo proyecto web en `AppParaExpo`, hermano de `ProyectoParaRasperrypiV5`. No hay backend. Cada juego tiene estado propio y comparte el cierre, las vidas y el sorteo. Las dos entregas salen de dos modos de Vite sobre ese código.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| Principio II: la exposición no usa Kotlin ni la app Android | El pedido exige una web de tablet, publicable y también abrible como un solo archivo, con los tres juegos funcionando | Meter los juegos en la app Android no produce el archivo autónomo ni el sitio estático, y modificaría el piloto |
| Principio III: no hay boceto elegido en `docs/ui-ux` | La composición de menú, tablero, opciones y cierre ya está descrita y aceptada en `spec.md` | Inventar otro boceto atrasaría una composición que el pedido ya fijó |
| Principio IV: los juegos no usan la paleta marfil de Inicio | La especificación pide una interfaz infantil colorida y el logo de TEO | Copiar la distribución de Inicio aleja el tablero horizontal del pedido |

## Phase 0 y Phase 1

La investigación está en [research.md](research.md). El modelo, los contratos y la guía de comprobación quedaron en [data-model.md](data-model.md), [contracts/](contracts/) y [quickstart.md](quickstart.md). No quedan incógnitas técnicas abiertas.
