# Contract: Voces incluidas

Todas las frases se generan antes de jugar, con la voz Micaela del módulo Cartesia ya usado por `ServidorDePC`. Durante una partida la aplicación solo reproduce archivos propios.

## Textos que salen del catálogo

Un archivo por texto distinto:

- Cada pista de sombras y de adivinanza.
- Cada pregunta de contar.
- Cada `nombreVisible` de sombras.
- Cada `nombreCorrecto` de adivinanza.
- Los números del 1 al 10, dichos como cantidad.

Si dos fichas comparten el mismo texto, comparten el mismo archivo.

## Textos fijos de la interfaz

| Momento | Texto | Dónde suena |
|---|---|---|
| Apertura | Hola!, soy Teo, cuál es tu nombre? | Solo la versión publicada, en la pantalla del nombre |
| Bienvenida | Una frase corta que nombra el juego | Al entrar a esa partida |
| Pista de Sombras | No era esa, te doy una pista | Junto con el aviso, antes de la pista del catálogo |
| Victoria | Variantes de cierre alegre, incluida la idea de «¡Muy bien! ¡Ganaste!» | Al ganar |
| Derrota | Variantes de cierre, incluida la idea de intentar otra vez | Al perder |

## Reacciones con al menos tres variantes

| Momento | Idea de la frase |
|---|---|
| Acierto | Muy bien, era esa |
| Error | Mmm, creo que no era esa |
| Opción ya descartada | Esa no era correcta, probá con otra |
| Volver a jugar | Qué bueno, juguemos de nuevo |
| Volver al menú | Dale, probemos otro juego |

La variante elegida no puede ser la única disponible al repetir el momento cinco veces.

## Orden al elegir una opción

1. Suena el nombre, la palabra o el número elegido.
2. Suena después la reacción de acierto o de error.
3. En un error de Sombras o al revelar una pista nueva de Adivina, suena el texto de esa pista.
4. Al aparecer el cierre, suena la frase de victoria o derrota.

Si un archivo no está o el navegador no lo reproduce, la jugada y el cierre siguen su curso.

## Generación

- Una corrida pide los textos que todavía no tienen archivo.
- No se llama a la voz otra vez si el archivo de ese texto ya existe.
- La clave no se copia dentro de `AppParaExpo` ni dentro del HTML autónomo.
- El recurso que viaja con la aplicación es el audio ya comprimido cuando ffmpeg está disponible. Si no lo está, la corrida se detiene y lo informa, en lugar de publicar cientos de WAV sin avisar.
