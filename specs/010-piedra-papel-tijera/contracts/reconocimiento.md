# Contract: Reconocimiento en el dispositivo

No es un servicio externo. Nada de este contrato sale del navegador de la tablet.

## Entradas que acepta el juego

| Clase, sin importar mayúsculas | Figura |
|---|---|
| `Closed_Fist` | piedra |
| `Open_Palm` | papel |
| `Victory` | tijera |

Cualquier otra categoría, una lista vacía o ninguna mano no son jugada.

## Observación válida

- Confianza mayor o igual a 0,7.
- Una sola mano. Si el modelo devolviera más de una, se usa solo la primera.
- El cuadro es el del elemento de video, no el que se ve reflejado en la pantalla.

## Confirmación

- La misma clase válida tiene que repetirse, sin huecos, durante cerca de 800 ms.
- Un cambio de clase, una confianza menor o la ausencia de mano ponen el contador en cero.
- Recién entonces empieza la cuenta 3, 2, 1, cerca de un segundo cada número.
- Durante la cuenta se sigue exigiendo esa misma clase. Si falta, la cuenta se anula y la computadora no elige.
- Al llegar a cero, la figura de la persona queda fija y no se reemplaza por un gesto posterior de esa ronda.

## Archivos que tienen que estar junto a la página publicada

- `reconocimiento/wasm/`, copiado del paquete `@mediapipe/tasks-vision` instalado.
- `reconocimiento/gesture_recognizer.task`.

Esas rutas son relativas a la página. No se piden a Google ni a un CDN al abrir el juego.

## Lo que no ocurre

- No se graba video ni se guarda una foto.
- No se conserva la clase ni la confianza después de salir de la pantalla.
- No se crea un segundo reconocedor si el primero sigue abierto.
- No se analiza un fotograma mientras el anterior no terminó.
- No hay más de cerca de 10 análisis por segundo.
- `ppt` no produce una fila de planilla.
