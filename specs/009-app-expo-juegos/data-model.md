# Data model: Juegos de exposición en tablet

Los datos de juego son copias locales. La visita no tiene cuenta ni almacenamiento permanente.

## Visita

| Campo | Regla |
|---|---|
| nombre | Obligatorio para ver el menú. Se recortan espacios. Entre 1 y 40 caracteres visibles. |
| pantalla | `nombre`, `menu`, `sombras`, `adivina` o `contar`. |

La visita nace en `nombre`. Confirmar el nombre pasa a `menu`. Elegir una tarjeta abre ese juego con una partida nueva. Volver al menú destruye la partida y regresa a `menu`. Recargar la página vuelve a `nombre`.

## Ficha de sombra

Copia de un elemento de `juego_siluetas` en `sombras.json`.

| Campo | Regla |
|---|---|
| id | Único. |
| silueta | Archivo PNG presente en la copia. En `delfin`, la copia usa el dibujo existente `Silueta_Delfín.png`. |
| nombreVisible | Primera respuesta, tal como está escrita. Es la única palabra del tablero. |
| pistas | Lista original. Hoy hay dos por ficha. |

Relación: una partida usa cinco fichas. Sus `nombreVisible` no se repiten.

## Partida de Sombras

| Campo | Regla |
|---|---|
| fichas | Cinco, en el orden visual de las siluetas. |
| nombres | Los cinco `nombreVisible`, en otro orden. |
| uniones | Parejas correctas ya aceptadas. |
| seleccion | Silueta elegida por toque, o vacía. |
| pistasMostradas | Cantidad de pistas ya usadas por cada `id`. |
| vidas | Entero de 3 a 0. |
| resultado | Vacío, `gano` o `perdio`. |

Transiciones:

1. Arrastre o segundo toque sobre el nombre de la silueta activa: se agrega la unión y se limpia la selección. Con cinco uniones, `resultado` pasa a `gano`.
2. Unión con otro nombre: no se guarda la línea. Se suma una pista de esa ficha según el orden 1, 3, 2, salteando ausentes. `vidas` baja 1. En 0, `resultado` pasa a `perdio`.
3. Soltar fuera de un nombre, cambiar de silueta o tocar algo ya resuelto: no cambia `vidas`.
4. Reiniciar reemplaza la partida por otra de cinco fichas y tres vidas.

## Ficha de adivinanza

| Campo | Regla |
|---|---|
| id | Único. |
| imagen | PNG presente. Oculta mientras `resultado` está vacío. |
| nombreCorrecto | Solo la primera respuesta. |
| pistas | Tres textos en el catálogo actual. |

## Partida de Adivina

| Campo | Regla |
|---|---|
| objetivo | Una ficha. |
| opciones | Cinco nombres: el correcto y cuatro primeras respuestas de otras fichas, mezclados. |
| descartadas | Nombres incorrectos ya elegidos. |
| pistasVisibles | Empieza con la pista 1. |
| vidas | De 3 a 0. |
| resultado | Vacío, `gano` o `perdio`. |

Transiciones:

1. Elegir `nombreCorrecto`: `resultado` pasa a `gano` y la imagen puede mostrarse.
2. Elegir otro nombre disponible: entra en `descartadas`, se agrega la siguiente pista existente del orden 1, 3, 2 y `vidas` baja 1. En 0, `resultado` pasa a `perdio` y la imagen puede mostrarse.
3. Elegir un nombre de `descartadas`: no cambia `vidas` ni las pistas.
4. Reiniciar elige otro objetivo cuando hay más de uno.

## Ejercicio de contar

| Campo | Regla |
|---|---|
| imagen | PNG presente. |
| pregunta | Texto del catálogo. |
| cantidad | Primera respuesta interpretada como entero. El catálogo actual usa del 1 al 10. |

## Partida de Contar

| Campo | Regla |
|---|---|
| ejercicio | Uno del catálogo. |
| opciones | Los enteros del 1 al 10, en ese orden. |
| descartadas | Números incorrectos ya elegidos. |
| vidas | De 3 a 0. |
| resultado | Vacío, `gano` o `perdio`. |

Elegir la cantidad correcta gana. Elegir otro número disponible lo descarta y resta una vida. El tercer error pierde. Reiniciar elige otro ejercicio cuando hay más de uno.

## Vidas y cierre

Las tres partidas comparten la representación: corazón derecho, corazón medio, corazón izquierdo. Se rompe el de la derecha con la primera vida perdida, el del medio con la segunda y el de la izquierda con la tercera.

El cierre aparece solo si `resultado` es `gano` o `perdio`. Ganar muestra el texto «¡Muy bien! ¡Ganaste!» y confeti. Perder muestra «¡Se terminó el juego! ¡Intentalo otra vez!». Las acciones son reiniciar esa partida o volver a `menu`.

## Frase hablada

| Campo | Regla |
|---|---|
| clave | Identificador estable del texto. |
| texto | Pista, pregunta, nombre, número o reacción. |
| archivo | Audio generado una vez y guardado en `AppParaExpo`. |
| momento | Apertura del nombre (solo publicada), bienvenida del juego, aparición de una pista o pregunta, opción elegida, acierto, error, opción ya descartada, victoria, derrota, volver a jugar o volver al menú. |

Las reacciones variables tienen al menos tres textos por momento: acierto, error, opción descartada, volver a jugar y volver al menú. Una partida no depende de que el audio termine para aceptar el toque.

## Resultado de visita

Solo lo produce la versión publicada, y solo al pasar `resultado` a `gano` o `perdio`.

| Campo | Regla |
|---|---|
| nombre | El de la visita. |
| juego | `sombras`, `adivina` o `contar`. |
| resultado | `gano` o `perdio`. |
| fechaHora | Momento local del cierre, en ISO 8601 con desplazamiento. |

Volver al menú con `resultado` vacío no crea esta entidad. La versión autónoma no la crea.
