# Data model: Piedra, papel o tijera

Fecha: 2026-10-26. El estado vive en memoria mientras la pantalla del juego está abierta. No se escribe almacenamiento ni planilla.

## Figura

| Campo | Valores |
|---|---|
| id | `piedra`, `papel`, `tijera` |
| nombre | Piedra, Papel, Tijera |
| imagen | `Imagen_Piedra.png`, `Imagen_Papel.png`, `Imagen_Tijera.png` |
| clase del modelo | `Closed_Fist`, `Open_Palm`, `Victory` |

Una figura puede ser la jugada de la persona, la de la computadora o una de las tres referencias. Las referencias no se ocultan.

## Regla

Para cada par (persona, computadora) el resultado es `gano`, `empato` o `perdio`.

| Persona | Computadora | Resultado |
|---|---|---|
| piedra | tijera | gano |
| piedra | papel | perdio |
| piedra | piedra | empato |
| papel | piedra | gano |
| papel | tijera | perdio |
| papel | papel | empato |
| tijera | papel | gano |
| tijera | piedra | perdio |
| tijera | tijera | empato |

`gano` se muestra como «¡GANASTE! ¡Muy bien!» en verde. `empato` como «¡EMPATE! ¡Los dos eligieron lo mismo!» en amarillo. `perdio` como «¡GANÉ! ¡Probá otra vez!» en rojo.

## Ronda

| Campo | Significado |
|---|---|
| figuraPersona | Vacía hasta que el gesto se confirma o se toca una figura |
| figuraComputadora | Vacía hasta que `figuraPersona` quedó fija |
| resultado | Vacío hasta que existen las dos figuras |
| origen | `gesto` o `boton`, solo para saber qué cancelar |

Al jugar otra vez o al salir, los tres campos vuelven a vacío. La computadora no se sortea con la persona todavía sin elegir.

## Observación

| Campo | Significado |
|---|---|
| clase | Una de las tres clases, u otra, o ninguna mano |
| confianza | Número de 0 a 1 |
| momento | Marca de tiempo de esa observación |

Una observación suma al tramo solo si la clase es una de las tres y la confianza es al menos 0,7. El tramo guarda la clase y el instante en que empezó. Se descarta si la clase cambia, no hay mano, la confianza baja o la pantalla se cierra. A los 800 ms de la misma clase, la ronda pasa a la cuenta.

## Cuenta

Muestra 3, después 2, después 1. Cada número permanece cerca de un segundo. La clase vigilada es la que abrió la cuenta. Si una observación posterior no es esa clase con confianza suficiente, la cuenta vuelve a vacío y no nace `figuraPersona`. Al terminar el 1, `figuraPersona` queda fija y el bucle deja de proponer otra figura hasta la próxima ronda.

## Sesión de cámara

| Estado | Qué ve la persona |
|---|---|
| entrada | «Activar cámara» y «Continuar sin cámara». Las tres figuras ya están |
| preparando | El permiso y la carga del modelo. Todavía no hay jugada |
| activa | El espejo, la guía de la mano y el mensaje de espera, de gesto desconocido o de detección |
| enCuenta | Los números sobre la cámara. Las figuras siguen visibles |
| resultado | Las dos jugadas, el mensaje y los dos botones de salida |
| impedida | El motivo en una frase, reintentar y seguir con las figuras |

«Continuar sin cámara» salta de `entrada` a la espera de un toque, sin pasar por `preparando`. Un toque en `activa` o `enCuenta` fija la figura del botón y va a `resultado`.

Volver al menú desde cualquier estado apaga las pistas, cierra el reconocedor y olvida la ronda. El menú no conserva esta sesión.

## Relación con la visita

La visita ya tiene nombre y pantalla. `ppt` es una pantalla más. No produce `ResultadoVisita` ni fila de planilla. Salir usa el mismo volver al menú de los otros juegos, y además la limpieza de la sesión de cámara.
