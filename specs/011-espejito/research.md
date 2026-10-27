# Research: Espejito en la exposición

Fecha: 2026-10-26. No quedan ítems `NEEDS CLARIFICATION`.

## Dónde vive el juego

- **Decision**: Ampliar `AppParaExpo`. `JuegoId` suma `espejito`. El menú agrega la tarjeta ESPEJITO con `Emocion_Feliz.png`. `Aplicacion` abre `PantallaEspejito`. Al completar las cuatro caras llama una vez a `registrarResultado` con resultado `gano`. Salir antes no llama. Volver a jugar arma otra sesión en la misma pantalla, sin remount, y esa sesión también puede anotar al completarse.
- **Rationale**: El menú, los botones, las voces, la planilla y las dos compilaciones ya existen. Un proyecto nuevo duplicaría la navegación. `anotaEnPlanilla` hoy vive en `juegos/ppt/planilla.ts` y es la compuerta que usa `Aplicacion`; alcanza con incluir `espejito` ahí y en el mapa `JUEGO` de `registro.ts`, con el texto `Espejito`.
- **Alternatives considered**: Anotar cada emoción por separado inflaría la hoja y contradice la especificación. Crear otra carpeta de aplicación rompe la consigna de no reconstruir la web.

## Imágenes

- **Decision**: Usar, sin renombrar ni redibujar, `Emocion_Feliz.png`, `Emocion_Triste.png`, `Emocion_Enojado.png` y `Emocion_Sorprendido.png`, ya presentes en `AppParaExpo/src/recursos/imagenes/`. La tarjeta del menú usa la cara feliz. No ejecutar `scripts/copiar-recursos.mjs`.
- **Rationale**: Esos cuatro nombres están en la carpeta que `urlImagen` ya recorre. El script de copia pisaría dibujos más nuevos. No hace falta traer imágenes de afuera.
- **Alternatives considered**: Emojis o un CDN quedan fuera del pedido. Guardar copias dentro de `src/juegos/espejito` obligaría a otro cargador.

## Detector

- **Decision**: `@mediapipe/tasks-vision` 1.1.1, clase `FaceLandmarker`, ya declarada en el paquete instalado. Opciones: `runningMode: 'VIDEO'`, `numFaces: 1`, `outputFaceBlendshapes: true`. Cada mirada llama a `detectForVideo(video, timestamp)` sobre el mismo elemento de video. Las categorías salen de `result.faceBlendshapes[0].categories`, con `categoryName` y `score`. Una sola instancia por sesión. Primero `delegate: 'GPU'` y, si esa creación falla, `delegate: 'CPU'`. El módulo se importa solo después de Comenzar a jugar.
- **Rationale**: Es el detector que usa `TEO-Juegos.html` y el que el pedido pide conservar. La versión 1.1.1 ya está en la exposición por el reconocedor de gestos, y su tipo expone exactamente `detectForVideo` y `outputFaceBlendshapes`. Crear un detector por fotograma recargaría el modelo.
- **Alternatives considered**: Comparar píxeles, medir formas a mano o llamar a un servicio externo queda fuera del pedido. Reutilizar `GestureRecognizer` no lee expresiones de la cara.

## Puntuación, tomada de la referencia

- **Decision**: Función pura. Para cada emoción, `incremento = max(0, valorActual - valorNeutral)` y la puntuación es la suma de incremento por peso, dividida por la suma de pesos. Los pesos y umbrales, copiados del HTML, son:
  - feliz: `mouthSmileLeft` 0,5, `mouthSmileRight` 0,5, umbral 0,18
  - triste: `mouthFrownLeft` 0,4, `mouthFrownRight` 0,4, `browInnerUp` 0,2, umbral 0,16
  - sorprendido: `jawOpen` 0,5, `eyeWideLeft` 0,25, `eyeWideRight` 0,25, umbral 0,10
  - enojado: `browDownLeft` 0,35, `browDownRight` 0,35, `noseSneerLeft` 0,15, `noseSneerRight` 0,15, umbral 0,16
  La emoción pedida se cumple solo si su puntuación alcanza su umbral y además al menos el 85 % de la puntuación más alta de las cuatro (`L8 = 0.85` en el HTML). Un nombre ausente vale 0. La referencia nula vale 0, pero Espejito no empieza un desafío con referencia nula.
- **Rationale**: Es la función `ui` y la condición `x8` del HTML. Los pesos de cada emoción suman 1, así que el resultado es el promedio ponderado. El piso 0,14 del HTML no se usa, porque las cuatro emociones tienen umbral propio y una puntuación menor al umbral no puede ser acierto.
- **Alternatives considered**: Elegir la emoción más alta sin umbral marcaría una mueca débil. Copiar también el salto a los 15 segundos y las estrellas mete el ranking que el pedido deja afuera.

## Calibración

