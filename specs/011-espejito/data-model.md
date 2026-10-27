# Data model: Espejito

Fecha: 2026-10-26. La sesión y la referencia de la cara viven en memoria mientras la pantalla está abierta. No se escriben fotos, videos ni datos del rostro. La única persistencia es la fila de la visita al completar las cuatro caras, por el mismo envío que los otros juegos.

## Emoción

| Campo | Valores |
|---|---|
| id | `feliz`, `triste`, `enojado`, `sorprendido` |
| nombre | Feliz, Triste, Enojado, Sorprendido |
| imagen | `Emocion_Feliz.png`, `Emocion_Triste.png`, `Emocion_Enojado.png`, `Emocion_Sorprendido.png` |
| instrucción | «¡Poné tu mejor sonrisa!», «¡Hagamos una carita triste!», «¡Mostrame tu mejor cara de enojado!», «¡Poné cara de sorpresa!» |
| felicitación | «¡Qué linda sonrisa!», «¡Qué carita triste! ¡Lo lograste!», «¡Qué cara de gruñón! ¡Muy bien!», «¡Uau! ¡Qué sorpresa!» |
| movimientos | Los de [reconocimiento.md](contracts/reconocimiento.md), con su peso |
| umbral | 0,18, 0,16, 0,16 y 0,10, en el orden feliz, triste, enojado y sorprendido |

Cada emoción entra una sola vez en la sesión. La imagen y el nombre siguen visibles durante todo el intento.

## Observación

Mapa de nombre de movimiento a número entre 0 y 1, leído de una sola cara. Un nombre que no vino vale 0. Si no hay cara, la observación está vacía y no se usa como referencia ni como acierto.

La observación no se guarda en disco. El conjunto de observaciones de la calibración se descarta en cuanto existe el promedio.

## Referencia tranquila

Promedio, por movimiento, de las observaciones de la calibración. Solo existe si hubo al menos una cara. El incremento de un movimiento es `max(0, actual - referencia)`.

## Puntuación

Cuatro números, uno por emoción. Cada uno es la suma de incremento por peso, dividida por la suma de pesos de esa emoción. La condición de la emoción pedida es verdadera solo si su puntuación es mayor o igual a su umbral y mayor o igual al 85 % de la mayor de las cuatro.

Ejemplos que la prueba automática tiene que fijar:

- Sonrisa clara en los dos lados, y el resto en cero: feliz supera 0,18 y es la mayor, así que la condición es verdadera.
- Feliz en 0,17, aunque sea la mayor: la condición es falsa.
- Feliz en 0,20 y triste en 0,30: feliz supera 0,18, pero 0,20 es menor que el 85 % de 0,30, así que la condición es falsa.

## Avance

| Campo | Significado |
|---|---|
| progreso | De 0 a 1 |
| último instante | Marca de la última actualización, o vacío al empezar |

Cada paso toma como máximo 250 ms. Si la condición es verdadera, suma `tiempo / 1000`. Si es falsa o no hay cara, resta `tiempo / 2000` y no baja de 0. El desafío se confirma cuando el progreso llega a 1. Un solo paso no puede confirmarlo. Al confirmar, el progreso queda en 1 y esa ronda no sigue actualizándolo.

## Calibración

| Hecho | Resultado |
|---|---|
| 5 o más caras y 1,5 s desde la primera | Empieza el primer desafío con el promedio |
| 8 s desde la primera cara, con alguna cara y sin llegar a la meta anterior | Empieza con el promedio de lo ya visto |
| 8 s desde la apertura y ninguna cara | Sigue en calibración, con el mensaje de acercarse |
| Una cara después de ese silencio | Vuelve a contar 5 observaciones y 1,5 s |

## Sesión

| Campo | Significado |
|---|---|
| orden | Las cuatro emociones, permutadas con un generador inyectable |
| índice | Cuál de las cuatro está en curso |
| referencia | La referencia tranquila de esta sesión |
| logro | Vacío, o la emoción ya confirmada de la ronda actual |
| fase | `entrada`, `preparando`, `calibrando`, `desafio`, `logrado`, `resumen`, `impedida` |

Al volver a jugar se sortea otro orden, se olvida la referencia y se calibra de nuevo. La cámara, si el permiso sigue aceptado, no vuelve a la pantalla de entrada. Al salir al menú se olvida toda la sesión.

## Fila de la visita

La visita ya tiene nombre y pantalla. `espejito` es una pantalla más. Al entrar en `resumen` se arma un `ResultadoVisita` con el nombre, el juego `espejito` y el resultado `gano`. El envío escribe `Juego` como `Espejito` y `Resultado` como `Ganó`. Una sesión produce una fila. Una sesión que no llega al resumen no produce fila. La compilación offline sigue sin enviar, igual que el resto de los juegos.

## Estados de la pantalla

El recorrido visible está en [recorrido-espejito.md](contracts/recorrido-espejito.md). La fase `impedida` guarda el motivo: permiso, ausente, ocupada, modelo o navegador.
