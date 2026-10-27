# Quickstart: comprobar Espejito

Fecha: 2026-10-26. Complementa [data-model.md](data-model.md), [contracts/recorrido-espejito.md](contracts/recorrido-espejito.md) y [contracts/reconocimiento.md](contracts/reconocimiento.md).

## Preparar

Desde `AppParaExpo`, con Node 20 o superior:

```text
npm install
npm run voces
npx vitest run src/juegos/espejito src/servicios/registro.test.ts src/juegos/ppt/planilla.test.ts
npm run build
npm run preview -- --host 127.0.0.1 --port 4173
```

No usar `npm test`: ese script vuelve a copiar las imágenes del peluche y pisa dibujos más nuevos de la exposición.

`npm run voces` solo crea los mp3 que todavía no están. La cámara se abre en `http://127.0.0.1:4173/`. Para ver que el modelo no se descarga, se corta la red después de `npm run build` y antes de jugar.

## Lógica sin cámara

`npx vitest run src/juegos/espejito` tiene que pasar y cubrir:

- Las cuatro puntuaciones de [data-model.md](data-model.md), incluidos el caso de 0,17 y el caso en que feliz supera su umbral pero no llega al 85 % de la mayor.
- Un paso de 250 ms no completa el avance. Un segundo efectivo sí. Soltar la expresión lo hace bajar y no lo borra de un golpe.
- Cinco observaciones a lo largo de un segundo y medio calibran. Ocho segundos sin cara no abren el desafío. Ocho segundos con alguna cara usan el promedio.
- Un generador fijo produce las cuatro emociones una sola vez. Completar la sesión pide `gano` una vez. Salir antes no lo pide.

La prueba de `registro.test.ts` tiene que esperar el cuerpo `Juego: Espejito` y `Resultado: Ganó`.

## Menú

1. Escribir un nombre y comprobar cinco tarjetas. ESPEJITO muestra la cara feliz.
2. Abrir Sombras, Adivina el objeto, Contar y Piedra, papel o tijera, volviendo al menú entre cada uno. Esos juegos siguen cerrando como antes.
3. Abrir Espejito. El navegador no pide la cámara. Se oye la bienvenida.
4. Volver al menú. La cámara sigue apagada.

Repetir la mirada del menú en 360×640, 800×1280, 1280×800 y 1366×768. En horizontal las cinco tarjetas se alcanzan. En vertical se pueden desplazar y el toque no depende de pasar el cursor.

## Cámara, en un dispositivo real

Esta parte queda pendiente si no hay cámara. Con una cámara frontal:

1. Tocar Comenzar a jugar. El permiso aparece ahí.
2. Negar el permiso una vez. Se ve el mensaje, Reintentar y Salir. Salir muestra el menú y no deja la cámara encendida.
3. Aceptar. La imagen es la cámara frontal, en espejo, sin deformarse. Durante la calibración se pide una cara tranquila.
4. Tapar la lente. Aparece «No te veo todavía. Acercate un poquito a la cámara» y no hay acierto.
5. Destapar y sostener cada expresión cerca de un segundo. El orden puede cambiar, y ninguna emoción se repite. Una mueca breve no alcanza.
6. En cada acierto se oye una sola felicitación y el avance de esa ronda se detiene.
7. Repetir la instrucción en un desafío y comprobar que vuelve la frase de esa cara.
8. Al cuarto acierto, el resumen ofrece Volver a jugar y Volver al menú principal. La hoja alojada recibe una sola fila Espejito / Ganó.
9. Volver a jugar completa otra sesión sin pasar por la entrada. Volver al menú apaga la imagen antes de que las tarjetas queden listas.
10. En el registro de red de esa partida no hay pedidos a un CDN ni a `storage.googleapis.com`. El modelo sale de `reconocimiento/face_landmarker.task`.

Probar también con menos luz y con las cuatro expresiones. Si una expresión real no se reconoce o se reconoce de más, se anota el caso antes de mover un umbral. Los textos, las cuatro emociones y la regla del segundo y del 85 % no se cambian para acomodar un solo ensayo.

## Offline

1. `npm run build:offline` deja `offline/AppParaExpo-Offline.html` y `offline/reconocimiento/face_landmarker.task`.
2. Abrir el HTML con doble clic no se usa como prueba de cámara. Si el navegador no muestra video ni carga el modelo, esa limitación se informa y el HTML se conserva para los juegos que ya funcionaban ahí.
3. Servir la carpeta `dist/` en la máquina, con la red cortada, y repetir una sesión. El juego no intenta descargar el reconocimiento.

## Qué mirar al terminar

- Espejito está en el menú y los otros cuatro juegos siguen abriendo.
- Hay una imagen distinta para cada emoción y permanece junto a la cámara.
- El acierto exige umbral, 85 % y un segundo.
- La felicitación y la fila no se repiten por fotograma.
- Al salir no queda una pista de cámara ni una voz de este juego.
- La cara real, si no hubo dispositivo, sigue anotada como pendiente.