- **Decision**: Al tener video y detector, se observa la cara. La primera cara fija el inicio. La calibración termina cuando hay al menos 5 observaciones y pasó 1,5 segundos desde esa primera cara; la referencia es el promedio de esas observaciones. Si a los 8 segundos desde la primera cara hay observaciones pero no se llegó a 5 y 1,5 segundos, se usa el promedio de lo ya visto y empieza el desafío. Si pasan 8 segundos desde que se abrió la calibración y no hubo ninguna cara, no empieza el desafío: sigue el mensaje de acercarse. Cuando después aparece una cara, vuelven a valer las 5 observaciones y el segundo y medio.
- **Rationale**: El HTML hace eso en `D8`, con `k8 = 5`, `S8 = 1500` y `Y8 = 8000`. La diferencia consciente es la muestra vacía: el HTML arrancaría el desafío con referencia 0, y la especificación lo prohíbe si no se vio el rostro. No se guardan las observaciones después de calcular el promedio, y nunca se guardan como imagen.
- **Alternatives considered**: Un perfil fijo para todos los niños ignora la cara tranquila de cada uno. Esperar sin límite sin ningún mensaje deja al niño sin saber qué hacer.

## Estabilidad

- **Decision**: Función pura, la `hd` del HTML. El avance empieza en 0. Cada paso suma como máximo 250 ms. Si la condición se cumple, el avance sube `tiempo / 1000`. Si no se cumple o no hay cara, baja `tiempo / 2000`, sin pasar de 0. Llega a 1 al completar un segundo efectivo. Un solo paso, aunque el reloj haya saltado, no acredita más de 250 ms y por eso no cierra el desafío. Al llegar a 1, esa ronda deja de mirar hasta el siguiente desafío.
- **Rationale**: `g8 = 1000`, `T8 = 2000` y `z8 = 250` evitan que un fotograma gane o borre el intento. El bucle de 10 Hz informa la ausencia de cara en cada paso, así que no hace falta el temporizador extra de 400 ms del HTML.
- **Alternatives considered**: Confirmar en el primer fotograma que supere el umbral produce falsos aciertos. Reiniciar a cero ante un fotograma perdido castiga un parpadeo del detector.

## Hilo principal, sin Worker

- **Decision**: El análisis corre en el hilo de la página, con un reloj de cerca de 100 ms. Si una inferencia sigue en curso, el tick se salta. No se usa Web Worker. El video no se vuelve a crear en cada resultado.
- **Rationale**: `detectForVideo` lee el elemento de video de la página. Pasarlo a un Worker exige copiar el cuadro y otra tubería. Diez miradas por segundo alcanzan para juntar un segundo con varias observaciones y dejan la pantalla capaz de recibir toques. Es el mismo ritmo que ya usa la cámara de Piedra, papel o tijera.
- **Alternatives considered**: Analizar cada fotograma de la cámara carga de más una tablet. Un Worker no evita que el modo video necesite el elemento visible.

## Modelo y WASM locales

- **Decision**: Conservar el WASM de `AppParaExpo/reconocimiento/wasm`, que ya corresponde al paquete 1.1.1. Extraer una sola vez el modelo embebido en `TEO-Juegos.html` (el campo comprimido `ol.model`) y guardarlo como `AppParaExpo/reconocimiento/face_landmarker.task`. En la partida, `FilesetResolver.forVisionTasks` apunta a `reconocimiento/wasm/` y el modelo a `reconocimiento/face_landmarker.task`, con direcciones relativas a la página. El plugin `reconocimiento-local` de `vite.config.ts` ya copia esa carpeta a `dist` y a `offline`. Durante el juego no hay solicitudes a `storage.googleapis.com`, a un CDN ni a otro servicio. No se reemplaza el WASM por el que trae el HTML.
- **Rationale**: El HTML arma el detector con un buffer local cuando trae los tres recursos, y si no, cae a una ruta `models/face_landmarker.task`. La exposición ya resolvió el WASM por archivos vecinos para el gesto. Mezclar el WASM del HTML con el JavaScript 1.1.1 puede desalinear el cargador. El `.task` sí es el modelo de la referencia y es el archivo que falta en el repositorio.
- **Alternatives considered**: Dejar la URL de Google cumple el primer arranque con internet y falla la consigna offline. Incrustar el HTML completo duplica un WASM que la app ya tiene y mete el ranking. Descargar el modelo al tocar Comenzar a jugar depende de la red.

## HTML autónomo y la cámara

- **Decision**: `npm run build:offline` sigue produciendo `offline/AppParaExpo-Offline.html` y, al lado, `offline/reconocimiento/` con el modelo de la cara y el WASM. Abrir ese HTML con doble clic no se presenta como prueba de cámara. Chrome bloquea `fetch` de archivos vecinos desde `file://`, y MediaPipe necesita ese `fetch` para el WASM. `getUserMedia` en `file://` tampoco es una prueba confiable. La prueba de cámara y de modelo local es `npm run build` y `npm run preview`, con la red cortada después de compilar. Si hace falta la tablet sin un servidor público, se sirve la carpeta `dist/` en la red local: los archivos ya viajan con la app.
- **Rationale**: La especificación pide conservar la entrega de un solo archivo e informar el límite, sin un CDN y sin borrar el modo offline. Es el mismo límite ya aceptado para la cámara de Piedra, papel o tijera.
- **Alternatives considered**: Convertir el modelo y el WASM en URLs de objeto dentro del HTML único agranda un archivo que ya pesa varios megabytes y no arregla el permiso de cámara en `file://`. Un flag de Chrome no es una entrega para la exposición.

