# Implementation Plan: Espejito en la exposición

**Branch**: `011-espejito` | **Date**: 2026-10-26 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/011-espejito/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Sumar Espejito a `AppParaExpo`, sin crear otra aplicación y sin tocar `ProyectoParaRasperrypiV5`, `ServidorDePC` ni el piloto Android. El menú gana una quinta tarjeta, ESPEJITO, con `Emocion_Feliz.png`. La cámara se pide recién al tocar Comenzar a jugar. Una sesión muestra feliz, triste, enojado y sorprendido una sola vez, en orden al azar, con la imagen que ya está en la aplicación. El reconocimiento corre en el dispositivo con Face Landmarker, en modo video, una cara y blendshapes. La puntuación, la calibración y el segundo de estabilidad salen de `TEO-Juegos.html`, sin su ranking, sus niveles ni sus estrellas. Al completar las cuatro caras se anota una sola fila Ganó y TEO felicita con la voz que ya usa la exposición.

## Technical Context

**Language/Version**: TypeScript 5 y React 19, empaquetados con Vite 6. Node.js 20 o superior para instalar, generar voces y compilar. La tablet solo necesita un navegador.

**Primary Dependencies**: Las que `AppParaExpo` ya usa. `@mediapipe/tasks-vision` 1.1.1 ya está instalado para Piedra, papel o tijera. Espejito usa `FaceLandmarker` de ese mismo paquete. El WASM ya está en `AppParaExpo/reconocimiento/wasm`. Se agrega una sola vez `face_landmarker.task`. Las voces nuevas salen de `npm run voces`, que llama a Cartesia a través de `scripts/generar-voces.py` sin editar `ServidorDePC`. Vitest prueba la lógica sin cámara.

**Storage**: Las cuatro imágenes ya están en `AppParaExpo/src/recursos/imagenes/`. El modelo nuevo vive junto a `gesture_recognizer.task` y el plugin actual lo copia a `dist/reconocimiento/` y a `offline/reconocimiento/`. La sesión, la referencia tranquila y la cámara quedan en memoria. La planilla alojada recibe una fila al cerrar las cuatro caras. La compilación offline sigue con el registro apagado.

**Testing**: Vitest para la puntuación, el 85 %, el segundo de estabilidad, la calibración y el orden de las cuatro emociones. `npx vitest run` también cubre la fila Espejito / Ganó. `npm run build` y `npm run preview` comprueban la cámara y que el modelo no se pide afuera. `npm run voces` genera los mp3 que falten. La cara real queda marcada como prueba de dispositivo. `npm test` no se usa, porque vuelve a copiar imágenes viejas.

**Target Platform**: La misma exposición: tablet horizontal primero, y también 360×640, 800×1280, 1280×800 y 1366×768. Chrome y Edge actuales. La cámara exige un contexto seguro (`localhost` o HTTPS).

**Project Type**: Ampliación de la aplicación web estática que ya está en `AppParaExpo`.

**Performance Goals**: Como máximo una inferencia a la vez y cerca de 10 por segundo, en el hilo de la página. La pantalla sigue aceptando toques. Un acierto confirmado se felicita antes de 2 segundos. La interfaz visible no se redibuja en cada fotograma de la cámara: el video queda en una referencia y el estado solo cambia cuando cambian el mensaje o el avance.

**Constraints**: Una sola cara, `numFaces: 1`, `outputFaceBlendshapes: true`, `detectForVideo`. El espejo es CSS y no entra al detector. No se guardan fotos, videos ni datos del rostro. No hay ranking, estrellas, niveles ni partida sin cámara. Al salir se apagan las pistas, el bucle y el detector, y se cortan las voces de este juego. Los umbrales iniciales son los de la referencia y solo se mueven si una prueba con cara real lo pide.

**Scale/Scope**: Una tarjeta más, una pantalla y una sesión de cuatro emociones. Los otros cuatro juegos conservan sus reglas. Una fila de planilla por sesión completa.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

La constitución 1.2.0 gobierna el piloto Android. Esta feature solo amplía la exposición web ya exceptuada en `009-app-expo-juegos` y `010-piedra-papel-tijera`.

| Principio | Resultado | Cómo se cumple |
|---|---|---|
| I. Conservar el funcionamiento existente | Pasa | No se edita el peluche, la app Android ni el servidor. Sombras, Adivina, Contar y Piedra, papel o tijera conservan reglas, voces y planilla. |
| II. Mantener la base técnica | Excepción ya justificada | La exposición sigue en la web de `AppParaExpo`. Face Landmarker se agrega a esa web, sobre el paquete que la exposición ya tiene. |
| III. Respetar las referencias visuales | Excepción ya justificada | No hay boceto de este juego en `docs/ui-ux`. La composición sale de `spec.md` y reutiliza botones, paleta y tarjetas de la exposición. |
| IV. Conservar la identidad acordada | Excepción acotada | El carácter visual de la exposición se mantiene. Las cuatro caras son los PNG ya presentes. La tarjeta nueva usa otro pastel del mismo menú. |
| V. Mostrar datos y estados reales | Pasa | El acierto sale de la expresión sostenida. Permiso, cara ausente y fallo del modelo se muestran como tales. No se simula una cámara ni se anota una sesión a medias. |
| VI. Garantizar una interfaz adaptable | Pasa en la web | Cuatro tamaños de `quickstart.md`, toques grandes y el rostro visible en vertical. |
| VII. Separar decisiones pendientes | Pasa | Tiempo de uso, pulsador, agenda y modo noche no entran. |
| VIII. Desarrollar por bloques | Pasa | Este plan cubre solo Espejito. `tasks.md` queda para el comando siguiente. |
| IX. Verificar antes de declarar terminado | Pasa | `quickstart.md` separa la lógica automática, la compilación publicada con cámara, el HTML autónomo y la prueba de cara real. |

