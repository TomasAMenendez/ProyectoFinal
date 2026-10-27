# Contract: Recorrido de Piedra, papel o tijera

Se suma a la navegación en memoria de la exposición. No hay una dirección nueva que el servidor tenga que resolver.

## Menú

- Después del nombre hay cuatro tarjetas del mismo tipo.
- La cuarta se llama Piedra, papel o tijera y muestra `Imagen_Tijera.png`.
- En un ancho de tablet horizontal las cuatro se alcanzan. En vertical pueden apilarse o partirse en dos filas, con cada tarjeta todavía grande.
- Un toque abre el juego y no dispara el permiso de cámara.
- Las tarjetas de Sombras, Adivina el objeto y Contar no cambian de juego ni de color.

## Entrada

- Se leen «¡Prepará tu mano!» y «Mostrá uno de estos tres gestos para jugar.»
- Se ven siempre las tres imágenes originales, con el nombre debajo de cada una. Cada imagen es un botón.
- Hay «Activar cámara» y «Continuar sin cámara».
- Hay una acción para volver al menú, la misma idea que en los otros juegos.

## Sin cámara

- No aparece el pedido del sistema.
- Tocar piedra, papel o tijera termina la ronda.
- El resultado muestra «Vos» y «Computadora», las dos imágenes, los dos nombres y el mensaje de victoria, empate o derrota.
- «Jugar otra vez» limpia ese resultado y espera otro toque.
- «Volver al menú principal» vuelve a las cuatro tarjetas.

## Con cámara

- El permiso del sistema aparece solo después de «Activar cámara».
- La imagen es la cámara frontal, sin audio, reflejada en horizontal y recortada con `cover`, sin estirarse.
- Una marca suave indica dónde poner la mano y no tapa las tres figuras.
- Sin mano: «Acercá tu mano a la cámara».
- Con mano y sin uno de los tres gestos: «No reconozco ese gesto. Probá con piedra, papel o tijera».
- Con el gesto sostenido: «¡Te detecté! Mantené el gesto...» y después 3, 2 y 1.
- Si el gesto se suelta durante la cuenta, la cuenta desaparece y vuelve el pedido.
- Al cerrar la cuenta se ve el mismo resultado que con los botones.
- Tocar una figura mientras la cámara está activa juega esa figura y corta la espera o la cuenta.
- «Jugar otra vez» deja la cámara encendida y vuelve a esperar un gesto, sin mostrar otra vez la entrada.
- «Volver al menú principal» apaga la cámara antes de mostrar el menú.

## Cuando la cámara no puede

- Permiso rechazado, cámara ausente, cámara ocupada, modelo que no carga o navegador sin esta capacidad: una frase clara, un reintento cuando corresponda y las tres figuras siguen jugando.
- Esas frases no usan códigos ni nombres de archivos.
