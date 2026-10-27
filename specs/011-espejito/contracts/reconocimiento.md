# Contract: Reconocimiento de Espejito

El análisis ocurre en el dispositivo, sobre la cámara en vivo. No identifica a la persona y no conserva la imagen.

## Arranque

- El detector se crea después de Comenzar a jugar, una vez por sesión de cámara.
- Modo `VIDEO`, `numFaces: 1`, `outputFaceBlendshapes: true`.
- Primero se intenta el delegado de la GPU del propio equipo. Si esa creación falla, se crea de nuevo con CPU. No se sale a internet para ese cambio.
- El WASM se lee de `reconocimiento/wasm/`.
- El modelo se lee de `reconocimiento/face_landmarker.task`.
- Esas dos rutas son relativas a la página. No hay URL de Google ni de un CDN en el código del juego.
- `detectForVideo` recibe el elemento de video y una marca de tiempo creciente. Se reutiliza la misma instancia.

## Lectura

- Si `faceBlendshapes` no trae una cara, la observación está vacía.
- Si trae una, se usa solo la primera. De cada categoría se leen `categoryName` y `score`.
- El video puede verse reflejado. Esa transformación es solo visual. El elemento que entra al detector no está reflejado, porque un reflejo cambia izquierda y derecha.

## Puntuación

Los pesos son:

| Emoción | Movimientos y pesos | Umbral |
|---|---|---|
| feliz | `mouthSmileLeft` 0,5, `mouthSmileRight` 0,5 | 0,18 |
| triste | `mouthFrownLeft` 0,4, `mouthFrownRight` 0,4, `browInnerUp` 0,2 | 0,16 |
| sorprendido | `jawOpen` 0,5, `eyeWideLeft` 0,25, `eyeWideRight` 0,25 | 0,10 |
| enojado | `browDownLeft` 0,35, `browDownRight` 0,35, `noseSneerLeft` 0,15, `noseSneerRight` 0,15 | 0,16 |

Para cada movimiento, el incremento es `max(0, actual - referencia)`. La puntuación de una emoción es la suma de incremento por peso, dividida por la suma de sus pesos. La pedida se cumple si su puntuación alcanza su umbral y al menos el 85 % de la puntuación más alta entre las cuatro. Por debajo del umbral, nunca se cumple.

## Tiempo

- El reloj de análisis apunta a unas 10 miradas por segundo.
- Si una mirada anterior no terminó, la nueva se salta.
- Cada actualización de avance acredita o descuenta como máximo 250 ms.
- Cumplir suma `tiempo / 1000`. No cumplir, o no ver la cara, resta `tiempo / 2000`.
- El acierto es el avance en 1. Un solo paso no llega.
- Al acertar, esa ronda no vuelve a llamar al detector hasta el próximo desafío o hasta salir.

## Calibración

- Hacen falta 5 observaciones y 1,5 segundos desde la primera cara, o el promedio de las observaciones que haya a los 8 segundos de haber visto alguna cara.
- Ocho segundos sin ninguna cara no abren el desafío.
- El promedio reemplaza a la lista de observaciones. No queda una imagen.

## Cámara

- `getUserMedia` con audio desactivado, cámara frontal ideal y 640×480 ideal, sin mínimo.
- El video se reproduce solo, en silencio y dentro de la página.
- Al salir, cada pista recibe `stop()`, el bucle se cancela, el detector recibe `close()` y las voces pendientes se cortan.

## Privacidad y red

- No se llama a `toDataURL`, no se sube el cuadro y no se escribe el rostro en almacenamiento.
- Con la red cortada, después de compilar, abrir el juego no pide el modelo ni el WASM afuera.
- El archivo suelto `offline/AppParaExpo-Offline.html` abierto con doble clic no es la prueba de este contrato. La prueba es la carpeta publicada, servida en local.