Revisión posterior al diseño: los contratos no agregan ranking ni envían la imagen. La fila de planilla sale por `registrarResultado`, una vez por sesión completa. La cámara se apaga al salir. La limitación de `file://` queda escrita y no se resuelve con un CDN. No aparecieron violaciones nuevas.

## Project Structure

### Documentation (this feature)

```text
specs/011-espejito/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── recorrido-espejito.md
│   └── reconocimiento.md
└── tasks.md              # lo crea /speckit-tasks, no este comando
```

### Source Code (repository root)

```text
AppParaExpo/
├── src/
│   ├── pantallas/
│   │   ├── PantallaMenu.tsx          # quinta tarjeta
│   │   ├── Aplicacion.tsx            # abre Espejito y anota al completar
│   │   └── PantallaEspejito.tsx
│   ├── juegos/
│   │   ├── comun/tipos.ts            # JuegoId incluye espejito
│   │   └── espejito/
│   │       ├── emociones.ts
│   │       ├── clasificacion.ts
│   │       ├── clasificacion.test.ts
│   │       ├── estabilidad.ts
│   │       ├── estabilidad.test.ts
│   │       ├── calibracion.ts
│   │       ├── calibracion.test.ts
│   │       ├── sesion.ts
│   │       ├── sesion.test.ts
│   │       ├── reconocedor.ts
│   │       ├── useCamara.ts
│   │       └── mensajes.ts
│   ├── servicios/registro.ts         # Juego "Espejito", Resultado "Ganó"
│   ├── juegos/ppt/planilla.ts        # anotaEnPlanilla también para espejito
│   ├── audio/textos-fijos.json       # frases nuevas, misma voz
│   ├── estilos/
│   │   ├── menu.css                  # cinco tarjetas
│   │   └── espejito.css
│   └── recursos/imagenes/
│       ├── Emocion_Feliz.png
│       ├── Emocion_Triste.png
│       ├── Emocion_Enojado.png
│       └── Emocion_Sorprendido.png
├── reconocimiento/
│   ├── wasm/                         # el que ya usa el gesto, sin cambiarlo
│   ├── gesture_recognizer.task
│   └── face_landmarker.task          # una copia local, nueva
├── scripts/generar-voces.py          # sin cambiar el generador
├── dist/reconocimiento/
├── offline/
│   ├── AppParaExpo-Offline.html      # sigue existiendo; la cámara no se promete en file://
│   └── reconocimiento/
└── README.md                         # una frase: Espejito anota Ganó al completar
```

**Structure Decision**: Se sigue el corte de `AppParaExpo`. La pantalla va en `src/pantallas`, la lógica pura en `src/juegos/espejito` y las imágenes siguen en `src/recursos/imagenes`, que es donde `urlImagen` ya las resuelve. No se crea un reproductor ni un registro nuevos. No se usa `MarcoJuego`, porque ese marco muestra corazones y este juego no tiene vidas. El ciclo de la cámara se escribe en `juegos/espejito/useCamara.ts`, copiando el apagado y el ritmo de Piedra, papel o tijera, para no cambiar el reconocedor de gestos. El modelo entra en la carpeta `reconocimiento/` que el plugin de Vite ya copia.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| Principio II: el reconocimiento no vive en la app Android | El pedido agrega el juego a la web de exposición y exige cámara en la tablet | Llevarlo al piloto Kotlin no actualiza `AppParaExpo` ni las dos entregas |
| Principio III: no hay boceto en `docs/ui-ux` | La composición de imagen, cámara y cierre ya está cerrada en `spec.md` | Un boceto nuevo reabriría una pantalla que el pedido ya describió |
| Principio IV: la quinta tarjeta usa otro pastel del menú | El menú actual ya es infantil y colorido, y cinco tarjetas necesitan distinguirse | Copiar el marfil de Inicio rompería las tarjetas que la exposición ya muestra |

## Phase 0 y Phase 1

La investigación está en [research.md](research.md). El modelo, los contratos y la guía de comprobación quedaron en [data-model.md](data-model.md), [contracts/](contracts/) y [quickstart.md](quickstart.md). No quedan incógnitas técnicas abiertas.
