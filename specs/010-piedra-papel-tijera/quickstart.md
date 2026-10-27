# Quickstart: comprobar Piedra, papel o tijera

Fecha: 2026-10-26. Complementa [data-model.md](data-model.md), [contracts/recorrido-ppt.md](contracts/recorrido-ppt.md) y [contracts/reconocimiento.md](contracts/reconocimiento.md).

## Preparar

Desde `AppParaExpo`, con Node 20 o superior:

```text
npm install
npx vitest run src/juegos/ppt
npm run build
npm run preview -- --host 127.0.0.1 --port 4173
```

No usar `npm test`: ese script vuelve a copiar las imágenes del peluche y pisa dibujos más nuevos de la exposición.

La cámara se abre en `http://127.0.0.1:4173/`. Para ver que el modelo no se descarga, se corta la red después de `npm run build` y antes de jugar.

## Reglas sin cámara

`npx vitest run src/juegos/ppt` tiene que pasar y cubrir:

- Las nueve parejas de [data-model.md](data-model.md).
- Un gesto de menos de 800 ms, uno por debajo de 0,7 y un cambio de clase no confirman.
- La misma clase durante 800 ms confirma.
- La cuenta se anula si la clase no sigue.
- Con un generador fijo, la computadora solo se elige después de la figura de la persona y puede devolver las tres figuras.

## Menú y figuras

1. Escribir un nombre y comprobar cuatro tarjetas. Abrir Sombras, volver, abrir Adivina, volver, abrir Contar y volver.
2. Abrir Piedra, papel o tijera. El navegador no pide la cámara todavía.
3. Continuar sin cámara. Tocar tijera. Según la figura de la computadora, el mensaje y el color coinciden con la tabla de reglas.
4. Jugar otra vez y comprobar que el resultado anterior no sigue en pantalla.
5. Volver al menú. Las cuatro tarjetas siguen ahí.

Repetir el paso 3 hasta haber visto, en el conjunto de la prueba automática, las nueve parejas. En la pantalla alcanza con forzar cada botón una vez y leer el mensaje.

## Cámara, en un dispositivo real

Estas comprobaciones no las reemplaza Vitest.

1. Activar cámara y aceptar el permiso. Se ve la cara como en un espejo y no hay audio.
2. Sin la mano en cuadro aparece «Acercá tu mano a la cámara».
3. Un gesto que no sea puño, palma ni tijera muestra el mensaje de gesto desconocido y no cuenta 3.
4. Un puño breve no llega a la cuenta. Un puño firme muestra «¡Te detecté! Mantené el gesto...» y luego 3, 2, 1.
5. Soltar el puño en el 2 cancela la cuenta y la computadora no muestra jugada.
6. Sostener palma y tijera, cada una hasta el final, y comprobar que la imagen de «Vos» es la correcta.
7. Jugar otra vez sin que vuelva a pedir la entrada inicial.
8. Volver al menú y comprobar que la luz de la cámara se apaga.
9. Rechazar el permiso, o tapar la cámara con otra aplicación, y terminar una ronda con los botones.

## Tamaños

Mirar el menú y una ronda en 360×640, 800×1280, 1280×800 y 1366×768. En 1280×800 la cámara ocupa el ancho principal y las tres figuras se leen sin quedar tapadas. En 360×640 la cámara queda arriba y las figuras abajo, todavía reconocibles.

## Sin internet

1. `npm run build:offline`.
2. Abrir `offline/AppParaExpo-Offline.html` con la red desactivada. El nombre, los tres juegos anteriores y una ronda por figuras de Piedra, papel o tijera funcionan.
3. En ese archivo abierto con doble clic, «Activar cámara» puede mostrar el mensaje de que el reconocimiento no está disponible. Eso es el límite conocido de `file://` en Chrome. No tiene que intentar una descarga.
4. Con la red desactivada, `npm run preview` de la carpeta `dist/` sí tiene que abrir la cámara y clasificar, porque el WASM y el modelo salieron en `dist/reconocimiento/`.

## Qué no comprobar aquí

- Que la planilla reciba una fila de este juego: no debe recibirla.
- Frases nuevas de voz: este juego no agrega ninguna.
