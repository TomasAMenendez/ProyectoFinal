# Quickstart: Juegos de exposición en tablet

Guía de comprobación para cuando `AppParaExpo` esté implementado. Los detalles de datos y de pantallas están en [data-model.md](data-model.md) y en [contracts/](contracts/).

## Requisitos

- Node.js 20 o superior.
- Python 3 disponible para la corrida de voces.
- La clave de Cartesia accesible como ya la lee `ServidorDePC/tts_cartesia.py`: variable `API_KEY_CARTESIA`, `ServidorDePC/.env` o `ServidorDePC/cartesia_api_key.txt`.
- ffmpeg disponible en el sistema, para comprimir las voces antes de armar el archivo único.
- Chrome o Edge actuales.
- No hace falta modificar `ProyectoParaRasperrypiV5` ni `ServidorDePC`.

## Preparar

Desde `AppParaExpo`:

```text
npm install
npm run recursos
npm run voces
```

`recursos` copia JSON y PNG a la carpeta de la aplicación. `voces` genera solo los audios que falten y los comprime. Una segunda corrida de `voces` no vuelve a llamar a la API si los archivos siguen presentes.

Copiar `.env.example` a `.env` y completar la URL y la clave de la planilla solo cuando se quiera probar el registro. Sin esos datos, los juegos igual deben abrirse.

## Versión publicada

```text
npm run dev
npm run build
npm run preview
```

Resultado esperado:

- `npm run build` termina sin error y deja `dist/` listo para copiar a un sitio estático.
- Con `preview`, el nombre, el menú y los tres juegos cargan.
- Las imágenes de las partidas se ven, incluida una partida de Sombras que pueda incluir al delfín.
- Al terminar una partida con la planilla configurada, aparece una fila nueva: nombre, juego, resultado y fecha y hora.
- Sin configuración de planilla, el cierre igual aparece.
- Publicar `dist/` dentro de un subdirectorio sigue mostrando imágenes, estilos y juegos.

## Versión autónoma

```text
npm run build:offline
```

Abrir `offline/AppParaExpo-Offline.html` con doble clic, con la red del equipo desactivada.

Resultado esperado:

- Se ve el pedido de nombre y después el menú.
- Los tres juegos abren, muestran sus imágenes y reproducen voces ya incluidas.
- Sombras acepta arrastre y también silueta más nombre.
- Un acierto y un error hacen lo descrito en [recorrido-ui.md](contracts/recorrido-ui.md).
- La tercera vida perdida muestra el cierre rojo. Completar el objetivo muestra el cierre verde con confeti.
- Volver a jugar arma otro desafío y restaura tres vidas.
- Volver al menú no conserva errores ni selecciones.
- El navegador no solicita recursos externos. No se agrega una fila a la planilla.

## Reglas automáticas

```text
npm test
```

Deben quedar cubiertos: cinco parejas únicas y nombres mezclados; orden de pistas 1, 3, 2 con una pista ausente; cuatro distractores distintos; cantidad tomada de la primera respuesta de contar; tres errores pierden; un acierto gana; una opción descartada no vuelve a restar; reiniciar limpia el estado; la entrega autónoma no dispara el registro.

## Tamaños

Revisar en el navegador, en horizontal y en vertical cuando el tamaño lo permita:

| Tamaño | Situación |
|---|---|
| 1280×800 | Tablet horizontal. Prioridad. Menú y los tres juegos sin desplazar la página para alcanzar la jugada. |
| 1366×768 | Computadora. |
| 800×1280 | Tablet vertical. |
| 360×640 | Celular vertical. |

En los cuatro, los botones de juego y las dos acciones del cierre se pueden tocar. El texto de las pistas y de las preguntas se puede leer.

## Hecho cuando

- Los tres juegos se pueden ganar y perder en las dos entregas.
- El archivo único supera la prueba sin red.
- `npm run build` termina bien.
- `ProyectoParaRasperrypiV5` y `ServidorDePC` no tienen cambios.
- El README explica cómo compilar, publicar, usar el HTML, copiarlo a la tablet y qué navegadores se comprobaron.
