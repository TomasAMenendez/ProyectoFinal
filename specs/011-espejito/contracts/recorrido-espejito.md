# Contract: Recorrido de Espejito

Se suma a la navegación en memoria de la exposición. No hay una dirección nueva que el servidor tenga que resolver.

## Menú

- Después del nombre hay cinco tarjetas del mismo tipo.
- La nueva se llama ESPEJITO y muestra `Emocion_Feliz.png`.
- En tablet horizontal las cinco se alcanzan. En vertical se apilan, con cada tarjeta todavía grande.
- Un toque abre el juego y no dispara el permiso de cámara.
- Sombras, Adivina el objeto, Contar y Piedra, papel o tijera no cambian de juego ni de color.

## Entrada

- Se ve el título ESPEJITO.
- Se oye «Vamos a jugar al espejito. Imitá la carita.».
- El botón principal es Comenzar a jugar.
- Hay una salida al menú, del mismo tipo que en los otros juegos.
- La cámara está apagada.

## Preparación

- El permiso del sistema aparece solo después de Comenzar a jugar.
- Se enciende la cámara frontal, sin audio, y se prepara el detector con los archivos locales.
- Si el permiso ya estaba aceptado, no se vuelve a mostrar la entrada.

## Calibración

- Se ve la cámara en espejo, recortada con `cover`, sin estirarse.
- Se oye y se lee «Mirá a la cámara, tranquilo, sin hacer ninguna cara.».
- Sin cara: «No te veo todavía. Acercate un poquito a la cámara». A los ocho segundos sin ninguna cara, el primer desafío no empieza.
- Con la cara visible el tiempo pedido, empieza el primer desafío y esa cara tranquila queda como referencia de la sesión.

## Desafío

- Se ven, durante todo el intento, el título, la instrucción de esa emoción, su imagen grande, su nombre, la cámara, el mensaje de estado y el avance.
- El orden es un sorteo de las cuatro emociones, una sola vez cada una.
- Sin cara: «No te veo todavía. Acercate un poquito a la cámara».
- Con cara y sin la expresión pedida: «¡Probá imitar la carita!».
- Mientras la expresión se sostiene, el avance crece y ese mensaje de intento no se presenta como si todavía no hubiera expresión.
- Esos dos avisos se dicen al entrar en ese estado, no en cada mirada.
- Repetir vuelve a decir la instrucción de la emoción actual.
- Un instante suelto no completa el avance.
- Si la cara se va o la expresión se suelta, el avance baja y el desafío sigue siendo el mismo.

## Logro

- Al completarse el segundo se cuenta un solo acierto y esa ronda deja de analizar.
- Se ve la felicitación de esa emoción, en el verde de acierto que la exposición ya usa, sin corazones, estrellas ni puntaje.
- Se oye esa felicitación una sola vez.
- Siguiente pasa a la otra emoción. Después de la cuarta, Siguiente abre el resumen.

## Resumen

- Se oye «¡Muy bien! Imitaste las cuatro caritas.».
- Se ofrece Volver a jugar y Volver al menú principal.
- La planilla alojada recibe una sola fila: nombre de la visita, Espejito, Ganó.
- Volver a jugar arma otro orden, vuelve a calibrar y deja la cámara disponible.
- Volver al menú principal apaga la cámara antes de mostrar las cinco tarjetas.

## Impedimento

- Permiso rechazado, cámara ausente, cámara ocupada, modelo que no se pudo preparar y navegador sin esta capacidad muestran una frase clara.
- Hay Reintentar cuando el intento puede repetirse, y Salir al menú en todos los casos.
- No hay un modo de juego sin cámara.

## Salida

- Menú, Salir o Volver al menú principal, desde cualquier fase, apagan la cámara, detienen el análisis y cortan las voces de este juego.
- El menú no conserva la sesión ni una fila a medias.