## Espejo y cámara

- **Decision**: `getUserMedia` con `audio: false`, `facingMode: { ideal: 'user' }` y tamaño ideal 640×480, sin mínimo. El video usa `autoplay`, `muted` y `playsInline`, `object-fit: cover` y un reflejo horizontal solo por CSS. A `detectForVideo` se le pasa el video sin invertir. Al desmontar, al volver al menú o al fallar de forma definitiva se hace `stop()` en cada pista, se corta el bucle, se llama a `close()` del detector y se cortan las voces pendientes.
- **Rationale**: El tipo de `FaceLandmarker` avisa que una imagen reflejada invierte izquierda y derecha de los blendshapes. El espejo es para el niño, no para la puntuación. La resolución ideal, y no mínima, evita dejar afuera a una cámara chica. Es el mismo pedido de cámara que ya funciona en el otro juego.
- **Alternatives considered**: Dibujar el video en un canvas invertido y detectar sobre ese canvas cambia las puntuaciones. Pedir 1280×720 como mínimo deja sin juego a cámaras modestas.

## Errores

- **Decision**: Mensajes propios de Espejito, con Reintentar y Salir. No se reutilizan los textos de Piedra, papel o tijera, porque esos ofrecen jugar con botones. Los motivos son permiso rechazado, cámara ausente, cámara ocupada, modelo que no carga y navegador sin esta capacidad. Un error al mirar un instante no cierra el juego. Si el detector queda inutilizable, se muestra el fallo y se ofrece reintentar o salir. La clasificación de `DOMException` sigue la misma tabla que `motivoDe` del otro juego: `NotAllowedError` y `SecurityError` son permiso, `NotFoundError` y `OverconstrainedError` son ausencia, `NotReadableError` y `AbortError` son cámara ocupada.
- **Rationale**: Este juego no tiene una partida válida sin cámara. Reutilizar el texto de los botones prometería un camino que la especificación no tiene.
- **Alternatives considered**: Extraer `motivoDe` a un módulo común toca el juego de gestos sin cambiar su comportamiento. Copiar la tabla corta deja ese juego quieto.

## Voces

- **Decision**: Agregar las frases a `src/audio/textos-fijos.json`. `bienvenidas.espejito` es «Vamos a jugar al espejito. Imitá la carita.». Un objeto `espejito` lleva la calibración «Mirá a la cámara, tranquilo, sin hacer ninguna cara.», las cuatro instrucciones, los dos mensajes de ayuda, las cuatro felicitaciones y el cierre «¡Muy bien! Imitaste las cuatro caritas.». `npm run voces` genera solo los mp3 que falten, con Micaela, y actualiza `indice.json`. La reproducción sigue en `reproductor.ts`: una voz a la vez. La felicitación es el aviso de éxito. Repetir vuelve a decir la instrucción de la emoción actual. Menú y volver a jugar usan `fraseMenu` y `fraseRejuego`, que ya existen. La ayuda se dice cuando el estado entra en ese mensaje, no en cada mirada.
- **Rationale**: `generar-voces.py` ya recorre todo `textos-fijos.json` y no hace falta otro generador. Un segundo sonido en el mismo turno corta la frase anterior, porque `reproductor.ts` incrementa el turno y pausa el audio vigente. La especificación ya tomó la felicitación hablada como sonido de éxito.
- **Alternatives considered**: Un efecto corto además de la frase se pisaría con Micaela o exigiría un reproductor paralelo. Generar las voces en el navegador depende de la red durante la exposición.

## Menú de cinco tarjetas

- **Decision**: La rejilla horizontal pasa de cuatro a cinco columnas desde un ancho cómodo de tablet. Por debajo de 800 px, y en vertical, se conserva la columna única que ya desplaza la página. La tarjeta nueva usa un pastel distinto de los cuatro actuales, del mismo tipo de borde, sombra y toque. El color propuesto es `#9fd0ff`.
- **Rationale**: Cinco tarjetas de `minmax(0, 1fr)` en 1280 px siguen siendo tocables. La columna única ya existe para el teléfono y no hay que inventar otro componente.
- **Alternatives considered**: Dejar `repeat(4)` empuja la quinta tarjeta a una fila distinta sin decidirlo. Achicar solo Espejito la haría ver como otro tipo de botón.

## Qué no se copia del HTML

- **Decision**: Quedan fuera el ranking, los niveles, las estrellas, el salto a los 15 segundos contado como fallo, la pista a los 10 segundos y los textos largos de felicitación del HTML. Los textos visibles y hablados son los de la especificación.
- **Rationale**: El pedido pide la tecnología y la lógica de detección, y rechaza el sistema de ranking. Los textos del HTML juzgan más de la cuenta o no coinciden con las frases elegidas.
- **Alternatives considered**: Traer el HTML como un iframe metería el ranking dentro de la exposición y sacaría el juego del menú, de la planilla y de las voces.
