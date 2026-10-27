# Contract: Recorrido visible

La aplicación muestra una sola pantalla a la vez. No hay dirección interna que el servidor tenga que resolver.

## Nombre

- Se ve antes del menú.
- Campo de texto, confirmación con un control grande y el logo de TEO.
- Un nombre vacío o de solo espacios no avanza.
- La versión publicada intenta decir «Hola!, soy Teo, cuál es tu nombre?». Si el navegador espera un gesto, esa frase suena en el primer toque y el pedido sigue escrito.
- La versión autónoma muestra el pedido y no exige esa frase.

## Menú

- Tres tarjetas: SOMBRAS, ADIVINA EL OBJETO y CONTAR.
- Cada tarjeta muestra el nombre y una presentación visual propia, armada con dibujos del catálogo.
- Un toque entra al juego y comienza una partida nueva.
- En horizontal las tarjetas aprovechan el ancho. En vertical se reordenan y siguen pudiéndose tocar sin quedar diminutas.

## Sombras

- Horizontal: cinco siluetas a la izquierda y cinco nombres a la derecha.
- Vertical: cinco siluetas arriba y cinco nombres abajo.
- Arrastrar muestra una línea temporal que sigue el puntero. Soltar en el nombre correcto deja una línea permanente. Soltar en otro nombre rompe la línea.
- Tocar una silueta la marca. Tocar después un nombre resuelve esa unión igual que el arrastre.
- Una pareja resuelta no vuelve a elegirse.
- El error muestra «No era esa, te doy una pista» y una pista de esa silueta.
- Hay una acción visible para volver al menú.

## Adivina el objeto

- Las pistas recibidas se leen juntas. La nueva queda debajo, sin borrar las anteriores.
- Cinco botones grandes con nombres. La posición correcta cambia de partida.
- El dibujo del objeto no aparece mientras la partida sigue abierta.
- La opción incorrecta queda visiblemente apagada.
- Hay una acción visible para volver al menú.

## Contar

- Imagen de la ficha, su pregunta y los botones del 1 al 10 en orden.
- Un número incorrecto queda apagado. Los demás siguen activos.
- Hay una acción visible para volver al menú.

## Vidas

- Tres corazones visibles durante la partida.
- El primero que se rompe es el de la derecha. Después el del medio, con el derecho ya vacío. El tercero es el de la izquierda y coincide con el fin de la partida.

## Cierre

- Victoria: fondo y acento verdes, «¡Muy bien! ¡Ganaste!» y confeti que deja leer el mensaje.
- Derrota: fondo y acento rojos, «¡Se terminó el juego! ¡Intentalo otra vez!».
- Acciones: VOLVER A JUGAR y VOLVER AL MENÚ PRINCIPAL.
- El aviso se nota y no ocupa toda la pantalla de forma innecesaria.
- En Adivina, el cierre puede mostrar el dibujo y el nombre del objeto.

## Tacto

- Los controles importantes caben en la pantalla de juego.
- El contacto no desplaza la página.
- No hace falta pasar el cursor por encima para descubrir una acción.
- Un doble contacto inmediato sobre la misma opción cuenta una sola jugada.
- Girar la pantalla conserva la partida en curso.
