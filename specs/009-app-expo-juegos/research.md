# Research: Juegos de exposición en tablet

Fecha: 2026-10-25. No quedan ítems `NEEDS CLARIFICATION`.

## Base de la aplicación

- **Decision**: React, TypeScript y Vite, en `AppParaExpo`, con CSS propio y sin enrutador.
- **Rationale**: No existe una web previa de estos juegos. El pedido fija esa base cuando no hay otra que reutilizar. El estado de pantalla en memoria evita rutas que un servidor tendría que resolver y permite abrir el archivo local.
- **Alternatives considered**: Seguir en Kotlin dentro de `ProyectoAndroid` no cumple las dos entregas. Un enrutador con historial del navegador falla al publicar en un subdirectorio o al abrir `file://` si se pide una ruta distinta de la página.

## Dos entregas, un origen

- **Decision**: Vite con `base: './'`. El modo publicado genera `dist/`. El modo autónomo usa `vite-plugin-singlefile`, inline de todos los recursos y escribe `offline/AppParaExpo-Offline.html`.
- **Rationale**: Las rutas relativas funcionan en un dominio y en un subdirectorio. El plugin deja un solo HTML con JavaScript y CSS incluidos, que es la forma pedida para copiarlo a la tablet. Los PNG y los audios se importan desde el código para que el límite de inline los incorpore. No hay CDN, fuentes remotas, Service Worker ni módulos externos en esa entrega.
- **Alternatives considered**: Un `index.html` al lado de una carpeta de imágenes sigue dependiendo de varios archivos. Un Service Worker no funciona de manera confiable en `file://`. Duplicar los juegos en HTML escrito a mano haría divergir las reglas.

## Recursos originales

- **Decision**: `scripts/copiar-recursos.mjs` copia a `AppParaExpo` los tres JSON y los PNG usados. En la copia, la ficha `delfin` apunta a la silueta copiada desde `Silueta_Delfín.png`. El origen no se edita.
- **Rationale**: Los tres JSON están presentes. Hay 54 sombras, 67 adivinanzas y 10 conteos. Todas las imágenes de adivinanza y de contar existen. La única diferencia de nombre es el acento de la silueta del delfín. El total de PNG del catálogo pesa alrededor de 1,7 MB.
- **Alternatives considered**: Inventar una silueta nueva contradice la especificación. Dejar el nombre sin acento haría que esa ficha no se pueda sortear.

## Tablero de Sombras

- **Decision**: Cada partida sortea cinco fichas con primera respuesta distinta. Los nombres se mezclan. La unión se implementa con Pointer Events, de modo que el dedo y el mouse compartan el arrastre y el doble toque. Las líneas van en un SVG sobre el tablero.
- **Rationale**: La victoria y la selección piden cinco parejas. El pedido exige las dos formas de unir y una sola implementación de puntero. Soltar fuera de un nombre cancela la línea sin gastar una vida.
- **Alternatives considered**: Cuatro siluetas no permiten ganar con la condición escrita. Tratar mouse y tacto por separado duplica los errores de la exposición.

## Pistas, vidas y cierre

- **Decision**: Una función compartida resuelve el orden primera, tercera y segunda, y saltea una pista ausente. Sombras avanza ese orden por silueta. Adivina muestra la primera al empezar y agrega la tercera y después la segunda. Tres vidas se rompen de derecha a izquierda. El cierre es un solo componente para los tres juegos.
- **Rationale**: El catálogo de sombras tiene dos pistas por ficha y el de adivinanza tres. La regla compartida evita que un juego descuente vidas de otra manera.
- **Alternatives considered**: Pedir una tercera pista inventada para sombras violaría el catálogo real.

## Voces

- **Decision**: `scripts/generar-voces.py` importa `synthesize_wav` de `ServidorDePC/tts_cartesia.py` y pide cada texto una vez, con la voz `micaela` (`2ab6bb3b-570f-4103-9bb3-5c6f4ab027be`), el modelo y el WAV que ese módulo ya usa. Si el archivo de salida ya existe, no vuelve a llamar. Después, si hay ffmpeg, convierte cada WAV a MP3 mono de voz y conserva el MP3 como recurso de la aplicación.
- **Rationale**: El pedido exige no usar la API durante las partidas. Reutilizar el módulo existente evita otra integración y no modifica el servidor. La clave sale de `API_KEY_CARTESIA` o de los archivos que ese módulo ya sabe leer. Un WAV largo, multiplicado por cientos de pistas, haría el HTML autónomo innecesariamente pesado. A 32 kbps, el habla de los catálogos queda en el orden de unos pocos megabytes.
- **Alternatives considered**: Llamar a Cartesia desde el navegador repetiría la API y exigiría internet. Dejar los WAV sin comprimir cumpliría la autonomía, con un archivo mucho más pesado para copiar a la tablet. Cambiar el módulo del servidor afectaría al peluche.

La bienvenida de la versión publicada intenta reproducirse al abrir la pantalla del nombre. Si el navegador bloquea el audio hasta un gesto, suena en el primer toque de esa pantalla y el texto sigue visible. La versión autónoma no incluye esa frase.

## Planilla

- **Decision**: La versión publicada hace un POST `text/plain` al Web App de Apps Script, con nombre, juego (`sombras`, `adivina` o `contar`), resultado (`gano` o `perdio`), fecha y hora local ISO y la clave. El programa entregado en `planilla-apps-script.txt` agrega una fila. La URL y la clave entran por variables de entorno de Vite y un `.env.example` vacío. El modo autónomo reemplaza el registro por una función que no envía nada y no incorpora esas variables.
- **Rationale**: `text/plain` evita el preflight que suele impedir el POST del navegador hacia Apps Script. El cierre no espera la respuesta. Así una planilla caída no frena al niño. Dejar la clave fuera del HTML autónomo evita repartirla con el archivo de la tablet.
- **Alternatives considered**: Una hoja escrita desde un backend propio contradice la entrega estática. Enviar también desde el archivo local contradice la especificación. Leer la respuesta del POST no es necesario para jugar.

## Comprobación

- **Decision**: Vitest cubre las reglas puras. La compilación publicada y la autónoma son comprobaciones obligatorias. El archivo se abre con la red desactivada. Los cuatro tamaños se revisan en el navegador.
- **Rationale**: Las reglas se pueden afirmar sin tablet. La fidelidad visual y `file://` no quedan demostrados solo con el build.
- **Alternatives considered**: Una suite de navegador automatizada puede sumarse después. No reemplaza abrir el HTML real sin red, que es la prueba que el pedido marca como obligatoria.
